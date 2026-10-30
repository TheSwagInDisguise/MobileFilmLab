package com.mobilefilmlab.camera.ui.viewmodel

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.*
import android.util.Size
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mobilefilmlab.camera.BuildConfig
import com.mobilefilmlab.camera.camera.*
import com.mobilefilmlab.camera.data.importer.*
import com.mobilefilmlab.camera.data.model.*
import com.mobilefilmlab.camera.data.repository.RecipeRepository
import com.mobilefilmlab.camera.gallery.PhotoRepository
import com.mobilefilmlab.camera.pipeline.*
import com.mobilefilmlab.camera.settings.*
import com.mobilefilmlab.camera.updates.ReleaseChecker
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.ByteArrayOutputStream
import kotlin.math.abs
import kotlin.math.roundToInt

enum class CameraMode { AUTO, PRO }

class CameraViewModel(application: Application) : AndroidViewModel(application) {
    val repository=RecipeRepository(application)
    val cameraEngine=CameraEngine(application)
    val photoRepository=PhotoRepository(application)
    private val store=SettingsStore(application)
    private var foreground=false
    val settings=MutableStateFlow(store.load(LensType.WIDE))
    val mode=MutableStateFlow(CameraMode.AUTO)
    val flashMode=MutableStateFlow(FlashMode.OFF)
    val activeLens=MutableStateFlow(LensType.WIDE)
    val cropFactor=MutableStateFlow(1f)
    val framingConfig=MutableStateFlow(FramingConfig.create(LensType.WIDE,false,null))
    val activeRecipe=repository.activeRecipe
    val allRecipes=repository.recipes
    val puckTone=MutableStateFlow(0f)
    val puckColor=MutableStateFlow(0f)
    val evCompensation=MutableStateFlow(0f)
    val iso=MutableStateFlow<Int?>(null)
    val exposureTimeNs=MutableStateFlow<Long?>(null)
    val whiteBalanceKelvin=MutableStateFlow(0)
    val focusMode=MutableStateFlow("AF-C")
    val isRecipeSheetOpen=MutableStateFlow(false)
    val isSettingsOpen=MutableStateFlow(false)
    val isFormatPickerOpen=MutableStateFlow(false)
    val pendingCamera=MutableStateFlow<CameraSpec?>(null)
    val isDiagnosticsOpen=MutableStateFlow(false)
    val isAboutOpen=MutableStateFlow(false)
    val isGalleryOpen=MutableStateFlow(false)
    val isCapturing=MutableStateFlow(false)
    val isShutterFlashing=MutableStateFlow(false)
    val lastCapturedUri=MutableStateFlow<Uri?>(null)
    val lastCapturedThumbnail=MutableStateFlow<Bitmap?>(null)
    val photos=MutableStateFlow<List<PhotoRepository.Photo>>(emptyList())
    val processingProgress=MutableStateFlow("")
    val userNotice=MutableStateFlow<String?>(null)
    val thermalStatus=MutableStateFlow("Normal")
    val updateStatus=MutableStateFlow("Updates are checked only when you tap Check")
    val availableUpdate=MutableStateFlow<ReleaseChecker.Release?>(null)
    private val power=application.getSystemService(Context.POWER_SERVICE) as PowerManager
    private var thermalRegistered=false
    private val thermalListener=if(Build.VERSION.SDK_INT>=29) PowerManager.OnThermalStatusChangedListener { status ->
        cameraEngine.setThermalLimited(status>=PowerManager.THERMAL_STATUS_SEVERE)
        thermalStatus.value=when { status>=PowerManager.THERMAL_STATUS_SEVERE -> "Hot · heavy processing paused"; status>=PowerManager.THERMAL_STATUS_MODERATE -> "Warm"; else -> "Normal" }
    } else null

    init {
        store.selectedCameraId()?.let { saved ->
            if(cameraEngine.cameraSpecs.value.any { it.cameraId==saved && it.isBackFacing }) cameraEngine.selectCamera(saved)
            else userNotice.value="The previous camera ID is not exposed after this firmware change; review the detected camera list."
        }
        val initial=cameraEngine.activeCameraSpec.value
        val loaded=store.load(initial?.lensType ?: LensType.WIDE,initial?.cameraId)
        settings.value=CaptureFormatPolicy.normalize(loaded)
        if(initial!=null && !CaptureFormatPolicy.supported(settings.value.format,initial)) {
            settings.value=CaptureFormatPolicy.normalize(loaded.copy(format=if(initial.hasJpegSupport) CaptureFormat.JPEG else CaptureFormat.RAW))
            userNotice.value="Saved file format is not available on this camera; using ${settings.value.format.label}."
        }
        cameraEngine.configure(settings.value)
        viewModelScope.launch { cameraEngine.cameraError.filterNotNull().collect { userNotice.value=it } }
        viewModelScope.launch { cameraEngine.activeCameraSpec.collect { spec ->
            if(spec!=null) {
                activeLens.value=spec.lensType
                reconcileProControls(spec)
                settings.value=settings.value.copy(calibration=store.load(spec.lensType,spec.cameraId).calibration)
                cameraEngine.configure(settings.value); updateFraming()
            }
        } }
        viewModelScope.launch { cameraEngine.captureSize.collect { updateFraming() } }
        viewModelScope.launch { cameraEngine.rawCaptureSize.collect { updateFraming() } }
        val listener=thermalListener
        if(Build.VERSION.SDK_INT>=29 && listener!=null) {
            runCatching {
                listener.onThermalStatusChanged(power.currentThermalStatus)
                power.addThermalStatusListener(ContextCompat.getMainExecutor(application),listener)
                thermalRegistered=true
            }.onFailure { thermalStatus.value="Thermal service unavailable" }
        }
        refreshPhotos()
    }
    fun onPermissionGranted() { cameraEngine.refreshCameraList(); refreshPhotos(); if(foreground && !isGalleryOpen.value) cameraEngine.openCamera() }
    fun onResume() { foreground=true; if(!isGalleryOpen.value) cameraEngine.openCamera() }
    fun onPause() { foreground=false; cameraEngine.closeCamera() }
    fun hardwareShutterAllowed(): Boolean = foreground && !isGalleryOpen.value && !isRecipeSheetOpen.value && !isSettingsOpen.value && !isFormatPickerOpen.value && pendingCamera.value==null && !isAboutOpen.value && !isDiagnosticsOpen.value
    private fun haptic(strong: Boolean=false) {
        if(!settings.value.haptics) return
        runCatching {
            val app=getApplication<Application>()
            val vibrator=if(Build.VERSION.SDK_INT>=31) (app.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator else app.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            val effect=if(Build.VERSION.SDK_INT>=29) VibrationEffect.createPredefined(if(strong) VibrationEffect.EFFECT_CLICK else VibrationEffect.EFFECT_TICK) else VibrationEffect.createOneShot(if(strong) 14 else 7,VibrationEffect.DEFAULT_AMPLITUDE)
            vibrator.vibrate(effect)
        }
    }
    private fun editable(): Boolean = !isCapturing.value
    fun setMode(new: CameraMode) {
        if(!editable()) return
        if(new==CameraMode.PRO && settings.value.pipeline in listOf(CapturePipeline.OEM_HDR,CapturePipeline.OEM_NIGHT)) {
            updateSettings(settings.value.copy(pipeline=CapturePipeline.STANDARD)); userNotice.value="Pro controls use the standard Camera2 session"
        }
        mode.value=new
        if(new==CameraMode.AUTO) {
            iso.value=null; exposureTimeNs.value=null; whiteBalanceKelvin.value=0; focusMode.value=cameraEngine.activeCameraSpec.value?.focusModes?.firstOrNull() ?: "N/A"; evCompensation.value=0f
            cameraEngine.resetAuto()
        }
        haptic()
    }
    fun toggleFlash() {
        if(!editable()) return
        if(cameraEngine.activeCameraSpec.value?.hasFlash!=true) { userNotice.value="Flash is not available on this camera"; return }
        if(settings.value.pipeline in listOf(CapturePipeline.OEM_HDR,CapturePipeline.OEM_NIGHT)) { userNotice.value="OEM HDR/Night controls its own flash; choose Camera2 natural for manual flash"; return }
        val next=when(flashMode.value) { FlashMode.OFF->FlashMode.AUTO; FlashMode.AUTO->FlashMode.ON; FlashMode.ON->FlashMode.TORCH; FlashMode.TORCH->FlashMode.OFF }
        if((iso.value!=null || exposureTimeNs.value!=null) && next in listOf(FlashMode.AUTO,FlashMode.ON)) { userNotice.value="Auto/forced flash needs auto exposure. Torch is available."; flashMode.value=FlashMode.TORCH }
        else flashMode.value=next
        cameraEngine.setFlashMode(flashMode.value); haptic()
    }
    fun selectCamera(cameraId: String) {
        if(!editable()) return
        val spec=CameraOptions.rear(cameraEngine.cameraSpecs.value).firstOrNull { it.cameraId==cameraId }
            ?: run { userNotice.value="That camera ID is no longer exposed by the firmware"; return }
        if(!CaptureFormatPolicy.supported(settings.value.format,spec)) { pendingCamera.value=spec; return }
        applyCamera(spec)
    }
    private fun applyCamera(spec: CameraSpec) {
        activeLens.value=spec.lensType
        settings.value=settings.value.copy(calibration=store.load(spec.lensType,spec.cameraId).calibration)
        store.save(settings.value,spec.lensType,spec.cameraId)
        cameraEngine.configure(settings.value); cameraEngine.selectCamera(spec.cameraId,cropFactor.value)
        updateFraming(); haptic()
    }
    fun confirmCameraFormatSwitch() {
        if(!editable()) return
        val spec=pendingCamera.value ?: return
        pendingCamera.value=null
        val format=if(spec.hasJpegSupport) CaptureFormat.JPEG else CaptureFormat.RAW
        settings.value=CaptureFormatPolicy.normalize(settings.value.copy(format=format))
        cropFactor.value=1f; applyCamera(spec)
        userNotice.value="${CameraOptions.label(spec)} uses ${format.label}; no unsupported file format was silently substituted."
    }
    fun setCrop(factor: Float) {
        if(!editable()) return
        if(!CaptureFormatPolicy.allowsCrop(settings.value.format)) { userNotice.value="RAW modes use uncropped 1× framing so the sensor data and JPEG pair stay aligned."; return }
        if(settings.value.pipeline in listOf(CapturePipeline.OEM_HDR,CapturePipeline.OEM_NIGHT) && factor!=1f) {
            updateSettings(settings.value.copy(pipeline=CapturePipeline.STANDARD)); userNotice.value="Cropped framing uses Camera2 natural"
        }
        cropFactor.value=factor.coerceIn(1f,3f)
        cameraEngine.activeCameraSpec.value?.cameraId?.let { cameraEngine.selectCamera(it,cropFactor.value) }
        updateFraming(); haptic()
    }
    fun toggle2xCrop() { setCrop(if(cropFactor.value==2f) 1f else 2f) }
    private fun updateFraming() {
        val spec=cameraEngine.activeCameraSpec.value
        val dimensions=if(settings.value.format==CaptureFormat.RAW) cameraEngine.rawCaptureSize.value else cameraEngine.captureSize.value
        val native=dimensions.let { it.width.toLong()*it.height/1_000_000f }.takeIf { it>0 }
            ?: if(settings.value.format==CaptureFormat.RAW) spec?.rawSizes?.firstOrNull()?.area?.div(1_000_000f) ?: 0f else spec?.maxResolutionMp ?: 0f
        framingConfig.value=FramingConfig(
            lensSlot=activeLens.value,cropFactor=cropFactor.value,is2xActive=cropFactor.value>1f,
            effectiveFocalLength35mm=((spec?.equivalentFocalLength35mm ?: 0)*cropFactor.value).roundToInt(),
            cropStatus=if(settings.value.outputSize==OutputSize.UPSCALE_28 && native<28.1f) HonestCropStatus.DIGITALLY_RESAMPLED else if(cropFactor.value>1f) HonestCropStatus.NATIVE_NO_RESAMPLE else HonestCropStatus.FULL_SENSOR,
            estimatedResolutionMp=native/(cropFactor.value*cropFactor.value)
        )
    }
    fun updateSettings(requested: CaptureSettings) {
        if(!editable()) return
        val spec=cameraEngine.activeCameraSpec.value
        if(requested.format!=settings.value.format && !CaptureFormatPolicy.supported(requested.format,spec)) {
            userNotice.value="${requested.format.label} is not exposed on this camera. ${spec?.rawUnavailableReason.orEmpty()}"; return
        }
        val s=CaptureFormatPolicy.normalize(requested).let { if(!it.format.writesJpeg) it.copy(compare=false) else it }
        val enteringRaw=s.format.writesRaw && settings.value.format!=s.format
        val enteringOem=s.pipeline in listOf(CapturePipeline.OEM_HDR,CapturePipeline.OEM_NIGHT) && settings.value.pipeline!=s.pipeline
        settings.value=s
        if(enteringRaw) {
            cropFactor.value=1f; isRecipeSheetOpen.value=false
            userNotice.value="${s.format.label}: native uncropped sensor data, standard Camera2. Looks affect JPEG only."
        }
        if(enteringOem) {
            mode.value=CameraMode.AUTO; iso.value=null; exposureTimeNs.value=null; whiteBalanceKelvin.value=0
            focusMode.value=spec?.focusModes?.firstOrNull() ?: "N/A"; evCompensation.value=0f
            flashMode.value=FlashMode.OFF; cropFactor.value=1f; cameraEngine.resetAuto(); cameraEngine.setFlashMode(FlashMode.OFF)
        }
        store.save(s,activeLens.value,spec?.cameraId); cameraEngine.configure(s)
        if(enteringRaw || enteringOem) spec?.cameraId?.let { cameraEngine.selectCamera(it,1f) }
        updateFraming(); haptic()
    }
    private fun requireManualSensor(): Boolean {
        if(!editable()) return false
        if(cameraEngine.activeCameraSpec.value?.manualSensor!=true) { userNotice.value="Manual sensor exposure is not exposed on this lens"; return false }
        return true
    }
    fun setEvCompensation(value: Float) {
        if(!editable() || !value.isFinite()) return
        val spec=cameraEngine.activeCameraSpec.value ?: return
        if(iso.value!=null || exposureTimeNs.value!=null) return
        if(!ProControlMath.hasEv(spec)) return
        val steps=ProControlMath.evSteps(value,spec)
        evCompensation.value=steps*spec.evStep
        cameraEngine.updateEvCompensation(steps)
    }
    fun setIso(value: Int?) {
        if(!requireManualSensor()) return
        val spec=cameraEngine.activeCameraSpec.value ?: return
        iso.value=value?.coerceIn(ProControlMath.isoRange(spec))
        cameraEngine.updateManualIso(iso.value)
        manualFlashGuard()
    }
    fun setShutter(value: Long?) {
        if(!requireManualSensor()) return
        val spec=cameraEngine.activeCameraSpec.value ?: return
        exposureTimeNs.value=value?.coerceIn(ProControlMath.exposureRange(spec))
        cameraEngine.updateManualShutter(exposureTimeNs.value)
        manualFlashGuard()
    }
    private fun manualFlashGuard() {
        if((iso.value!=null || exposureTimeNs.value!=null) && flashMode.value in listOf(FlashMode.AUTO,FlashMode.ON)) {
            flashMode.value=FlashMode.OFF; cameraEngine.setFlashMode(FlashMode.OFF)
        }
    }
    fun setWhiteBalance(value: Int) {
        if(!editable() || cameraEngine.activeCameraSpec.value?.manualWb!=true) return
        whiteBalanceKelvin.value=if(value==0) 0 else ProControlMath.kelvin(value)
        cameraEngine.updateManualKelvin(whiteBalanceKelvin.value)
    }
    fun setFocusMode(value: String) {
        if(!editable() || value !in cameraEngine.activeCameraSpec.value?.focusModes.orEmpty()) return
        focusMode.value=value
        cameraEngine.updateFocusMode(value)
    }
    fun finishProAdjustment() { if(editable()) haptic() }

    /** Lens changes cannot leave an old sensor's out-of-range value in the UI or request. */
    private fun reconcileProControls(spec: CameraSpec) {
        iso.value=if(spec.manualSensor) iso.value?.coerceIn(ProControlMath.isoRange(spec)) else null
        exposureTimeNs.value=if(spec.manualSensor) exposureTimeNs.value?.coerceIn(ProControlMath.exposureRange(spec)) else null
        if(!spec.manualWb) whiteBalanceKelvin.value=0
        val ev=ProControlMath.evSteps(evCompensation.value,spec)
        evCompensation.value=if(ProControlMath.hasEv(spec)) ev*spec.evStep else 0f
        if(focusMode.value !in spec.focusModes) focusMode.value=spec.focusModes.firstOrNull() ?: "N/A"
        cameraEngine.updateManualIso(iso.value); cameraEngine.updateManualShutter(exposureTimeNs.value)
        cameraEngine.updateManualKelvin(whiteBalanceKelvin.value); cameraEngine.updateEvCompensation(ev)
        cameraEngine.updateFocusMode(focusMode.value)
        manualFlashGuard()
    }
    fun tunedRecipe(): Recipe = activeRecipe.value.copy(puckTone=puckTone.value,puckColor=puckColor.value)
    fun selectRecipe(recipe: Recipe) {
        if(!editable()) return
        repository.setActiveRecipe(recipe); puckTone.value=recipe.puckTone; puckColor.value=recipe.puckColor; haptic()
    }
    fun updatePuck(tone: Float,color: Float) { if(editable()) { puckTone.value=tone.coerceIn(-100f,100f); puckColor.value=color.coerceIn(-100f,100f) } }
    fun resetPuck() { updatePuck(0f,0f); haptic() }
    fun saveAsCustomRecipe(name: String) {
        val custom=Recipe.createCustom(name.take(80),activeRecipe.value,puckTone.value,puckColor.value)
        viewModelScope.launch {
            runCatching { withContext(Dispatchers.IO) { repository.saveCustomRecipe(custom) } }.onSuccess { userNotice.value="Saved ${custom.name}" }.onFailure { userNotice.value="Could not save recipe: ${it.message}" }
        }
    }
    fun toggleFavourite(recipe: Recipe) { viewModelScope.launch { withContext(Dispatchers.IO) { repository.toggleFavourite(recipe) }; haptic() } }
    fun deleteRecipe(recipe: Recipe) { viewModelScope.launch { withContext(Dispatchers.IO) { repository.deleteCustomRecipe(recipe.id) }; selectRecipe(activeRecipe.value) } }
    fun triggerShutter() {
        if(!foreground || isGalleryOpen.value || !cameraEngine.isCameraStreaming.value) { userNotice.value="Wait for the camera to become ready"; return }
        if(!isCapturing.compareAndSet(false,true)) return
        val recipe=tunedRecipe(); val captureSettings=settings.value; val factor=cropFactor.value
        haptic(true)
        viewModelScope.launch {
            var captured: CaptureBundle?=null
            isShutterFlashing.value=true
            launch { delay(75); isShutterFlashing.value=false }
            try {
                val heavy=Build.VERSION.SDK_INT<29 || runCatching { power.currentThermalStatus<PowerManager.THERMAL_STATUS_SEVERE }.getOrDefault(true)
                if(!heavy && captureSettings.pipeline in listOf(CapturePipeline.OEM_HDR,CapturePipeline.OEM_NIGHT)) error("Phone is hot. Choose Camera2 natural or let it cool before an OEM HDR/Night capture.")
                if(!heavy && (captureSettings.pipeline==CapturePipeline.BURST || captureSettings.zsl)) userNotice.value="Phone is hot: using a single standard capture"
                val bundle=cameraEngine.capturePhoto(factor,heavy).getOrThrow(); captured=bundle
                if(captureSettings.format.writesRaw) {
                    val raw=bundle.raw ?: error("RAW image missing; no JPEG-only replacement was saved")
                    processingProgress.value="Writing native DNG…"
                    val saved=RawMediaStoreSaver.save(getApplication(),captureSettings.format,raw.width,raw.height,
                        writeRaw={ stream -> try { raw.writeTo(stream) } finally { raw.close() } },
                        makeJpeg={
                            processingProgress.value="Saving paired JPEG…"
                            withContext(Dispatchers.Default) { ImageProcessor.processCapture(getApplication(),bundle,recipe,captureSettings) }
                        }).getOrThrow()
                    lastCapturedUri.value=saved.preferred
                    userNotice.value=if(saved.jpeg!=null) "Saved RAW + JPEG pair · DNG ${saved.rawWidth} × ${saved.rawHeight}; JPEG ${saved.jpegWidth} × ${saved.jpegHeight}"
                        else "Saved ungraded RAW DNG · ${saved.rawWidth} × ${saved.rawHeight}"
                    refreshPhotos(saved.preferred)
                } else {
                    processingProgress.value=if(bundle.frames.size==3) "Aligning and merging frames…" else "Saving photo…"
                    val output=withContext(Dispatchers.Default) { ImageProcessor.processCapture(getApplication(),bundle,recipe,captureSettings) }
                    val uri=MediaStoreSaver.savePhoto(getApplication(),output.bytes,recipe.name).getOrThrow()
                    lastCapturedUri.value=uri; userNotice.value="Saved ${output.width} × ${output.height} · ${recipe.name}"
                    refreshPhotos(uri)
                }
            } catch(e: CancellationException) { throw e }
            catch(e: OutOfMemoryError) { userNotice.value="Not enough image memory. Try JPEG or a smaller advertised RAW stream/lens." }
            catch(e: Exception) { userNotice.value="Photo not saved: ${e.message ?: e.javaClass.simpleName}" }
            finally { runCatching { captured?.close() }; processingProgress.value=""; isCapturing.value=false; isShutterFlashing.value=false }
        }
    }
    fun refreshPhotos(preferred: Uri?=null) {
        viewModelScope.launch {
            runCatching { withContext(Dispatchers.IO) { photoRepository.list() } }.onSuccess { list ->
                photos.value=list; val uri=preferred ?: list.firstOrNull()?.uri
                if(uri!=null) {
                    lastCapturedUri.value=uri
                    runCatching { withContext(Dispatchers.IO) {
                        if(Build.VERSION.SDK_INT>=29) getApplication<Application>().contentResolver.loadThumbnail(uri,Size(160,160),null)
                        else ImageDecoder.decodeBitmap(ImageDecoder.createSource(getApplication<Application>().contentResolver,uri)) { d,_,_ -> d.setTargetSize(160,160); d.allocator=ImageDecoder.ALLOCATOR_SOFTWARE }
                    } }.onSuccess { lastCapturedThumbnail.value=it }.onFailure { lastCapturedThumbnail.value=null }
                } else { lastCapturedUri.value=null; lastCapturedThumbnail.value=null }
            }.onFailure { userNotice.value="Gallery is unavailable: ${it.message}" }
        }
    }
    fun openGallery() {
        if(!editable()) return
        refreshPhotos(); cameraEngine.closeCamera(); isGalleryOpen.value=true
    }
    fun closeGallery() { isGalleryOpen.value=false; if(foreground) cameraEngine.openCamera() }
    fun deletePhoto(photo: PhotoRepository.Photo) {
        viewModelScope.launch {
            runCatching { withContext(Dispatchers.IO) { photoRepository.delete(photo) } }
                .onSuccess { refreshPhotos(); userNotice.value="Photo deleted" }.onFailure { userNotice.value="Could not delete photo: ${it.message}" }
        }
    }
    fun exportRecipe(uri: Uri) {
        val recipe=tunedRecipe()
        viewModelScope.launch { runCatching { withContext(Dispatchers.IO) {
            val json=RecipeJsonSerializer.toJson(listOf(recipe))
            getApplication<Application>().contentResolver.openOutputStream(uri,"wt")?.bufferedWriter(Charsets.UTF_8)?.use { it.write(json) } ?: error("Document could not be opened")
        } }.onSuccess { userNotice.value="Recipe exported" }.onFailure { userNotice.value="Export failed: ${it.message}" } }
    }
    fun importRecipes(uri: Uri,qr: Boolean=false) {
        viewModelScope.launch {
            runCatching { withContext(Dispatchers.IO) {
                val resolver=getApplication<Application>().contentResolver
                val imported=if(qr) {
                    val bitmap=ImageDecoder.decodeBitmap(ImageDecoder.createSource(resolver,uri)) { decoder,info,_ ->
                        val ratio=(maxOf(info.size.width,info.size.height)/1600f).coerceAtLeast(1f)
                        decoder.setTargetSize((info.size.width/ratio).roundToInt(),(info.size.height/ratio).roundToInt()); decoder.allocator=ImageDecoder.ALLOCATOR_SOFTWARE
                    }
                    try { RecipeQrCodec.scan(bitmap).getOrThrow() } finally { bitmap.recycle() }
                } else {
                    val text=resolver.openInputStream(uri)?.use { input ->
                        val out=ByteArrayOutputStream(); val buffer=ByteArray(8192)
                        while(true) { val n=input.read(buffer); if(n<0) break; require(out.size()+n<=RecipeJsonSerializer.MAX_BYTES) { "Recipe file exceeds 256KB" }; out.write(buffer,0,n) }
                        out.toString("UTF-8")
                    } ?: error("Cannot open document")
                    RecipeJsonSerializer.fromJson(text).getOrThrow()
                }
                repository.importRecipes(imported)
            } }.onSuccess { imported -> selectRecipe(imported.first()); userNotice.value="Imported ${imported.size} custom recipe(s)" }
                .onFailure { userNotice.value="Import failed: ${it.message}" }
        }
    }
    fun checkUpdates() {
        if(updateStatus.value=="Checking GitHub releases…") return
        updateStatus.value="Checking GitHub releases…"
        viewModelScope.launch { ReleaseChecker.check().onSuccess { release ->
            availableUpdate.value=release; updateStatus.value=if(release==null) "No newer ${ReleaseChecker.channelLabel} build found" else "Available: ${release.tag}"
        }.onFailure { updateStatus.value="Could not check updates. Use the releases page instead." } }
    }
    fun getDiagnosticsReport(): String = CameraDiagnosticsReport.generateReport(cameraEngine.cameraSpecs.value)+
        "\n${cameraEngine.diagnosticSummary()}\nThermal: ${thermalStatus.value}\nBuild: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})\n"+
        "Default: neutral Camera2 ISP JPEG, not proprietary Oppo stock-app processing.\n"+
        "Active calibration (user-adjustable, unmeasured): ${settings.value.calibration}\n"+
        "Files: ${settings.value.format.label}\nCamera ID: ${cameraEngine.activeCameraSpec.value?.cameraId}\n"+
        "Output: ${settings.value.outputSize.label}\nPipeline request: ${settings.value.pipeline.label}\n"
    override fun onCleared() {
        val listener=thermalListener
        if(Build.VERSION.SDK_INT>=29 && thermalRegistered && listener!=null) runCatching { power.removeThermalStatusListener(listener) }
        cameraEngine.stopBackgroundThread(); repository.close()
        super.onCleared()
    }
}

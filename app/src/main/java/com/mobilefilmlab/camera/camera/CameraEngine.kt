package com.mobilefilmlab.camera.camera

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.hardware.camera2.params.*
import android.media.Image
import android.media.ImageReader
import android.media.ImageWriter
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Rational
import android.util.Size
import android.view.Surface
import androidx.core.content.ContextCompat
import androidx.exifinterface.media.ExifInterface
import com.mobilefilmlab.camera.data.model.CameraSpec
import com.mobilefilmlab.camera.data.model.LensType
import com.mobilefilmlab.camera.data.model.RawStreamSelector
import com.mobilefilmlab.camera.data.model.CameraOptions
import com.mobilefilmlab.camera.data.model.StreamSize
import com.mobilefilmlab.camera.pipeline.CropEngine
import com.mobilefilmlab.camera.settings.CapturePipeline
import com.mobilefilmlab.camera.settings.CaptureSettings
import com.mobilefilmlab.camera.settings.CaptureFormatPolicy
import kotlinx.coroutines.*
import kotlinx.coroutines.android.asCoroutineDispatcher
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.ByteArrayInputStream
import java.util.Locale
import java.util.concurrent.Executor
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.roundToInt


enum class FlashMode { OFF, AUTO, ON, TORCH }

/** Camera resources and request builders are confined to the camera handler. Image/result pairs use sensor timestamps. */
class CameraEngine(private val context: Context) {
    private val manager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private val discovery = CameraDiscovery(context)
    private val thread = HandlerThread("MobileFilmLab-Camera").apply { start() }
    private val handler = Handler(thread.looper)
    private val dispatcher = handler.asCoroutineDispatcher("Camera2")
    private val executor = Executor { handler.post(it) }
    private val captureMutex = Mutex()
    private val _cameraSpecs = MutableStateFlow<List<CameraSpec>>(emptyList())
    val cameraSpecs = _cameraSpecs.asStateFlow()
    private val _activeCameraSpec = MutableStateFlow<CameraSpec?>(null)
    val activeCameraSpec = _activeCameraSpec.asStateFlow()
    private val _isCameraStreaming = MutableStateFlow(false)
    val isCameraStreaming = _isCameraStreaming.asStateFlow()
    private val _cameraError = MutableStateFlow<String?>(null)
    val cameraError = _cameraError.asStateFlow()
    private val _pipelineStatus = MutableStateFlow("Camera not started")
    val pipelineStatus = _pipelineStatus.asStateFlow()
    private val _captureSize = MutableStateFlow(Size(0,0))
    val captureSize = _captureSize.asStateFlow()
    private val _rawCaptureSize = MutableStateFlow(Size(0,0))
    val rawCaptureSize = _rawCaptureSize.asStateFlow()
    private val latestResult = MutableStateFlow<TotalCaptureResult?>(null)
    private val _progress = MutableStateFlow("")
    val progress = _progress.asStateFlow()

    @Volatile private var resumed = false
    private var generation = 0
    private var sessionEpoch = 0
    private var opening = false
    private var device: CameraDevice? = null
    private var session: CameraCaptureSession? = null
    private var extensionSession: CameraExtensionSession? = null
    private var jpegReader: ImageReader? = null
    private var rawReader: ImageReader? = null
    private var rawLifetime: RetainedResource? = null
    private var rawAttemptIndex = 0
    private var rawRetries = 0
    private var rawAttempts: List<StreamSize> = emptyList()
    private var previewSurface: Surface? = null
    private var previewSize = Size(1920,1080)
    private var previewBuilder: CaptureRequest.Builder? = null
    @Volatile private var settings = CaptureSettings()
    @Volatile private var cropFactor = 1f
    @Volatile private var deviceOrientation = 0
    fun updateDeviceOrientation(degrees: Int) { if(degrees>=0) deviceOrientation=degrees }
    private fun jpegOrientation()=CaptureOrientation.jpeg(getSensorOrientation(),deviceOrientation,activeCameraSpec.value?.isBackFacing==false)
    private var flash = FlashMode.OFF
    private var evSteps = 0
    private var manualIso: Int? = null
    private var manualTime: Long? = null
    private var kelvin = 0
    private var focusMode = "AF-C"
    private var focusRegion: MeteringRectangle? = null
    private var selectedExtension: Int? = null
    private var useMaximumMode = false
    private var forceStandard = false
    private var disableZsl = false
    private var thermalLimited = false
    fun setThermalLimited(value: Boolean) { handler.post {
        if(thermalLimited!=value) { thermalLimited=value; if(resumed && settings.zsl) restartInternal() }
    } }
    private var attempts: List<ResolutionSelector.Candidate> = emptyList()
    private var attemptIndex = 0

    private var privateReader: ImageReader? = null
    private var writer: ImageWriter? = null
    private data class ZslFrame(val image: Image, val result: TotalCaptureResult, val receivedAt: Long=android.os.SystemClock.elapsedRealtimeNanos())
    private val zslRing = ArrayDeque<ZslFrame>()
    private val zslImages = linkedMapOf<Long,Image>()
    private val previewResults = linkedMapOf<Long,TotalCaptureResult>()
    private data class Shot(
        val spec: CameraSpec, val crop: Float, val label: String,
        val deferred: CompletableDeferred<CapturedJpeg> = CompletableDeferred(),
        var timestamp: Long = -1, var bytes: ByteArray? = null, var result: TotalCaptureResult? = null,
        val sourceResult: TotalCaptureResult? = null, val sourceSize: Pair<Int,Int>? = null, var extensionComplete: Boolean = false,
        val manual: Boolean = false, val manualWb: Boolean = false,
        val wantRaw: Boolean = false, val wantJpeg: Boolean = true, val rawCharacteristics: CameraCharacteristics? = null,
        val orientation: Int = 0, var rawImage: ResourceLease<Image>? = null,
        var jpegTimestamp: Long = -1, var completedRaw: RawCapture? = null
    )
    private val pending = mutableSetOf<Shot>()
    private val unmatchedJpegs = linkedMapOf<Long,ByteArray>()
    private val unmatchedRaw = linkedMapOf<Long,ResourceLease<Image>>()
    private var extensionShot: Shot? = null

    init { refreshCameraList() }
    fun refreshCameraList() {
        val found = discovery.discoverCameras()
        _cameraSpecs.value = found
        val previous = _activeCameraSpec.value?.cameraId
        val photoRoutes=CameraOptions.rear(found)
        _activeCameraSpec.value = photoRoutes.firstOrNull { it.cameraId == previous }
            ?: photoRoutes.filter { it.lensType == LensType.WIDE }.minByOrNull { if(it.isLogicalMultiCamera) 1 else 0 }
            ?: photoRoutes.firstOrNull()
    }
    fun startBackgroundThread() { resumed = true }
    fun stopBackgroundThread() { closeCamera(); thread.quitSafely() }
    fun openCamera() {
        resumed = true
        handler.post { if (resumed && !opening && device == null && previewSurface?.isValid == true) openInternal() }
    }
    fun bindPreviewSurface(surface: Surface, width: Int, height: Int) {
        handler.post {
            val changed = previewSurface !== surface || previewSize != Size(width,height)
            previewSurface = surface; previewSize = Size(width,height)
            if (resumed && changed) restartInternal()
            else if (resumed && device == null && !opening) openInternal()
        }
    }
    fun unbindPreviewSurface() { handler.post { previewSurface = null; closeInternal() } }
    fun closeCamera() { resumed = false; handler.post { closeInternal() } }
    private fun closeInternal() {
        generation++; sessionEpoch++; opening = false
        pending.toList().forEach {
            it.deferred.completeExceptionally(IllegalStateException("Camera closed; please try again"))
            it.rawImage?.close(); it.rawImage=null
        }
        clearRawInbox()
        pending.clear(); unmatchedJpegs.clear(); extensionShot = null
        closeStreams()
        runCatching { device?.close() }; device = null
        _isCameraStreaming.value = false; latestResult.value = null
    }
    private fun closeStreams() {
        runCatching { session?.close() }; session=null
        if (Build.VERSION.SDK_INT >= 31) runCatching { extensionSession?.close() }
        extensionSession=null; previewBuilder=null
        clearZsl(); runCatching { writer?.close() }; writer=null
        runCatching { privateReader?.close() }; privateReader=null
        runCatching { jpegReader?.close() }; jpegReader=null
        clearRawInbox()
        // Do not invalidate an acquired RAW image while an IO worker is writing its DNG.
        rawLifetime?.retire(); rawLifetime=null; rawReader=null
        _rawCaptureSize.value=Size(0,0)
    }
    private fun clearZsl() {
        zslRing.forEach { it.image.close() }; zslRing.clear()
        zslImages.values.forEach { it.close() }; zslImages.clear(); previewResults.clear()
    }
    private fun restartInternal() {
        closeInternal()
        if (resumed && previewSurface?.isValid == true) openInternal()
    }
    @SuppressLint("MissingPermission")
    private fun openInternal() {
        if (!resumed || previewSurface?.isValid != true) return
        if (ContextCompat.checkSelfPermission(context,Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            _cameraError.value="Camera permission is required"; return
        }
        val spec = _activeCameraSpec.value ?: run { _cameraError.value="No accessible rear camera"; return }
        val gen = generation
        opening=true; forceStandard=false; disableZsl=false; attemptIndex=0; rawAttemptIndex=0; rawRetries=0
        try {
            manager.openCamera(spec.openCameraId, object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    if (gen != generation || !resumed) { camera.close(); return }
                    opening=false; device=camera; prepareSession()
                }
                override fun onDisconnected(camera: CameraDevice) {
                    camera.close()
                    if (gen == generation) { closeInternal(); _cameraError.value="Camera disconnected. Return to the app to retry." }
                }
                override fun onError(camera: CameraDevice, error: Int) {
                    camera.close()
                    if (gen == generation) { closeInternal(); _cameraError.value="Camera ${spec.cameraId} cannot open (code $error). No substitute lens was used." }
                }
            },handler)
        } catch (e: Exception) { opening=false; _cameraError.value="Cannot open camera: ${e.message}" }
    }
    /** Select an exact advertised route, never the first camera in a hardcoded lens category. */
    fun selectCamera(cameraId: String, factor: Float = 1f) {
        val match=CameraOptions.rear(cameraSpecs.value).firstOrNull { it.cameraId==cameraId }
        if(match==null) { _cameraError.value="Camera ID $cameraId is no longer exposed by this firmware"; return }
        val different=_activeCameraSpec.value?.cameraId!=match.cameraId
        _activeCameraSpec.value=match
        cropFactor=if(settings.format.writesRaw) 1f else factor.coerceIn(1f,3f)
        handler.post {
            focusRegion=null
            if(different || settings.zsl) restartInternal() else updateRepeatingInternal()
        }
    }
    fun configure(s: CaptureSettings) {
        handler.post {
            val next=CaptureFormatPolicy.normalize(s)
            val reconfigure = settings.pipeline != next.pipeline || settings.zsl != next.zsl || settings.format != next.format
            settings=next
            if(next.format.writesRaw) cropFactor=1f
            if (reconfigure && resumed) restartInternal()
        }
    }
    private fun chars(): CameraCharacteristics = manager.getCameraCharacteristics(_activeCameraSpec.value!!.cameraId)
    private fun parentChars(): CameraCharacteristics = manager.getCameraCharacteristics(_activeCameraSpec.value!!.openCameraId)
    fun getSensorOrientation(): Int = runCatching { chars().get(CameraCharacteristics.SENSOR_ORIENTATION) ?: 90 }.getOrDefault(90)
    fun getOptimalPreviewSize(viewWidth: Int, viewHeight: Int): Size {
        return runCatching {
            val spec = activeCameraSpec.value ?: return@runCatching Size(1920,1080)
            val pc = manager.getCameraCharacteristics(spec.openCameraId)
            var sizes = pc.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)?.getOutputSizes(SurfaceTexture::class.java)?.toList().orEmpty()
            if (spec.physicalOutputId != null) {
                val physical = chars().get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)?.getOutputSizes(SurfaceTexture::class.java)?.toSet().orEmpty()
                sizes=sizes.filter { it in physical }
            }
            if (Build.VERSION.SDK_INT >= 31) {
                val ext = if(settings.format.writesRaw) null else extensionFor(settings.pipeline)
                if (ext != null && ext in spec.extensionModes && !forceStandard) {
                    sizes = manager.getCameraExtensionCharacteristics(spec.openCameraId).getExtensionSupportedSizes(ext,SurfaceTexture::class.java)
                }
            }
            val allowed = sizes.filter { it.width <= 1920 && it.height <= 1440 }
            allowed.filter { kotlin.math.abs(it.width.toFloat()/it.height - 4f/3f) < 0.04f }.maxByOrNull { it.width*it.height }
                ?: allowed.maxByOrNull { it.width*it.height } ?: sizes.firstOrNull() ?: Size(1920,1080)
        }.getOrDefault(Size(1920,1080))
    }
    private fun extensionFor(p: CapturePipeline): Int? = if (Build.VERSION.SDK_INT >= 31) when(p) {
        CapturePipeline.OEM_HDR -> CameraExtensionCharacteristics.EXTENSION_HDR
        CapturePipeline.OEM_NIGHT -> CameraExtensionCharacteristics.EXTENSION_NIGHT
        else -> null
    } else null
    private fun prepareSession() {
        val camera=device ?: return
        val spec=activeCameraSpec.value ?: return
        val surface=previewSurface ?: return
        if (!surface.isValid) return
        val gen=generation; val epoch=++sessionEpoch
        closeStreams()
        if(settings.format.writesRaw && !spec.hasRawSupport) {
            _isCameraStreaming.value=false
            _cameraError.value="RAW_SENSOR is not exposed on this camera. Choose JPEG or a RAW-capable lens. ${spec.rawUnavailableReason}"
            return
        }
        if(settings.format.writesRaw) { prepareRawSession(camera,spec,surface,gen,epoch); return }
        val wantedExt=extensionFor(settings.pipeline)
        selectedExtension=if (!forceStandard && wantedExt in spec.extensionModes && spec.physicalOutputId == null) wantedExt else null
        if (wantedExt != null && selectedExtension == null) _pipelineStatus.value="OEM extension unavailable on this lens; using Camera2"
        try {
            val candidates=mutableListOf<ResolutionSelector.Candidate>()
            val map=chars().get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            if (selectedExtension != null && Build.VERSION.SDK_INT >= 31) {
                manager.getCameraExtensionCharacteristics(spec.openCameraId).getExtensionSupportedSizes(selectedExtension!!,ImageFormat.JPEG).forEach {
                    candidates+=ResolutionSelector.Candidate(it.width,it.height)
                }
            } else {
                (map?.getOutputSizes(ImageFormat.JPEG)?.toList().orEmpty()+map?.getHighResolutionOutputSizes(ImageFormat.JPEG)?.toList().orEmpty()).forEach {
                    candidates+=ResolutionSelector.Candidate(it.width,it.height)
                }
                if (Build.VERSION.SDK_INT >= 31 && !settings.format.writesRaw && !settings.zsl && settings.pipeline != CapturePipeline.BURST) {
                    chars().get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP_MAXIMUM_RESOLUTION)?.let { maxMap ->
                        (maxMap.getOutputSizes(ImageFormat.JPEG)?.toList().orEmpty()+maxMap.getHighResolutionOutputSizes(ImageFormat.JPEG)?.toList().orEmpty()).forEach {
                            candidates+=ResolutionSelector.Candidate(it.width,it.height,true)
                        }
                    }
                }
            }
            val limit=if (settings.pipeline == CapturePipeline.BURST || settings.zsl) 12_600_000L else 55_000_000L
            val best=ResolutionSelector.select(candidates,limit) ?: throw IllegalStateException("No JPEG output exposed")
            attempts=(listOf(best)+candidates.filter { it != best && !it.maximumMode }.sortedByDescending { it.area }).distinctBy { Triple(it.width,it.height,it.maximumMode) }
            val selected=attempts.getOrElse(attemptIndex) { attempts.last() }
            useMaximumMode=selected.maximumMode
            _captureSize.value=Size(selected.width,selected.height)
            val reader=ImageReader.newInstance(selected.width,selected.height,ImageFormat.JPEG,4)
            jpegReader=reader
            reader.setOnImageAvailableListener({ r -> if (gen == generation && epoch == sessionEpoch) readJpeg(r) else runCatching { r.acquireLatestImage()?.close() } },handler)
            if (selectedExtension != null && Build.VERSION.SDK_INT >= 31) {
                camera.createExtensionSession(ExtensionSessionConfiguration(selectedExtension!!,
                    listOf(OutputConfiguration(surface),OutputConfiguration(reader.surface)),executor,
                    object : CameraExtensionSession.StateCallback() {
                        override fun onConfigured(s: CameraExtensionSession) {
                            if (gen != generation || epoch != sessionEpoch) { s.close(); return }
                            extensionSession=s
                            try {
                                val b=camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply { addTarget(surface) }
                                previewBuilder=b
                                s.setRepeatingRequest(b.build(),executor,object : CameraExtensionSession.ExtensionCaptureCallback() {})
                                _isCameraStreaming.value=true; _cameraError.value=null
                                _pipelineStatus.value=if (selectedExtension == CameraExtensionCharacteristics.EXTENSION_HDR) "OEM HDR active" else "OEM Night active"
                            } catch (e: Exception) { fallbackSession("OEM preview failed",gen,epoch) }
                        }
                        override fun onConfigureFailed(s: CameraExtensionSession) { s.close(); fallbackSession("OEM session rejected",gen,epoch) }
                    }))
                return
            }
            val outputs=mutableListOf(OutputConfiguration(surface),OutputConfiguration(reader.surface))
            spec.physicalOutputId?.let { physical -> outputs.forEach { it.setPhysicalCameraId(physical) } }
            if (useMaximumMode && Build.VERSION.SDK_INT >= 31) {
                outputs[1].addSensorPixelModeUsed(CameraMetadata.SENSOR_PIXEL_MODE_MAXIMUM_RESOLUTION)
            }
            val canZsl=!settings.format.writesRaw && settings.zsl && !thermalLimited && !disableZsl && !useMaximumMode && spec.privateReprocessing && spec.physicalOutputId == null && cropFactor == 1f
            var input: InputConfiguration?=null
            if (canZsl) {
                val parentMap=parentChars().get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                val privateSizes=parentMap?.getInputSizes(ImageFormat.PRIVATE)?.toList().orEmpty()
                    .intersect(parentMap?.getOutputSizes(ImageFormat.PRIVATE)?.toSet().orEmpty())
                val z=privateSizes.filter { it.width.toLong()*it.height <= 12_600_000L }.maxByOrNull { it.width.toLong()*it.height }
                if (z != null) {
                    input=InputConfiguration(z.width,z.height,ImageFormat.PRIVATE)
                    privateReader=ImageReader.newInstance(z.width,z.height,ImageFormat.PRIVATE,6).apply {
                        setOnImageAvailableListener({ r ->
                            if (gen != generation || epoch != sessionEpoch) { runCatching { r.acquireLatestImage()?.close() }; return@setOnImageAvailableListener }
                            while(true) {
                                val im=runCatching { r.acquireNextImage() }.getOrNull() ?: break
                                zslImages[im.timestamp]=im; matchZsl(im.timestamp)
                                while (zslImages.size > 2) { val key=zslImages.keys.first(); zslImages.remove(key)?.close() }
                            }
                        },handler)
                    }
                    outputs+=OutputConfiguration(privateReader!!.surface)
                }
            }
            val config=SessionConfiguration(SessionConfiguration.SESSION_REGULAR,outputs,executor,
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(s: CameraCaptureSession) {
                        if (gen != generation || epoch != sessionEpoch) { s.close(); return }
                        session=s
                        try {
                            if (s.isReprocessable) writer=ImageWriter.newInstance(requireNotNull(s.inputSurface),2)
                            val b=camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
                                addTarget(surface); privateReader?.surface?.let { addTarget(it) }
                            }
                            previewBuilder=b; updateRepeatingInternal()
                            _isCameraStreaming.value=true; _cameraError.value=null
                            val fallback=if (wantedExt != null) " · OEM unavailable" else ""
                            _pipelineStatus.value=when {
                                rawReader != null -> "Camera2 · ${settings.format.badge} · native RAW_SENSOR + JPEG companion"
                                privateReader != null -> "Camera2 · PRIVATE reprocessing ZSL active"
                                settings.zsl -> "Camera2 · ZSL unavailable/fallback"
                                settings.pipeline == CapturePipeline.BURST -> "Camera2 · experimental 3-frame JPEG fusion selected"
                                else -> "Camera2 native JPEG$fallback"
                            }
                        } catch (e: Exception) { fallbackSession("Preview request failed",gen,epoch) }
                    }
                    override fun onConfigureFailed(s: CameraCaptureSession) { s.close(); fallbackSession("Stream combination rejected",gen,epoch) }
                })
            if (input != null) config.inputConfiguration=input
            camera.createCaptureSession(config)
        } catch (e: Exception) { fallbackSession(e.message ?: "Session creation failed",gen,epoch) }
    }
    private fun prepareRawSession(camera: CameraDevice,spec: CameraSpec,surface: Surface,gen: Int,epoch: Int) {
        selectedExtension=null; useMaximumMode=false
        try {
            rawAttempts=RawStreamSelector.candidates(spec.rawSizes)
            val size=rawAttempts.getOrNull(rawAttemptIndex) ?: error("No safe native RAW_SENSOR size is advertised")
            val rr=ImageReader.newInstance(size.width,size.height,ImageFormat.RAW_SENSOR,2)
            val lifetime=RetainedResource { runCatching { rr.close() }; Unit }
            rawReader=rr; rawLifetime=lifetime; _rawCaptureSize.value=Size(size.width,size.height)
            rr.setOnImageAvailableListener({ r ->
                if(gen==generation && epoch==sessionEpoch) readRaw(r,lifetime)
                else runCatching { r.acquireLatestImage()?.close() }
            },handler)
            val outputs=mutableListOf(OutputConfiguration(surface),OutputConfiguration(rr.surface))
            if(settings.format.writesJpeg) {
                val map=chars().get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                val sizes=map?.getOutputSizes(ImageFormat.JPEG)?.toList().orEmpty()+map?.getHighResolutionOutputSizes(ImageFormat.JPEG)?.toList().orEmpty()
                attempts=sizes.map { ResolutionSelector.Candidate(it.width,it.height) }.filter { it.area<=55_000_000L }.distinct().sortedByDescending { it.area }
                val jpeg=attempts.getOrNull(attemptIndex) ?: error("No JPEG companion size is advertised")
                val reader=ImageReader.newInstance(jpeg.width,jpeg.height,ImageFormat.JPEG,3)
                jpegReader=reader; _captureSize.value=Size(jpeg.width,jpeg.height)
                reader.setOnImageAvailableListener({ r ->
                    if(gen==generation && epoch==sessionEpoch) readJpeg(r) else runCatching { r.acquireLatestImage()?.close() }
                },handler)
                outputs+=OutputConfiguration(reader.surface)
            } else {
                // True RAW-only capture: no JPEG reader or still-JPEG target, only the normal preview.
                attempts=emptyList(); _captureSize.value=Size(0,0)
            }
            spec.physicalOutputId?.let { id -> outputs.forEach { it.setPhysicalCameraId(id) } }
            camera.createCaptureSession(SessionConfiguration(SessionConfiguration.SESSION_REGULAR,outputs,executor,
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(s: CameraCaptureSession) {
                        if(gen!=generation || epoch!=sessionEpoch) { s.close(); return }
                        session=s
                        try {
                            val builder=camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply { addTarget(surface) }
                            previewBuilder=builder; applySettings(builder,true)
                            s.setRepeatingRequest(builder.build(),previewCallback,handler)
                            _isCameraStreaming.value=true; _cameraError.value=null
                            _pipelineStatus.value="Camera2 · ${settings.format.badge} · native RAW_SENSOR"
                        } catch(e: Exception) { fallbackSession("RAW preview request failed",gen,epoch) }
                    }
                    override fun onConfigureFailed(s: CameraCaptureSession) { s.close(); fallbackSession("RAW stream combination rejected",gen,epoch) }
                }))
            handler.postDelayed({ if(gen==generation && epoch==sessionEpoch && !_isCameraStreaming.value) fallbackSession("RAW session setup timed out",gen,epoch) },7000)
        } catch(e: Exception) { fallbackSession(e.message ?: "RAW session creation failed",gen,epoch) }
    }
    private fun fallbackSession(reason: String, gen: Int, epoch: Int) {
        if (gen != generation || epoch != sessionEpoch) return
        if(settings.format.writesRaw) {
            when {
                rawRetries<5 && rawAttemptIndex+1<rawAttempts.size -> {
                    rawRetries++; rawAttemptIndex++
                    _pipelineStatus.value="$reason; trying another advertised native RAW stream"; prepareSession()
                }
                rawRetries<5 && attemptIndex+1<attempts.size && attemptIndex<3 -> {
                    rawRetries++; attemptIndex++; rawAttemptIndex=0
                    _pipelineStatus.value="$reason; trying a smaller JPEG companion with native RAW"; prepareSession()
                }
                else -> {
                    _isCameraStreaming.value=false
                    _cameraError.value="$reason. This firmware rejected the requested RAW + preview${if(settings.format.writesJpeg) "/JPEG" else ""} stream combination. No JPEG-only substitute was saved. Choose JPEG to continue."
                }
            }
            return
        }
        when {
            selectedExtension != null && !forceStandard -> { forceStandard=true; _cameraError.value="$reason; falling back to standard Camera2"; prepareSession() }
            privateReader != null && !disableZsl -> { disableZsl=true; _cameraError.value="$reason; ZSL disabled for this session"; prepareSession() }
            attemptIndex+1 < attempts.size && attemptIndex < 4 -> { attemptIndex++; _pipelineStatus.value="$reason; trying a smaller native JPEG stream"; prepareSession() }
            else -> { _isCameraStreaming.value=false; _cameraError.value="$reason. This lens/stream is not usable on the current firmware." }
        }
    }
    private val previewCallback=object : CameraCaptureSession.CaptureCallback() {
        override fun onCaptureCompleted(s: CameraCaptureSession, r: CaptureRequest, result: TotalCaptureResult) {
            if (s !== session) return
            latestResult.value=result
            if (privateReader != null) {
                val ts=result.get(CaptureResult.SENSOR_TIMESTAMP) ?: return
                previewResults[ts]=result; matchZsl(ts)
                while (previewResults.size > 8) previewResults.remove(previewResults.keys.first())
            }
        }
    }
    private fun matchZsl(ts: Long) {
        val im=zslImages[ts] ?: return; val result=previewResults[ts] ?: return
        zslImages.remove(ts); previewResults.remove(ts)
        zslRing.addLast(ZslFrame(im,result)); while (zslRing.size > 3) zslRing.removeFirst().image.close()
    }
    private fun applySettings(b: CaptureRequest.Builder, preview: Boolean, bracketEv: Int = 0) {
        val c=chars(); val p=parentChars(); val spec=activeCameraSpec.value ?: return
        val keys=p.availableCaptureRequestKeys.toSet()
        val physicalKeys=p.availablePhysicalCameraRequestKeys?.toSet().orEmpty()
        fun <T> set(k: CaptureRequest.Key<T>, v: T) {
            if(k in keys) runCatching { b.set(k,v) }
            spec.physicalOutputId?.let { id -> if(k in physicalKeys) runCatching { b.setPhysicalCameraKey(k,v,id) } }
        }
        val afs=(c.get(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES) ?: intArrayOf())
        val desired=when(focusMode) { "AF-S" -> CaptureRequest.CONTROL_AF_MODE_AUTO; "MACRO" -> CaptureRequest.CONTROL_AF_MODE_MACRO; else -> CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE }
        set(CaptureRequest.CONTROL_AF_MODE, if (desired in afs) desired else afs.firstOrNull() ?: CaptureRequest.CONTROL_AF_MODE_OFF)
        focusRegion?.let { rect ->
            if ((p.get(CameraCharacteristics.CONTROL_MAX_REGIONS_AF) ?: 0) > 0) set(CaptureRequest.CONTROL_AF_REGIONS,arrayOf(rect))
            if ((p.get(CameraCharacteristics.CONTROL_MAX_REGIONS_AE) ?: 0) > 0) set(CaptureRequest.CONTROL_AE_REGIONS,arrayOf(rect))
        }
        val isManual=(manualIso != null || manualTime != null) && spec.manualSensor
        val latest=latestResult.value
        val base=spec.physicalOutputId?.let { latest?.physicalCameraResults?.get(it) } ?: latest
        val aeModes=(p.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_MODES) ?: intArrayOf())
        if (isManual || (bracketEv != 0 && spec.manualSensor)) {
            val iso=(manualIso ?: base?.get(CaptureResult.SENSOR_SENSITIVITY) ?: spec.minIso).coerceIn(spec.minIso,spec.maxIso)
            val time=((manualTime ?: base?.get(CaptureResult.SENSOR_EXPOSURE_TIME) ?: 16_666_667L) * 2.0.pow(bracketEv)).toLong().coerceIn(spec.minExposureNs,spec.maxExposureNs)
            set(CaptureRequest.CONTROL_AE_MODE,CaptureRequest.CONTROL_AE_MODE_OFF)
            set(CaptureRequest.SENSOR_SENSITIVITY,iso); set(CaptureRequest.SENSOR_EXPOSURE_TIME,time)
            val maxFrame=c.get(CameraCharacteristics.SENSOR_INFO_MAX_FRAME_DURATION) ?: max(time,33_333_333L)
            set(CaptureRequest.SENSOR_FRAME_DURATION,max(time,33_333_333L).coerceAtMost(maxFrame))
            set(CaptureRequest.FLASH_MODE,if (flash == FlashMode.TORCH && spec.hasFlash) CaptureRequest.FLASH_MODE_TORCH else CaptureRequest.FLASH_MODE_OFF)
        } else {
            val want=when {
                !spec.hasFlash -> CaptureRequest.CONTROL_AE_MODE_ON
                flash == FlashMode.AUTO -> CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH
                flash == FlashMode.ON -> CaptureRequest.CONTROL_AE_MODE_ON_ALWAYS_FLASH
                else -> CaptureRequest.CONTROL_AE_MODE_ON
            }
            set(CaptureRequest.CONTROL_AE_MODE,if (want in aeModes) want else CaptureRequest.CONTROL_AE_MODE_ON)
            set(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION,(evSteps+if (bracketEv != 0 && spec.evStep > 0) (bracketEv/spec.evStep).roundToInt() else 0).coerceIn(spec.minEv,spec.maxEv))
            set(CaptureRequest.FLASH_MODE,when {
                !spec.hasFlash -> CaptureRequest.FLASH_MODE_OFF
                flash == FlashMode.TORCH -> CaptureRequest.FLASH_MODE_TORCH
                !preview && flash == FlashMode.ON -> CaptureRequest.FLASH_MODE_SINGLE
                else -> CaptureRequest.FLASH_MODE_OFF
            })
        }
        if (kelvin > 0 && spec.manualWb) {
            set(CaptureRequest.CONTROL_AWB_MODE,CaptureRequest.CONTROL_AWB_MODE_OFF)
            set(CaptureRequest.COLOR_CORRECTION_MODE,CaptureRequest.COLOR_CORRECTION_MODE_TRANSFORM_MATRIX)
            set(CaptureRequest.COLOR_CORRECTION_GAINS,kelvinGains(kelvin))
            val identity=ColorSpaceTransform(arrayOf(Rational(1,1),Rational(0,1),Rational(0,1),Rational(0,1),Rational(1,1),Rational(0,1),Rational(0,1),Rational(0,1),Rational(1,1)))
            set(CaptureRequest.COLOR_CORRECTION_TRANSFORM,base?.get(CaptureResult.COLOR_CORRECTION_TRANSFORM) ?: identity)
        } else set(CaptureRequest.CONTROL_AWB_MODE,CaptureRequest.CONTROL_AWB_MODE_AUTO)
        // Crop once: preview is zoomed; still JPEG always uses the full stream, then software crop is applied once.
        c.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE)?.let {
            set(CaptureRequest.SCALER_CROP_REGION,CropEngine.computeCropRegion(it,if (preview) cropFactor else 1f))
        }
        if (Build.VERSION.SDK_INT >= 30 && CaptureRequest.CONTROL_ZOOM_RATIO in keys) set(CaptureRequest.CONTROL_ZOOM_RATIO,1f)
        if (!preview) {
            if(settings.format.writesRaw && CaptureRequest.STATISTICS_LENS_SHADING_MAP_MODE_ON in
                (c.get(CameraCharacteristics.STATISTICS_INFO_AVAILABLE_LENS_SHADING_MAP_MODES) ?: intArrayOf())) {
                set(CaptureRequest.STATISTICS_LENS_SHADING_MAP_MODE,CaptureRequest.STATISTICS_LENS_SHADING_MAP_MODE_ON)
            }
            set(CaptureRequest.JPEG_QUALITY,98.toByte())
            set(CaptureRequest.JPEG_ORIENTATION,jpegOrientation())
            if (useMaximumMode && Build.VERSION.SDK_INT >= 31) set(CaptureRequest.SENSOR_PIXEL_MODE,CaptureRequest.SENSOR_PIXEL_MODE_MAXIMUM_RESOLUTION)
            if (CaptureRequest.EDGE_MODE_HIGH_QUALITY in (c.get(CameraCharacteristics.EDGE_AVAILABLE_EDGE_MODES) ?: intArrayOf())) set(CaptureRequest.EDGE_MODE,CaptureRequest.EDGE_MODE_HIGH_QUALITY)
            if (CaptureRequest.NOISE_REDUCTION_MODE_HIGH_QUALITY in (c.get(CameraCharacteristics.NOISE_REDUCTION_AVAILABLE_NOISE_REDUCTION_MODES) ?: intArrayOf())) set(CaptureRequest.NOISE_REDUCTION_MODE,CaptureRequest.NOISE_REDUCTION_MODE_HIGH_QUALITY)
            if (CaptureRequest.COLOR_CORRECTION_ABERRATION_MODE_HIGH_QUALITY in (c.get(CameraCharacteristics.COLOR_CORRECTION_AVAILABLE_ABERRATION_MODES) ?: intArrayOf())) set(CaptureRequest.COLOR_CORRECTION_ABERRATION_MODE,CaptureRequest.COLOR_CORRECTION_ABERRATION_MODE_HIGH_QUALITY)
            if (CaptureRequest.DISTORTION_CORRECTION_MODE_HIGH_QUALITY in (c.get(CameraCharacteristics.DISTORTION_CORRECTION_AVAILABLE_MODES) ?: intArrayOf())) set(CaptureRequest.DISTORTION_CORRECTION_MODE,CaptureRequest.DISTORTION_CORRECTION_MODE_HIGH_QUALITY)
        }
    }
    private fun kelvinGains(k: Int): RggbChannelVector {
        // Approximate Kelvin WB, not a measured proprietary per-sensor calibration.
        val t=((k.coerceIn(2500,9900)-5600)/4300f)
        return RggbChannelVector((2f+t*1.2f).coerceIn(1f,4f),1f,1f,(2f-t*1.2f).coerceIn(1f,4f))
    }
    private fun updateRepeatingInternal() {
        val b=previewBuilder ?: return
        if (extensionSession != null) return
        runCatching { applySettings(b,true); session?.setRepeatingRequest(b.build(),previewCallback,handler) }
            .onFailure { _cameraError.value="Control rejected: ${it.message}" }
    }
    fun updateRepeatingRequest() { handler.post { updateRepeatingInternal() } }
    fun setFlashMode(mode: FlashMode) { handler.post { flash=mode; updateRepeatingInternal() } }
    fun updateFocusMode(mode: String) { handler.post { focusMode=mode; focusRegion=null; updateRepeatingInternal() } }
    fun updateEvCompensation(step: Int) { handler.post { evSteps=step; updateRepeatingInternal() } }
    fun updateManualIso(iso: Int?) { handler.post { manualIso=iso; updateRepeatingInternal() } }
    fun updateManualShutter(ns: Long?) { handler.post { manualTime=ns; updateRepeatingInternal() } }
    fun updateManualKelvin(value: Int) { handler.post { kelvin=value; updateRepeatingInternal() } }
    fun resetAuto() { handler.post { manualIso=null; manualTime=null; kelvin=0; focusMode="AF-C"; evSteps=0; focusRegion=null; updateRepeatingInternal() } }
    fun triggerFocusAndMetering(x: Float, y: Float, viewAspect: Float=0.75f) {
        handler.post {
            if (extensionSession != null) { _cameraError.value="Tap metering is controlled by the OEM extension in this mode"; return@post }
            val s=session ?: return@post; val b=previewBuilder ?: return@post
            runCatching {
                val c=chars(); val p=parentChars(); val array=c.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE) ?: return@runCatching
                val crop=CropEngine.computeCropRegion(array,cropFactor)
                val point=PreviewGeometry.mapToSensor(x,y,viewAspect,previewSize.width.toFloat()/previewSize.height,getSensorOrientation())
                val bw=(crop.width()*0.12f).roundToInt().coerceIn(1,crop.width())
                val bh=(crop.height()*0.12f).roundToInt().coerceIn(1,crop.height())
                val left=(crop.left+point.x*crop.width()-bw/2f).roundToInt().coerceIn(crop.left,crop.right-bw)
                val top=(crop.top+point.y*crop.height()-bh/2f).roundToInt().coerceIn(crop.top,crop.bottom-bh)
                focusRegion=MeteringRectangle(Rect(left,top,left+bw,top+bh),MeteringRectangle.METERING_WEIGHT_MAX)
                applySettings(b,true)
                val supportsAf=(p.get(CameraCharacteristics.CONTROL_MAX_REGIONS_AF) ?: 0)>0 && CaptureRequest.CONTROL_AF_MODE_AUTO in (c.get(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES) ?: intArrayOf())
                if (supportsAf) { b.set(CaptureRequest.CONTROL_AF_MODE,CaptureRequest.CONTROL_AF_MODE_AUTO); b.set(CaptureRequest.CONTROL_AF_TRIGGER,CaptureRequest.CONTROL_AF_TRIGGER_START) }
                s.capture(b.build(),previewCallback,handler)
                b.set(CaptureRequest.CONTROL_AF_TRIGGER,CaptureRequest.CONTROL_AF_TRIGGER_IDLE)
                s.setRepeatingRequest(b.build(),previewCallback,handler)
                val gen=generation
                handler.postDelayed({ if (gen == generation) updateRepeatingInternal() },3000)
            }.onFailure { _cameraError.value="Focus/metering request was not accepted" }
        }
    }
    private suspend fun prepare3A() {
        if(latestResult.value?.get(CaptureResult.CONTROL_AF_STATE) in listOf(CaptureResult.CONTROL_AF_STATE_ACTIVE_SCAN,CaptureResult.CONTROL_AF_STATE_PASSIVE_SCAN)) {
            _progress.value="Focusing…"
            withTimeoutOrNull(1000) { latestResult.filterNotNull().first {
                it.get(CaptureResult.CONTROL_AF_STATE) !in listOf(CaptureResult.CONTROL_AF_STATE_ACTIVE_SCAN,CaptureResult.CONTROL_AF_STATE_PASSIVE_SCAN)
            } }
        }
        if (flash !in listOf(FlashMode.AUTO,FlashMode.ON) || activeCameraSpec.value?.hasFlash != true || manualIso != null || manualTime != null) return
        val b=previewBuilder ?: return; val s=session ?: return
        _progress.value="Metering flash…"
        val before=latestResult.value?.get(CaptureResult.SENSOR_TIMESTAMP) ?: 0L
        b.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER,CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START)
        s.capture(b.build(),previewCallback,handler)
        b.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER,CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_IDLE)
        s.setRepeatingRequest(b.build(),previewCallback,handler)
        delay(120)
        withTimeoutOrNull(1800) {
            latestResult.filterNotNull().first {
                (it.get(CaptureResult.SENSOR_TIMESTAMP) ?: 0L)>before && it.get(CaptureResult.CONTROL_AE_STATE) in
                    listOf(CaptureResult.CONTROL_AE_STATE_CONVERGED,CaptureResult.CONTROL_AE_STATE_FLASH_REQUIRED,CaptureResult.CONTROL_AE_STATE_LOCKED)
            }
        }
    }
    suspend fun capturePhoto(factor: Float, allowHeavy: Boolean=true): Result<CaptureBundle> {
        var undelivered: CaptureBundle?=null
        try {
            val result=withContext(dispatcher) {
                captureMutex.withLock {
                    val spec=activeCameraSpec.value ?: return@withLock Result.failure(IllegalStateException("No camera selected"))
                    if(!_isCameraStreaming.value || device==null) return@withLock Result.failure(IllegalStateException("Camera not ready; no image was saved"))
                    try {
                        val frames=mutableListOf<CapturedJpeg>()
                        if(settings.format.writesRaw) {
                            check(spec.hasRawSupport && rawReader!=null) { "RAW_SENSOR is not ready on this camera; no JPEG-only fallback was saved" }
                            check(factor==1f) { "RAW capture uses the uncropped sensor stream. Select 1×." }
                            prepare3A(); _progress.value="Capturing ${settings.format.badge}…"
                            frames+=captureOne(spec,1f,0,wantRaw=true)
                        } else if(extensionSession!=null && Build.VERSION.SDK_INT>=31) {
                            _progress.value="Processing ${pipelineStatus.value}…"
                            frames+=captureOne(spec,factor,0,extension=true)
                        } else {
                            prepare3A()
                            val burst=settings.pipeline==CapturePipeline.BURST && allowHeavy && flash==FlashMode.OFF && manualIso==null && manualTime==null
                            if(burst) {
                                for((i,ev) in listOf(-1,0,1).withIndex()) {
                                    _progress.value="Capturing frame ${i+1}/3…"
                                    if(!spec.manualSensor) {
                                        val old=evSteps
                                        try {
                                            evSteps=(old+if(spec.evStep>0) (ev/spec.evStep).roundToInt() else 0).coerceIn(spec.minEv,spec.maxEv)
                                            updateRepeatingInternal(); delay(220); frames+=captureOne(spec,factor,0)
                                        } finally { evSteps=old; updateRepeatingInternal() }
                                    } else frames+=captureOne(spec,factor,ev,bracket=true)
                                }
                            } else {
                                _progress.value="Capturing…"
                                val zsl=if(settings.zsl && allowHeavy && flash==FlashMode.OFF && manualIso==null && manualTime==null && factor==1f) selectZsl() else null
                                frames+=captureOne(spec,factor,0,zsl=zsl)
                            }
                        }
                        val bundle=CaptureBundle(frames,factor,if(frames.size==3) "Experimental 3-frame fusion" else frames.first().metadata.pipeline)
                        undelivered=bundle
                        Result.success(bundle)
                    } catch(e: TimeoutCancellationException) {
                        Result.failure(IllegalStateException("Capture timed out waiting for matching image/metadata; no complete photo pair was saved"))
                    } catch(e: CancellationException) { throw e }
                    catch(e: Exception) { Result.failure(e) }
                    finally {
                        _progress.value=""
                        pending.toList().forEach { if(!it.deferred.isCompleted) it.deferred.cancel(); it.rawImage?.close(); it.rawImage=null }
                        pending.clear(); unmatchedJpegs.clear(); clearRawInbox(); extensionShot=null
                        runCatching { updateRepeatingInternal() }
                    }
                }
            }
            undelivered=null // Ownership is transferred only after cancellable dispatcher delivery succeeds.
            return result
        } finally { undelivered?.close() }
    }
    private fun selectZsl(): ZslFrame? {
        val now=android.os.SystemClock.elapsedRealtimeNanos()
        val selected=zslRing.lastOrNull {
            val state=it.result.get(CaptureResult.CONTROL_AF_STATE)
            val ae=it.result.get(CaptureResult.CONTROL_AE_STATE)
            now-it.receivedAt in 0L..500_000_000L && state !in listOf(CaptureResult.CONTROL_AF_STATE_ACTIVE_SCAN,CaptureResult.CONTROL_AF_STATE_PASSIVE_SCAN,CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED) &&
                ae in listOf(CaptureResult.CONTROL_AE_STATE_CONVERGED,CaptureResult.CONTROL_AE_STATE_LOCKED)
        } ?: return null
        zslRing.remove(selected); return selected
    }
    private suspend fun captureOne(spec: CameraSpec, factor: Float, ev: Int, extension: Boolean=false, zsl: ZslFrame?=null, bracket: Boolean=false, wantRaw: Boolean=false): CapturedJpeg {
        val camera=device ?: throw IllegalStateException("Camera closed")
        val wantJpeg=!wantRaw || settings.format.writesJpeg
        val reader=if(wantJpeg) jpegReader ?: throw IllegalStateException("JPEG stream closed") else null
        val label=when { extension -> pipelineStatus.value; zsl != null -> "Camera2 PRIVATE ZSL reprocess"; bracket -> "Bracket ${ev}EV"; wantRaw -> if(wantJpeg) "Camera2 native RAW_SENSOR + JPEG" else "Camera2 native RAW_SENSOR"; else -> "Camera2 native JPEG" }
        val shot=Shot(spec,factor,label,sourceResult=zsl?.result,sourceSize=zsl?.image?.let { it.width to it.height },manual=manualIso != null || manualTime != null,manualWb=kelvin>0,
            wantRaw=wantRaw,wantJpeg=wantJpeg,bytes=if(wantJpeg) null else ByteArray(0),
            rawCharacteristics=if(wantRaw) chars() else null,orientation=jpegOrientation())
        pending+=shot
        val b=if (zsl != null) camera.createReprocessCaptureRequest(zsl.result) else camera.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
        reader?.let { b.addTarget(it.surface) }; if(wantRaw) b.addTarget(rawReader?.surface ?: error("RAW stream closed")); b.setTag(shot)
        if (extension) {
            b.set(CaptureRequest.JPEG_ORIENTATION,jpegOrientation())
            extensionShot=shot
            if (Build.VERSION.SDK_INT >= 31) extensionSession!!.capture(b.build(),executor,object : CameraExtensionSession.ExtensionCaptureCallback() {
                override fun onCaptureStarted(s: CameraExtensionSession,r: CaptureRequest,timestamp: Long) { shot.timestamp=timestamp }
                override fun onCaptureResultAvailable(s: CameraExtensionSession,r: CaptureRequest,result: TotalCaptureResult) { shot.result=result; completeShot(shot,true) }
                override fun onCaptureSequenceCompleted(s: CameraExtensionSession,sequenceId: Int) { shot.extensionComplete=true; completeShot(shot,true) }
                override fun onCaptureFailed(s: CameraExtensionSession,r: CaptureRequest) { shot.deferred.completeExceptionally(IllegalStateException("OEM capture failed; select Camera2 natural to retry")) }
            })
        } else {
            if (zsl != null) {
                b.set(CaptureRequest.JPEG_QUALITY,98.toByte()); b.set(CaptureRequest.JPEG_ORIENTATION,jpegOrientation())
                try { writer!!.queueInputImage(zsl.image) } catch (e: Exception) { zsl.image.close(); throw e }
            } else {
                applySettings(b,false,ev)
                if (bracket && spec.manualSensor && ev == 0) {
                    val baseline=latestResult.value
                    b.set(CaptureRequest.CONTROL_AE_MODE,CaptureRequest.CONTROL_AE_MODE_OFF)
                    b.set(CaptureRequest.SENSOR_SENSITIVITY,(baseline?.get(CaptureResult.SENSOR_SENSITIVITY) ?: spec.minIso).coerceIn(spec.minIso,spec.maxIso))
                    b.set(CaptureRequest.SENSOR_EXPOSURE_TIME,(baseline?.get(CaptureResult.SENSOR_EXPOSURE_TIME) ?: 16_666_667L).coerceIn(spec.minExposureNs,spec.maxExposureNs))
                }
            }
            session!!.capture(b.build(),object : CameraCaptureSession.CaptureCallback() {
                override fun onCaptureStarted(s: CameraCaptureSession,r: CaptureRequest,timestamp: Long,frame: Long) {
                    shot.timestamp=timestamp; attachBuffered(shot); completeShot(shot)
                }
                override fun onCaptureCompleted(s: CameraCaptureSession,r: CaptureRequest,result: TotalCaptureResult) {
                    shot.result=shot.sourceResult ?: result
                    if (shot.timestamp < 0) shot.timestamp=result.get(CaptureResult.SENSOR_TIMESTAMP) ?: -1
                    attachBuffered(shot)
                    completeShot(shot)
                }
                override fun onCaptureFailed(s: CameraCaptureSession,r: CaptureRequest,failure: CaptureFailure) { shot.deferred.completeExceptionally(IllegalStateException("Still capture failed (reason ${failure.reason})")) }
                override fun onCaptureSequenceAborted(s: CameraCaptureSession,id: Int) { shot.deferred.completeExceptionally(IllegalStateException("Capture interrupted")) }
            },handler)
        }
        var delivered=false
        try {
            val captured=withTimeout(if(extension) 60_000L else 15_000L) { shot.deferred.await() }
            delivered=true; return captured
        } finally {
            pending.remove(shot); shot.rawImage?.close(); shot.rawImage=null
            if(!delivered) shot.completedRaw?.close()
            if(extensionShot===shot) extensionShot=null
        }
    }
    private fun readJpeg(r: ImageReader) {
        try {
            while (true) {
                val im=r.acquireNextImage() ?: break
                val ts=im.timestamp
                val bytes=try { val buffer=im.planes[0].buffer; ByteArray(buffer.remaining()).also { buffer.get(it) } } finally { im.close() }
                val ext=extensionShot
                if (ext != null) { ext.bytes=bytes; completeShot(ext,true); continue }
                val shot=pending.firstOrNull { !it.deferred.isCompleted && ts in timestamps(it) }
                if (shot != null) { shot.bytes=bytes; shot.jpegTimestamp=ts; completeShot(shot) }
                else if (pending.isNotEmpty()) { unmatchedJpegs[ts]=bytes; while(unmatchedJpegs.size>3) unmatchedJpegs.remove(unmatchedJpegs.keys.first()) }
            }
        } catch (e: Exception) { pending.forEach { it.deferred.completeExceptionally(e) } }
    }
    private fun rawResult(s: Shot): CaptureResult? = s.spec.physicalOutputId?.let { s.result?.physicalCameraResults?.get(it) } ?: s.result
    private fun timestamps(s: Shot): Set<Long> = setOfNotNull(s.timestamp.takeIf { it>=0 },
        s.result?.get(CaptureResult.SENSOR_TIMESTAMP),rawResult(s)?.get(CaptureResult.SENSOR_TIMESTAMP))
    private fun attachBuffered(s: Shot) {
        for(ts in timestamps(s)) {
            if(s.bytes==null) unmatchedJpegs.remove(ts)?.let { s.bytes=it; s.jpegTimestamp=ts }
            if(s.wantRaw && s.rawImage==null) s.rawImage=unmatchedRaw.remove(ts)
        }
    }
    private fun clearRawInbox() { unmatchedRaw.values.forEach { it.close() }; unmatchedRaw.clear() }
    private fun readRaw(reader: ImageReader,lifetime: RetainedResource) {
        try {
            while(true) {
                val image=reader.acquireNextImage() ?: break
                if(pending.none { it.wantRaw && !it.deferred.isCompleted }) { image.close(); continue }
                val lease=try { lifetime.retain(image) } catch(e: Exception) { image.close(); throw e }
                val ts=image.timestamp
                val shot=pending.firstOrNull { it.wantRaw && !it.deferred.isCompleted && ts in timestamps(it) }
                if(shot!=null) {
                    shot.rawImage?.close(); shot.rawImage=lease; completeShot(shot)
                } else {
                    unmatchedRaw.put(ts,lease)?.close()
                    while(unmatchedRaw.size>2) unmatchedRaw.remove(unmatchedRaw.keys.first())?.close()
                }
            }
        } catch(e: Exception) { pending.filter { it.wantRaw }.forEach { it.deferred.completeExceptionally(e) } }
    }
    private fun completeShot(s: Shot, extension: Boolean=false) {
        val bytes=s.bytes ?: return
        if((!extension && s.result==null) || (extension && s.result==null && !s.extensionComplete) || s.deferred.isCompleted) return
        if(s.wantRaw && s.rawImage==null) return
        var raw: RawCapture?=null
        try {
            if(s.wantRaw) {
                val image=requireNotNull(s.rawImage)
                val result=if(s.spec.physicalOutputId!=null) {
                    s.result?.physicalCameraResults?.get(s.spec.physicalOutputId)
                        ?: error("Firmware returned no physical RAW capture metadata; no substitute DNG was written")
                } else requireNotNull(s.result)
                check(SensorFrameMatch.matches(image.value.timestamp,result.get(CaptureResult.SENSOR_TIMESTAMP),s.jpegTimestamp.takeIf { it>=0 },s.wantJpeg)) {
                    "RAW, JPEG and sensor metadata are not from the same frame"
                }
                raw=RawCapture(image,requireNotNull(s.rawCharacteristics),result,s.orientation,s.spec.cameraId)
            }
            val capture=CapturedJpeg(bytes,metadata(s,bytes),raw)
            if(s.deferred.complete(capture)) { s.completedRaw=raw; s.rawImage=null; s.bytes=null }
            else raw?.close()
        } catch(e: Exception) { raw?.close(); s.deferred.completeExceptionally(e) }
    }
    private fun metadata(s: Shot, bytes: ByteArray): PhotoExifMetadata {
        val r=s.result
        val physical=if (s.spec.physicalOutputId != null) r?.physicalCameraResults?.get(s.spec.physicalOutputId) else null
        fun <T> value(k: CaptureResult.Key<T>): T? = physical?.get(k) ?: r?.get(k)
        val exif=if(bytes.isNotEmpty()) runCatching { ExifInterface(ByteArrayInputStream(bytes)) }.getOrNull() else null
        val aperture=value(CaptureResult.LENS_APERTURE) ?: exif?.getAttributeDouble(ExifInterface.TAG_F_NUMBER,0.0)?.takeIf { it>0 }?.toFloat() ?: s.spec.apertureFNumber.takeIf { it>0 }
        val focal=value(CaptureResult.LENS_FOCAL_LENGTH) ?: exif?.getAttributeDouble(ExifInterface.TAG_FOCAL_LENGTH,0.0)?.takeIf { it>0 }?.toFloat() ?: s.spec.focalLengthMm.takeIf { it>0 }
        val model=exif?.getAttribute(ExifInterface.TAG_MODEL) ?: Build.MODEL
        val cameraName=if(model.startsWith(Build.MANUFACTURER,true)) model else "${Build.MANUFACTURER} $model"
        val lens=when(s.spec.lensType) { LensType.ULTRA_WIDE -> "ultra wide camera"; LensType.WIDE -> "wide camera"; LensType.TELEPHOTO -> "telephoto camera"; LensType.PERISCOPE -> "periscope camera"; LensType.FRONT -> "front camera" }
        val baseEq=s.spec.equivalentFocalLength35mm
        val flashState=value(CaptureResult.FLASH_STATE)
        val flashTag=if(flashState == CaptureResult.FLASH_STATE_FIRED || flashState == CaptureResult.FLASH_STATE_PARTIAL) 1
            else exif?.getAttributeInt(ExifInterface.TAG_FLASH,-1)?.takeIf { it>=0 } ?: if(flashState!=null) 16 else null
        return PhotoExifMetadata(
            iso=value(CaptureResult.SENSOR_SENSITIVITY) ?: exif?.getAttributeInt(ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY,0)?.takeIf { it>0 },
            exposureTimeNs=value(CaptureResult.SENSOR_EXPOSURE_TIME) ?: exif?.getAttributeDouble(ExifInterface.TAG_EXPOSURE_TIME,0.0)?.takeIf { it>0 }?.let { (it*1e9).toLong() },
            fNumber=aperture, focalLengthMm=focal, equivalent35mm=(baseEq*s.crop).roundToInt().takeIf { it>0 },
            evBias=value(CaptureResult.CONTROL_AE_EXPOSURE_COMPENSATION)?.let { it*s.spec.evStep },
            cameraMake=exif?.getAttribute(ExifInterface.TAG_MAKE) ?: Build.MANUFACTURER,
            cameraModel=exif?.getAttribute(ExifInterface.TAG_MODEL) ?: Build.MODEL,
            lensDescription=lens,
            lensModel="$cameraName $lens ${baseEq}mm${if (aperture != null) String.format(Locale.US," f/%.1f",aperture) else ""}",
            flashFired=flashTag?.let { it and 1 != 0 }, flashExif=flashTag, lensType=s.spec.lensType,
            pipeline=s.label, sourceWidth=if(!s.wantJpeg) s.rawImage?.value?.width ?: 0 else s.sourceSize?.first ?: exif?.getAttributeInt(ExifInterface.TAG_IMAGE_WIDTH,0) ?: 0,
            sourceHeight=if(!s.wantJpeg) s.rawImage?.value?.height ?: 0 else s.sourceSize?.second ?: exif?.getAttributeInt(ExifInterface.TAG_IMAGE_LENGTH,0) ?: 0,
            note=s.sourceSize?.let { "PRIVATE reprocess input ${it.first}x${it.second}; JPEG output ${captureSize.value}" }.orEmpty(),
            manualExposure=s.manual,manualWhiteBalance=s.manualWb
        )
    }
    fun diagnosticSummary(): String = "Pipeline: ${pipelineStatus.value}\nSelected JPEG stream: ${captureSize.value}\nCapture files: ${settings.format.label}; RAW_SENSOR stream: ${rawCaptureSize.value}\nCrop factor: $cropFactor (software once)\nZSL ring: ${zslRing.size}/3; PRIVATE input: ${privateReader?.let { "${it.width}x${it.height}" } ?: "none"}\nNo synthetic captures; actual native sizes only."
}

package com.mobilefilmlab.camera.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import android.util.Size
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mobilefilmlab.camera.camera.CameraDiagnosticsReport
import com.mobilefilmlab.camera.camera.CameraEngine
import com.mobilefilmlab.camera.camera.FlashMode
import com.mobilefilmlab.camera.data.model.FramingConfig
import com.mobilefilmlab.camera.data.model.LensType
import com.mobilefilmlab.camera.data.model.Recipe
import com.mobilefilmlab.camera.data.repository.RecipeRepository
import com.mobilefilmlab.camera.pipeline.ImageProcessor
import com.mobilefilmlab.camera.pipeline.MediaStoreSaver
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class CameraMode {
    AUTO,
    PRO
}

class CameraViewModel(application: Application) : AndroidViewModel(application) {
    val repository = RecipeRepository(application)
    val cameraEngine = CameraEngine(application)

    // Mode: Auto vs Pro
    private val _mode = MutableStateFlow(CameraMode.AUTO)
    val mode: StateFlow<CameraMode> = _mode.asStateFlow()

    // Flash / Torch
    private val _flashMode = MutableStateFlow(FlashMode.OFF)
    val flashMode: StateFlow<FlashMode> = _flashMode.asStateFlow()

    // Active Lens & Framing
    private val _activeLens = MutableStateFlow(LensType.WIDE)
    val activeLens: StateFlow<LensType> = _activeLens.asStateFlow()

    private val _is2xActive = MutableStateFlow(false)
    val is2xActive: StateFlow<Boolean> = _is2xActive.asStateFlow()

    private val _framingConfig = MutableStateFlow(FramingConfig.create(LensType.WIDE, false, cameraEngine.activeCameraSpec.value))
    val framingConfig: StateFlow<FramingConfig> = _framingConfig.asStateFlow()

    // Active Recipe & 2D Pad
    val activeRecipe = repository.activeRecipe
    val allRecipes = repository.recipes

    private val _puckTone = MutableStateFlow(0f)   // -100 .. +100
    val puckTone: StateFlow<Float> = _puckTone.asStateFlow()

    private val _puckColor = MutableStateFlow(0f)  // -100 .. +100
    val puckColor: StateFlow<Float> = _puckColor.asStateFlow()

    // Pro Controls
    private val _evCompensation = MutableStateFlow(0.0f) // -2.0 .. +2.0
    val evCompensation: StateFlow<Float> = _evCompensation.asStateFlow()

    private val _iso = MutableStateFlow("AUTO")
    val iso: StateFlow<String> = _iso.asStateFlow()

    private val _shutterSpeed = MutableStateFlow("AUTO")
    val shutterSpeed: StateFlow<String> = _shutterSpeed.asStateFlow()

    private val _whiteBalanceKelvin = MutableStateFlow(5600)
    val whiteBalanceKelvin: StateFlow<Int> = _whiteBalanceKelvin.asStateFlow()

    private val _focusMode = MutableStateFlow("AF-C")
    val focusMode: StateFlow<String> = _focusMode.asStateFlow()

    // UI Dialog States
    val isRecipeSheetOpen = MutableStateFlow(false)
    val isDiagnosticsOpen = MutableStateFlow(false)
    val isAboutOpen = MutableStateFlow(false)

    // Capture state & feedback
    val isCapturing = MutableStateFlow(false)
    val isShutterFlashing = MutableStateFlow(false)
    val lastCapturedUri = MutableStateFlow<Uri?>(null)
    val lastCapturedThumbnail = MutableStateFlow<Bitmap?>(null)
    val userNotice = MutableStateFlow<String?>(null)

    init {
        viewModelScope.launch {
            cameraEngine.cameraError.collect { err ->
                if (err != null) {
                    userNotice.value = err
                }
            }
        }
    }

    fun onPermissionGranted() {
        cameraEngine.refreshCameraList()
        cameraEngine.openCamera()
    }

    fun setMode(newMode: CameraMode) {
        _mode.value = newMode
    }

    fun toggleFlash() {
        val next = when (_flashMode.value) {
            FlashMode.OFF -> FlashMode.AUTO
            FlashMode.AUTO -> FlashMode.TORCH
            FlashMode.TORCH -> FlashMode.OFF
        }
        _flashMode.value = next
        cameraEngine.setFlashMode(next)
    }

    fun selectLens(lens: LensType) {
        _activeLens.value = lens
        cameraEngine.selectLens(lens, _is2xActive.value)
        updateFraming()
    }

    fun toggle2xCrop() {
        _is2xActive.value = !_is2xActive.value
        cameraEngine.selectLens(_activeLens.value, _is2xActive.value)
        updateFraming()
    }

    private fun updateFraming() {
        _framingConfig.value = FramingConfig.create(
            _activeLens.value,
            _is2xActive.value,
            cameraEngine.activeCameraSpec.value
        )
    }

    fun cycleEvCompensation() {
        val steps = listOf(-2.0f, -1.0f, -0.3f, 0.0f, +0.3f, +1.0f, +2.0f)
        val currentIdx = steps.indexOfFirst { kotlin.math.abs(it - _evCompensation.value) < 0.15f }
        val nextIdx = (if (currentIdx >= 0) currentIdx + 1 else 0) % steps.size
        val nextEv = steps[nextIdx]
        _evCompensation.value = nextEv
        cameraEngine.updateEvCompensation((nextEv / 0.333f).toInt())
    }

    fun cycleIso() {
        val options = listOf("AUTO", "50", "100", "200", "400", "800", "1600", "3200")
        val currentIdx = options.indexOf(_iso.value).coerceAtLeast(0)
        val next = options[(currentIdx + 1) % options.size]
        _iso.value = next
        cameraEngine.updateManualIso(next.toIntOrNull())
    }

    fun cycleShutter() {
        val options = listOf("AUTO", "1/4000", "1/2000", "1/1000", "1/500", "1/250", "1/125", "1/60", "1/30")
        val currentIdx = options.indexOf(_shutterSpeed.value).coerceAtLeast(0)
        val next = options[(currentIdx + 1) % options.size]
        _shutterSpeed.value = next

        val ns = when (next) {
            "1/4000" -> 250_000L
            "1/2000" -> 500_000L
            "1/1000" -> 1_000_000L
            "1/500" -> 2_000_000L
            "1/250" -> 4_000_000L
            "1/125" -> 8_000_000L
            "1/60" -> 16_666_667L
            "1/30" -> 33_333_333L
            else -> null
        }
        cameraEngine.updateManualShutter(ns)
    }

    fun cycleWhiteBalance() {
        val options = listOf(3200, 4000, 5600, 6500, 7500)
        val currentIdx = options.indexOf(_whiteBalanceKelvin.value).coerceAtLeast(0)
        val next = options[(currentIdx + 1) % options.size]
        _whiteBalanceKelvin.value = next
        cameraEngine.updateManualKelvin(next)
    }

    fun cycleFocus() {
        val options = listOf("AF-C", "AF-S", "MACRO")
        val currentIdx = options.indexOf(_focusMode.value).coerceAtLeast(0)
        val next = options[(currentIdx + 1) % options.size]
        _focusMode.value = next
        cameraEngine.updateFocusMode(next)
    }

    fun selectRecipe(recipe: Recipe) {
        repository.setActiveRecipe(recipe)
        _puckTone.value = recipe.puckTone
        _puckColor.value = recipe.puckColor
    }

    fun updatePuck(tone: Float, color: Float) {
        _puckTone.value = tone.coerceIn(-100f, 100f)
        _puckColor.value = color.coerceIn(-100f, 100f)
    }

    fun resetPuck() {
        _puckTone.value = 0f
        _puckColor.value = 0f
    }

    fun saveAsCustomRecipe(name: String) {
        val base = activeRecipe.value
        val custom = Recipe.createCustom(
            name = name,
            baseRecipe = base,
            puckTone = _puckTone.value,
            puckColor = _puckColor.value
        )
        repository.saveCustomRecipe(custom)
        userNotice.value = "Saved \"${custom.name}\" to custom recipes"
    }

    fun toggleFavourite(recipe: Recipe) {
        repository.toggleFavourite(recipe)
    }

    fun triggerShutter() {
        if (isCapturing.value) return
        isCapturing.value = true

        viewModelScope.launch {
            try {
                // Visual shutter flash feedback
                isShutterFlashing.value = true
                launch {
                    delay(80)
                    isShutterFlashing.value = false
                }

                val cropFactor = if (_is2xActive.value) 2.0f else 1.0f
                val captureResult = cameraEngine.capturePhoto(cropFactor)

                captureResult.onSuccess { (bitmap, cropRect) ->
                    val tunedRecipe = activeRecipe.value.copy(
                        puckTone = _puckTone.value,
                        puckColor = _puckColor.value
                    )

                    val processed = ImageProcessor.processBitmap(bitmap, tunedRecipe, cropRect)
                    val jpegBytes = ImageProcessor.encodeJpegWithExif(processed, tunedRecipe)

                    val saveResult = MediaStoreSaver.savePhoto(
                        getApplication(),
                        jpegBytes,
                        tunedRecipe.name
                    )

                    saveResult.onSuccess { uri ->
                        lastCapturedUri.value = uri
                        userNotice.value = "Captured with ${tunedRecipe.name}"
                        loadThumbnail(uri)
                    }.onFailure { err ->
                        userNotice.value = "Failed to save photo: ${err.message}"
                    }
                }.onFailure { err ->
                    userNotice.value = "Capture failed: ${err.message}"
                }
            } finally {
                isCapturing.value = false
            }
        }
    }

    private fun loadThumbnail(uri: Uri) {
        try {
            val resolver = getApplication<Application>().contentResolver
            val thumb = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                resolver.loadThumbnail(uri, Size(120, 120), null)
            } else {
                null
            }
            lastCapturedThumbnail.value = thumb
        } catch (e: Exception) {
            // ignore thumbnail load failure
        }
    }

    fun getDiagnosticsReport(): String {
        return CameraDiagnosticsReport.generateReport(cameraEngine.cameraSpecs.value)
    }
}

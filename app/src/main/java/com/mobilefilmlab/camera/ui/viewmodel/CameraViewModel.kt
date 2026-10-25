package com.mobilefilmlab.camera.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mobilefilmlab.camera.camera.CameraDiagnosticsReport
import com.mobilefilmlab.camera.camera.CameraEngine
import com.mobilefilmlab.camera.data.model.CameraSpec
import com.mobilefilmlab.camera.data.model.FramingConfig
import com.mobilefilmlab.camera.data.model.LensType
import com.mobilefilmlab.camera.data.model.Recipe
import com.mobilefilmlab.camera.data.model.RecipeCatalog
import com.mobilefilmlab.camera.data.repository.RecipeRepository
import com.mobilefilmlab.camera.pipeline.ImageProcessor
import com.mobilefilmlab.camera.pipeline.MediaStoreSaver
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

    // UI Dialog States
    val isRecipeSheetOpen = MutableStateFlow(false)
    val isDiagnosticsOpen = MutableStateFlow(false)
    val isAboutOpen = MutableStateFlow(false)

    // Capture state & feedback
    val isCapturing = MutableStateFlow(false)
    val lastCapturedUri = MutableStateFlow<Uri?>(null)
    val userNotice = MutableStateFlow<String?>(null)

    fun setMode(newMode: CameraMode) {
        _mode.value = newMode
    }

    fun selectLens(lens: LensType) {
        _activeLens.value = lens
        cameraEngine.selectLens(lens)
        updateFraming()
    }

    fun toggle2xCrop() {
        _is2xActive.value = !_is2xActive.value
        updateFraming()
    }

    private fun updateFraming() {
        _framingConfig.value = FramingConfig.create(
            _activeLens.value,
            _is2xActive.value,
            cameraEngine.activeCameraSpec.value
        )
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

    fun getDiagnosticsReport(): String {
        return CameraDiagnosticsReport.generateReport(cameraEngine.cameraSpecs.value)
    }
}

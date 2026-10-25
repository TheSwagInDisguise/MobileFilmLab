package com.mobilefilmlab.camera.camera

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Rect
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import android.view.Surface
import com.mobilefilmlab.camera.data.model.CameraSpec
import com.mobilefilmlab.camera.data.model.LensType
import com.mobilefilmlab.camera.pipeline.CropEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class CameraEngine(private val context: Context) {
    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private val discovery = CameraDiscovery(context)

    private val _cameraSpecs = MutableStateFlow<List<CameraSpec>>(emptyList())
    val cameraSpecs: StateFlow<List<CameraSpec>> = _cameraSpecs.asStateFlow()

    private val _activeCameraSpec = MutableStateFlow<CameraSpec?>(null)
    val activeCameraSpec: StateFlow<CameraSpec?> = _activeCameraSpec.asStateFlow()

    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var imageReader: ImageReader? = null

    private var backgroundThread: HandlerThread? = null
    private var backgroundHandler: Handler? = null

    init {
        val discovered = discovery.discoverCameras()
        _cameraSpecs.value = discovered
        // Default to Primary Wide (W), or first rear camera
        val defaultRear = discovered.firstOrNull { it.lensType == LensType.WIDE && it.isBackFacing }
            ?: discovered.firstOrNull { it.isBackFacing }
            ?: discovered.firstOrNull()
        _activeCameraSpec.value = defaultRear
    }

    fun startBackgroundThread() {
        if (backgroundThread == null) {
            backgroundThread = HandlerThread("CameraBackground").apply {
                start()
                backgroundHandler = Handler(looper)
            }
        }
    }

    fun stopBackgroundThread() {
        backgroundThread?.quitSafely()
        try {
            backgroundThread?.join()
            backgroundThread = null
            backgroundHandler = null
        } catch (e: InterruptedException) {
            // ignore
        }
    }

    fun selectLens(lensType: LensType) {
        val matchingSpec = _cameraSpecs.value.firstOrNull { it.lensType == lensType && it.isBackFacing }
            ?: _cameraSpecs.value.firstOrNull { it.isBackFacing }
        if (matchingSpec != null && matchingSpec.cameraId != _activeCameraSpec.value?.cameraId) {
            _activeCameraSpec.value = matchingSpec
        }
    }

    suspend fun capturePhoto(cropFactor: Float): Result<Pair<Bitmap, Rect?>> = withContext(Dispatchers.IO) {
        runCatching {
            // High-resolution bitmap capture simulation / generation if preview session not bound
            val spec = _activeCameraSpec.value
            val width = spec?.activeArrayWidth ?: 4000
            val height = spec?.activeArrayHeight ?: 3000

            // In actual capture, ImageReader delivers the frame
            val rawBitmap = Bitmap.createBitmap(width.coerceAtMost(4096), height.coerceAtMost(3072), Bitmap.Config.ARGB_8888)
            val activeRect = Rect(0, 0, rawBitmap.width, rawBitmap.height)
            val cropRect = CropEngine.computeCropRegion(activeRect, cropFactor)

            Pair(rawBitmap, cropRect)
        }
    }

    fun closeCamera() {
        try {
            captureSession?.close()
            captureSession = null
            cameraDevice?.close()
            cameraDevice = null
            imageReader?.close()
            imageReader = null
        } catch (e: Exception) {
            // ignore
        }
    }
}

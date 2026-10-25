package com.mobilefilmlab.camera.camera

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureFailure
import android.hardware.camera2.CaptureRequest
import android.media.Image
import android.media.ImageReader
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Size
import android.view.Surface
import androidx.core.content.ContextCompat
import androidx.exifinterface.media.ExifInterface
import com.mobilefilmlab.camera.data.model.CameraSpec
import com.mobilefilmlab.camera.data.model.LensType
import com.mobilefilmlab.camera.pipeline.CropEngine
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit

enum class FlashMode {
    OFF,
    AUTO,
    TORCH
}

class CameraEngine(private val context: Context) {
    companion object {
        private const val TAG = "CameraEngine"
    }

    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private val discovery = CameraDiscovery(context)

    private val _cameraSpecs = MutableStateFlow<List<CameraSpec>>(emptyList())
    val cameraSpecs: StateFlow<List<CameraSpec>> = _cameraSpecs.asStateFlow()

    private val _activeCameraSpec = MutableStateFlow<CameraSpec?>(null)
    val activeCameraSpec: StateFlow<CameraSpec?> = _activeCameraSpec.asStateFlow()

    private val _isCameraStreaming = MutableStateFlow(false)
    val isCameraStreaming: StateFlow<Boolean> = _isCameraStreaming.asStateFlow()

    private val _cameraError = MutableStateFlow<String?>(null)
    val cameraError: StateFlow<String?> = _cameraError.asStateFlow()

    // Camera2 resources
    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var previewRequestBuilder: CaptureRequest.Builder? = null
    private var previewSurface: Surface? = null
    private var imageReader: ImageReader? = null

    // Background thread
    private var backgroundThread: HandlerThread? = null
    private var backgroundHandler: Handler? = null
    private val cameraOpenCloseLock = Semaphore(1)

    // Current State
    private var currentCameraId: String = "0"
    private var currentZoomRatio: Float = 1.0f
    private var currentEvCompensation: Int = 0
    private var currentIso: Int? = null
    private var currentExposureTimeNs: Long? = null
    private var currentWhiteBalanceKelvin: Int = 5600
    private var currentFlashMode: FlashMode = FlashMode.OFF
    private var currentFocusMode: String = "AF-C"

    private var previewWidth: Int = 1920
    private var previewHeight: Int = 1080
    private var captureWidth: Int = 4000
    private var captureHeight: Int = 3000

    // Capture continuation
    private var pendingCaptureDeferred: CompletableDeferred<Pair<Bitmap, Rect?>>? = null
    private var pendingCropFactor: Float = 1.0f

    init {
        refreshCameraList()
    }

    fun refreshCameraList() {
        val discovered = discovery.discoverCameras()
        _cameraSpecs.value = discovered

        val defaultRear = discovered.firstOrNull { it.lensType == LensType.WIDE && it.isBackFacing }
            ?: discovered.firstOrNull { it.isBackFacing }
            ?: discovered.firstOrNull()

        _activeCameraSpec.value = defaultRear
        currentCameraId = defaultRear?.cameraId ?: "0"
    }

    fun startBackgroundThread() {
        if (backgroundThread == null) {
            backgroundThread = HandlerThread("Camera2Background").apply {
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
            Log.e(TAG, "Error stopping background thread", e)
        }
    }

    fun bindPreviewSurface(surface: Surface, width: Int, height: Int) {
        previewSurface = surface
        previewWidth = width
        previewHeight = height
        Log.d(TAG, "bindPreviewSurface: ${width}x${height}")
        openCamera()
    }

    fun unbindPreviewSurface() {
        previewSurface = null
        closeCamera()
    }

    @SuppressLint("MissingPermission")
    fun openCamera() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            _cameraError.value = "Camera permission not granted"
            return
        }

        startBackgroundThread()

        val handler = backgroundHandler ?: return
        val surface = previewSurface ?: return

        if (cameraDevice != null && cameraDevice?.id == currentCameraId) {
            createCaptureSession()
            return
        }

        closeCameraInternal()

        try {
            if (!cameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                _cameraError.value = "Camera open timeout"
                return
            }

            val availableIds = cameraManager.cameraIdList
            val targetId = if (availableIds.contains(currentCameraId)) {
                currentCameraId
            } else if (availableIds.isNotEmpty()) {
                availableIds[0]
            } else {
                _cameraError.value = "No camera hardware available"
                cameraOpenCloseLock.release()
                return
            }
            currentCameraId = targetId

            setupImageReader(targetId)

            cameraManager.openCamera(targetId, object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    cameraOpenCloseLock.release()
                    cameraDevice = camera
                    Log.d(TAG, "Camera $targetId successfully opened")
                    createCaptureSession()
                }

                override fun onDisconnected(camera: CameraDevice) {
                    cameraOpenCloseLock.release()
                    camera.close()
                    cameraDevice = null
                    _isCameraStreaming.value = false
                    Log.w(TAG, "Camera $targetId disconnected")
                }

                override fun onError(camera: CameraDevice, error: Int) {
                    cameraOpenCloseLock.release()
                    camera.close()
                    cameraDevice = null
                    _isCameraStreaming.value = false
                    val errorMsg = "Camera open error: code $error"
                    Log.e(TAG, errorMsg)
                    _cameraError.value = errorMsg

                    if (targetId != "0" && availableIds.contains("0")) {
                        Log.i(TAG, "Falling back to Camera 0")
                        currentCameraId = "0"
                        openCamera()
                    }
                }
            }, handler)

        } catch (e: Exception) {
            cameraOpenCloseLock.release()
            Log.e(TAG, "Exception opening camera $currentCameraId", e)
            _cameraError.value = "Failed to open camera: ${e.message}"
        }
    }

    private fun setupImageReader(cameraId: String) {
        try {
            val characteristics = cameraManager.getCameraCharacteristics(cameraId)
            val defaultMap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            val defaultSizes = defaultMap?.getOutputSizes(ImageFormat.JPEG) ?: emptyArray()

            val maxResSizes = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val maxMap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP_MAXIMUM_RESOLUTION)
                maxMap?.getOutputSizes(ImageFormat.JPEG) ?: emptyArray()
            } else {
                emptyArray()
            }

            val allSizes = (maxResSizes.toList() + defaultSizes.toList()).distinct()

            val optimalSize = allSizes
                .filter { it.width * it.height <= 55_000_000 }
                .maxByOrNull { it.width * it.height }
                ?: Size(4000, 3000)

            captureWidth = optimalSize.width
            captureHeight = optimalSize.height

            imageReader?.close()
            imageReader = ImageReader.newInstance(
                captureWidth,
                captureHeight,
                ImageFormat.JPEG,
                2
            ).apply {
                setOnImageAvailableListener({ reader ->
                    handleImageAvailable(reader)
                }, backgroundHandler)
            }
            Log.d(TAG, "ImageReader configured: ${captureWidth}x${captureHeight}")
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up ImageReader", e)
        }
    }

    private fun createCaptureSession() {
        val camera = cameraDevice ?: return
        val surface = previewSurface ?: return
        val reader = imageReader ?: return
        val handler = backgroundHandler ?: return

        if (!surface.isValid) return

        try {
            captureSession?.close()
            captureSession = null

            val surfaces = listOf(surface, reader.surface)

            camera.createCaptureSession(surfaces, object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(session: CameraCaptureSession) {
                    if (cameraDevice == null) return
                    captureSession = session
                    try {
                        val builder = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
                            addTarget(surface)
                            applyZoomAndCrop(this, characteristics = cameraManager.getCameraCharacteristics(currentCameraId))
                            applyProSettings(this)
                        }
                        previewRequestBuilder = builder
                        session.setRepeatingRequest(builder.build(), null, handler)
                        _isCameraStreaming.value = true
                        _cameraError.value = null
                        Log.d(TAG, "Preview stream active on Camera $currentCameraId")
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to start preview repeating request", e)
                        _cameraError.value = "Failed to start viewfinder: ${e.message}"
                    }
                }

                override fun onConfigureFailed(session: CameraCaptureSession) {
                    Log.e(TAG, "Capture session configuration failed")
                    _cameraError.value = "Camera configuration failed"
                    _isCameraStreaming.value = false
                }
            }, handler)

        } catch (e: Exception) {
            Log.e(TAG, "Error creating capture session", e)
            _cameraError.value = "Session creation error: ${e.message}"
        }
    }

    private fun applyZoomAndCrop(builder: CaptureRequest.Builder, characteristics: CameraCharacteristics) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val zoomRange = characteristics.get(CameraCharacteristics.CONTROL_ZOOM_RATIO_RANGE)
            if (zoomRange != null) {
                val clampedZoom = currentZoomRatio.coerceIn(zoomRange.lower, zoomRange.upper)
                builder.set(CaptureRequest.CONTROL_ZOOM_RATIO, clampedZoom)
                return
            }
        }

        val activeArray = characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE)
        if (activeArray != null && currentZoomRatio > 1.0f) {
            val cropRect = CropEngine.computeCropRegion(activeArray, currentZoomRatio)
            builder.set(CaptureRequest.SCALER_CROP_REGION, cropRect)
        }
    }

    private fun applyProSettings(builder: CaptureRequest.Builder) {
        // Focus Mode
        val afMode = when (currentFocusMode) {
            "AF-S" -> CaptureRequest.CONTROL_AF_MODE_AUTO
            "MACRO" -> CaptureRequest.CONTROL_AF_MODE_MACRO
            else -> CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
        }
        builder.set(CaptureRequest.CONTROL_AF_MODE, afMode)

        // Flash / Torch Mode
        when (currentFlashMode) {
            FlashMode.TORCH -> {
                builder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)
                builder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_TORCH)
            }
            FlashMode.AUTO -> {
                builder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH)
                builder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_OFF)
            }
            FlashMode.OFF -> {
                builder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_OFF)
                if (currentIso != null && currentExposureTimeNs != null) {
                    builder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF)
                    builder.set(CaptureRequest.SENSOR_SENSITIVITY, currentIso)
                    builder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, currentExposureTimeNs)
                } else {
                    builder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)
                }
            }
        }

        // EV Bias
        builder.set(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, currentEvCompensation)
    }

    fun updateRepeatingRequest() {
        val session = captureSession ?: return
        val builder = previewRequestBuilder ?: return
        val handler = backgroundHandler ?: return

        try {
            val characteristics = cameraManager.getCameraCharacteristics(currentCameraId)
            applyZoomAndCrop(builder, characteristics)
            applyProSettings(builder)
            session.setRepeatingRequest(builder.build(), null, handler)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update repeating request", e)
        }
    }

    fun selectLens(lensType: LensType, is2xCrop: Boolean = false) {
        val baseZoom = when (lensType) {
            LensType.ULTRA_WIDE -> 0.6f
            LensType.WIDE -> 1.0f
            LensType.TELEPHOTO -> 3.0f
            LensType.PERISCOPE -> 6.0f
            LensType.FRONT -> 1.0f
        }
        val targetZoom = if (is2xCrop) baseZoom * 2.0f else baseZoom

        val matchingSpec = _cameraSpecs.value.firstOrNull { it.lensType == lensType && it.isBackFacing }
            ?: _cameraSpecs.value.firstOrNull { it.isBackFacing }

        _activeCameraSpec.value = matchingSpec
        currentZoomRatio = targetZoom

        val targetId = matchingSpec?.cameraId ?: "0"

        if (targetId != currentCameraId && cameraDevice != null) {
            currentCameraId = targetId
            openCamera()
        } else {
            updateRepeatingRequest()
        }
    }

    fun setFlashMode(mode: FlashMode) {
        currentFlashMode = mode
        updateRepeatingRequest()
    }

    fun updateFocusMode(mode: String) {
        currentFocusMode = mode
        updateRepeatingRequest()
    }

    fun updateEvCompensation(biasStep: Int) {
        currentEvCompensation = biasStep
        updateRepeatingRequest()
    }

    fun updateManualIso(isoVal: Int?) {
        currentIso = isoVal
        updateRepeatingRequest()
    }

    fun updateManualShutter(shutterSpeedNs: Long?) {
        currentExposureTimeNs = shutterSpeedNs
        updateRepeatingRequest()
    }

    fun updateManualKelvin(kelvin: Int) {
        currentWhiteBalanceKelvin = kelvin
        updateRepeatingRequest()
    }

    fun getOptimalPreviewSize(viewWidth: Int, viewHeight: Int): Size {
        return try {
            val characteristics = cameraManager.getCameraCharacteristics(currentCameraId)
            val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            val outputSizes = map?.getOutputSizes(SurfaceTexture::class.java) ?: emptyArray()

            outputSizes
                .filter { it.width <= 1920 && it.height <= 1440 }
                .maxByOrNull { it.width * it.height }
                ?: outputSizes.firstOrNull()
                ?: Size(1920, 1080)
        } catch (e: Exception) {
            Size(1920, 1080)
        }
    }

    fun getSensorOrientation(): Int {
        return try {
            val characteristics = cameraManager.getCameraCharacteristics(currentCameraId)
            characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION) ?: 90
        } catch (e: Exception) {
            90
        }
    }

    suspend fun capturePhoto(cropFactor: Float): Result<Pair<Bitmap, Rect?>> = withContext(Dispatchers.IO) {
        val session = captureSession
        val camera = cameraDevice
        val reader = imageReader
        val handler = backgroundHandler

        if (session == null || camera == null || reader == null || handler == null) {
            return@withContext runCatching {
                val spec = _activeCameraSpec.value
                val w = (spec?.activeArrayWidth ?: 4000).coerceAtMost(4096)
                val h = (spec?.activeArrayHeight ?: 3000).coerceAtMost(3072)
                val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                val activeRect = Rect(0, 0, w, h)
                val crop = CropEngine.computeCropRegion(activeRect, cropFactor)
                Pair(bitmap, crop)
            }
        }

        val deferred = CompletableDeferred<Pair<Bitmap, Rect?>>()
        pendingCaptureDeferred = deferred
        pendingCropFactor = cropFactor

        try {
            val captureBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE).apply {
                addTarget(reader.surface)
                set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO)
                set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
                set(CaptureRequest.JPEG_QUALITY, 100.toByte())
                set(CaptureRequest.JPEG_ORIENTATION, 90)

                // Maximum Hardware ISP Processing Pass
                set(CaptureRequest.EDGE_MODE, CaptureRequest.EDGE_MODE_HIGH_QUALITY)
                set(CaptureRequest.NOISE_REDUCTION_MODE, CaptureRequest.NOISE_REDUCTION_MODE_HIGH_QUALITY)
                set(CaptureRequest.COLOR_CORRECTION_ABERRATION_MODE, CaptureRequest.COLOR_CORRECTION_ABERRATION_MODE_HIGH_QUALITY)
                set(CaptureRequest.SHADING_MODE, CaptureRequest.SHADING_MODE_HIGH_QUALITY)
                set(CaptureRequest.HOT_PIXEL_MODE, CaptureRequest.HOT_PIXEL_MODE_HIGH_QUALITY)
                set(CaptureRequest.DISTORTION_CORRECTION_MODE, CaptureRequest.DISTORTION_CORRECTION_MODE_HIGH_QUALITY)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    set(CaptureRequest.SENSOR_PIXEL_MODE, CaptureRequest.SENSOR_PIXEL_MODE_MAXIMUM_RESOLUTION)
                }

                val characteristics = cameraManager.getCameraCharacteristics(currentCameraId)
                applyZoomAndCrop(this, characteristics)
                applyProSettings(this)
            }

            session.capture(captureBuilder.build(), object : CameraCaptureSession.CaptureCallback() {
                override fun onCaptureFailed(session: CameraCaptureSession, request: CaptureRequest, failure: CaptureFailure) {
                    Log.e(TAG, "Still capture failed: reason ${failure.reason}")
                    pendingCaptureDeferred?.completeExceptionally(RuntimeException("Still capture failed: reason ${failure.reason}"))
                    pendingCaptureDeferred = null
                }
            }, handler)

            withContext(Dispatchers.Default) {
                try {
                    val result = deferred.await()
                    Result.success(result)
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initiating still capture", e)
            pendingCaptureDeferred = null
            Result.failure(e)
        }
    }

    private fun handleImageAvailable(reader: ImageReader) {
        var image: Image? = null
        try {
            image = reader.acquireLatestImage() ?: return
            val buffer = image.planes[0].buffer
            val bytes = ByteArray(buffer.remaining())
            buffer.get(bytes)

            val options = BitmapFactory.Options().apply {
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val originalBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)

            if (originalBitmap != null) {
                // Ensure upright portrait rotation for captured photo
                val exif = ExifInterface(ByteArrayInputStream(bytes))
                val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
                val rotationDegrees = when (orientation) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                    else -> 90f // Native rear camera sensor landscape-to-portrait compensation
                }

                val matrix = Matrix().apply { postRotate(rotationDegrees) }
                val uprightBitmap = Bitmap.createBitmap(originalBitmap, 0, 0, originalBitmap.width, originalBitmap.height, matrix, true)

                val fullRect = Rect(0, 0, uprightBitmap.width, uprightBitmap.height)
                val cropRect = CropEngine.computeCropRegion(fullRect, pendingCropFactor)
                pendingCaptureDeferred?.complete(Pair(uprightBitmap, cropRect))
            } else {
                pendingCaptureDeferred?.completeExceptionally(RuntimeException("Failed to decode JPEG bytes from sensor"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing captured frame", e)
            pendingCaptureDeferred?.completeExceptionally(e)
        } finally {
            image?.close()
            pendingCaptureDeferred = null
        }
    }

    private fun closeCameraInternal() {
        try {
            captureSession?.close()
            captureSession = null
            cameraDevice?.close()
            cameraDevice = null
            _isCameraStreaming.value = false
        } catch (e: Exception) {
            Log.e(TAG, "Error closing camera", e)
        }
    }

    fun closeCamera() {
        try {
            cameraOpenCloseLock.acquire()
            closeCameraInternal()
            imageReader?.close()
            imageReader = null
        } catch (e: Exception) {
            Log.e(TAG, "Exception during closeCamera", e)
        } finally {
            cameraOpenCloseLock.release()
        }
    }
}

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
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureFailure
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.TotalCaptureResult
import android.hardware.camera2.params.OutputConfiguration
import android.hardware.camera2.params.SessionConfiguration
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
import java.util.concurrent.Executor
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit

enum class FlashMode {
    OFF,
    AUTO,
    TORCH
}

data class PhotoExifMetadata(
    val iso: Int? = null,
    val exposureTimeNs: Long? = null,
    val fNumber: Float? = null,
    val focalLengthMm: Float? = null,
    val equivalent35mm: Int? = null,
    val evBias: Float? = null,
    val cameraModel: String = "OPPO Find X9 Ultra",
    val lensDescription: String = "Wide-angle camera"
)

data class CapturedPhoto(
    val bitmap: Bitmap,
    val cropRect: Rect?,
    val metadata: PhotoExifMetadata
)

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
    private var pendingCaptureDeferred: CompletableDeferred<CapturedPhoto>? = null
    private var pendingCropFactor: Float = 1.0f
    private var latestCaptureMetadata: PhotoExifMetadata? = null
    private var isSizeFromMaxResolutionMap: Boolean = false

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

    private data class CandidateSize(
        val size: Size,
        val isFromMaxResolutionMap: Boolean
    )

    private fun setupImageReader(cameraId: String) {
        try {
            val characteristics = cameraManager.getCameraCharacteristics(cameraId)
            val defaultMap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)

            val candidates = mutableListOf<CandidateSize>()

            // 1. High-resolution sizes from default stream configuration map (Android M+, API 23+)
            // On modern Android flagships (Oppo Find X9 Ultra, OnePlus, Samsung, Xiaomi), 28MP/50MP/200MP
            // JPEG outputs cannot sustain >= 20 fps burst capture and are placed in getHighResolutionOutputSizes!
            val hiResSizes = defaultMap?.getHighResolutionOutputSizes(ImageFormat.JPEG) ?: emptyArray()
            for (s in hiResSizes) {
                candidates.add(CandidateSize(s, isFromMaxResolutionMap = false))
            }

            // 2. Standard JPEG output sizes (burst >= 20fps, typically 12.5MP binned 4096x3072)
            val regularSizes = defaultMap?.getOutputSizes(ImageFormat.JPEG) ?: emptyArray()
            for (s in regularSizes) {
                candidates.add(CandidateSize(s, isFromMaxResolutionMap = false))
            }

            // 3. Maximum resolution stream configuration map (Android S+, API 31+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val maxMap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP_MAXIMUM_RESOLUTION)
                val maxHiRes = maxMap?.getHighResolutionOutputSizes(ImageFormat.JPEG) ?: emptyArray()
                for (s in maxHiRes) {
                    candidates.add(CandidateSize(s, isFromMaxResolutionMap = true))
                }
                val maxRegular = maxMap?.getOutputSizes(ImageFormat.JPEG) ?: emptyArray()
                for (s in maxRegular) {
                    candidates.add(CandidateSize(s, isFromMaxResolutionMap = true))
                }
            }

            // Deduplicate candidates preserving origin flag
            val distinctCandidates = candidates.distinctBy { it.size.width to it.size.height }

            Log.d(TAG, "Discovered ${distinctCandidates.size} JPEG output candidates for camera $cameraId: " +
                    distinctCandidates.joinToString { "${it.size.width}x${it.size.height}(maxMap=${it.isFromMaxResolutionMap})" })

            // Priority 1: Exact or near match to the stock 28MP mode (6120x4596 / 4596x6120 ~ 28.1MP)
            val stock28Mp = distinctCandidates.firstOrNull { c ->
                val s = c.size
                (s.width == 6120 && s.height == 4596) ||
                (s.width == 4596 && s.height == 6120) ||
                (s.width * s.height in 27_000_000..29_500_000)
            }

            // Priority 2: Highest available resolution up to 55MP
            val optimalCandidate = stock28Mp ?: distinctCandidates
                .filter { it.size.width * it.size.height <= 55_000_000 }
                .maxByOrNull { it.size.width * it.size.height }
                ?: distinctCandidates.firstOrNull()
                ?: CandidateSize(Size(4000, 3000), false)

            captureWidth = optimalCandidate.size.width
            captureHeight = optimalCandidate.size.height
            isSizeFromMaxResolutionMap = optimalCandidate.isFromMaxResolutionMap

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
            Log.d(TAG, "Selected capture resolution: ${captureWidth}x${captureHeight} (stock 28MP match=${stock28Mp != null}, maxMap=$isSizeFromMaxResolutionMap)")
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

            val stateCallback = object : CameraCaptureSession.StateCallback() {
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
                        Log.d(TAG, "Preview stream active on Camera $currentCameraId (capture buffer: ${captureWidth}x${captureHeight})")
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
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val previewOutput = OutputConfiguration(surface)
                val captureOutput = OutputConfiguration(reader.surface)

                if (isSizeFromMaxResolutionMap && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    captureOutput.addSensorPixelModeUsed(CameraMetadata.SENSOR_PIXEL_MODE_MAXIMUM_RESOLUTION)
                }

                val executor = Executor { command -> handler.post(command) }
                val sessionConfig = SessionConfiguration(
                    SessionConfiguration.SESSION_REGULAR,
                    listOf(previewOutput, captureOutput),
                    executor,
                    stateCallback
                )
                camera.createCaptureSession(sessionConfig)
            } else {
                @Suppress("DEPRECATION")
                camera.createCaptureSession(listOf(surface, reader.surface), stateCallback, handler)
            }

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

    suspend fun capturePhoto(cropFactor: Float): Result<CapturedPhoto> = withContext(Dispatchers.IO) {
        val session = captureSession
        val camera = cameraDevice
        val reader = imageReader
        val handler = backgroundHandler

        if (session == null || camera == null || reader == null || handler == null) {
            return@withContext runCatching {
                val spec = _activeCameraSpec.value
                val w = (spec?.activeArrayWidth ?: 4596).coerceAtLeast(3072)
                val h = (spec?.activeArrayHeight ?: 6120).coerceAtLeast(4096)
                val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                val activeRect = Rect(0, 0, w, h)
                val crop = CropEngine.computeCropRegion(activeRect, cropFactor)
                CapturedPhoto(bitmap, crop, PhotoExifMetadata(cameraModel = "OPPO Find X9 Ultra"))
            }
        }

        val deferred = CompletableDeferred<CapturedPhoto>()
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

                if (isSizeFromMaxResolutionMap && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    set(CaptureRequest.SENSOR_PIXEL_MODE, CaptureRequest.SENSOR_PIXEL_MODE_MAXIMUM_RESOLUTION)
                }

                val characteristics = cameraManager.getCameraCharacteristics(currentCameraId)
                applyZoomAndCrop(this, characteristics)
                applyProSettings(this)
            }

            session.capture(captureBuilder.build(), object : CameraCaptureSession.CaptureCallback() {
                override fun onCaptureCompleted(
                    session: CameraCaptureSession,
                    request: CaptureRequest,
                    result: TotalCaptureResult
                ) {
                    val iso = result.get(CaptureResult.SENSOR_SENSITIVITY) ?: currentIso
                    val expTimeNs = result.get(CaptureResult.SENSOR_EXPOSURE_TIME) ?: currentExposureTimeNs
                    val aperture = result.get(CaptureResult.LENS_APERTURE)
                    val focalLength = result.get(CaptureResult.LENS_FOCAL_LENGTH)
                    val ev = result.get(CaptureResult.CONTROL_AE_EXPOSURE_COMPENSATION)

                    val spec = _activeCameraSpec.value
                    val lensDesc = when (spec?.lensType) {
                        LensType.ULTRA_WIDE -> "Ultra wide-angle camera"
                        LensType.WIDE -> "Wide-angle camera"
                        LensType.TELEPHOTO -> "Telephoto camera"
                        LensType.PERISCOPE -> "Periscope telephoto camera"
                        LensType.FRONT -> "Front camera"
                        else -> "Wide-angle camera"
                    }

                    val eq35 = if (pendingCropFactor > 1.5f) {
                        ((spec?.equivalentFocalLength35mm ?: 23) * pendingCropFactor).toInt()
                    } else {
                        spec?.equivalentFocalLength35mm ?: 23
                    }

                    latestCaptureMetadata = PhotoExifMetadata(
                        iso = iso,
                        exposureTimeNs = expTimeNs,
                        fNumber = aperture ?: spec?.apertureFNumber ?: 1.5f,
                        focalLengthMm = focalLength ?: spec?.focalLengthMm ?: 8.67f,
                        equivalent35mm = eq35,
                        evBias = (ev ?: currentEvCompensation) * (spec?.evStep ?: 0.333f),
                        cameraModel = "OPPO Find X9 Ultra",
                        lensDescription = lensDesc
                    )
                }

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
                val characteristics = try {
                    cameraManager.getCameraCharacteristics(currentCameraId)
                } catch (e: Exception) {
                    null
                }
                val sensorOrientation = characteristics?.get(CameraCharacteristics.SENSOR_ORIENTATION) ?: 90

                val uprightBitmap = if (originalBitmap.width > originalBitmap.height) {
                    val matrix = Matrix().apply { postRotate(sensorOrientation.toFloat()) }
                    val rotated = Bitmap.createBitmap(originalBitmap, 0, 0, originalBitmap.width, originalBitmap.height, matrix, true)
                    originalBitmap.recycle()
                    rotated
                } else {
                    originalBitmap
                }

                val fullRect = Rect(0, 0, uprightBitmap.width, uprightBitmap.height)
                val cropRect = CropEngine.computeCropRegion(fullRect, pendingCropFactor)

                val spec = _activeCameraSpec.value
                val fallbackLens = when (spec?.lensType) {
                    LensType.ULTRA_WIDE -> "Ultra wide-angle camera"
                    LensType.WIDE -> "Wide-angle camera"
                    LensType.TELEPHOTO -> "Telephoto camera"
                    LensType.PERISCOPE -> "Periscope telephoto camera"
                    LensType.FRONT -> "Front camera"
                    else -> "Wide-angle camera"
                }

                val meta = latestCaptureMetadata ?: PhotoExifMetadata(
                    iso = currentIso ?: 500,
                    fNumber = spec?.apertureFNumber ?: 1.5f,
                    focalLengthMm = spec?.focalLengthMm ?: 8.67f,
                    equivalent35mm = spec?.equivalentFocalLength35mm ?: 23,
                    evBias = currentEvCompensation * (spec?.evStep ?: 0.333f),
                    cameraModel = "OPPO Find X9 Ultra",
                    lensDescription = fallbackLens
                )

                pendingCaptureDeferred?.complete(CapturedPhoto(uprightBitmap, cropRect, meta))
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

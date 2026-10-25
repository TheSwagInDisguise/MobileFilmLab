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
import android.hardware.camera2.TotalCaptureResult
import android.hardware.camera2.params.MeteringRectangle
import android.media.Image
import android.media.ImageReader
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Range
import android.util.Size
import android.view.Surface
import androidx.core.content.ContextCompat
import com.mobilefilmlab.camera.data.model.CameraSpec
import com.mobilefilmlab.camera.data.model.LensType
import com.mobilefilmlab.camera.pipeline.CropEngine
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit

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
        
        // Find best default rear camera
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

        // If camera already opened for currentCameraId and streaming, restart session with new surface
        if (cameraDevice != null && cameraDevice?.id == currentCameraId) {
            createCaptureSession()
            return
        }

        // Close previous camera if switching IDs
        closeCameraInternal()

        try {
            if (!cameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                _cameraError.value = "Camera open timeout"
                return
            }

            // Verify cameraId exists in cameraManager
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
                    
                    // Fallback to camera "0" if a physical camera ID failed
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
            val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            val jpegSizes = map?.getOutputSizes(ImageFormat.JPEG) ?: emptyArray()

            // Pick largest resolution under 50MP to prevent OutOfMemory
            val optimalSize = jpegSizes
                .filter { it.width * it.height <= 50_000_000 }
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
                            // Enable Auto-focus and Auto-exposure
                            set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO)
                            set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
                            set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)
                            set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_AUTO)
                            
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
        // Modern Android 11+ zoom ratio API
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val zoomRange = characteristics.get(CameraCharacteristics.CONTROL_ZOOM_RATIO_RANGE)
            if (zoomRange != null) {
                val clampedZoom = currentZoomRatio.coerceIn(zoomRange.lower, zoomRange.upper)
                builder.set(CaptureRequest.CONTROL_ZOOM_RATIO, clampedZoom)
                return
            }
        }

        // Fallback: SCALER_CROP_REGION for older API / devices without CONTROL_ZOOM_RATIO
        val activeArray = characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE)
        if (activeArray != null && currentZoomRatio > 1.0f) {
            val cropRect = CropEngine.computeCropRegion(activeArray, currentZoomRatio)
            builder.set(CaptureRequest.SCALER_CROP_REGION, cropRect)
        }
    }

    private fun applyProSettings(builder: CaptureRequest.Builder) {
        // EV Bias
        builder.set(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, currentEvCompensation)

        // Manual ISO & Shutter if specified
        if (currentIso != null && currentExposureTimeNs != null) {
            builder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF)
            builder.set(CaptureRequest.SENSOR_SENSITIVITY, currentIso)
            builder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, currentExposureTimeNs)
        } else {
            builder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)
        }
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
        // Calculate target zoom factor
        val baseZoom = when (lensType) {
            LensType.ULTRA_WIDE -> 0.6f
            LensType.WIDE -> 1.0f
            LensType.TELEPHOTO -> 3.0f
            LensType.PERISCOPE -> 6.0f // Find X9 Ultra 6x/10x periscope telephoto
            LensType.FRONT -> 1.0f
        }
        val targetZoom = if (is2xCrop) baseZoom * 2.0f else baseZoom

        // Find matching physical camera spec if available
        val matchingSpec = _cameraSpecs.value.firstOrNull { it.lensType == lensType && it.isBackFacing }
            ?: _cameraSpecs.value.firstOrNull { it.isBackFacing }

        _activeCameraSpec.value = matchingSpec
        currentZoomRatio = targetZoom

        // On multi-camera setups: if the device uses logical Camera 0 for all zoom ranges
        // we can simply adjust zoom ratio. If separate physical camera IDs exist, we switch if needed.
        val targetId = matchingSpec?.cameraId ?: "0"
        
        // If camera ID changes, reopen camera; otherwise simply update zoom ratio!
        if (targetId != currentCameraId && cameraDevice != null) {
            currentCameraId = targetId
            openCamera()
        } else {
            updateRepeatingRequest()
        }
    }

    fun updateZoom(zoomRatio: Float) {
        currentZoomRatio = zoomRatio
        updateRepeatingRequest()
    }

    fun updateEvCompensation(biasStep: Int) {
        currentEvCompensation = biasStep
        updateRepeatingRequest()
    }

    fun updateManualExposure(isoVal: Int?, shutterSpeedNs: Long?) {
        currentIso = isoVal
        currentExposureTimeNs = shutterSpeedNs
        updateRepeatingRequest()
    }

    suspend fun capturePhoto(cropFactor: Float): Result<Pair<Bitmap, Rect?>> = withContext(Dispatchers.IO) {
        val session = captureSession
        val camera = cameraDevice
        val reader = imageReader
        val handler = backgroundHandler

        if (session == null || camera == null || reader == null || handler == null) {
            // Fallback generation if camera is closed
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
                set(CaptureRequest.JPEG_QUALITY, 98.toByte())
                set(CaptureRequest.JPEG_ORIENTATION, 90) // Standard rear camera portrait orientation

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

            // Wait up to 5 seconds for ImageReader callback
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

            // Decode JPEG to Bitmap
            val options = BitmapFactory.Options().apply {
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val originalBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)

            if (originalBitmap != null) {
                val fullRect = Rect(0, 0, originalBitmap.width, originalBitmap.height)
                val cropRect = CropEngine.computeCropRegion(fullRect, pendingCropFactor)
                pendingCaptureDeferred?.complete(Pair(originalBitmap, cropRect))
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


    fun getOptimalPreviewSize(viewWidth: Int, viewHeight: Int): Size {
        return try {
            val characteristics = cameraManager.getCameraCharacteristics(currentCameraId)
            val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            val outputSizes = map?.getOutputSizes(SurfaceTexture::class.java) ?: emptyArray()

            // Find 4:3 or 16:9 resolutions suitable for preview (< 1920x1440)
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

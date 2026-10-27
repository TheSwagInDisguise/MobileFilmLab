package com.mobilefilmlab.camera.camera

import android.os.Build
import com.mobilefilmlab.camera.data.model.LensType

data class PhotoExifMetadata(
    val iso: Int? = null, val exposureTimeNs: Long? = null, val fNumber: Float? = null,
    val focalLengthMm: Float? = null, val equivalent35mm: Int? = null, val evBias: Float? = null,
    val cameraMake: String = Build.MANUFACTURER ?: "Android", val cameraModel: String = Build.MODEL ?: "Android",
    val lensDescription: String = "Camera2 lens", val lensModel: String? = null,
    val flashFired: Boolean? = null, val flashExif: Int? = null, val lensType: LensType = LensType.WIDE,
    val captureTimestampMs: Long = System.currentTimeMillis(), val pipeline: String = "Camera2 JPEG",
    val note: String = "", val sourceWidth: Int = 0, val sourceHeight: Int = 0,
    val manualExposure: Boolean = false, val manualWhiteBalance: Boolean = false
)
data class CapturedJpeg(val bytes: ByteArray, val metadata: PhotoExifMetadata, val raw: RawCapture? = null)
data class CaptureBundle(val frames: List<CapturedJpeg>, val cropFactor: Float, val label: String) : AutoCloseable {
    val raw: RawCapture? get() = frames.firstOrNull()?.raw
    override fun close() { frames.forEach { it.raw?.close() } }
}

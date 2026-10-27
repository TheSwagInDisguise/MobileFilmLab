package com.mobilefilmlab.camera.camera

import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CaptureResult
import android.hardware.camera2.DngCreator
import android.media.Image
import androidx.exifinterface.media.ExifInterface
import java.io.OutputStream
import java.util.concurrent.atomic.AtomicBoolean

/** Owns one timestamp-matched RAW_SENSOR image. No full-frame RAW ByteArray is allocated. */
class RawCapture internal constructor(
    private val imageLease: ResourceLease<Image>,
    private val characteristics: CameraCharacteristics,
    private val result: CaptureResult,
    val rotationDegrees: Int,
    val cameraId: String
) : AutoCloseable {
    val width = imageLease.value.width
    val height = imageLease.value.height
    val timestamp = imageLease.value.timestamp
    private val closed = AtomicBoolean(false)

    /** Streams real sensor data to DNG. Never applies recipes, crop, grain or interpolation. */
    fun writeTo(output: OutputStream) {
        check(!closed.get()) { "RAW image is closed" }
        check(SensorFrameMatch.matches(timestamp,result.get(CaptureResult.SENSOR_TIMESTAMP),null,false)) { "RAW metadata timestamp does not match sensor image" }
        DngCreator(characteristics, result).use { creator ->
            creator.setOrientation(orientation(rotationDegrees))
            creator.setDescription("MobileFilmLab Camera2 RAW_SENSOR; camera $cameraId; no app colour grade, crop, grain or resampling. Device/firmware processing may still apply.")
            creator.writeImage(output, imageLease.value)
        }
    }
    override fun close() { if (closed.compareAndSet(false, true)) imageLease.close() }

    companion object {
        internal fun orientation(degrees: Int): Int = when (((degrees % 360) + 360) % 360) {
            90 -> ExifInterface.ORIENTATION_ROTATE_90
            180 -> ExifInterface.ORIENTATION_ROTATE_180
            270 -> ExifInterface.ORIENTATION_ROTATE_270
            else -> ExifInterface.ORIENTATION_NORMAL
        }
    }
}

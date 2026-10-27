package com.mobilefilmlab.camera.settings

/** Saved files, not a JPEG renamed as RAW. DNG always comes from a RAW_SENSOR image. */
enum class CaptureFormat(val label: String, val writesJpeg: Boolean, val writesRaw: Boolean) {
    JPEG("JPEG only", true, false),
    RAW_JPEG("RAW + JPEG", true, true),
    RAW("RAW (DNG) only", false, true);

    val badge: String get() = when (this) { JPEG -> "JPEG"; RAW_JPEG -> "RAW+JPEG"; RAW -> "RAW" }
}

/** RAW must correspond to one standard exposure; no pretend RAW of a processed stack. */
object CaptureFormatPolicy {
    fun normalize(settings: CaptureSettings): CaptureSettings = if (settings.format.writesRaw) {
        settings.copy(pipeline = CapturePipeline.STANDARD, zsl = false, outputSize = OutputSize.NATIVE)
    } else settings

    fun supported(format: CaptureFormat, camera: com.mobilefilmlab.camera.data.model.CameraSpec?): Boolean = camera!=null &&
        (!format.writesRaw || camera.hasRawSupport) && (!format.writesJpeg || camera.hasJpegSupport)

    fun allowsCrop(format: CaptureFormat): Boolean = !format.writesRaw
    fun allowsPipeline(format: CaptureFormat, pipeline: CapturePipeline): Boolean =
        !format.writesRaw || pipeline == CapturePipeline.STANDARD
}

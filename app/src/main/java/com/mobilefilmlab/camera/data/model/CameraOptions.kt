package com.mobilefilmlab.camera.data.model

import java.util.Locale

/** Camera2 stream dimensions. No inference from a marketing sensor megapixel count. */
data class StreamSize(val width: Int, val height: Int) {
    val area: Long get() = width.toLong() * height
}

/** A logical camera is a route, not an additional physical lens. Keep that visible. */
object CameraOptions {
    fun rear(specs: List<CameraSpec>): List<CameraSpec> = specs.filter { it.isBackFacing && (it.hasJpegSupport || it.hasRawSupport) }
        .distinctBy { it.cameraId }
        .sortedWith(compareBy<CameraSpec> { it.equivalentFocalLength35mm.takeIf { n -> n > 0 } ?: Int.MAX_VALUE }
            .thenBy { if (it.isLogicalMultiCamera) 0 else 1 }.thenBy { it.cameraId })

    fun focalLabel(spec: CameraSpec): String = when {
        spec.equivalentFocalLength35mm > 0 -> "${spec.equivalentFocalLength35mm}mm"
        spec.focalLengthMm > 0 -> String.format(Locale.US, "%.1fmm*", spec.focalLengthMm)
        else -> "ID ${spec.cameraId}"
    }

    fun label(spec: CameraSpec): String = if (spec.isLogicalMultiCamera) "Auto · ${focalLabel(spec)}" else focalLabel(spec)

    fun description(spec: CameraSpec): String = buildString {
        append(label(spec)).append(if (spec.equivalentFocalLength35mm > 0) " approx. 35mm equivalent" else " (physical focal length; equivalent unavailable)")
        append(" · ID ").append(spec.cameraId)
        append(if (spec.isLogicalMultiCamera) " · logical/automatic route" else if (spec.physicalOutputId != null) " · physical output via ${spec.openCameraId}" else " · directly openable")
        append(if (spec.hasRawSupport) " · RAW advertised" else " · JPEG only: ${spec.rawUnavailableReason}")
    }
}

/** Bound RAW memory without resizing sensor data. Try advertised native sizes only. */
object RawStreamSelector {
    const val MAX_PIXELS = 55_000_000L
    fun candidates(sizes: List<StreamSize>): List<StreamSize> = sizes
        .filter { it.width > 0 && it.height > 0 && it.area <= MAX_PIXELS }
        .distinct().sortedByDescending { it.area }.take(4)
}

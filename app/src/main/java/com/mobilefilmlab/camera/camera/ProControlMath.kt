package com.mobilefilmlab.camera.camera

import com.mobilefilmlab.camera.data.model.CameraSpec
import java.util.Locale
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.roundToLong

/** Shared UI/request bounds. Positions are logarithmic for photographic exposure controls. */
object ProControlMath {
    const val MAX_EXPOSURE_NS = 30_000_000_000L
    const val MIN_KELVIN = 2500
    const val MAX_KELVIN = 9900

    fun isoRange(spec: CameraSpec): IntRange {
        val min = spec.minIso.coerceAtLeast(1)
        return min..spec.maxIso.coerceAtLeast(min)
    }

    fun exposureRange(spec: CameraSpec): LongRange {
        val max = minOf(spec.maxExposureNs.coerceAtLeast(1),
            spec.maxFrameDurationNs?.takeIf { it > 0 } ?: Long.MAX_VALUE, MAX_EXPOSURE_NS)
        return spec.minExposureNs.coerceIn(1, max)..max
    }

    fun evSteps(value: Float, spec: CameraSpec): Int {
        if (!value.isFinite() || !spec.evStep.isFinite() || spec.evStep <= 0 || spec.minEv > spec.maxEv) return 0
        return (value / spec.evStep).roundToInt().coerceIn(spec.minEv, spec.maxEv)
    }

    fun hasEv(spec: CameraSpec?) = spec != null && spec.evStep.isFinite() && spec.evStep > 0 && spec.minEv < spec.maxEv

    fun logarithmicValue(position: Float, min: Long, max: Long): Long {
        require(min > 0 && max >= min)
        val p = if (position.isFinite()) position.coerceIn(0f, 1f) else 0f
        if (p == 0f || min == max) return min
        if (p == 1f) return max
        return exp(ln(min.toDouble()) + p * (ln(max.toDouble()) - ln(min.toDouble())))
            .roundToLong().coerceIn(min, max)
    }

    fun logarithmicPosition(value: Long, min: Long, max: Long): Float {
        require(min > 0 && max >= min)
        if (min == max) return 0f
        return ((ln(value.coerceIn(min, max).toDouble()) - ln(min.toDouble())) /
            (ln(max.toDouble()) - ln(min.toDouble()))).toFloat().coerceIn(0f, 1f)
    }

    fun kelvin(value: Int) = (value.coerceIn(MIN_KELVIN, MAX_KELVIN) / 100f).roundToInt().times(100).coerceIn(MIN_KELVIN, MAX_KELVIN)

    fun exposureLabel(ns: Long): String {
        if (ns <= 0) return "Auto"
        val seconds = ns / 1_000_000_000.0
        return if (seconds < 0.5) "1/${(1.0 / seconds).roundToLong()}"
        else String.format(Locale.ROOT, if (seconds % 1.0 < 0.005) "%.0f s" else "%.2f s", seconds)
    }

    fun evLabel(ev: Float) = String.format(Locale.ROOT, "%+.1f", ev)

    /** Allow a long in-flight preview plus the still exposure, not just a fixed 15-second deadline. */
    fun captureTimeoutMs(exposureNs: Long?, extension: Boolean = false): Long {
        val exposureMs = ((exposureNs ?: 0L).coerceIn(0L, MAX_EXPOSURE_NS) + 999_999L) / 1_000_000L
        return max(if (extension) 60_000L else 15_000L, exposureMs * 2 + 10_000L)
    }
}

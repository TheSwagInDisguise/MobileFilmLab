package com.mobilefilmlab.camera.data.model

import android.graphics.Rect

enum class HonestCropStatus(val label: String, val shortDescription: String) {
    FULL_SENSOR(
        label = "Full native stream",
        shortDescription = "Entire exposed JPEG stream. This does not imply access to the sensor maximum mode."
    ),
    NATIVE_NO_RESAMPLE(
        label = "Native-pixel crop / no-resample",
        shortDescription = "Sensor region exported 1:1 without interpolation or scaling."
    ),
    REDUCED_RESOLUTION(
        label = "Reduced-resolution crop",
        shortDescription = "Cropped sensor area output at its native unscaled pixel dimensions."
    ),
    DIGITALLY_RESAMPLED(
        label = "Interpolated export enabled",
        shortDescription = "Enlarged digital crop interpolated for target output resolution."
    )
}

data class FramingConfig(
    val lensSlot: LensType,
    val cropFactor: Float, // 1.0f or 2.0f
    val is2xActive: Boolean,
    val effectiveFocalLength35mm: Int,
    val cropStatus: HonestCropStatus,
    val estimatedResolutionMp: Float
) {
    companion object {
        fun create(lens: LensType, is2x: Boolean, baseSpec: CameraSpec?): FramingConfig {
            val factor = if (is2x) 2.0f else 1.0f
            val baseFocal = baseSpec?.equivalentFocalLength35mm ?: lens.approxFocalLength35mm
            val effectiveFocal = (baseFocal * factor).toInt()
            val baseMp = baseSpec?.maxResolutionMp ?: 0.0f
            val effMp = if (is2x) (baseMp / 4.0f) else baseMp

            val status = if (!is2x) {
                HonestCropStatus.FULL_SENSOR
            } else {
                HonestCropStatus.NATIVE_NO_RESAMPLE
            }

            return FramingConfig(
                lensSlot = lens,
                cropFactor = factor,
                is2xActive = is2x,
                effectiveFocalLength35mm = effectiveFocal,
                cropStatus = status,
                estimatedResolutionMp = effMp
            )
        }
    }
}

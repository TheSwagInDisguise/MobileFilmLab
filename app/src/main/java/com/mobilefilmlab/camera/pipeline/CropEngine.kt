package com.mobilefilmlab.camera.pipeline

import android.graphics.Rect
import com.mobilefilmlab.camera.data.model.HonestCropStatus

data class CropBounds(val left: Int, val top: Int, val right: Int, val bottom: Int) {
    val width: Int get() = right - left
    val height: Int get() = bottom - top
}

object CropEngine {

    fun computeCropBounds(totalWidth: Int, totalHeight: Int, cropFactor: Float): CropBounds {
        if (cropFactor <= 1.0f) return CropBounds(0, 0, totalWidth, totalHeight)
        val cropWidth = (totalWidth / cropFactor).toInt()
        val cropHeight = (totalHeight / cropFactor).toInt()
        val left = (totalWidth - cropWidth) / 2
        val top = (totalHeight - cropHeight) / 2
        return CropBounds(left, top, left + cropWidth, top + cropHeight)
    }

    fun computeCropRegion(activeArray: Rect, cropFactor: Float): Rect {
        val bounds = computeCropBounds(activeArray.width(), activeArray.height(), cropFactor)
        return Rect(
            activeArray.left + bounds.left,
            activeArray.top + bounds.top,
            activeArray.left + bounds.right,
            activeArray.top + bounds.bottom
        )
    }

    fun classifyCrop(
        cropFactor: Float,
        targetOutputWidth: Int,
        actualCropWidth: Int
    ): HonestCropStatus {
        return when {
            cropFactor <= 1.01f -> HonestCropStatus.FULL_SENSOR
            targetOutputWidth == actualCropWidth -> HonestCropStatus.NATIVE_NO_RESAMPLE
            targetOutputWidth < actualCropWidth -> HonestCropStatus.REDUCED_RESOLUTION
            else -> HonestCropStatus.DIGITALLY_RESAMPLED
        }
    }

    fun calculateEquivalentFocal(baseFocal35mm: Int, cropFactor: Float): Int {
        return (baseFocal35mm * cropFactor).toInt()
    }
}

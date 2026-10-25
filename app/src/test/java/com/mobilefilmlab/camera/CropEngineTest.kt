package com.mobilefilmlab.camera

import com.mobilefilmlab.camera.data.model.HonestCropStatus
import com.mobilefilmlab.camera.pipeline.CropEngine
import org.junit.Assert.assertEquals
import org.junit.Test

class CropEngineTest {

    @Test
    fun testComputeCropBounds() {
        val bounds = CropEngine.computeCropBounds(4000, 3000, 2.0f)
        assertEquals(2000, bounds.width)
        assertEquals(1500, bounds.height)
        assertEquals(1000, bounds.left)
        assertEquals(750, bounds.top)
    }

    @Test
    fun testFullSensorCropClassification() {
        val status = CropEngine.classifyCrop(1.0f, 4000, 4000)
        assertEquals(HonestCropStatus.FULL_SENSOR, status)
    }

    @Test
    fun testNativeNoResampleClassification() {
        val status = CropEngine.classifyCrop(2.0f, 2000, 2000)
        assertEquals(HonestCropStatus.NATIVE_NO_RESAMPLE, status)
    }

    @Test
    fun testDigitallyResampledClassification() {
        val status = CropEngine.classifyCrop(2.0f, 4000, 2000)
        assertEquals(HonestCropStatus.DIGITALLY_RESAMPLED, status)
    }

    @Test
    fun testEquivalentFocalCalculation() {
        val base23 = CropEngine.calculateEquivalentFocal(23, 2.0f)
        assertEquals(46, base23)

        val base70 = CropEngine.calculateEquivalentFocal(70, 2.0f)
        assertEquals(140, base70)
    }
}

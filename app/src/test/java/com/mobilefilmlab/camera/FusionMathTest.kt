package com.mobilefilmlab.camera

import com.mobilefilmlab.camera.processing.FusionMath as F
import org.junit.Assert.*
import org.junit.Test

class FusionMathTest {
    @Test fun exactSrgbTransferRoundTripsAllJpegChannelValues() {
        for(value in 0..255) assertEquals(value,F.toSrgb(F.toLinear(value)))
        assertEquals(10/255.0/12.92,F.toLinear(10),1e-10)
    }
    @Test fun realExposureProductsAreUsedInsteadOfAssumingBracketOrderOrStops() {
        assertArrayEquals(doubleArrayOf(2.0,1.0,.5),F.normalizers(listOf(500.0,1000.0,2000.0))!!,1e-10)
        assertArrayEquals(doubleArrayOf(.5,1.0,2.0),F.normalizers(listOf(2000.0,1000.0,500.0))!!,1e-10)
    }
    @Test fun absentNonFiniteNonPositiveOrUnsafeRatiosCannotBeInvented() {
        for(values in listOf(listOf(null,1.0,2.0),listOf(0.0,1.0,2.0),listOf(-1.0,1.0,2.0),listOf(Double.NaN,1.0,2.0),listOf(1.0,1000.0,2.0),listOf(Double.POSITIVE_INFINITY,1.0,2.0))) assertNull(F.normalizers(values))
    }
    @Test fun recoveredHighlightsKeepHeadroomUntilDisplayMappingWithAn18PercentAnchor() {
        assertEquals(.18,F.displayTone(.18,4.0),1e-8)
        assertEquals(1.0,F.displayTone(4.0,4.0),1e-8)
        assertTrue(F.displayTone(1.1,2.0)<F.displayTone(1.8,2.0))
        val values=(0..800).map {F.displayTone(it/100.0,4.0)}
        assertTrue(values.zipWithNext().all {it.first<=it.second})
        for(i in 0..100) assertEquals(i/100.0,F.displayTone(i/100.0,1.0),1e-10)
    }
}

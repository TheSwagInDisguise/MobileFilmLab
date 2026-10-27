package com.mobilefilmlab.camera

import com.mobilefilmlab.camera.camera.ResolutionSelector as R
import org.junit.Assert.*
import org.junit.Test

/** Tests the actual production selector, not a copy of its logic. */
class HighResolutionSelectorTest {
    @Test fun prefersExposedStockSizedNativeStream() {
        assertEquals(R.Candidate(6120,4596),R.select(listOf(R.Candidate(4096,3072),R.Candidate(6120,4596),R.Candidate(8160,6120,true))))
    }
    @Test fun burstMemoryLimitIsRespected() {
        assertEquals(4096,R.select(listOf(R.Candidate(4096,3072),R.Candidate(8160,6120)),12_600_000)?.width)
    }
    @Test fun absent28mpIsNeverInvented() { assertEquals(R.Candidate(4096,3072),R.select(listOf(R.Candidate(4096,3072)))) }
    @Test fun emptyMapFailsInsteadOfInventingACameraFrame() { assertNull(R.select(emptyList())) }
    @Test fun maximumPixelModeIsNotLost() { assertTrue(R.select(listOf(R.Candidate(6120,4596,true)))!!.maximumMode) }
    @Test fun pixelAreaUsesLongArithmetic() { assertEquals(1920,R.select(listOf(R.Candidate(1920,1080),R.Candidate(100000,100000)))!!.width) }
}

package com.mobilefilmlab.camera

import android.util.Size
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class HighResolutionSelectorTest {

    data class Candidate(val width: Int, val height: Int, val isFromMaxResolutionMap: Boolean)

    private fun selectOptimalResolution(candidates: List<Candidate>): Candidate {
        // Priority 1: Match stock Oppo 28MP mode (6120x4596 or 4596x6120 ~ 28.1MP)
        val stock28Mp = candidates.firstOrNull { c ->
            (c.width == 6120 && c.height == 4596) ||
            (c.width == 4596 && c.height == 6120) ||
            ((c.width.toLong() * c.height) in 27_000_000L..29_500_000L)
        }

        // Priority 2: Maximum available resolution up to 55MP
        return stock28Mp ?: candidates
            .filter { (it.width.toLong() * it.height) <= 55_000_000L }
            .maxByOrNull { it.width.toLong() * it.height }
            ?: candidates.firstOrNull()
            ?: Candidate(4000, 3000, false)
    }

    @Test
    fun testSelects28MpStockModeWhenPresent() {
        val candidates = listOf(
            Candidate(1920, 1080, false),
            Candidate(4096, 3072, false), // 12.5 MP default binned
            Candidate(6120, 4596, false), // 28.1 MP Oppo stock mode
            Candidate(8160, 6120, true)   // 50 MP full remosaic
        )

        val selected = selectOptimalResolution(candidates)
        assertEquals(6120, selected.width)
        assertEquals(4596, selected.height)
    }

    @Test
    fun testSelectsMaxResolutionWhen28MpAbsent() {
        val candidates = listOf(
            Candidate(1920, 1080, false),
            Candidate(4096, 3072, false),
            Candidate(8160, 6120, true) // 50 MP
        )

        val selected = selectOptimalResolution(candidates)
        assertEquals(8160, selected.width)
        assertEquals(6120, selected.height)
    }

    @Test
    fun testFallbackToDefault12MpWhenNoHiRes() {
        val candidates = listOf(
            Candidate(1920, 1080, false),
            Candidate(4096, 3072, false)
        )

        val selected = selectOptimalResolution(candidates)
        assertEquals(4096, selected.width)
        assertEquals(3072, selected.height)
    }

    @Test
    fun testPortraitUprightMath() {
        // Sensor output is 6120x4596 (landscape sensor)
        val sensorW = 6120
        val sensorH = 4596
        
        // When rotated 90 degrees to upright portrait:
        val portraitW = sensorH
        val portraitH = sensorW
        assertEquals(4596, portraitW)
        assertEquals(6120, portraitH)
        assertEquals(28_127_520L, portraitW.toLong() * portraitH)
    }
}

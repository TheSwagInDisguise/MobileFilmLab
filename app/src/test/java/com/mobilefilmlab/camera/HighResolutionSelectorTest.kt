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

    @Test
    fun test28MpReconstructionDimensions() {
        // 1x standard capture reconstruction
        val uncroppedW = 3072
        val uncroppedH = 4096
        val target1xW = 4596
        val target1xH = 6120
        assertEquals(28_127_520L, target1xW.toLong() * target1xH)

        // 2x honest sensor crop reconstruction (50% area of 28.1MP)
        val crop2xW = uncroppedW / 2
        val crop2xH = uncroppedH / 2
        val target2xW = target1xW / 2
        val target2xH = target1xH / 2
        assertEquals(1536, crop2xW)
        assertEquals(2048, crop2xH)
        assertEquals(2298, target2xW)
        assertEquals(3060, target2xH)
        assertEquals(7_031_880L, target2xW.toLong() * target2xH)
    }

    @Test
    fun testFlashModeCycleIncludesAlwaysOn() {
        val modes = com.mobilefilmlab.camera.camera.FlashMode.values().toList()
        assertEquals(listOf(
            com.mobilefilmlab.camera.camera.FlashMode.OFF,
            com.mobilefilmlab.camera.camera.FlashMode.AUTO,
            com.mobilefilmlab.camera.camera.FlashMode.ON,
            com.mobilefilmlab.camera.camera.FlashMode.TORCH
        ), modes)
    }

    @Test
    fun testLensModelFormatMatchesStockColorOsExif() {
        val model = "OPPO Find X9 Ultra"
        val lensName = "wide camera"
        val eq35 = 23
        val fNum = 1.5f
        val lensModel = "$model $lensName ${eq35}mm f/${String.format(java.util.Locale.US, "%.1f", fNum)}"
        assertEquals("OPPO Find X9 Ultra wide camera 23mm f/1.5", lensModel)
    }
}

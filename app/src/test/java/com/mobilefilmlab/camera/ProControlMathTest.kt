package com.mobilefilmlab.camera

import com.mobilefilmlab.camera.camera.ProControlMath as M
import com.mobilefilmlab.camera.data.model.CameraSpec
import com.mobilefilmlab.camera.data.model.LensType
import org.junit.Assert.*
import org.junit.Test

internal fun proTestCamera() = CameraSpec("test",true,false,focalLengthMm=6f,equivalentFocalLength35mm=23,
    sensorWidthMm=10f,sensorHeightMm=8f,apertureFNumber=1.8f,activeArrayWidth=4000,activeArrayHeight=3000,
    maxResolutionMp=12f,hasRawSupport=false,lensType=LensType.WIDE,displayName="Test only",
    manualSensor=true,manualWb=true,minIso=100,maxIso=6400,minExposureNs=125_000L,
    maxExposureNs=30_000_000_000L,maxFrameDurationNs=30_000_000_000L,focusModes=listOf("AF-C","AF-S"))

class ProControlMathTest {
    @Test fun isoSliderUsesGeometricMidpointNotLinearSensitivity() {
        assertEquals(800L,M.logarithmicValue(.5f,100,6400))
        assertEquals(.5f,M.logarithmicPosition(800,100,6400),.0001f)
    }
    @Test fun slidersIncludeExactHardwareEndpointsAndClampOverscroll() {
        assertEquals(100L,M.logarithmicValue(-1f,100,6400))
        assertEquals(6400L,M.logarithmicValue(2f,100,6400))
        assertEquals(0f,M.logarithmicPosition(1,100,6400),0f)
        assertEquals(1f,M.logarithmicPosition(99999,100,6400),0f)
    }
    @Test fun logSliderIsMonotonicAndRoundTripsAcrossExposureRange() {
        var previous=0L
        (0..100).forEach {
            val value=M.logarithmicValue(it/100f,125_000L,30_000_000_000L)
            assertTrue(value>=previous); previous=value
            assertEquals(it/100f,M.logarithmicPosition(value,125_000L,30_000_000_000L),.0001f)
        }
    }
    @Test fun fixedRangesAndNonFiniteInputDoNotProduceNanOrOverflow() {
        assertEquals(100L,M.logarithmicValue(.5f,100,100))
        assertEquals(0f,M.logarithmicPosition(100,100,100),0f)
        assertEquals(100L,M.logarithmicValue(Float.NaN,100,6400))
        assertEquals(100L,M.logarithmicValue(Float.POSITIVE_INFINITY,100,6400))
    }
    @Test fun evSnapsToReportedStepsAndBounds() {
        val s=proTestCamera()
        assertEquals(2,M.evSteps(.7f,s))
        assertEquals(-6,M.evSteps(-10f,s))
        assertEquals(6,M.evSteps(10f,s))
        assertEquals(0,M.evSteps(Float.NaN,s))
        assertFalse(M.hasEv(s.copy(evStep=0f)))
        assertFalse(M.hasEv(s.copy(minEv=0,maxEv=0)))
    }
    @Test fun exposureBoundsRespectSensorAndFrameDurationNotJustUiRange() {
        val s=proTestCamera().copy(minExposureNs=500_000L,maxExposureNs=20_000_000_000L,maxFrameDurationNs=2_000_000_000L)
        assertEquals(500_000L..2_000_000_000L,M.exposureRange(s))
        assertEquals(30_000_000_000L,M.exposureRange(s.copy(maxExposureNs=90_000_000_000L,maxFrameDurationNs=null)).last)
    }
    @Test fun malformedBoundsRemainSafeAndNonEmpty() {
        val s=proTestCamera().copy(minIso=500,maxIso=100,minExposureNs=200,maxExposureNs=100,maxFrameDurationNs=0)
        assertEquals(500..500,M.isoRange(s))
        assertEquals(100L..100L,M.exposureRange(s))
    }
    @Test fun whiteBalanceQuantizesHundredKelvinStepsWithExplicitAutoElsewhere() {
        assertEquals(2500,M.kelvin(1000))
        assertEquals(5600,M.kelvin(5580))
        assertEquals(9900,M.kelvin(15000))
    }
    @Test fun longExposureDeadlineIncludesAnInflightPreviewAndStill() {
        assertEquals(15_000L,M.captureTimeoutMs(null))
        assertEquals(15_000L,M.captureTimeoutMs(1_000_000_000L))
        assertEquals(30_000L,M.captureTimeoutMs(10_000_000_000L))
        assertEquals(70_000L,M.captureTimeoutMs(30_000_000_000L))
        assertEquals(60_000L,M.captureTimeoutMs(null,true))
    }
    @Test fun displayedExposureValuesAreLocaleIndependentAndPhotographic() {
        assertEquals("1/125",M.exposureLabel(8_000_000L))
        assertEquals("0.50 s",M.exposureLabel(500_000_000L))
        assertEquals("30 s",M.exposureLabel(30_000_000_000L))
        assertEquals("+0.3",M.evLabel(1f/3f))
    }
}

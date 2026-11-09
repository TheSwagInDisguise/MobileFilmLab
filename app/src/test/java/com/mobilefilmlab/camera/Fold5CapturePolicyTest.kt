package com.mobilefilmlab.camera

import com.mobilefilmlab.camera.camera.NativeCapturePolicy
import com.mobilefilmlab.camera.camera.ResolutionSelector.Candidate
import com.mobilefilmlab.camera.data.model.DeviceProfile
import com.mobilefilmlab.camera.settings.*
import org.junit.Assert.*
import org.junit.Test

class Fold5CapturePolicyTest {
    private val fold=DeviceProfile.GALAXY_Z_FOLD5
    private val sizes=listOf(Candidate(8160,6120,true),Candidate(4000,3000),Candidate(1920,1080),Candidate(6120,4596,highResolution=true))
    @Test fun recognisesSamsungFold5RegionalModelFamilyWithoutMatchingOtherBrandsOrFolds() {
        listOf("SM-F946B","SM-F946B/DS","SM-F946U","SM-F946U1","SM-F946N","SM-F946W","SM-F9460").forEach {
            assertEquals(fold,DeviceProfile.detect("Samsung",it))
        }
        assertEquals(fold,DeviceProfile.detect(" SAMSUNG "," sm-f946b "))
        assertEquals(DeviceProfile.GENERIC,DeviceProfile.detect("Oppo","SM-F946B"))
        assertEquals(DeviceProfile.GENERIC,DeviceProfile.detect("Samsung","SM-F956B"))
        assertEquals(DeviceProfile.GENERIC,DeviceProfile.detect(null,null))
    }
    @Test fun balancedFold5PrefersActualSmallNormalStreamsAndNeverRetriesLargerOnes() {
        val attempts=NativeCapturePolicy.attempts(sizes,CaptureSettings(),fold)
        assertEquals(listOf(Candidate(4000,3000),Candidate(1920,1080)),attempts)
        assertTrue(attempts.all {!it.maximumMode && it.area<=12_600_000L})
    }
    @Test fun maximumNativeIsExplicitAndNeverInventsMarketingPixelCounts() {
        val attempts=NativeCapturePolicy.attempts(sizes,CaptureSettings(captureResolution=CaptureResolution.MAXIMUM_NATIVE),fold)
        assertEquals(Candidate(8160,6120,true),attempts.first())
        assertTrue(attempts.all {it in sizes})
        assertTrue(attempts.zipWithNext().all {it.first.area>=it.second.area})
    }
    @Test fun oppoAndOtherDevicesKeepTheirOriginalNativePreference() {
        assertEquals(6120,NativeCapturePolicy.attempts(sizes,CaptureSettings(),DeviceProfile.GENERIC).first().width)
    }
    @Test fun fusionAndZslDoNotAccidentallyFallBackToLargeOrMaximumModeBuffers() {
        for(s in listOf(CaptureSettings(pipeline=CapturePipeline.BURST),CaptureSettings(zsl=true))) {
            assertEquals(4000,NativeCapturePolicy.attempts(sizes,s.copy(captureResolution=CaptureResolution.MAXIMUM_NATIVE),DeviceProfile.GENERIC).first().width)
        }
    }
    @Test fun absentSmallSizesUseTheSmallestRealStreamAndEmptyOrUnsafeMapsFailClosed() {
        val real=Candidate(4608,3456)
        assertEquals(listOf(real),NativeCapturePolicy.attempts(listOf(real,Candidate(8160,6120)),CaptureSettings(),fold))
        assertTrue(NativeCapturePolicy.attempts(emptyList(),CaptureSettings(),fold).isEmpty())
        assertTrue(NativeCapturePolicy.attempts(listOf(Candidate(20000,20000),Candidate(0,0)),CaptureSettings(),fold).isEmpty())
    }
    @Test fun ordinaryStreamsWinOverStallingHighResolutionStreamsWithinTheBudget() {
        assertEquals(3840,NativeCapturePolicy.attempts(listOf(Candidate(4000,3000,highResolution=true),Candidate(3840,2160)),CaptureSettings(),fold).first().width)
    }
}

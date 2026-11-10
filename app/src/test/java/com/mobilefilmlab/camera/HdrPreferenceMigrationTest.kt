package com.mobilefilmlab.camera

import android.content.Context
import com.mobilefilmlab.camera.data.model.*
import com.mobilefilmlab.camera.settings.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34])
class HdrPreferenceMigrationTest {
    private val context get()=RuntimeEnvironment.getApplication()
    private val prefs get()=context.getSharedPreferences("capture_settings",Context.MODE_PRIVATE)
    @Before fun clean() {prefs.edit().clear().commit()}
    private fun fold()=SettingsStore(context,DeviceProfile.GALAXY_Z_FOLD5)
    @Test fun freshFold5JpegGetsHandheldSmartHdrNotSlowerQuality() {
        val s=fold().load(LensType.WIDE)
        assertEquals(CapturePipeline.SMART_HDR,s.pipeline);assertEquals(CaptureFormat.JPEG,s.format)
        assertFalse(s.hdrSaveOriginal);assertFalse(s.zsl)
    }
    @Test fun legacyStandardIsMigratedOnceButSubsequentOriginalOptOutSurvivesReload() {
        prefs.edit().putString("pipeline","STANDARD").putFloat("WIDE_r",1.04f).commit()
        val store=fold();assertEquals(CapturePipeline.SMART_HDR,store.load(LensType.WIDE).pipeline)
        assertEquals(1.04f,store.load(LensType.WIDE).calibration.red,.0001f)
        store.save(store.load(LensType.WIDE).copy(pipeline=CapturePipeline.STANDARD),LensType.WIDE)
        assertEquals(CapturePipeline.STANDARD,fold().load(LensType.WIDE).pipeline)
    }
    @Test fun existingOtherPipelineChoicesAreNeverOverwritten() {
        for(p in listOf(CapturePipeline.BURST,CapturePipeline.OEM_HDR,CapturePipeline.OEM_NIGHT,CapturePipeline.QUALITY_HDR)) {
            prefs.edit().clear().putString("pipeline",p.name).commit()
            assertEquals(p,fold().load(LensType.WIDE).pipeline)
        }
    }
    @Test fun nonFold5KeepsOriginalDefaultAndExplicitHdrCanStillBeSaved() {
        val generic=SettingsStore(context,DeviceProfile.GENERIC)
        assertEquals(CapturePipeline.STANDARD,generic.load(LensType.WIDE).pipeline)
        generic.save(CaptureSettings(pipeline=CapturePipeline.QUALITY_HDR,hdrSaveOriginal=true),LensType.WIDE,"0")
        val loaded=SettingsStore(context,DeviceProfile.GENERIC).load(LensType.WIDE,"0")
        assertEquals(CapturePipeline.QUALITY_HDR,loaded.pipeline);assertTrue(loaded.hdrSaveOriginal)
    }
    @Test fun existingRawPreferenceRemainsGenuineStandardCaptureOnFold5() {
        prefs.edit().putString("format","RAW_JPEG").putBoolean("zsl",true).putString("output","UPSCALE_28").commit()
        val s=fold().load(LensType.WIDE)
        assertEquals(CaptureFormat.RAW_JPEG,s.format);assertEquals(CapturePipeline.STANDARD,s.pipeline)
        assertFalse(s.zsl);assertEquals(OutputSize.NATIVE,s.outputSize)
    }
}

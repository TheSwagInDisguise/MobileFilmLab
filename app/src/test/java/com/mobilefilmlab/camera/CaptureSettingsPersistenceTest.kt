package com.mobilefilmlab.camera

import android.content.Context
import com.mobilefilmlab.camera.data.model.LensType
import com.mobilefilmlab.camera.settings.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34])
class CaptureSettingsPersistenceTest {
    private fun context()=RuntimeEnvironment.getApplication()
    @Test fun anExistingInstallWithoutAFormatPreferenceStillDefaultsToJpeg() {
        val prefs=context().getSharedPreferences("capture_settings",Context.MODE_PRIVATE)
        prefs.edit().clear().putFloat("WIDE_r",1.06f).commit()
        val loaded=SettingsStore(context()).load(LensType.WIDE,"0")
        assertEquals(CaptureFormat.JPEG,loaded.format);assertEquals(1.06f,loaded.calibration.red,.0001f)
    }
    @Test fun fileFormatAndExactCameraIdSurviveReload() {
        val store=SettingsStore(context());store.save(CaptureSettings(format=CaptureFormat.RAW_JPEG),LensType.TELEPHOTO,"logical/physical:7")
        val reload=SettingsStore(context());assertEquals("logical/physical:7",reload.selectedCameraId());assertEquals(CaptureFormat.RAW_JPEG,reload.load(LensType.TELEPHOTO,"logical/physical:7").format)
    }
    @Test fun twoCamerasInTheSameLegacyCategoryHaveIndependentGains() {
        val store=SettingsStore(context())
        store.save(CaptureSettings(calibration=LensCalibration(red=1.08f)),LensType.TELEPHOTO,"3")
        store.save(CaptureSettings(calibration=LensCalibration(red=.94f)),LensType.TELEPHOTO,"4")
        assertEquals(1.08f,store.load(LensType.TELEPHOTO,"3").calibration.red,.0001f)
        assertEquals(.94f,store.load(LensType.TELEPHOTO,"4").calibration.red,.0001f)
    }
    @Test fun legacyGainsRemainAsFallbackUntilACameraGetsItsOwnSettings() {
        val prefs=context().getSharedPreferences("capture_settings",Context.MODE_PRIVATE);prefs.edit().clear().putFloat("WIDE_b",1.09f).commit()
        val store=SettingsStore(context());assertEquals(1.09f,store.load(LensType.WIDE,"0").calibration.blue,.0001f)
        store.save(CaptureSettings(calibration=LensCalibration(blue=.92f)),LensType.WIDE,"0")
        assertEquals(.92f,store.load(LensType.WIDE,"0").calibration.blue,.0001f)
        assertEquals(1.09f,store.load(LensType.WIDE,"1").calibration.blue,.0001f)
    }
}

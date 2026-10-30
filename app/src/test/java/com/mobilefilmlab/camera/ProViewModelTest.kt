package com.mobilefilmlab.camera

import android.os.Looper
import androidx.lifecycle.ViewModelStore
import com.mobilefilmlab.camera.data.model.CameraSpec
import com.mobilefilmlab.camera.ui.viewmodel.CameraMode
import com.mobilefilmlab.camera.ui.viewmodel.CameraViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.util.ReflectionHelpers

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34])
class ProViewModelTest {
    private lateinit var vm: CameraViewModel
    private lateinit var store: ViewModelStore
    private lateinit var camera: MutableStateFlow<CameraSpec?>
    @Before fun setup() {
        vm=CameraViewModel(RuntimeEnvironment.getApplication())
        store=ViewModelStore().apply { put("pro-test",vm) }
        vm.onPause()
        camera=ReflectionHelpers.getField(vm.cameraEngine,"_activeCameraSpec")
        camera.value=proTestCamera()
        shadowOf(Looper.getMainLooper()).idle()
        vm.setMode(CameraMode.PRO)
    }
    @After fun close() { store.clear(); shadowOf(Looper.getMainLooper()).idle() }

    @Test fun directSettersClampHardwareValuesAndRestoreAutoWithoutCycling() {
        vm.setIso(90000); assertEquals(6400,vm.iso.value)
        vm.setShutter(90_000_000_000L); assertEquals(30_000_000_000L,vm.exposureTimeNs.value)
        vm.setWhiteBalance(5580); assertEquals(5600,vm.whiteBalanceKelvin.value)
        vm.setIso(null); vm.setShutter(null); vm.setWhiteBalance(0)
        assertNull(vm.iso.value); assertNull(vm.exposureTimeNs.value); assertEquals(0,vm.whiteBalanceKelvin.value)
    }
    @Test fun evQuantizesAndCannotChangeDuringManualExposureOrCapture() {
        vm.setEvCompensation(.7f); assertEquals(2f/3,vm.evCompensation.value,.0001f)
        vm.setIso(200); vm.setEvCompensation(-2f)
        assertEquals(2f/3,vm.evCompensation.value,.0001f)
        vm.isCapturing.value=true; vm.setIso(400); vm.setShutter(8_000_000L)
        assertEquals(200,vm.iso.value); assertNull(vm.exposureTimeNs.value)
    }
    @Test fun cameraChangesReboundValuesAndClearUnsupportedManualControls() {
        vm.setIso(6400); vm.setShutter(20_000_000_000L); vm.setWhiteBalance(5600)
        camera.value=proTestCamera().copy(cameraId="second",maxIso=800,maxFrameDurationNs=1_000_000_000L)
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(800,vm.iso.value); assertEquals(1_000_000_000L,vm.exposureTimeNs.value)
        camera.value=proTestCamera().copy(cameraId="no-manual",manualSensor=false,manualWb=false,focusModes=emptyList())
        shadowOf(Looper.getMainLooper()).idle()
        assertNull(vm.iso.value); assertNull(vm.exposureTimeNs.value); assertEquals(0,vm.whiteBalanceKelvin.value)
        vm.setIso(400); assertNull(vm.iso.value)
    }
    @Test fun focusModesAreExplicitSupportedChoicesAndAutoResetsExposureState() {
        vm.setFocusMode("MACRO"); assertEquals("AF-C",vm.focusMode.value)
        vm.setFocusMode("AF-S"); assertEquals("AF-S",vm.focusMode.value)
        vm.setIso(400); vm.setShutter(8_000_000L); vm.setMode(CameraMode.AUTO)
        assertNull(vm.iso.value); assertNull(vm.exposureTimeNs.value)
        assertEquals("AF-C",vm.focusMode.value)
    }
}

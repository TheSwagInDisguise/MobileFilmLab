package com.mobilefilmlab.camera

import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import com.mobilefilmlab.camera.camera.ProControlMath
import com.mobilefilmlab.camera.settings.CaptureSettings
import com.mobilefilmlab.camera.settings.OutputSize
import com.mobilefilmlab.camera.ui.components.ManualControlStrip
import com.mobilefilmlab.camera.ui.components.SettingsDialog
import com.mobilefilmlab.camera.ui.theme.MobileFilmLabTheme
import com.mobilefilmlab.camera.ui.theme.TextPrimary
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34],qualifiers="w360dp-h780dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ReadableProUiTest {
    @get:Rule val compose=createComposeRule()

    @Test fun selectingIsoOpensEditorWithoutCyclingAndSliderUsesLogScale() {
        var iso by mutableStateOf<Int?>(null)
        compose.setContent { MobileFilmLabTheme {
            ManualControlStrip(0f,iso,null,0,"AF-C",proTestCamera(),true,{}, { iso=it },{},{},{},{})
        } }
        compose.onNodeWithContentDescription("Sensor sensitivity control").performClick()
        compose.runOnIdle { assertNull("Opening a tab must not change exposure",iso) }
        compose.onNodeWithContentDescription("ISO").assertIsNotEnabled()
        compose.onNodeWithContentDescription("Auto control").performClick()
        compose.onNodeWithContentDescription("ISO").assertIsEnabled()
            .performSemanticsAction(SemanticsActions.SetProgress) { it(.75f) }
        compose.runOnIdle { assertEquals(ProControlMath.logarithmicValue(.75f,100,6400).toInt(),iso) }
        compose.onNodeWithContentDescription("Auto control").performClick()
        compose.runOnIdle { assertNull(iso) }
    }
    @Test fun evSliderSnapsToCameraStepsAndExplainsManualExposureLockout() {
        var ev by mutableStateOf(0f)
        var iso by mutableStateOf<Int?>(null)
        val spec=proTestCamera()
        compose.setContent { MobileFilmLabTheme {
            ManualControlStrip(ev,iso,null,0,"AF-C",spec,true,{ ev=it },{ iso=it },{},{},{},{})
        } }
        compose.onNodeWithContentDescription("Exposure compensation")
            .performSemanticsAction(SemanticsActions.SetProgress) { it(2.2f) }
        compose.runOnIdle { assertEquals(2*spec.evStep,ev,.0001f); iso=200 }
        compose.onNodeWithContentDescription("Exposure compensation").assertIsNotEnabled()
        compose.onNodeWithText("Set ISO and SEC to Auto to use EV compensation.").assertIsDisplayed()
    }
    @Test fun missingManualCapabilityDisablesIsoWithAVisibleReason() {
        compose.setContent { MobileFilmLabTheme {
            ManualControlStrip(0f,null,null,0,"N/A",proTestCamera().copy(manualSensor=false,focusModes=emptyList()),true,{},{},{},{},{},{})
        } }
        compose.onNodeWithContentDescription("Sensor sensitivity control").performClick()
        compose.onNodeWithContentDescription("Auto control").assertIsNotEnabled()
        compose.onNodeWithText("Manual ISO is not exposed on this camera.").assertIsDisplayed()
    }
    @Test fun shutterAndWhiteBalanceHaveSlidersAndFocusIsADirectChoice() {
        var shutter by mutableStateOf<Long?>(null)
        var kelvin by mutableStateOf(0)
        var focus by mutableStateOf("AF-C")
        compose.setContent { MobileFilmLabTheme {
            ManualControlStrip(0f,null,shutter,kelvin,focus,proTestCamera(),true,{},{},{ shutter=it },{ kelvin=it },{ focus=it },{})
        } }
        compose.onNodeWithContentDescription("Shutter speed control").performClick()
        compose.onNodeWithContentDescription("Auto control").performClick()
        compose.onNodeWithContentDescription("Shutter speed")
            .performSemanticsAction(SemanticsActions.SetProgress) { it(1f) }
        compose.runOnIdle { assertEquals(30_000_000_000L,shutter) }
        compose.onNodeWithContentDescription("White balance control").performClick()
        compose.onNodeWithContentDescription("Auto control").performClick()
        compose.onNodeWithContentDescription("White balance temperature")
            .performSemanticsAction(SemanticsActions.SetProgress) { it(6500f) }
        compose.runOnIdle { assertEquals(6500,kelvin) }
        compose.onNodeWithContentDescription("Autofocus mode control").performClick()
        compose.onNodeWithText("AF-S").performClick()
        compose.runOnIdle { assertEquals("AF-S",focus) }
        compose.onNodeWithText("MACRO").assertDoesNotExist()
    }

    @Test fun themeSuppliesLightContentAndSettingsKeepsDoneVisibleAtLargeFontScale() {
        var contentColor=Color.Unspecified
        var settings by mutableStateOf(CaptureSettings())
        compose.setContent { MobileFilmLabTheme {
            val color=LocalContentColor.current
            SideEffect { contentColor=color }
            val density=LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density,fontScale=1.5f)) {
                SettingsDialog(settings,proTestCamera(),1,"Standard Camera2","Normal","Not checked",null,
                    { settings=it },{},{},{})
            }
        } }
        compose.runOnIdle { assertEquals(TextPrimary,contentColor) }
        compose.onNodeWithText("Done").assertIsDisplayed()
        compose.onNodeWithText("Output pixels").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(OutputSize.NATIVE.label).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Done").assertIsDisplayed()
    }
}

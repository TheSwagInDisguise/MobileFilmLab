package com.mobilefilmlab.camera

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.mobilefilmlab.camera.ui.theme.*
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.pow

class DarkThemeContrastTest {
    private fun luminance(c: Color): Double {
        val rgb=c.toArgb()
        fun channel(shift: Int): Double {
            val v=(rgb shr shift and 255)/255.0
            return if(v<=.04045) v/12.92 else ((v+.055)/1.055).pow(2.4)
        }
        return .2126*channel(16)+.7152*channel(8)+.0722*channel(0)
    }
    private fun contrast(a: Color,b: Color): Double {
        val x=luminance(a); val y=luminance(b)
        return (maxOf(x,y)+.05)/(minOf(x,y)+.05)
    }
    @Test fun allBodyAndHelperTokensMeetNormalTextContrastOnEveryDarkPanel() {
        listOf(TextPrimary,TextSecondary,TextMuted).forEach { text ->
            listOf(BgDark,SurfaceDark,SurfaceCard).forEach { bg ->
                assertTrue("Dark-theme text contrast ${contrast(text,bg)} is below 4.5:1",contrast(text,bg)>=4.5)
            }
        }
    }
    @Test fun goldReadoutsAndBlackOnGoldSelectionsMeetNormalTextContrast() {
        assertTrue(contrast(AccentGold,SurfaceDark)>=4.5)
        assertTrue(contrast(Color.Black,AccentGold)>=4.5)
    }
}

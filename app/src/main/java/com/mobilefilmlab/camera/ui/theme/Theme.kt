package com.mobilefilmlab.camera.ui.theme

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = AccentGold,
    secondary = AccentOrange,
    tertiary = AccentBlue,
    background = BgDark,
    surface = SurfaceDark,
    surfaceVariant = SurfaceCard,
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    onSurfaceVariant = TextSecondary,
    outline = TextMuted
)

@Composable
fun MobileFilmLabTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = DarkColorScheme, typography = Typography) {
        // MaterialTheme supplies a colour scheme, but is not a Surface. Custom dark
        // panels must not inherit Compose's default black LocalContentColor.
        CompositionLocalProvider(LocalContentColor provides TextPrimary, content = content)
    }
}

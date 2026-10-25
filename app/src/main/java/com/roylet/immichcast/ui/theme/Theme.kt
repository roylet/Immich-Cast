package com.roylet.immichcast.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Typography
import androidx.tv.material3.darkColorScheme

object Palette {
    val Background = Color(0xFF0B0D12)
    val Surface = Color(0xFF151922)
    val SurfaceRaised = Color(0xFF1D2230)
    val Primary = Color(0xFF8FA8FF)
    val OnPrimary = Color(0xFF0B1338)
    val Text = Color(0xFFE8EAF2)
    val TextMuted = Color(0xFF9AA1B5)
    val Danger = Color(0xFFFF7A7A)
    val Success = Color(0xFF7DDC9C)
}

private val colors = darkColorScheme(
    primary = Palette.Primary,
    onPrimary = Palette.OnPrimary,
    secondary = Palette.Primary,
    background = Palette.Background,
    onBackground = Palette.Text,
    surface = Palette.Surface,
    onSurface = Palette.Text,
    surfaceVariant = Palette.SurfaceRaised,
    onSurfaceVariant = Palette.TextMuted,
    error = Palette.Danger,
    border = Color(0xFF2A3042),
)

private val base = Typography()

private val typography = base.copy(
    displaySmall = base.displaySmall.copy(fontWeight = FontWeight.SemiBold),
    headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.SemiBold),
    headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
    titleLarge = base.titleLarge.copy(fontWeight = FontWeight.SemiBold),
    labelSmall = TextStyle(fontSize = 11.sp, letterSpacing = 1.2.sp, fontWeight = FontWeight.Medium),
)

@Composable
fun ImmichCastTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colors, typography = typography) {
        // tv-material only sets a content colour inside Surfaces; default all text to on-background.
        CompositionLocalProvider(LocalContentColor provides colors.onBackground, content = content)
    }
}

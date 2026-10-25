package com.roylet.immichcast.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Typography
import androidx.tv.material3.darkColorScheme
import com.roylet.immichcast.R

/**
 * Dark take on ImmichUp's neo-brutalist theme: thick cream outlines and hard offset
 * shadows instead of soft elevation, an orange accent, and Archivo display type.
 */
object Palette {
    val Background = Color(0xFF141311)
    val Dot = Color(0xFF2B2824)
    val Card = Color(0xFF1F1D1A)
    val Secondary = Color(0xFF3A2A1F)
    /** Outlines and hard shadows. */
    val Ink = Color(0xFFF3EBDD)
    val Text = Color(0xFFF7F1E6)
    val TextMuted = Color(0xFFA9A193)
    val Primary = Color(0xFFFF5A1F)
    val OnPrimary = Color(0xFF111111)
    val Destructive = Color(0xFFFF5C67)
    val Success = Color(0xFF3DBE6E)
    val Warning = Color(0xFFFFC53D)
}

val Archivo = FontFamily(
    Font(R.font.archivo_medium, FontWeight.Medium),
    Font(R.font.archivo_bold, FontWeight.Bold),
    Font(R.font.archivo_extrabold, FontWeight.ExtraBold),
    Font(R.font.archivo_black, FontWeight.Black),
)

private val colors = darkColorScheme(
    primary = Palette.Primary,
    onPrimary = Palette.OnPrimary,
    secondary = Palette.Secondary,
    onSecondary = Palette.Text,
    background = Palette.Background,
    onBackground = Palette.Text,
    surface = Palette.Card,
    onSurface = Palette.Text,
    surfaceVariant = Palette.Card,
    onSurfaceVariant = Palette.TextMuted,
    error = Palette.Destructive,
    border = Palette.Ink,
)

private val base = Typography()

private fun display(size: Int, weight: FontWeight = FontWeight.Black, spacing: Float = -0.5f) =
    TextStyle(fontFamily = Archivo, fontWeight = weight, fontSize = size.sp, letterSpacing = spacing.sp)

private val typography = base.copy(
    displayMedium = display(52),
    displaySmall = display(40),
    headlineMedium = display(32),
    headlineSmall = display(24),
    titleLarge = display(22, FontWeight.ExtraBold, 0f),
    titleMedium = display(17, FontWeight.ExtraBold, 0f),
    titleSmall = display(15, FontWeight.Bold, 0f),
    labelLarge = display(14, FontWeight.Bold, 1f),
    labelMedium = display(12, FontWeight.ExtraBold, 1.2f),
    labelSmall = display(11, FontWeight.ExtraBold, 1.4f),
)

@Composable
fun ImmichCastTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colors, typography = typography) {
        // tv-material only sets a content colour inside Surfaces; default all text to on-background.
        CompositionLocalProvider(LocalContentColor provides colors.onBackground, content = content)
    }
}

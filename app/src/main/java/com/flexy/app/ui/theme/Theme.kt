package com.flexy.app.ui.theme

import android.app.Activity
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.graphics.ColorUtils
import androidx.core.view.WindowCompat
import com.flexy.app.data.FlexySettings

enum class ThemeMode(val label: String) { DARK("Dark"), AMOLED("AMOLED"), LIGHT("Light") }

enum class AccentColor(val label: String, val color: Color) {
    CYAN("Cyan", Color(0xFF00E5FF)),
    GREEN("Green", Color(0xFF39FF88)),
    PURPLE("Purple", Color(0xFFB57CFF)),
    PINK("Pink", Color(0xFFFF3D9A)),
    ORANGE("Orange", Color(0xFFFF9F1C))
}

data class Feedback(val haptics: Boolean = true, val sounds: Boolean = true)

val LocalFeedback = staticCompositionLocalOf { Feedback() }

private val NeonPurple = Color(0xFFB57CFF)

private fun neonDark(accent: Color, bg: Color, card: Color): ColorScheme = darkColorScheme(
    primary = accent, onPrimary = Color(0xFF031014),
    secondary = accent, onSecondary = Color(0xFF031014),
    tertiary = NeonPurple, onTertiary = Color(0xFF12001F),
    background = bg, onBackground = Color(0xFFEAF0FA),
    surface = bg, onSurface = Color(0xFFEAF0FA),
    surfaceVariant = card, onSurfaceVariant = Color(0xFF9AA6BD),
    surfaceContainer = card, surfaceContainerHigh = card, surfaceContainerHighest = card,
    outline = Color(0xFF2B3347), outlineVariant = Color(0xFF1E2535),
    error = Color(0xFFFF5470)
)

private fun neonLight(accent: Color): ColorScheme {
    val strong = Color(ColorUtils.blendARGB(accent.toArgb(), 0xFF000000.toInt(), 0.40f))
    return lightColorScheme(
        primary = strong, onPrimary = Color.White,
        secondary = strong, onSecondary = Color.White,
        tertiary = Color(0xFF7B3FD6), onTertiary = Color.White,
        background = Color(0xFFF1F4FA), onBackground = Color(0xFF10131A),
        surface = Color(0xFFF1F4FA), onSurface = Color(0xFF10131A),
        surfaceVariant = Color(0xFFE3E8F2), onSurfaceVariant = Color(0xFF515C72),
        surfaceContainer = Color(0xFFE3E8F2), surfaceContainerHigh = Color(0xFFE3E8F2),
        surfaceContainerHighest = Color(0xFFE3E8F2),
        outline = Color(0xFFB7C0D2), outlineVariant = Color(0xFFD5DBE8),
        error = Color(0xFFD62D4C)
    )
}

private val base = Typography()
private val FlexyTypography = base.copy(
    headlineLarge = base.headlineLarge.copy(fontWeight = FontWeight.Black, letterSpacing = 0.5.sp),
    headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
    titleLarge = base.titleLarge.copy(fontWeight = FontWeight.Bold),
    titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    labelLarge = base.labelLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp),
    labelMedium = base.labelMedium.copy(fontWeight = FontWeight.SemiBold)
)

@Composable
fun FlexyTheme(settings: FlexySettings, content: @Composable () -> Unit) {
    val accent = settings.accent.color
    val scheme = when (settings.theme) {
        ThemeMode.LIGHT -> neonLight(accent)
        ThemeMode.AMOLED -> neonDark(accent, Color.Black, Color(0xFF0E1016))
        ThemeMode.DARK -> neonDark(accent, Color(0xFF0A0D14), Color(0xFF151A26))
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val controller = WindowCompat.getInsetsController(window, view)
            val light = settings.theme == ThemeMode.LIGHT
            controller.isAppearanceLightStatusBars = light
            controller.isAppearanceLightNavigationBars = light
        }
    }

    CompositionLocalProvider(LocalFeedback provides Feedback(settings.haptics, settings.sounds)) {
        MaterialTheme(colorScheme = scheme, typography = FlexyTypography, content = content)
    }
}

package com.ilyne.helloszigetkmp.presentation.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * App-wide semantic color roles, shaped like Material3's ColorScheme (same role names, same on-x pairing convention) but sourced from
 * Sziget's own brand palette rather than Material's defaults.
 */
data class AppColorScheme(
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val background: Color,
    val onBackground: Color,
    val surface: Color,
    val onSurface: Color,
    val surfaceVariant: Color,
    val onSurfaceVariant: Color,
    val outline: Color,
    val outlineVariant: Color,
    val error: Color,
    val onError: Color,
)

val LightAppColors = AppColorScheme(
    primary = Color(0xFFE8354A),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFCE4E7),
    onPrimaryContainer = Color(0xFFE8354A),
    background = Color.White,
    onBackground = Color(0xFF1A1A1A),
    surface = Color.White,
    onSurface = Color(0xFF1A1A1A),
    surfaceVariant = Color(0xFFEDEDF0),
    onSurfaceVariant = Color(0xFFAAAAAA),
    outline = Color(0xFFCCCCCC),
    outlineVariant = Color(0xFFE5E5E8),
    error = Color(0xFFB3261E),
    onError = Color.White,
)

val DarkAppColors = AppColorScheme(
    primary = Color(0xFFFF6B7D),
    onPrimary = Color(0xFF1A1A1A),
    primaryContainer = Color(0xFF5C1420),
    onPrimaryContainer = Color(0xFFFF6B7D),
    background = Color(0xFF121212),
    onBackground = Color(0xFFEDEDED),
    surface = Color(0xFF121212),
    onSurface = Color(0xFFEDEDED),
    surfaceVariant = Color(0xFF2A2A2E),
    onSurfaceVariant = Color(0xFFAAAAAA),
    outline = Color(0xFF4A4A4E),
    outlineVariant = Color(0xFF33333A),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF1A1A1A),
)

val LocalAppColors = staticCompositionLocalOf { LightAppColors }

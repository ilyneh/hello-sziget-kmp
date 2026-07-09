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

    val secondary: Color,
    val onSecondary: Color,
    val secondaryContainer: Color,
    val onSecondaryContainer: Color,

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
    primary = SzigetPalette.PrimaryBlue,
    onPrimary = SzigetPalette.SunshineYellow,
    primaryContainer = SzigetPalette.CreamCanvas,
    onPrimaryContainer = SzigetPalette.PrimaryBlue,
    secondary = SzigetPalette.SunshineYellow,
    onSecondary = SzigetPalette.Navy,
    secondaryContainer = SzigetPalette.Ink,
    onSecondaryContainer = SzigetPalette.CardSurface,
    background = SzigetPalette.AppCanvas,
    onBackground = SzigetPalette.Ink,
    surface = SzigetPalette.AppCanvas,
    onSurface = SzigetPalette.Ink,
    surfaceVariant = SzigetPalette.CreamCanvas,
    onSurfaceVariant = SzigetPalette.MutedText,
    outline = SzigetPalette.Hairline,
    outlineVariant = SzigetPalette.CreamCanvas,
    error = SzigetPalette.Coral,
    onError = SzigetPalette.CardSurface,
)

val DarkAppColors = AppColorScheme(
    primary = SzigetPalette.PrimaryBlue,
    onPrimary = SzigetPalette.CardSurface,
    primaryContainer = SzigetPalette.Navy,
    onPrimaryContainer = SzigetPalette.PrimaryBlue,
    secondary = SzigetPalette.Ink,
    onSecondary = SzigetPalette.CardSurface,
    secondaryContainer = SzigetPalette.Ink,
    onSecondaryContainer = SzigetPalette.CardSurface,
    background = SzigetPalette.Ink,
    onBackground = SzigetPalette.CreamCanvas,
    surface = SzigetPalette.Navy,
    onSurface = SzigetPalette.CreamCanvas,
    surfaceVariant = SzigetPalette.DarkTeal,
    onSurfaceVariant = SzigetPalette.FaintText,
    outline = SzigetPalette.FaintText,
    outlineVariant = SzigetPalette.MutedText,
    error = SzigetPalette.Coral,
    onError = SzigetPalette.Ink,
)

val LocalAppColors = staticCompositionLocalOf { LightAppColors }

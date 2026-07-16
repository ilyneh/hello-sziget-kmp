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

    /** Text/icon color for content drawn on a saturated accent surface (e.g. stage/genre chips, artist cards), independent of light/dark theme. */
    val onAccent: Color,

    // --- Fixed brand accents ---
    // These carry the same value in both LightAppColors and DarkAppColors: they're used as
    // flat, sticker-like brand chrome (nav bar, headers, cards, list rows) that's meant to
    // read the same regardless of the system's light/dark setting, the same way
    // [WelcomeBackground]'s hero gradient is theme-invariant by design. Each is reused across
    // several unrelated surfaces (background, border, text) rather than a single fixed role,
    // so they're named after the brand color itself rather than a specific role.
    val navy: Color,
    val highlightYellow: Color,
    val highlightMagenta: Color,
    val coral: Color,
    val tealGreen: Color,
    /** Label text drawn on a [tealGreen] surface (e.g. engagement-count card). */
    val onTealGreen: Color,
    val lightBlue: Color,
    val mediumBlue: Color,
    val warmOrange: Color,
    val redOrange: Color,
    /** Text-field border tone (unfocused state). */
    val fieldBorder: Color,
    /** Muted content color for text/icons drawn on [navy] (e.g. unselected bottom-nav item). */
    val mutedOnNavy: Color,
    /** Faint divider/muted-icon tone, and disabled-content color for [ActionButtonDefaults]-style chrome. */
    val hairline: Color,
    /** Disabled-container fill for [ActionButtonDefaults]-style chrome. */
    val creamCanvas: Color,
    /** Fallback color when a real data-driven color (e.g. an artist's stage color) is unavailable. */
    val fallbackGray: Color,
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
    onAccent = Color.White,
    navy = SzigetPalette.Navy,
    highlightYellow = SzigetPalette.SunshineYellow,
    highlightMagenta = SzigetPalette.Magenta,
    coral = SzigetPalette.Coral,
    tealGreen = SzigetPalette.TealGreen,
    onTealGreen = SzigetPalette.CardSurface,
    lightBlue = SzigetPalette.LightBlue,
    mediumBlue = SzigetPalette.MediumBlue,
    warmOrange = SzigetPalette.WarmOrange,
    redOrange = SzigetPalette.RedOrange,
    fieldBorder = SzigetPalette.DarkTeal,
    mutedOnNavy = SzigetPalette.FaintText,
    hairline = SzigetPalette.Hairline,
    creamCanvas = SzigetPalette.CreamCanvas,
    fallbackGray = SzigetPalette.FallbackGray,
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
    onAccent = Color.White,
    navy = SzigetPalette.Navy,
    highlightYellow = SzigetPalette.SunshineYellow,
    highlightMagenta = SzigetPalette.Magenta,
    coral = SzigetPalette.Coral,
    tealGreen = SzigetPalette.TealGreen,
    onTealGreen = SzigetPalette.CardSurface,
    lightBlue = SzigetPalette.LightBlue,
    mediumBlue = SzigetPalette.MediumBlue,
    warmOrange = SzigetPalette.WarmOrange,
    redOrange = SzigetPalette.RedOrange,
    fieldBorder = SzigetPalette.DarkTeal,
    mutedOnNavy = SzigetPalette.FaintText,
    hairline = SzigetPalette.Hairline,
    creamCanvas = SzigetPalette.CreamCanvas,
    fallbackGray = SzigetPalette.FallbackGray,
)

val LocalAppColors = staticCompositionLocalOf { LightAppColors }

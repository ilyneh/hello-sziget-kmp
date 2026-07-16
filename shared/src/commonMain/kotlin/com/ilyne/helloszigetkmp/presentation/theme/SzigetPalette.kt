package com.ilyne.helloszigetkmp.presentation.theme

import androidx.compose.ui.graphics.Color

/**
 * Raw Sziget brand palette. These are the literal design-system swatches with no semantic
 * meaning attached — [AppColorScheme] maps a subset of them onto semantic roles (primary,
 * surface, etc.) for actual use in composables.
 */
object SzigetPalette {
    val PrimaryBlue = Color(0xFF4A5FE0)
    val MediumBlue = Color(0xFF90AEFF)
    val LightBlue = Color(0xFFB5C9FF)
    val Magenta = Color(0xFFA6178A)
    val Red = Color(0xFFFF084A)
    val Coral = Color(0xFFF2617A)
    val HotPink = Color(0xFFFF5B8A)
    val WarmOrange = Color(0xFFFF8E62)
    val RedOrange = Color(0xFFFB6C37)
    val SunshineYellow = Color(0xFFF5EB3D)
    val TealGreen = Color(0xFF178C6E)
    val Peach = Color(0xFFFBD8C8)
    val CreamCanvas = Color(0xFFFBF6EC)
    val Navy = Color(0xFF171B4D)
    val DarkTeal = Color(0xFF0B4A52)

    val Ink = Color(0xFF111111)
    val MutedText = Color(0xFF999999)
    val FaintText = Color(0xFFB8B8B8)

    val AppCanvas = Peach
    val CardSurface = Color(0xFFFFFDF7)
    val ControlFill = Color(0xFFF1EADF)
    val Hairline = Color(0xFFE7DFD0)

    // Fallback swatches used when a real data-driven color (stage, artist) is unavailable.
    val FallbackGray = Color.Gray
    val UnknownStageGray = Color.DarkGray
}

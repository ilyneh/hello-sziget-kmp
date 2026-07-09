package com.ilyne.helloszigetkmp.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import hello_sziget_kmp.shared.generated.resources.Res
import hello_sziget_kmp.shared.generated.resources.syne_bold
import hello_sziget_kmp.shared.generated.resources.syne_extrabold
import hello_sziget_kmp.shared.generated.resources.syne_medium
import hello_sziget_kmp.shared.generated.resources.syne_regular
import hello_sziget_kmp.shared.generated.resources.syne_semibold
import org.jetbrains.compose.resources.Font

/** Mirrors MaterialTheme's `MaterialTheme.colorScheme` accessor pattern for [AppColorScheme]. */
object AppTheme {
    val colors: AppColorScheme
        @Composable get() = LocalAppColors.current
}

@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val appColors = if (darkTheme) DarkAppColors else LightAppColors

    // Also mirror the palette into a real Material3 ColorScheme so components and screens
    // that read MaterialTheme.colorScheme directly (NavigationBar, Scaffold, etc.) stay in
    // sync with the app's brand colors instead of falling back to Material's defaults.
    // Remembered on darkTheme so this isn't rebuilt on every unrelated recomposition.
    val materialColorScheme = remember(darkTheme, appColors) {
        if (darkTheme) {
            darkColorScheme(
                primary = appColors.primary,
                onPrimary = appColors.onPrimary,
                primaryContainer = appColors.primaryContainer,
                onPrimaryContainer = appColors.onPrimaryContainer,
                secondary = appColors.secondary,
                onSecondary = appColors.onSecondary,
                secondaryContainer = appColors.secondaryContainer,
                onSecondaryContainer = appColors.onSecondaryContainer,
                background = appColors.background,
                onBackground = appColors.onBackground,
                surface = appColors.surface,
                onSurface = appColors.onSurface,
                surfaceVariant = appColors.surfaceVariant,
                onSurfaceVariant = appColors.onSurfaceVariant,
                outline = appColors.outline,
                outlineVariant = appColors.outlineVariant,
                error = appColors.error,
                onError = appColors.onError,
            )
        } else {
            lightColorScheme(
                primary = appColors.primary,
                onPrimary = appColors.onPrimary,
                primaryContainer = appColors.primaryContainer,
                onPrimaryContainer = appColors.onPrimaryContainer,
                secondary = appColors.secondary,
                onSecondary = appColors.onSecondary,
                secondaryContainer = appColors.secondaryContainer,
                onSecondaryContainer = appColors.onSecondaryContainer,
                background = appColors.background,
                onBackground = appColors.onBackground,
                surface = appColors.surface,
                onSurface = appColors.onSurface,
                surfaceVariant = appColors.surfaceVariant,
                onSurfaceVariant = appColors.onSurfaceVariant,
                outline = appColors.outline,
                outlineVariant = appColors.outlineVariant,
                error = appColors.error,
                onError = appColors.onError,
            )
        }
    }

    // Font() is @Composable so it must be called here directly (remember's calculation
    // block disallows composable calls) — the returned Font values compare equal by
    // resource+weight across recompositions, so keying remember on them still lets the
    // Typography rebuild below be skipped once the fonts have loaded.
    val syneRegular = Font(Res.font.syne_regular, FontWeight.Normal)
    val syneMedium = Font(Res.font.syne_medium, FontWeight.Medium)
    val syneSemiBold = Font(Res.font.syne_semibold, FontWeight.SemiBold)
    val syneBold = Font(Res.font.syne_bold, FontWeight.Bold)
    val syneExtraBold = Font(Res.font.syne_extrabold, FontWeight.ExtraBold)

    val typography = remember(syneRegular, syneMedium, syneSemiBold, syneBold, syneExtraBold) {
        createTypography(FontFamily(fonts = listOf(syneRegular, syneMedium, syneSemiBold, syneBold, syneExtraBold)))
    }

    CompositionLocalProvider(LocalAppColors provides appColors) {
        MaterialTheme(
            colorScheme = materialColorScheme,
            typography = typography,
            content = content,
        )
    }
}

fun createTypography(fontFamily: FontFamily): Typography {
    val defaultTypography = Typography()
    return Typography(
        displayLarge = defaultTypography.displayLarge.copy(fontFamily = fontFamily),
        displayMedium = defaultTypography.displayMedium.copy(fontFamily = fontFamily),
        displaySmall = defaultTypography.displaySmall.copy(fontFamily = fontFamily),
        headlineLarge = defaultTypography.headlineLarge.copy(fontFamily = fontFamily),
        headlineMedium = defaultTypography.headlineMedium.copy(fontFamily = fontFamily),
        headlineSmall = defaultTypography.headlineSmall.copy(fontFamily = fontFamily),
        titleLarge = defaultTypography.titleLarge.copy(fontFamily = fontFamily),
        titleMedium = defaultTypography.titleMedium.copy(fontFamily = fontFamily),
        titleSmall = defaultTypography.titleSmall.copy(fontFamily = fontFamily),
        bodyLarge = defaultTypography.bodyLarge.copy(fontFamily = fontFamily),
        bodyMedium = defaultTypography.bodyMedium.copy(fontFamily = fontFamily),
        bodySmall = defaultTypography.bodySmall.copy(fontFamily = fontFamily),
        labelLarge = defaultTypography.labelLarge.copy(fontFamily = fontFamily),
        labelMedium = defaultTypography.labelMedium.copy(fontFamily = fontFamily),
        labelSmall = defaultTypography.labelSmall.copy(fontFamily = fontFamily),
    )
}

package com.ilyne.hello_sziget_kmp.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

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
    val materialColorScheme = when {
        darkTheme -> darkColorScheme(
            primary = appColors.primary,
            onPrimary = appColors.onPrimary,
            primaryContainer = appColors.primaryContainer,
            onPrimaryContainer = appColors.onPrimaryContainer,
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
        else -> lightColorScheme(
            primary = appColors.primary,
            onPrimary = appColors.onPrimary,
            primaryContainer = appColors.primaryContainer,
            onPrimaryContainer = appColors.onPrimaryContainer,
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

    CompositionLocalProvider(LocalAppColors provides appColors) {
        MaterialTheme(colorScheme = materialColorScheme, content = content)
    }
}

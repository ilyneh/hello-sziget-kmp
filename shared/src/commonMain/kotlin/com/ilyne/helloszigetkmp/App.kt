package com.ilyne.helloszigetkmp

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.ilyne.helloszigetkmp.auth.SzigetAuthService
import com.ilyne.helloszigetkmp.navigation.AppNavGraph
import com.ilyne.helloszigetkmp.navigation.Login
import com.ilyne.helloszigetkmp.navigation.Main
import com.ilyne.helloszigetkmp.presentation.theme.AppTheme
import org.koin.compose.koinInject

@Composable
fun App() {
    val authService = koinInject<SzigetAuthService>()
    val startDestination = remember {
        if (authService.restoreSession()) Main else Login
    }

    AppTheme {
        AppNavGraph(startDestination = startDestination)
    }
}

package com.ilyne.helloszigetkmp

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.navigation.compose.rememberNavController
import com.ilyne.helloszigetkmp.auth.SzigetAuthService
import com.ilyne.helloszigetkmp.navigation.AppNavGraph
import com.ilyne.helloszigetkmp.navigation.Login
import com.ilyne.helloszigetkmp.navigation.Main
import com.ilyne.helloszigetkmp.presentation.theme.AppTheme
import org.koin.compose.koinInject

@Composable
fun App() {
    val authService = koinInject<SzigetAuthService>()
    val navController = rememberNavController()
    val startDestination = remember {
        if (authService.restoreSession()) Main else Login
    }

    LaunchedEffect(Unit) {
        authService.sessionInvalidated.collect {
            navController.navigate(Login) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    AppTheme {
        AppNavGraph(
            navController = navController,
            startDestination = startDestination
        )
    }
}

package com.ilyne.helloszigetkmp

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.ilyne.helloszigetkmp.core.auth.SzigetAuthService
import com.ilyne.helloszigetkmp.navigation.AppNavGraph
import com.ilyne.helloszigetkmp.navigation.Login
import com.ilyne.helloszigetkmp.navigation.Main
import com.ilyne.helloszigetkmp.presentation.theme.AppTheme
import org.koin.compose.koinInject

@Composable
fun App() {
    val authService = koinInject<SzigetAuthService>()
    val navController = rememberNavController()
    var startDestination by remember { mutableStateOf<Any?>(null) }

    LaunchedEffect(Unit) {
        // Suspends until CurrentUserProvider is either populated from the local DB or the
        // session is invalidated, so Main is never reachable before the current user is set.
        startDestination = if (authService.restoreSession()) Main else Login
    }

    LaunchedEffect(Unit) {
        authService.sessionInvalidated.collect {
            // The NavHost may not have composed yet (startDestination still null while
            // restoreSession() is in flight), in which case navController has no graph set
            // and navigate() would throw. Just point the eventual start destination at Login.
            if (startDestination == null) {
                startDestination = Login
            } else {
                navController.navigate(Login) {
                    popUpTo(0) { inclusive = true }
                }
            }
        }
    }

    AppTheme {
        val destination = startDestination
        if (destination == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            AppNavGraph(
                navController = navController,
                startDestination = destination
            )
        }
    }
}

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
fun App(modifier: Modifier = Modifier) {
    val authService = koinInject<SzigetAuthService>()
    val navController = rememberNavController()
    var startDestination by remember { mutableStateOf<Any?>(null) }
    var sessionWasInvalidated by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        // Suspends until CurrentUserProvider is either populated from the local DB or the
        // session is invalidated, so Main is never reachable before the current user is set.
        val restored = authService.restoreSession()
        // If sessionInvalidated already fired while this was in flight, don't let a stale
        // "restored" result clobber the Login destination it set.
        if (!sessionWasInvalidated) {
            startDestination = if (restored) Main else Login
        }
    }

    LaunchedEffect(Unit) {
        authService.sessionInvalidated.collect {
            sessionWasInvalidated = true
            // startDestination being non-null doesn't guarantee the NavHost has actually
            // recomposed and called setGraph() yet - Compose state writes and recompositions
            // aren't synchronous, so this coroutine can resume and observe the new
            // startDestination before that recomposition has run. Ask the NavController
            // directly (its graph getter throws the same way navigate() would) instead of
            // inferring readiness from our own state.
            val graphIsSet = runCatching { navController.graph }.isSuccess
            if (!graphIsSet) {
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
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            AppNavGraph(
                navController = navController,
                startDestination = destination,
                modifier = modifier,
            )
        }
    }
}

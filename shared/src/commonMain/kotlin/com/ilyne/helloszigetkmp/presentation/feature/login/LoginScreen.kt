package com.ilyne.helloszigetkmp.presentation.feature.login

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ilyne.helloszigetkmp.presentation.feature.login.component.SignInCard
import com.ilyne.helloszigetkmp.presentation.feature.login.component.WelcomeBackground
import org.koin.compose.viewmodel.koinViewModel

private const val LOGIN_SCREEN = "login_screen"

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel = koinViewModel<LoginViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val currentOnLoginSuccess by rememberUpdatedState(onLoginSuccess)
    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                LoginEffect.NavigateToMain -> currentOnLoginSuccess()
            }
        }
    }

    Box(modifier = modifier.fillMaxSize().testTag(LOGIN_SCREEN)) {
        WelcomeBackground(modifier = Modifier.fillMaxSize())
        SignInCard(
            isLoading = uiState.isLoading,
            error = uiState.error,
            onSignIn = viewModel::signInWithGoogle,
        )
    }
}

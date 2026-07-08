package com.ilyne.helloszigetkmp.presentation.login

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ilyne.helloszigetkmp.presentation.feature.login.LoginEffect
import com.ilyne.helloszigetkmp.presentation.feature.login.LoginViewModel
import com.ilyne.helloszigetkmp.presentation.login.components.SignInCard
import com.ilyne.helloszigetkmp.presentation.login.components.WelcomeBackground
import org.koin.compose.viewmodel.koinViewModel

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

    WelcomeBackground(modifier = modifier.fillMaxSize())
    SignInCard(
        isLoading = uiState.isLoading,
        error = uiState.error,
        onSignIn = viewModel::signInWithGoogle,
    )
}

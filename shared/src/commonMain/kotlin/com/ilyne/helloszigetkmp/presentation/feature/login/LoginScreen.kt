package com.ilyne.helloszigetkmp.presentation.feature.login

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ilyne.helloszigetkmp.presentation.feature.login.component.DebugConfigSheet
import com.ilyne.helloszigetkmp.presentation.feature.login.component.SignInCard
import com.ilyne.helloszigetkmp.presentation.feature.login.component.WelcomeBackground
import hello_sziget_kmp.shared.generated.resources.Res
import hello_sziget_kmp.shared.generated.resources.login_debug_button
import org.jetbrains.compose.resources.stringResource
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
        // Only rendered when AppConfiguring.isDebug() is true (see LoginUiState.isDebugBuild) -
        // never appears in a release build regardless of any persisted DebugConfigStore state.
        if (uiState.isDebugBuild) {
            TextButton(
                onClick = viewModel::showDebugSheet,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .testTag("login_debug_button"),
            ) {
                Text(text = stringResource(Res.string.login_debug_button))
            }
        }
    }

    if (uiState.isDebugBuild && uiState.isDebugSheetVisible) {
        DebugConfigSheet(
            baseUrl = uiState.debugBaseUrl,
            token = uiState.debugToken,
            skipGoogleSignIn = uiState.debugSkipGoogleSignIn,
            onBaseUrlChange = viewModel::updateDebugBaseUrl,
            onTokenChange = viewModel::updateDebugToken,
            onSkipGoogleSignInChange = viewModel::updateDebugSkipGoogleSignIn,
            onSave = viewModel::saveDebugConfig,
            onDismiss = viewModel::dismissDebugSheet,
        )
    }
}

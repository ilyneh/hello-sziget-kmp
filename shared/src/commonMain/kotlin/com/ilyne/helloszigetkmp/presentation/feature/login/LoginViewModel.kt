package com.ilyne.helloszigetkmp.presentation.feature.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ilyne.helloszigetkmp.core.api.SzigetApiService
import com.ilyne.helloszigetkmp.core.auth.CurrentUserProvider
import com.ilyne.helloszigetkmp.core.auth.SzigetAuthService
import com.ilyne.helloszigetkmp.core.config.AppConfiguring
import com.ilyne.helloszigetkmp.core.config.DebugConfigStore
import com.ilyne.helloszigetkmp.core.config.SKIP_GOOGLE_SIGN_IN
import com.ilyne.helloszigetkmp.core.repository.UserRepository
import com.ilyne.helloszigetkmp.core.sync.UsersSyncService
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

data class LoginUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    // Gates the debug bottom-sheet button entirely - false in any non-debug build (see
    // AppConfiguring.isDebug()), so the button/sheet never render outside a debug build.
    val isDebugBuild: Boolean = false,
    val isDebugSheetVisible: Boolean = false,
    val debugBaseUrl: String = "",
    val debugToken: String = "",
    val debugSkipGoogleSignIn: Boolean = false,
)

sealed class LoginEffect {
    data object NavigateToMain : LoginEffect()
}

class LoginViewModel(
    private val szigetAuthService: SzigetAuthService,
    private val userRepository: UserRepository,
    private val usersSyncService: UsersSyncService,
    private val currentUserProvider: CurrentUserProvider,
    private val appConfig: AppConfiguring,
    private val debugConfigStore: DebugConfigStore,
) : ViewModel(),
    KoinComponent {
    // Resolved lazily: the authenticated SzigetApiService only exists in Koin
    // once szigetAuthService.signIn() has loaded its module below.
    private val apiService: SzigetApiService by inject()

    private val _uiState = MutableStateFlow(
        LoginUiState(
            isDebugBuild = appConfig.isDebug(),
            debugBaseUrl = debugConfigStore.getBaseUrlOverride().orEmpty(),
            debugToken = debugConfigStore.getTokenOverride().orEmpty(),
            debugSkipGoogleSignIn = debugConfigStore.getSkipGoogleSignIn(),
        ),
    )
    val uiState = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<LoginEffect>()
    val effects = _effects.asSharedFlow()

    fun signInWithGoogle() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                // In a debug build, DebugConfigStore's runtime override (defaulting to the
                // compile-time SKIP_GOOGLE_SIGN_IN flag until the debug sheet changes it) decides
                // this; outside debug, only the compile-time flag is ever consulted.
                val skipGoogleSignIn = if (appConfig.isDebug()) debugConfigStore.getSkipGoogleSignIn() else SKIP_GOOGLE_SIGN_IN
                if (skipGoogleSignIn) {
                    szigetAuthService.localSignIn()
                } else {
                    szigetAuthService.signIn()
                }

                val currentUser = userRepository.syncCurrentUser(apiService.getMe())
                currentUserProvider.set(currentUser)
                usersSyncService.fetchAllUsers(apiService)

                _effects.emit(LoginEffect.NavigateToMain)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }

    fun showDebugSheet() {
        if (!appConfig.isDebug()) return
        _uiState.update { it.copy(isDebugSheetVisible = true) }
    }

    fun dismissDebugSheet() {
        _uiState.update { it.copy(isDebugSheetVisible = false) }
    }

    fun updateDebugBaseUrl(value: String) {
        _uiState.update { it.copy(debugBaseUrl = value) }
    }

    fun updateDebugToken(value: String) {
        _uiState.update { it.copy(debugToken = value) }
    }

    fun updateDebugSkipGoogleSignIn(value: Boolean) {
        _uiState.update { it.copy(debugSkipGoogleSignIn = value) }
    }

    fun saveDebugConfig() {
        if (!appConfig.isDebug()) return
        val state = _uiState.value
        debugConfigStore.setBaseUrlOverride(state.debugBaseUrl)
        debugConfigStore.setTokenOverride(state.debugToken)
        debugConfigStore.setSkipGoogleSignIn(state.debugSkipGoogleSignIn)
        _uiState.update { it.copy(isDebugSheetVisible = false) }
    }
}

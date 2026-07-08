package com.ilyne.helloszigetkmp.presentation.feature.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ilyne.helloszigetkmp.config.SKIP_GOOGLE_SIGN_IN
import com.ilyne.helloszigetkmp.auth.SzigetAuthService
import com.ilyne.helloszigetkmp.data.api.SzigetApiService
import com.ilyne.helloszigetkmp.data.repository.UserRepository
import com.ilyne.helloszigetkmp.data.sync.UsersSyncService
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
)

sealed class LoginEffect {
    data object NavigateToMain : LoginEffect()
}

class LoginViewModel(
    private val szigetAuthService: SzigetAuthService,
    private val userRepository: UserRepository,
    private val usersSyncService: UsersSyncService,
) : ViewModel(), KoinComponent {

    // Resolved lazily: the authenticated SzigetApiService only exists in Koin
    // once szigetAuthService.signIn() has loaded its module below.
    private val apiService: SzigetApiService by inject()

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<LoginEffect>()
    val effects = _effects.asSharedFlow()

    fun signInWithGoogle() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                if (SKIP_GOOGLE_SIGN_IN) {
                    szigetAuthService.localSignIn()
                } else {
                    szigetAuthService.signIn()
                }

                userRepository.syncCurrentUser(apiService.getMe())
                usersSyncService.fetchAllUsers(apiService)

                _effects.emit(LoginEffect.NavigateToMain)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }
}

package com.ilyne.helloszigetkmp.auth

import com.ilyne.helloszigetkmp.config.BEARER_TOKEN_LOCALHOST
import com.ilyne.helloszigetkmp.data.api.auth.SzigetAuthApiService
import com.ilyne.helloszigetkmp.data.api.auth.TokenDto
import com.ilyne.helloszigetkmp.di.createAuthenticatedApiModule
import com.ilyne.helloszigetkmp.di.presentationModule
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.koin.core.context.loadKoinModules
import org.koin.core.context.unloadKoinModules
import org.koin.core.module.Module

class SzigetAuthService(
    val authProvider: GoogleAuthProvider,
    val tokenStorage: TokenStorage,
    val szigetAuthApiService: SzigetAuthApiService,
) {

    private var authenticatedApiModule: Module? = null

    private val _sessionInvalidated = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val sessionInvalidated = _sessionInvalidated.asSharedFlow()

    suspend fun signIn() {
        val authUser = authProvider.signIn()
        val token = szigetAuthApiService.googleLogin(googleToken = authUser.idToken)
        tokenStorage.save(token)
        loadAuthenticatedModules(token)
    }

    suspend fun logout() {
//        tokenStorage.read()?.let {
//            szigetAuthApiService.logout(refreshToken = it.refreshToken)
//        }
        invalidateSession()
    }

    fun localSignIn() {
        val token = TokenDto(
            accessToken = BEARER_TOKEN_LOCALHOST,
            refreshToken = "",
            tokenType = "bearer"
        )
        tokenStorage.save(token)
        loadAuthenticatedModules(token)
    }

    fun restoreSession(): Boolean {
        val tokenFromStorage = tokenStorage.read() ?: return false
        loadAuthenticatedModules(tokenFromStorage)
        return true
    }

    fun invalidateSession() {
        val moduleToUnload = authenticatedApiModule ?: return
        authenticatedApiModule = null

        tokenStorage.clear()
        unloadKoinModules(presentationModule)
        unloadKoinModules(moduleToUnload)

        _sessionInvalidated.tryEmit(Unit)
    }

    private fun loadAuthenticatedModules(token: TokenDto) {
        val apiModule = createAuthenticatedApiModule(
            accessToken = token.accessToken,
            refreshToken = token.refreshToken,
            tokenStorage = tokenStorage,
            onSessionInvalidated = ::invalidateSession
        )
        authenticatedApiModule = apiModule
        loadKoinModules(apiModule)
        loadKoinModules(presentationModule)
    }
}

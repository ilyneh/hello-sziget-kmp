package com.ilyne.helloszigetkmp.auth

import com.ilyne.helloszigetkmp.config.BEARER_TOKEN_LOCALHOST
import com.ilyne.helloszigetkmp.data.api.auth.SzigetAuthApiService
import com.ilyne.helloszigetkmp.data.api.auth.TokenDto
import com.ilyne.helloszigetkmp.di.createAuthenticatedApiModule
import com.ilyne.helloszigetkmp.di.presentationModule
import org.koin.core.context.loadKoinModules

class SzigetAuthService(
    val authProvider: GoogleAuthProvider,
    val tokenStorage: TokenStorage,
    val szigetAuthApiService: SzigetAuthApiService,
) {
    suspend fun signIn() {
        val authUser = authProvider.signIn()
        val token = szigetAuthApiService.googleLogin(googleToken = authUser.idToken)
        signInWithToken(token)
    }

    fun localSignIn() {
        signInWithToken(
            token = TokenDto(
                accessToken = BEARER_TOKEN_LOCALHOST,
                refreshToken = "",
                tokenType = "bearer"
            )
        )
    }

    private fun signInWithToken(token: TokenDto) {
        tokenStorage.save(token)
        loadAuthenticatedModules(token)
    }

    private fun loadAuthenticatedModules(token: TokenDto) {
        val apiModule = createAuthenticatedApiModule(accessToken = token.accessToken, refreshToken = token.refreshToken)
        loadKoinModules(apiModule)
        loadKoinModules(presentationModule)
    }
}

package com.ilyne.helloszigetkmp.auth

import com.ilyne.helloszigetkmp.data.api.auth.SzigetAuthApiService
import com.ilyne.helloszigetkmp.di.createAuthenticatedApiModule
import com.ilyne.helloszigetkmp.di.presentationModule
import org.koin.core.context.loadKoinModules

class SzigetAuthService(
    val authProvider: GoogleAuthProvider,
    val szigetAuthApiService: SzigetAuthApiService
) {
    suspend fun signIn() {
        val authUser = authProvider.signIn()

        // verify token with backend
        val token = szigetAuthApiService.googleLogin(authUser.idToken)

        val apiModule = createAuthenticatedApiModule(token.accessToken, token.refreshToken)
        loadKoinModules(apiModule)
        loadKoinModules(presentationModule)
    }
}

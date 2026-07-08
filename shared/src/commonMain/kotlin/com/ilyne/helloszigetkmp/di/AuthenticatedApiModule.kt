package com.ilyne.helloszigetkmp.di

import com.ilyne.helloszigetkmp.auth.TokenStorage
import com.ilyne.helloszigetkmp.config.BASE_URL_LOCALHOST
import com.ilyne.helloszigetkmp.data.api.SzigetApiService
import com.ilyne.helloszigetkmp.network.createApiHttpClient
import org.koin.core.module.Module
import org.koin.dsl.module

fun createAuthenticatedApiModule(
    accessToken: String,
    refreshToken: String,
    tokenStorage: TokenStorage,
    onSessionInvalidated: () -> Unit
): Module {
    val client = createApiHttpClient(
        baseUrl = BASE_URL_LOCALHOST,
        accessToken = accessToken,
        refreshToken = refreshToken,
        tokenStorage = tokenStorage,
        onSessionInvalidated = onSessionInvalidated
    )
    val apiService = SzigetApiService(client = client, baseUrl = BASE_URL_LOCALHOST)
    val module = module {
        single { client }
        single { apiService }
    }
    return module
}

package com.ilyne.helloszigetkmp.di

import com.ilyne.helloszigetkmp.core.auth.TokenStorage
import com.ilyne.helloszigetkmp.core.api.SzigetApiService
import com.ilyne.helloszigetkmp.core.network.createApiHttpClient
import org.koin.core.module.Module
import org.koin.dsl.module

fun createAuthenticatedApiModule(
    baseUrl: String,
    accessToken: String,
    refreshToken: String,
    tokenStorage: TokenStorage,
    onSessionInvalidated: () -> Unit
): Module {
    val client = createApiHttpClient(
        baseUrl = baseUrl,
        accessToken = accessToken,
        refreshToken = refreshToken,
        tokenStorage = tokenStorage,
        onSessionInvalidated = onSessionInvalidated
    )
    val apiService = SzigetApiService(client = client, baseUrl = baseUrl)
    val module = module {
        single { client }
        single { apiService }
    }
    return module
}

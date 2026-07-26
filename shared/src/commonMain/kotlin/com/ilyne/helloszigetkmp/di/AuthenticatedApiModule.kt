package com.ilyne.helloszigetkmp.di

import com.ilyne.helloszigetkmp.core.api.SzigetApiService
import com.ilyne.helloszigetkmp.core.auth.TokenStorage
import com.ilyne.helloszigetkmp.core.network.createApiHttpClient
import io.ktor.client.HttpClient
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.koin.dsl.onClose

val authenticatedHttpClientQualifier = named("authenticated")

fun createAuthenticatedApiModule(
    baseUrl: String,
    accessToken: String,
    refreshToken: String,
    tokenStorage: TokenStorage,
    onSessionInvalidated: suspend () -> Unit,
    isDebug: Boolean,
): Module {
    val client = createApiHttpClient(
        baseUrl = baseUrl,
        accessToken = accessToken,
        refreshToken = refreshToken,
        tokenStorage = tokenStorage,
        onSessionInvalidated = onSessionInvalidated,
        isDebug = isDebug,
    )
    val apiService = SzigetApiService(client = client, baseUrl = baseUrl)
    val module = module {
        // Named so this never shadows appModule's unqualified base HttpClient single - Koin's
        // default allowOverride=true would otherwise let this replace that binding, and unloading
        // this module would then delete the HttpClient key entirely rather than restoring the
        // base one. onClose releases the engine's connection pool/threads on unload instead of
        // leaking them across a logout or a re-issued session.
        single<HttpClient>(authenticatedHttpClientQualifier) { client } onClose { it?.close() }
        single { apiService }
    }
    return module
}

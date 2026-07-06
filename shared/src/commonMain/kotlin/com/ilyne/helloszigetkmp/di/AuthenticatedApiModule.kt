package com.ilyne.helloszigetkmp.di

import com.ilyne.helloszigetkmp.config.BASE_URL_LOCALHOST
import com.ilyne.helloszigetkmp.data.api.SzigetApiService
import com.ilyne.helloszigetkmp.network.createApiHttpClient
import org.koin.core.module.Module
import org.koin.dsl.module


fun createAuthenticatedApiModule(accessToken: String, refreshToken: String): Module {
    val client = createApiHttpClient(BASE_URL_LOCALHOST, accessToken, refreshToken)
    val apiService = SzigetApiService(client = client, baseUrl = BASE_URL_LOCALHOST)
    val module = module {
        single { client }
        single { apiService }
    }
    return module
}

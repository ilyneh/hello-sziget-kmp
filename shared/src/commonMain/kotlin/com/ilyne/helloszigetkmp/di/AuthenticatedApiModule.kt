package com.ilyne.helloszigetkmp.di

import com.ilyne.helloszigetkmp.BASE_URL_LOCALHOST
import com.ilyne.helloszigetkmp.data.api.SzigetApiService
import com.ilyne.helloszigetkmp.network.createApiHttpClient
import org.koin.dsl.module

fun createAuthenticatedApiModule(accessToken: String, refreshToken: String) = module {
    single { createApiHttpClient(BASE_URL_LOCALHOST, accessToken, refreshToken) }
    single { SzigetApiService(client = get(), baseUrl = BASE_URL_LOCALHOST) }
}

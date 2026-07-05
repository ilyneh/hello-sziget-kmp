package com.ilyne.helloszigetkmp.di

import com.ilyne.helloszigetkmp.BASE_URL_LOCALHOST
import com.ilyne.helloszigetkmp.auth.GoogleAuthProvider
import com.ilyne.helloszigetkmp.auth.SzigetAuthService
import com.ilyne.helloszigetkmp.data.api.auth.SzigetAuthApiService
import com.ilyne.helloszigetkmp.data.db.SzigetDatabase
import com.ilyne.helloszigetkmp.data.db.createDatabase
import com.ilyne.helloszigetkmp.data.db.getDatabaseBuilder
import com.ilyne.helloszigetkmp.network.baseHttpClient
import com.ilyne.helloszigetkmp.presentation.login.LoginViewModel
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

fun initKoin(platformModules: List<Module> = emptyList()) {
    startKoin {
        modules(platformModules + appModule)
    }
}

val appModule = module {
    // Auth
    single { GoogleAuthProvider() }
    single { baseHttpClient }
    single { SzigetAuthApiService(client = get(), baseUrl = BASE_URL_LOCALHOST) }
    single { SzigetAuthService(authProvider = get(), szigetAuthApiService = get()) }

    // Database
    single { createDatabase(getDatabaseBuilder()) }
    single { get<SzigetDatabase>().artistDao() }
    single { get<SzigetDatabase>().stageDao() }
    single { get<SzigetDatabase>().setTimeDao() }

    // View Models
    viewModel { LoginViewModel(szigetAuthService = get()) }
}

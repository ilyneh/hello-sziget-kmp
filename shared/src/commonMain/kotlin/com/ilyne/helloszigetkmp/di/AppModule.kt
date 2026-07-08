package com.ilyne.helloszigetkmp.di

import com.ilyne.helloszigetkmp.config.BASE_URL_LOCALHOST
import com.ilyne.helloszigetkmp.auth.GoogleAuthProvider
import com.ilyne.helloszigetkmp.auth.LogoutService
import com.ilyne.helloszigetkmp.auth.SzigetAuthService
import com.ilyne.helloszigetkmp.auth.TokenStorage
import com.ilyne.helloszigetkmp.data.api.auth.SzigetAuthApiService
import com.ilyne.helloszigetkmp.data.db.SzigetDatabase
import com.ilyne.helloszigetkmp.data.db.createDatabase
import com.ilyne.helloszigetkmp.data.db.getDatabaseBuilder
import com.ilyne.helloszigetkmp.data.repository.UserRepository
import com.ilyne.helloszigetkmp.data.sync.UsersSyncService
import com.ilyne.helloszigetkmp.network.baseHttpClient
import com.ilyne.helloszigetkmp.presentation.feature.login.LoginViewModel
import com.russhwolf.settings.Settings
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
    single { Settings() }
    single { GoogleAuthProvider() }
    single { TokenStorage(settings = get()) }
    single { baseHttpClient }
    single { SzigetAuthApiService(client = get(), baseUrl = BASE_URL_LOCALHOST) }
    single { SzigetAuthService(authProvider = get(), tokenStorage = get(), szigetAuthApiService = get()) }
    single { LogoutService(session = get()) }

    // Database
    single { createDatabase(getDatabaseBuilder()) }
    single { get<SzigetDatabase>().artistDao() }
    single { get<SzigetDatabase>().stageDao() }
    single { get<SzigetDatabase>().setTimeDao() }
    single { get<SzigetDatabase>().userDao() }
    single { get<SzigetDatabase>().friendDao() }

    // Repositories
    single { UserRepository(dao = get()) }

    // Background services
    single { UsersSyncService(userRepository = get()) }

    // View Models
    viewModel { LoginViewModel(szigetAuthService = get(), userRepository = get(), usersSyncService = get()) }
}

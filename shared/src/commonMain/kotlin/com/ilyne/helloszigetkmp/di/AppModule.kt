package com.ilyne.helloszigetkmp.di

import com.ilyne.helloszigetkmp.core.auth.GoogleAuthProvider
import com.ilyne.helloszigetkmp.core.auth.LogoutService
import com.ilyne.helloszigetkmp.core.auth.SzigetAuthService
import com.ilyne.helloszigetkmp.core.auth.TokenStorage
import com.ilyne.helloszigetkmp.core.config.AppConfig
import com.ilyne.helloszigetkmp.core.api.auth.SzigetAuthApiService
import com.ilyne.helloszigetkmp.core.db.SzigetDatabase
import com.ilyne.helloszigetkmp.core.db.createDatabase
import com.ilyne.helloszigetkmp.core.db.getDatabaseBuilder
import com.ilyne.helloszigetkmp.core.repository.UserRepository
import com.ilyne.helloszigetkmp.core.settings.createSecureSettings
import com.ilyne.helloszigetkmp.core.sync.UsersSyncService
import com.ilyne.helloszigetkmp.core.network.baseHttpClient
import com.ilyne.helloszigetkmp.presentation.feature.login.LoginViewModel
import com.ilyne.helloszigetkmp.presentation.feature.schedule.filter.ScheduleFilterStorage
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
    single { AppConfig() }
    single { createSecureSettings() }
    single { GoogleAuthProvider() }
    single { TokenStorage(settings = get()) }
    single { ScheduleFilterStorage(settings = get()) }
    single { baseHttpClient }
    single { SzigetAuthApiService(client = get(), appConfig = get()) }
    single { SzigetAuthService(appConfig = get(), authProvider = get(), tokenStorage = get(), szigetAuthApiService = get()) }
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

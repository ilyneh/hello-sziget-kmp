package com.ilyne.helloszigetkmp.di

import com.ilyne.helloszigetkmp.core.auth.CurrentUserProvider
import com.ilyne.helloszigetkmp.core.api.auth.SzigetAuthApiService
import com.ilyne.helloszigetkmp.core.auth.GoogleAuthProvider
import com.ilyne.helloszigetkmp.core.auth.LogoutService
import com.ilyne.helloszigetkmp.core.auth.SzigetAuthService
import com.ilyne.helloszigetkmp.core.auth.TokenStorage
import com.ilyne.helloszigetkmp.core.config.AppConfig
import com.ilyne.helloszigetkmp.core.db.SzigetDatabase
import com.ilyne.helloszigetkmp.core.db.createDatabase
import com.ilyne.helloszigetkmp.core.db.getDatabaseBuilder
import com.ilyne.helloszigetkmp.core.network.createBaseHttpClient
import com.ilyne.helloszigetkmp.core.repository.SoftRefreshGate
import com.ilyne.helloszigetkmp.core.repository.UserRepository
import com.ilyne.helloszigetkmp.core.settings.createSecureSettings
import com.ilyne.helloszigetkmp.core.sync.UsersSyncService
import com.ilyne.helloszigetkmp.presentation.feature.login.LoginViewModel
import com.ilyne.helloszigetkmp.presentation.feature.schedule.filter.ScheduleFilterStorage
import com.russhwolf.settings.Settings
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module

val secureSettingsQualifier = named("secure")

fun initKoin(platformModules: List<Module> = emptyList()) {
    startKoin {
        modules(platformModules + appModule)
    }
}

val appModule = module {
    // Auth
    single { AppConfig() }
    // Non-sensitive settings (e.g. soft-refresh timestamps, filter prefs): plain storage
    // (SharedPreferences / NSUserDefaults) that is wiped when the app is uninstalled.
    single { Settings() }
    // Sensitive settings (auth tokens): Keychain/EncryptedSharedPreferences-backed, which on
    // iOS deliberately survives app deletion - only use this for data that should persist
    // across a reinstall.
    single(secureSettingsQualifier) { createSecureSettings() }
    single { GoogleAuthProvider() }
    single { TokenStorage(settings = get(secureSettingsQualifier)) }
    single { ScheduleFilterStorage(settings = get()) }
    single { CurrentUserProvider() }
    single { createBaseHttpClient(isDebug = get<AppConfig>().isDebug()) }
    single { SzigetAuthApiService(client = get(), appConfig = get()) }
    single {
        SzigetAuthService(
            appConfig = get(),
            authProvider = get(),
            tokenStorage = get(),
            szigetAuthApiService = get(),
            userRepository = get(),
            currentUserProvider = get(),
            artistDao = get(),
            userDao = get(),
            friendDao = get(),
            settings = get(),
        )
    }
    single { LogoutService(session = get()) }

    // Database
    single {
        val settings = get<Settings>()
        createDatabase(getDatabaseBuilder(), onDestructiveMigration = { SoftRefreshGate.clearAll(settings) })
    }
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
    viewModel {
        LoginViewModel(
            szigetAuthService = get(),
            userRepository = get(),
            usersSyncService = get(),
            currentUserProvider = get(),
        )
    }
}

package com.ilyne.helloszigetkmp.di

import com.ilyne.helloszigetkmp.BASE_URL_LOCALHOST
import com.ilyne.helloszigetkmp.auth.GoogleAuthProvider
import com.ilyne.helloszigetkmp.data.api.SzigetApiService
import com.ilyne.helloszigetkmp.data.api.createHttpClient
import com.ilyne.helloszigetkmp.data.db.SzigetDatabase
import com.ilyne.helloszigetkmp.data.db.createDatabase
import com.ilyne.helloszigetkmp.data.db.getDatabaseBuilder
import com.ilyne.helloszigetkmp.data.repository.ArtistRepository
import com.ilyne.helloszigetkmp.data.repository.ScheduleRepository
import com.ilyne.helloszigetkmp.presentation.login.LoginViewModel
import com.ilyne.helloszigetkmp.presentation.schedule.ScheduleViewModel
import com.ilyne.helloszigetkmp.presentation.discover.DiscoverViewModel
import com.ilyne.helloszigetkmp.presentation.lineup.LineupViewModel
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
    // Network
    single { createHttpClient() }
    single { SzigetApiService(client = get(), baseUrl = BASE_URL_LOCALHOST) }

    // Database
    single { createDatabase(getDatabaseBuilder()) }
    single { get<SzigetDatabase>().artistDao() }
    single { get<SzigetDatabase>().stageDao() }
    single { get<SzigetDatabase>().setTimeDao() }

    // Repositories
    single { ArtistRepository(api = get(), dao = get()) }
    single { ScheduleRepository(api = get(), setTimeDao = get(), stageDao = get(), artistDao = get()) }

    // Auth
    single { GoogleAuthProvider() }

    // ViewModels
    viewModel { LoginViewModel(authProvider = get()) }
    viewModel { ScheduleViewModel(scheduleRepository = get(), artistRepository = get()) }
    viewModel { DiscoverViewModel(artistRepository = get()) }
    viewModel { LineupViewModel(artistRepository = get()) }
}

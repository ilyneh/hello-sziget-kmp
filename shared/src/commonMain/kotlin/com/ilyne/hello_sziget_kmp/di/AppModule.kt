package com.ilyne.hello_sziget_kmp.di

import com.ilyne.hello_sziget_kmp.BASE_URL_LOCALHOST
import com.ilyne.hello_sziget_kmp.auth.GoogleAuthProvider
import com.ilyne.hello_sziget_kmp.data.api.SzigetApiService
import com.ilyne.hello_sziget_kmp.data.api.createHttpClient
import com.ilyne.hello_sziget_kmp.data.db.SzigetDatabase
import com.ilyne.hello_sziget_kmp.data.db.createDatabase
import com.ilyne.hello_sziget_kmp.data.db.getDatabaseBuilder
import com.ilyne.hello_sziget_kmp.data.repository.ArtistRepository
import com.ilyne.hello_sziget_kmp.data.repository.ScheduleRepository
import com.ilyne.hello_sziget_kmp.presentation.login.LoginViewModel
import com.ilyne.hello_sziget_kmp.presentation.schedule.ScheduleViewModel
import com.ilyne.hello_sziget_kmp.presentation.discover.DiscoverViewModel
import com.ilyne.hello_sziget_kmp.presentation.lineup.LineupViewModel
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

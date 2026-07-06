package com.ilyne.helloszigetkmp.di

import com.ilyne.helloszigetkmp.data.repository.ArtistRepository
import com.ilyne.helloszigetkmp.data.repository.FriendRepository
import com.ilyne.helloszigetkmp.data.repository.ScheduleRepository
import com.ilyne.helloszigetkmp.presentation.discover.DiscoverViewModel
import com.ilyne.helloszigetkmp.presentation.lineup.LineupViewModel
import com.ilyne.helloszigetkmp.presentation.profile.ProfileViewModel
import com.ilyne.helloszigetkmp.presentation.schedule.ScheduleViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val presentationModule = module {
    // Repositories
    single { ArtistRepository(api = get(), dao = get()) }
    single { ScheduleRepository(api = get(), setTimeDao = get(), stageDao = get(), artistDao = get()) }
    single { FriendRepository(api = get(), friendDao = get(), userDao = get()) }

    // ViewModels
    viewModel { ScheduleViewModel(scheduleRepository = get(), artistRepository = get()) }
    viewModel { DiscoverViewModel(artistRepository = get()) }
    viewModel { LineupViewModel(artistRepository = get()) }
    viewModel { ProfileViewModel(friendRepository = get(), userRepository = get()) }
}

package com.ilyne.helloszigetkmp.di

import com.ilyne.helloszigetkmp.core.repository.ArtistRepository
import com.ilyne.helloszigetkmp.core.repository.FriendRepository
import com.ilyne.helloszigetkmp.core.repository.ScheduleRepository
import com.ilyne.helloszigetkmp.domain.usecase.GetLikedArtistCountUseCase
import com.ilyne.helloszigetkmp.domain.usecase.GetSetTimeDaysUseCase
import com.ilyne.helloszigetkmp.presentation.feature.addfriend.AddFriendViewModel
import com.ilyne.helloszigetkmp.presentation.feature.discover.DiscoverViewModel
import com.ilyne.helloszigetkmp.presentation.feature.lineup.MyLineupViewModel
import com.ilyne.helloszigetkmp.presentation.feature.profile.ProfileViewModel
import com.ilyne.helloszigetkmp.presentation.feature.schedule.ScheduleViewModel
import com.ilyne.helloszigetkmp.presentation.feature.schedule.filter.ScheduleFilterViewModel
import com.ilyne.helloszigetkmp.presentation.feature.schedule.filter.usecase.GetActiveFiltersTextUseCase
import com.ilyne.helloszigetkmp.presentation.feature.schedule.usecase.GetFilteredScheduleContentUseCase
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val presentationModule = module {
    // Repositories
    single { ArtistRepository(api = get(), dao = get()) }
    single { ScheduleRepository(api = get(), setTimeDao = get(), stageDao = get(), artistDao = get()) }
    single { FriendRepository(api = get(), friendDao = get(), userDao = get()) }

    // UseCases
    single { GetSetTimeDaysUseCase(scheduleRepository = get()) }
    single { GetLikedArtistCountUseCase(artistRepository = get()) }
    single { GetFilteredScheduleContentUseCase() }
    single { GetActiveFiltersTextUseCase() }

    // ViewModels
    viewModel {
        ScheduleViewModel(
            scheduleRepository = get(),
            artistRepository = get(),
            friendRepository = get(),
            getSetTimeDaysUseCase = get(),
            getFilteredScheduleContentUseCase = get(),
            getActiveFiltersTextUseCase = get(),
            scheduleFilterStorage = get(),
        )
    }
    viewModel { ScheduleFilterViewModel() }
    viewModel { DiscoverViewModel(artistRepository = get()) }
    viewModel {
        MyLineupViewModel(
            artistRepository = get(),
            scheduleRepository = get()
        )
    }
    viewModel { ProfileViewModel(
        friendRepository = get(),
        userRepository = get(),
        usersSyncService = get(),
        artistRepository = get(),
        getLikedArtistCountUseCase = get()
    ) }
    viewModel { AddFriendViewModel(friendRepository = get(), userRepository = get()) }

}

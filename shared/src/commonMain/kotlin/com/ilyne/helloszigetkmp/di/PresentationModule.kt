package com.ilyne.helloszigetkmp.di

import com.ilyne.helloszigetkmp.core.repository.ArtistRepository
import com.ilyne.helloszigetkmp.core.repository.FriendRepository
import com.ilyne.helloszigetkmp.core.repository.ScheduleRepository
import com.ilyne.helloszigetkmp.domain.usecase.GetLikedArtistCountUseCase
import com.ilyne.helloszigetkmp.domain.usecase.GetSetTimeDaysUseCase
import com.ilyne.helloszigetkmp.presentation.feature.addfriend.AddFriendViewModel
import com.ilyne.helloszigetkmp.presentation.feature.artistdetail.ArtistDetailViewModel
import com.ilyne.helloszigetkmp.presentation.feature.discover.DiscoverViewModel
import com.ilyne.helloszigetkmp.presentation.feature.discover.filter.DiscoverFilterViewModel
import com.ilyne.helloszigetkmp.presentation.feature.discover.filter.usecase.GetActiveDiscoverFiltersTextUseCase
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
    single { ArtistRepository(api = get(), dao = get(), settings = get()) }
    single { ScheduleRepository(api = get(), setTimeDao = get(), stageDao = get(), artistDao = get(), settings = get()) }
    single { FriendRepository(api = get(), friendDao = get(), userDao = get(), settings = get()) }

    // UseCases
    single { GetSetTimeDaysUseCase(scheduleRepository = get()) }
    single { GetLikedArtistCountUseCase(artistRepository = get()) }
    single { GetFilteredScheduleContentUseCase() }
    single { GetActiveFiltersTextUseCase() }
    single { GetActiveDiscoverFiltersTextUseCase() }

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
            scheduleViewModeStorage = get(),
        )
    }
    viewModel { ScheduleFilterViewModel() }
    viewModel { DiscoverViewModel(artistRepository = get(), getActiveDiscoverFiltersTextUseCase = get()) }
    viewModel { DiscoverFilterViewModel() }
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
        getLikedArtistCountUseCase = get(),
        api = get(),
        currentUserProvider = get(),
    ) }
    viewModel { AddFriendViewModel(friendRepository = get(), userRepository = get(), currentUserProvider = get()) }
    viewModel {
        ArtistDetailViewModel(artistRepository = get(), friendRepository = get(), scheduleRepository = get())
    }

}

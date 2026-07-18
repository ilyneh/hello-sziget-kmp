package com.ilyne.helloszigetkmp.presentation.feature.schedule.filter.usecase

import com.ilyne.helloszigetkmp.domain.model.PerformanceType
import com.ilyne.helloszigetkmp.domain.model.displayName
import com.ilyne.helloszigetkmp.domain.model.genreGroupsFor
import com.ilyne.helloszigetkmp.presentation.feature.schedule.filter.ScheduleFilter
import hello_sziget_kmp.shared.generated.resources.Res
import hello_sziget_kmp.shared.generated.resources.schedule_active_filter_friends_going
import hello_sziget_kmp.shared.generated.resources.schedule_filter_favorites
import org.jetbrains.compose.resources.getString

class GetActiveFiltersTextUseCase {
    suspend operator fun invoke(filter: ScheduleFilter): List<String> {
        val filtersText = mutableListOf<String>()

        if (filter.showFavorites) {
            filtersText.add(getString(Res.string.schedule_filter_favorites))
        }

        if (filter.showFriendsGoing) {
            filtersText.add(getString(Res.string.schedule_active_filter_friends_going))
        }

        val activeTypes = PerformanceType.entries.filter { type ->
            genreGroupsFor(type).any { it in filter.selectedGenreGroups }
        }

        activeTypes.forEach { filtersText.add(it.displayName()) }

        return filtersText
    }
}

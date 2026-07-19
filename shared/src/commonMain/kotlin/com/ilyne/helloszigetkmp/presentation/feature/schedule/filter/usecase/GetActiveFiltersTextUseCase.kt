package com.ilyne.helloszigetkmp.presentation.feature.schedule.filter.usecase

import com.ilyne.helloszigetkmp.domain.model.PerformanceType
import com.ilyne.helloszigetkmp.domain.model.displayName
import com.ilyne.helloszigetkmp.domain.model.genreGroupsFor
import com.ilyne.helloszigetkmp.presentation.feature.schedule.filter.ScheduleFilter

sealed class ActiveFilterItem {
    data object Favorites : ActiveFilterItem()

    data object FriendsGoing : ActiveFilterItem()

    data class Custom(
        val text: String,
    ) : ActiveFilterItem()
}

class GetActiveFiltersTextUseCase {
    operator fun invoke(filter: ScheduleFilter): List<ActiveFilterItem> {
        val filterItems = mutableListOf<ActiveFilterItem>()

        if (filter.showFavorites) {
            filterItems.add(ActiveFilterItem.Favorites)
        }

        if (filter.showFriendsGoing) {
            filterItems.add(ActiveFilterItem.FriendsGoing)
        }

        val activeTypes = PerformanceType.entries.filter { type ->
            genreGroupsFor(type).any { it in filter.selectedGenreGroups }
        }

        activeTypes.forEach { filterItems.add(ActiveFilterItem.Custom(it.displayName())) }

        return filterItems
    }
}

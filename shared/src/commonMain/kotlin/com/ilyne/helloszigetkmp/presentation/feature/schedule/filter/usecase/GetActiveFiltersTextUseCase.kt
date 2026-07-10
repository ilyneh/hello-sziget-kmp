package com.ilyne.helloszigetkmp.presentation.feature.schedule.filter.usecase

import com.ilyne.helloszigetkmp.domain.model.PerformanceType
import com.ilyne.helloszigetkmp.domain.model.displayName
import com.ilyne.helloszigetkmp.presentation.feature.schedule.filter.ScheduleFilter

class GetActiveFiltersTextUseCase {

    operator fun invoke(filter: ScheduleFilter): List<String> {
        val filtersText = mutableListOf<String>()

        if (filter.showFavorites) {
            filtersText.add("Favorites")
        }

        if (filter.showFriendsGoing) {
            filtersText.add("Friends going")
        }

        if (filter.selectedPerformanceTypes != setOf(PerformanceType.MUSIC)) {
            filter.selectedPerformanceTypes.forEach { filtersText.add(it.displayName()) }
        }

        return filtersText
    }
}

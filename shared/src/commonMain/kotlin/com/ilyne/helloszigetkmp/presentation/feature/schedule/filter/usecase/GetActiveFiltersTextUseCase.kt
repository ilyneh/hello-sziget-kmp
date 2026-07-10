package com.ilyne.helloszigetkmp.presentation.feature.schedule.filter.usecase

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

        return filtersText
    }
}

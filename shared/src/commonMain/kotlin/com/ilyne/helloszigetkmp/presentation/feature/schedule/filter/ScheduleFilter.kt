package com.ilyne.helloszigetkmp.presentation.feature.schedule.filter

import kotlinx.serialization.Serializable

@Serializable
data class ScheduleFilter(
    val showFavorites: Boolean = false,
    val showFriendsGoing: Boolean = false,
    val hideEmptyStages: Boolean = true,
    val showExtraDays: Boolean = false,
) {

    fun activeCount(): Int {
        var count = 0

        if (showFavorites) count++
        if (showFriendsGoing) count++
        if (hideEmptyStages) count++
        if (showExtraDays) count++

        return count
    }

}

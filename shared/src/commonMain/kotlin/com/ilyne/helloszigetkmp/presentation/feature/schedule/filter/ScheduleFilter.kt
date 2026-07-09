package com.ilyne.helloszigetkmp.presentation.feature.schedule.filter

import kotlinx.serialization.Serializable

@Serializable
data class ScheduleFilter(
    val showFavoritesOnly: Boolean = false,
    val showFriendsGoing: Boolean = false,
    val hideEmptyStages: Boolean = true
) {

    fun activeCount(): Int {
        var count = 0

        if (showFavoritesOnly) count++
        if (showFriendsGoing) count++
        if (hideEmptyStages) count++

        return count
    }

}

package com.ilyne.helloszigetkmp.presentation.feature.schedule.filter

import com.ilyne.helloszigetkmp.domain.model.GenreGroup
import com.ilyne.helloszigetkmp.domain.model.PerformanceType
import kotlinx.serialization.Serializable

private val DEFAULT_PERFORMANCE_TYPES = setOf(PerformanceType.MUSIC)
private val DEFAULT_GENRE_GROUPS = GenreGroup.entries.toSet()

@Serializable
data class ScheduleFilter(
    val showFavorites: Boolean = false,
    val showFriendsGoing: Boolean = false,
    val hideEmptyStages: Boolean = true,
    val showExtraDays: Boolean = false,
    val selectedPerformanceTypes: Set<PerformanceType> = DEFAULT_PERFORMANCE_TYPES,
    val selectedGenreGroups: Set<GenreGroup> = DEFAULT_GENRE_GROUPS,
) {

    fun activeCount(): Int {
        var count = 0

        if (showFavorites) count++
        if (showFriendsGoing) count++
        if (hideEmptyStages) count++
        if (showExtraDays) count++
        if (selectedPerformanceTypes != DEFAULT_PERFORMANCE_TYPES || selectedGenreGroups != DEFAULT_GENRE_GROUPS) count++

        return count
    }

}

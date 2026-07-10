package com.ilyne.helloszigetkmp.presentation.feature.schedule.filter

import com.ilyne.helloszigetkmp.domain.model.GenreGroup
import com.ilyne.helloszigetkmp.domain.model.PerformanceType
import com.ilyne.helloszigetkmp.domain.model.genreGroupsFor
import kotlinx.serialization.Serializable

// A performance type only "has" its genres in `selectedGenreGroups` while it's turned on —
// there is no separate selected-types set, so unchecking a type and its genres are the same
// operation and can never drift out of sync.
private val DEFAULT_GENRE_GROUPS = genreGroupsFor(PerformanceType.MUSIC).toSet()

@Serializable
data class ScheduleFilter(
    val showFavorites: Boolean = false,
    val showFriendsGoing: Boolean = false,
    val hideEmptyStages: Boolean = true,
    val showExtraDays: Boolean = false,
    val selectedGenreGroups: Set<GenreGroup> = DEFAULT_GENRE_GROUPS,
) {

    fun activeCount(): Int {
        var count = 0

        if (showFavorites) count++
        if (showFriendsGoing) count++
        if (hideEmptyStages) count++
        if (showExtraDays) count++
        if (selectedGenreGroups != DEFAULT_GENRE_GROUPS) count++

        return count
    }

}

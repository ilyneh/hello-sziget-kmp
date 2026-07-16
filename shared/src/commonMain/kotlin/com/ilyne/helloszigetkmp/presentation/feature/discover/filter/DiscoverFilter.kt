package com.ilyne.helloszigetkmp.presentation.feature.discover.filter

import com.ilyne.helloszigetkmp.domain.model.GenreGroup
import kotlinx.serialization.Serializable

// Mirrors ScheduleFilter's genre-group selection: a performance type only "has" its genres in
// `selectedGenreGroups` while it's turned on, so there is no separate selected-types set.
private val DEFAULT_GENRE_GROUPS = GenreGroup.entries.toSet()

@Serializable
data class DiscoverFilter(
    val selectedGenreGroups: Set<GenreGroup> = DEFAULT_GENRE_GROUPS,
) {
    fun activeCount(): Int {
        var count = 0

        if (selectedGenreGroups != DEFAULT_GENRE_GROUPS) count++

        return count
    }
}

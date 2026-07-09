package com.ilyne.helloszigetkmp.presentation.feature.schedule.filter.usecase

import com.ilyne.helloszigetkmp.domain.model.ArtistFriendsFavorited
import com.ilyne.helloszigetkmp.domain.model.SetTime
import com.ilyne.helloszigetkmp.domain.model.Stage
import com.ilyne.helloszigetkmp.presentation.feature.schedule.ScheduleUiState
import com.ilyne.helloszigetkmp.presentation.feature.schedule.filter.ScheduleFilter
import com.ilyne.helloszigetkmp.util.datetime.normalizedFestivalHour
import com.ilyne.helloszigetkmp.util.datetime.normalizedFestivalHourFraction
import com.ilyne.helloszigetkmp.util.datetime.toLocalDateTime
import kotlin.collections.filter
import kotlin.time.Clock


class MapAndFilterSetTimesUseCase {

    data class Response(
        val setTimes: List<ScheduleUiState.SetTime>,
        val stages: List<Stage>,
        val gridMinHour: Int,
        val gridMaxHour: Int,
    )

    operator fun invoke(
        filter: ScheduleFilter,
        setTimes: List<SetTime>,
        favoritedByArtistId: Map<String, ArtistFriendsFavorited>,
        allStages: List<Stage>,
    ): Response {
        val currentTimeMillis = Clock.System.now().toEpochMilliseconds()
        val validSetTimes = setTimes.filter { it.startTime != it.endTime }
        val setTimesFiltered = validSetTimes.filter(filter, favoritedByArtistId)

        val stages = if (filter.hideEmptyStages) {
            setTimesFiltered.mapNotNull { it.stage }.distinctBy { it.id }
        } else {
            allStages
        }

        val setTimesUiModel = setTimesFiltered
            .map { setTime ->
                ScheduleUiState.SetTime(
                    id = setTime.id,
                    startTime = setTime.startTime,
                    endTime = setTime.endTime,
                    hideEndTime = setTime.hideEndTime,
                    artist = setTime.artist,
                    artistFriendsFavorited = setTime.artist?.let { artist ->
                        favoritedByArtistId[artist.id]
                            ?: ArtistFriendsFavorited(
                                artist = artist,
                                friendsFavorited = emptyList()
                            )
                    },
                    stage = setTime.stage,
                    isInThePast = setTime.endTime < currentTimeMillis,
                    startHourFraction = setTime.startTime.normalizedFestivalHourFraction(),
                    endHourFraction = setTime.endTime.normalizedFestivalHourFraction(),
                )
            }

        val gridHourRange = getGridHourRange(setTimesFiltered)

        return Response(
            setTimes = setTimesUiModel,
            stages = stages,
            gridMinHour = gridHourRange.minHour,
            gridMaxHour = gridHourRange.maxHour
        )
    }

    private fun List<SetTime>.filter(
        filter: ScheduleFilter,
        favoritedByArtistId: Map<String, ArtistFriendsFavorited>
    ) = filter { setTime ->
        val matchesFavorites = !filter.showFavoritesOnly || setTime.artist?.isFavorited == true
        val matchesFriendsGoing = !filter.showFriendsGoing
            || favoritedByArtistId[setTime.artistId]?.friendsFavorited?.isNotEmpty() == true
        return@filter matchesFavorites && matchesFriendsGoing
    }

    private data class GridHourRange(val minHour: Int, val maxHour: Int)

    private fun getGridHourRange(setTimes: List<SetTime>): GridHourRange {
        var minHour = 6
        var maxHour = 6

        if (setTimes.isEmpty()) return GridHourRange(minHour = minHour, maxHour = maxHour)

        for (setTime in setTimes) {
            val startHour = setTime.startTime.toLocalDateTime().hour.let(::normalizedFestivalHour)
            val endHour = setTime.endTime.toLocalDateTime().hour.let(::normalizedFestivalHour)
            if (startHour < minHour) minHour = startHour
            if (endHour > maxHour) maxHour = endHour
        }
        return GridHourRange(minHour = minHour, maxHour = maxHour + 1)
    }
}

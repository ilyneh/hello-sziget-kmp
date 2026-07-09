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
        val setTimesFiltered = setTimes
            .filter { it.startTime != it.endTime }
            .filter(filter, favoritedByArtistId)

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

        return Response(
            setTimes = setTimesUiModel,
            stages = stages,
            gridMinHour = getGridMinHour(setTimesFiltered),
            gridMaxHour = getGridMaxHour(setTimesFiltered)
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


    private fun getGridMinHour(setTimes: List<SetTime>) =
        setTimes.minOfOrNull {
            it.startTime
                .toLocalDateTime()
                .hour
                .let(::normalizedFestivalHour)
        } ?: 0

    private fun getGridMaxHour(setTimes: List<SetTime>) =
        setTimes.maxOfOrNull {
            it.endTime
                .toLocalDateTime()
                .hour
                .let(::normalizedFestivalHour)
        }?.plus(1) ?: 0
}

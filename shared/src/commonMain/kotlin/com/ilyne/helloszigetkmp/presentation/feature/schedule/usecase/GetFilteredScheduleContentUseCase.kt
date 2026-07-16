package com.ilyne.helloszigetkmp.presentation.feature.schedule.usecase

import com.ilyne.helloszigetkmp.domain.model.Artist
import com.ilyne.helloszigetkmp.domain.model.ArtistFriendsFavorited
import com.ilyne.helloszigetkmp.domain.model.SetTime
import com.ilyne.helloszigetkmp.domain.model.Stage
import com.ilyne.helloszigetkmp.domain.model.passesGenreFilter
import com.ilyne.helloszigetkmp.presentation.feature.schedule.ScheduleUiState
import com.ilyne.helloszigetkmp.presentation.feature.schedule.filter.ScheduleFilter
import com.ilyne.helloszigetkmp.util.datetime.normalizedFestivalHourFractionRange
import com.ilyne.helloszigetkmp.util.datetime.normalizedFestivalHourRange
import kotlin.time.Clock

class GetFilteredScheduleContentUseCase {
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
                val (startHourFraction, endHourFraction) =
                    normalizedFestivalHourFractionRange(setTime.startTime, setTime.endTime)
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
                                friendsFavorited = emptyList(),
                            )
                    },
                    stage = setTime.stage,
                    isInThePast = setTime.endTime < currentTimeMillis,
                    startHourFraction = startHourFraction,
                    endHourFraction = endHourFraction,
                )
            }

        val gridHourRange = getGridHourRange(setTimesFiltered)

        return Response(
            setTimes = setTimesUiModel,
            stages = stages,
            gridMinHour = gridHourRange.minHour,
            gridMaxHour = gridHourRange.maxHour,
        )
    }

    private fun List<SetTime>.filter(
        filter: ScheduleFilter,
        favoritedByArtistId: Map<String, ArtistFriendsFavorited>,
    ) = filter { setTime ->
        val hasFriendsFavorited = favoritedByArtistId[setTime.artistId]?.friendsFavorited?.isNotEmpty() == true
        val passesFavoritesFilter = when {
            !filter.showFavorites && !filter.showFriendsGoing -> {
                true
            }

            else -> {
                (filter.showFavorites && setTime.artist?.isFavorited == true) ||
                    (filter.showFriendsGoing && hasFriendsFavorited)
            }
        }

        passesFavoritesFilter && passesPerformanceTypeFilter(setTime.artist, filter)
    }

    private fun passesPerformanceTypeFilter(
        artist: Artist?,
        filter: ScheduleFilter,
    ): Boolean = passesGenreFilter(artist?.tags, filter.selectedGenreGroups)

    private data class GridHourRange(
        val minHour: Int,
        val maxHour: Int,
    )

    private fun getGridHourRange(setTimes: List<SetTime>): GridHourRange {
        if (setTimes.isEmpty()) return GridHourRange(minHour = 6, maxHour = 6)

        var minHour = Int.MAX_VALUE
        var maxHour = Int.MIN_VALUE
        for (setTime in setTimes) {
            val (startHour, endHour) = normalizedFestivalHourRange(setTime.startTime, setTime.endTime)
            if (startHour < minHour) minHour = startHour
            if (endHour > maxHour) maxHour = endHour
        }
        return GridHourRange(minHour = minHour, maxHour = maxHour + 1)
    }
}

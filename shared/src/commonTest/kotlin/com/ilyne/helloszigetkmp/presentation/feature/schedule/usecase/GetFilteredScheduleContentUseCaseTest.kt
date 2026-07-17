package com.ilyne.helloszigetkmp.presentation.feature.schedule.usecase

import com.ilyne.helloszigetkmp.domain.model.Artist
import com.ilyne.helloszigetkmp.domain.model.ArtistFriendsFavorited
import com.ilyne.helloszigetkmp.domain.model.GenreGroup
import com.ilyne.helloszigetkmp.domain.model.SetTime
import com.ilyne.helloszigetkmp.domain.model.Stage
import com.ilyne.helloszigetkmp.domain.model.User
import com.ilyne.helloszigetkmp.presentation.feature.schedule.filter.ScheduleFilter
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * [GetFilteredScheduleContentUseCase.invoke] applies the favorites/friends-going/genre filters to a raw set time list,
 * optionally hides stages with no remaining sets, and computes the grid's min/max hour from whatever survives filtering.
 */
class GetFilteredScheduleContentUseCaseTest {
    private val budapest = TimeZone.of("Europe/Budapest")
    private val useCase = GetFilteredScheduleContentUseCase()

    private fun millisAt(isoLocalDateTime: String): Long = LocalDateTime.parse(isoLocalDateTime).toInstant(budapest).toEpochMilliseconds()

    private val mainStage = Stage(id = "stage-1", name = "Main Stage", description = null)
    private val secondStage = Stage(id = "stage-2", name = "Second Stage", description = null)

    private fun artist(
        id: String,
        isFavorited: Boolean = false,
        tags: List<String>? = listOf("genre-techno"),
    ) = Artist(id = id, name = "Artist $id", bio = null, imageUrl = null, isFavorited = isFavorited, tags = tags)

    private fun setTime(
        id: String,
        artist: Artist,
        stage: Stage? = mainStage,
        startTime: Long = millisAt("2026-08-07T14:00:00"),
        endTime: Long = millisAt("2026-08-07T15:00:00"),
    ) = SetTime(
        id = id,
        artistId = artist.id,
        stageId = stage?.id,
        startTime = startTime,
        endTime = endTime,
        hideEndTime = false,
        artist = artist,
        stage = stage,
    )

    private fun noOpFilter() =
        ScheduleFilter(
            showFavorites = false,
            showFriendsGoing = false,
            hideEmptyStages = false,
            selectedGenreGroups = setOf(GenreGroup.TECHNO),
        )

    @Test
    fun noFiltersApplied_passesAllSetTimesThrough() {
        val artistA = artist(id = "a")
        val artistB = artist(id = "b")
        val setTimes = listOf(setTime(id = "1", artist = artistA), setTime(id = "2", artist = artistB))

        val result = useCase(
            filter = noOpFilter(),
            setTimes = setTimes,
            favoritedByArtistId = emptyMap(),
            allStages = listOf(mainStage),
        )

        assertEquals(listOf("1", "2"), result.setTimes.map { it.id })
    }

    @Test
    fun favoritesOnlyFilter_keepsOnlyFavoritedArtists() {
        val favoritedArtist = artist(id = "a", isFavorited = true)
        val nonFavoritedArtist = artist(id = "b", isFavorited = false)
        val setTimes = listOf(
            setTime(id = "1", artist = favoritedArtist),
            setTime(id = "2", artist = nonFavoritedArtist),
        )
        val filter = noOpFilter().copy(showFavorites = true)

        val result = useCase(
            filter = filter,
            setTimes = setTimes,
            favoritedByArtistId = emptyMap(),
            allStages = listOf(mainStage),
        )

        assertEquals(listOf("1"), result.setTimes.map { it.id })
    }

    @Test
    fun friendsGoingFilter_keepsOnlyArtistsWithFriendsFavorited() {
        val artistWithFriends = artist(id = "a")
        val artistWithoutFriends = artist(id = "b")
        val setTimes = listOf(
            setTime(id = "1", artist = artistWithFriends),
            setTime(id = "2", artist = artistWithoutFriends),
        )
        val favoritedByArtistId = mapOf(
            "a" to ArtistFriendsFavorited(
                artist = artistWithFriends,
                friendsFavorited = listOf(User(id = "friend-1", name = "Friend", imageUrl = null)),
            ),
            "b" to ArtistFriendsFavorited(artist = artistWithoutFriends, friendsFavorited = emptyList()),
        )
        val filter = noOpFilter().copy(showFriendsGoing = true)

        val result = useCase(
            filter = filter,
            setTimes = setTimes,
            favoritedByArtistId = favoritedByArtistId,
            allStages = listOf(mainStage),
        )

        assertEquals(listOf("1"), result.setTimes.map { it.id })
    }

    @Test
    fun combinedFavoritesAndFriendsGoingFilter_keepsEitherMatch() {
        val favoritedOnly = artist(id = "a", isFavorited = true)
        val friendsGoingOnly = artist(id = "b", isFavorited = false)
        val neither = artist(id = "c", isFavorited = false)
        val setTimes = listOf(
            setTime(id = "1", artist = favoritedOnly),
            setTime(id = "2", artist = friendsGoingOnly),
            setTime(id = "3", artist = neither),
        )
        val favoritedByArtistId = mapOf(
            "b" to ArtistFriendsFavorited(
                artist = friendsGoingOnly,
                friendsFavorited = listOf(User(id = "friend-1", name = "Friend", imageUrl = null)),
            ),
        )
        val filter = noOpFilter().copy(showFavorites = true, showFriendsGoing = true)

        val result = useCase(
            filter = filter,
            setTimes = setTimes,
            favoritedByArtistId = favoritedByArtistId,
            allStages = listOf(mainStage),
        )

        // "a" passes via isFavorited, "b" passes via friendsFavorited, "c" matches neither condition.
        assertEquals(listOf("1", "2"), result.setTimes.map { it.id })
    }

    @Test
    fun genreFilter_keepsOnlyArtistsMatchingSelectedGenreGroups() {
        val technoArtist = artist(id = "a", tags = listOf("genre-techno"))
        val comedyArtist = artist(id = "b", tags = listOf("genre-comedy"))
        val setTimes = listOf(setTime(id = "1", artist = technoArtist), setTime(id = "2", artist = comedyArtist))
        val filter = noOpFilter().copy(selectedGenreGroups = setOf(GenreGroup.TECHNO))

        val result = useCase(
            filter = filter,
            setTimes = setTimes,
            favoritedByArtistId = emptyMap(),
            allStages = listOf(mainStage),
        )

        assertEquals(listOf("1"), result.setTimes.map { it.id })
    }

    @Test
    fun genreFilter_unrecognizedTags_fallBackToUnknownGroup() {
        val unrecognizedTagArtist = artist(id = "a", tags = listOf("genre-does-not-exist"))
        val noTagsArtist = artist(id = "b", tags = null)
        val setTimes = listOf(setTime(id = "1", artist = unrecognizedTagArtist), setTime(id = "2", artist = noTagsArtist))

        val excludingUnknown = noOpFilter().copy(selectedGenreGroups = setOf(GenreGroup.TECHNO))
        val resultExcluding = useCase(
            filter = excludingUnknown,
            setTimes = setTimes,
            favoritedByArtistId = emptyMap(),
            allStages = listOf(mainStage),
        )
        assertEquals(emptyList(), resultExcluding.setTimes.map { it.id })

        val includingUnknown = noOpFilter().copy(selectedGenreGroups = setOf(GenreGroup.UNKNOWN))
        val resultIncluding = useCase(
            filter = includingUnknown,
            setTimes = setTimes,
            favoritedByArtistId = emptyMap(),
            allStages = listOf(mainStage),
        )
        assertEquals(listOf("1", "2"), resultIncluding.setTimes.map { it.id })
    }

    @Test
    fun hideEmptyStages_true_returnsOnlyStagesWithRemainingSetTimes() {
        val artistA = artist(id = "a")
        val setTimes = listOf(setTime(id = "1", artist = artistA, stage = mainStage))
        val filter = noOpFilter().copy(hideEmptyStages = true)

        val result = useCase(
            filter = filter,
            setTimes = setTimes,
            favoritedByArtistId = emptyMap(),
            allStages = listOf(mainStage, secondStage),
        )

        assertEquals(listOf(mainStage), result.stages)
    }

    @Test
    fun hideEmptyStages_true_afterFilteringRemovesAllSets_producesNoStages() {
        val nonFavoritedArtist = artist(id = "a", isFavorited = false)
        val setTimes = listOf(setTime(id = "1", artist = nonFavoritedArtist, stage = mainStage))
        val filter = noOpFilter().copy(hideEmptyStages = true, showFavorites = true)

        val result = useCase(
            filter = filter,
            setTimes = setTimes,
            favoritedByArtistId = emptyMap(),
            allStages = listOf(mainStage, secondStage),
        )

        assertTrue(result.stages.isEmpty())
    }

    @Test
    fun hideEmptyStages_false_returnsAllStagesRegardlessOfFiltering() {
        val nonFavoritedArtist = artist(id = "a", isFavorited = false)
        val setTimes = listOf(setTime(id = "1", artist = nonFavoritedArtist, stage = mainStage))
        val filter = noOpFilter().copy(hideEmptyStages = false, showFavorites = true)

        val result = useCase(
            filter = filter,
            setTimes = setTimes,
            favoritedByArtistId = emptyMap(),
            allStages = listOf(mainStage, secondStage),
        )

        assertEquals(listOf(mainStage, secondStage), result.stages)
    }

    @Test
    fun gridHourRange_populatedList_spansMinStartHourToMaxEndHourPlusOne() {
        val artistA = artist(id = "a")
        val artistB = artist(id = "b")
        val setTimes = listOf(
            // 14:00-16:00 stays fully post-cutoff: startHour=14, endHour=16.
            setTime(
                id = "1",
                artist = artistA,
                startTime = millisAt("2026-08-07T14:00:00"),
                endTime = millisAt("2026-08-07T16:00:00"),
            ),
            // 23:00-02:00 crosses midnight into the pre-cutoff window: startHour=23, endHour=(2+24)=26.
            setTime(
                id = "2",
                artist = artistB,
                startTime = millisAt("2026-08-07T23:00:00"),
                endTime = millisAt("2026-08-08T02:00:00"),
            ),
        )

        val result = useCase(
            filter = noOpFilter(),
            setTimes = setTimes,
            favoritedByArtistId = emptyMap(),
            allStages = listOf(mainStage),
        )

        // gridMinHour is the minimum start hour across sets (14), gridMaxHour is the max end hour (26) + 1.
        assertEquals(14, result.gridMinHour)
        assertEquals(27, result.gridMaxHour)
    }

    @Test
    fun gridHourRange_emptyFilteredList_defaultsToSixToSix() {
        val result = useCase(
            filter = noOpFilter(),
            setTimes = emptyList(),
            favoritedByArtistId = emptyMap(),
            allStages = listOf(mainStage),
        )

        assertEquals(6, result.gridMinHour)
        assertEquals(6, result.gridMaxHour)
    }

    @Test
    fun gridHourRange_allSetTimesFilteredOut_defaultsToSixToSix() {
        val nonFavoritedArtist = artist(id = "a", isFavorited = false)
        val setTimes = listOf(setTime(id = "1", artist = nonFavoritedArtist))
        val filter = noOpFilter().copy(showFavorites = true)

        val result = useCase(
            filter = filter,
            setTimes = setTimes,
            favoritedByArtistId = emptyMap(),
            allStages = listOf(mainStage),
        )

        assertEquals(6, result.gridMinHour)
        assertEquals(6, result.gridMaxHour)
    }
}

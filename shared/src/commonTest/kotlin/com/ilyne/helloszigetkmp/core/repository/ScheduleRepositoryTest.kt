package com.ilyne.helloszigetkmp.core.repository

import com.ilyne.helloszigetkmp.core.api.SzigetApiService
import com.ilyne.helloszigetkmp.core.db.dao.ArtistDao
import com.ilyne.helloszigetkmp.core.db.dao.SetTimeDao
import com.ilyne.helloszigetkmp.core.db.dao.StageDao
import com.ilyne.helloszigetkmp.core.db.entity.ArtistEntity
import com.ilyne.helloszigetkmp.core.db.entity.SetTimeEntity
import com.ilyne.helloszigetkmp.core.db.entity.StageEntity
import com.ilyne.helloszigetkmp.core.db.model.SetTimeWithArtistStageSummary
import com.ilyne.helloszigetkmp.domain.model.Artist
import com.ilyne.helloszigetkmp.domain.model.SetTime
import com.ilyne.helloszigetkmp.domain.model.Stage
import com.russhwolf.settings.MapSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respondOk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Direct tests for [ScheduleRepository.observeSetTimesForDay], which combines set-times, stages and artists
 * from their respective DAOs and manually joins them by id into domain [SetTime]/[Stage] objects.
 */
class ScheduleRepositoryTest {
    private val stage1 = StageEntity(id = "stage-1", name = "Main Stage", description = "Big stage")
    private val stage2 = StageEntity(id = "stage-2", name = "A38", description = null)

    private val artist1 = ArtistEntity(
        id = "artist-1",
        name = "Artist One",
        bio = "bio one",
        imageUrl = "https://example.test/1.jpg",
        isFavorited = true,
        tags = listOf("techno"),
    )
    private val artist2 = ArtistEntity(
        id = "artist-2",
        name = "Artist Two",
        bio = null,
        imageUrl = null,
        isFavorited = false,
        tags = null,
    )

    @Test
    fun observeSetTimesForDay_joinsSetTimeWithMatchingStageAndArtist() =
        runTest {
            val setTime = SetTimeEntity(
                id = "st-1",
                artistId = artist1.id,
                stageId = stage1.id,
                startTime = 1_000L,
                endTime = 2_000L,
                hideEndTime = false,
            )

            val repository = repository(
                setTimes = listOf(setTime),
                stages = listOf(stage1, stage2),
                artists = listOf(artist1, artist2),
            )

            val result = repository.observeSetTimesForDay(dayStartMillis = 0L, dayEndMillis = 3_000L).first()

            assertEquals(
                listOf(
                    SetTime(
                        id = "st-1",
                        artistId = artist1.id,
                        stageId = stage1.id,
                        startTime = 1_000L,
                        endTime = 2_000L,
                        hideEndTime = false,
                        artist = Artist(
                            id = artist1.id,
                            name = artist1.name,
                            bio = artist1.bio,
                            imageUrl = artist1.imageUrl,
                            isFavorited = artist1.isFavorited,
                            tags = artist1.tags,
                        ),
                        stage = Stage(id = stage1.id, name = stage1.name, description = stage1.description),
                    ),
                ),
                result.setTimes,
            )
            assertEquals(
                listOf(
                    Stage(id = stage1.id, name = stage1.name, description = stage1.description),
                    Stage(id = stage2.id, name = stage2.name, description = stage2.description),
                ),
                result.stages,
            )
        }

    @Test
    fun observeSetTimesForDay_danglingArtistAndStageReferences_areMappedToNullInsteadOfCrashing() =
        runTest {
            val setTime = SetTimeEntity(
                id = "st-dangling",
                artistId = "missing-artist",
                stageId = "missing-stage",
                startTime = 1_000L,
                endTime = 2_000L,
                hideEndTime = false,
            )

            val repository = repository(
                setTimes = listOf(setTime),
                stages = listOf(stage1),
                artists = listOf(artist1),
            )

            val result = repository.observeSetTimesForDay(dayStartMillis = 0L, dayEndMillis = 3_000L).first()

            // The set time itself is still included, but its artist/stage lookups gracefully resolve to null
            // rather than throwing or being dropped from the list.
            assertEquals(1, result.setTimes.size)
            val mapped = result.setTimes.first()
            assertEquals("st-dangling", mapped.id)
            assertEquals("missing-artist", mapped.artistId)
            assertEquals("missing-stage", mapped.stageId)
            assertEquals(null, mapped.artist)
            assertEquals(null, mapped.stage)
        }

    @Test
    fun observeSetTimesForDay_setTimeWithNullStageId_hasNullStageWithoutCrashing() =
        runTest {
            val setTime = SetTimeEntity(
                id = "st-no-stage",
                artistId = artist1.id,
                stageId = null,
                startTime = 1_000L,
                endTime = 2_000L,
                hideEndTime = false,
            )

            val repository = repository(
                setTimes = listOf(setTime),
                stages = listOf(stage1),
                artists = listOf(artist1),
            )

            val result = repository.observeSetTimesForDay(dayStartMillis = 0L, dayEndMillis = 3_000L).first()

            assertEquals(1, result.setTimes.size)
            assertEquals(null, result.setTimes.first().stageId)
            assertEquals(null, result.setTimes.first().stage)
        }

    @Test
    fun observeSetTimesForDay_onlyIncludesSetTimesWithinTheRequestedDayWindow() =
        runTest {
            // The DAO's own query (observeByDay) is what performs the day filtering in production (a SQL
            // WHERE clause on startTime); this fake mirrors that behavior so the repository's combine logic
            // is exercised against a realistic, already-filtered stream rather than doing its own filtering.
            val dayOneSetTime = SetTimeEntity(
                id = "st-day-1",
                artistId = artist1.id,
                stageId = stage1.id,
                startTime = 1_000L,
                endTime = 1_500L,
                hideEndTime = false,
            )
            val dayTwoSetTime = SetTimeEntity(
                id = "st-day-2",
                artistId = artist2.id,
                stageId = stage2.id,
                startTime = 10_000L,
                endTime = 10_500L,
                hideEndTime = false,
            )

            val repository = repository(
                setTimes = listOf(dayOneSetTime, dayTwoSetTime),
                stages = listOf(stage1, stage2),
                artists = listOf(artist1, artist2),
            )

            val dayOneResult = repository.observeSetTimesForDay(dayStartMillis = 0L, dayEndMillis = 5_000L).first()
            assertEquals(listOf("st-day-1"), dayOneResult.setTimes.map { it.id })

            val dayTwoResult = repository.observeSetTimesForDay(dayStartMillis = 5_000L, dayEndMillis = 15_000L).first()
            assertEquals(listOf("st-day-2"), dayTwoResult.setTimes.map { it.id })
        }

    private fun repository(
        setTimes: List<SetTimeEntity>,
        stages: List<StageEntity>,
        artists: List<ArtistEntity>,
    ): ScheduleRepository {
        val setTimeDao = object : SetTimeDao {
            override fun observeByDay(
                dayStartMillis: Long,
                dayEndMillis: Long,
            ): Flow<List<SetTimeEntity>> = flowOf(setTimes.filter { it.startTime >= dayStartMillis && it.startTime < dayEndMillis })

            override fun observeByArtist(artistId: String): Flow<List<SetTimeEntity>> = flowOf(setTimes.filter { it.artistId == artistId })

            override suspend fun upsertAll(setTimes: List<SetTimeEntity>) {}

            override fun observeSetTimeRange(): Flow<SetTimeDao.SetTimeRange> =
                flowOf(SetTimeDao.SetTimeRange(minStart = 0L, maxStart = 0L))

            override fun observeFavorites(): Flow<List<SetTimeWithArtistStageSummary>> = flowOf(emptyList())
        }
        val stageDao = object : StageDao {
            override fun observeAll(): Flow<List<StageEntity>> = flowOf(stages)

            override suspend fun upsertAll(stages: List<StageEntity>) {}
        }
        val artistDao = object : ArtistDao {
            override fun observeAll(): Flow<List<ArtistEntity>> = flowOf(artists)

            override fun observeById(id: String): Flow<ArtistEntity?> = flowOf(artists.find { it.id == id })

            override fun observeFavorites(): Flow<List<ArtistEntity>> = flowOf(artists.filter { it.isFavorited })

            override suspend fun upsertAll(artists: List<ArtistEntity>) {}

            override suspend fun setFavorited(
                id: String,
                isFavorited: Boolean,
            ) {}

            override fun searchByName(query: String): Flow<List<ArtistEntity>> = flowOf(emptyList())

            override suspend fun deleteAll() {}
        }
        val api = SzigetApiService(
            client = HttpClient(MockEngine) { engine { addHandler { respondOk() } } },
            baseUrl = "https://unused.test",
        )
        return ScheduleRepository(
            api = api,
            setTimeDao = setTimeDao,
            stageDao = stageDao,
            artistDao = artistDao,
            settings = MapSettings(),
        )
    }
}

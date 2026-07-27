package com.ilyne.helloszigetkmp.core.db.dao

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.ilyne.helloszigetkmp.core.db.SzigetDatabase
import com.ilyne.helloszigetkmp.core.db.entity.ArtistEntity
import com.ilyne.helloszigetkmp.core.db.entity.SetTimeEntity
import com.ilyne.helloszigetkmp.core.db.entity.StageEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SetTimeDaoTest {
    private lateinit var database: SzigetDatabase
    private lateinit var setTimeDao: SetTimeDao

    @BeforeTest
    fun setUp() {
        database =
            Room
                .inMemoryDatabaseBuilder<SzigetDatabase>()
                .setDriver(BundledSQLiteDriver())
                .setQueryCoroutineContext(Dispatchers.IO)
                .build()
        setTimeDao = database.setTimeDao()
    }

    @AfterTest
    fun tearDown() {
        database.close()
    }

    private suspend fun seedArtists(vararg ids: String) =
        database.artistDao().upsertAll(
            ids.map { id ->
                ArtistEntity(id = id, name = id, bio = null, imageUrl = null, isFavorited = false, tags = null)
            },
        )

    private suspend fun seedStages(vararg ids: String) =
        database.stageDao().upsertAll(
            ids.map { id -> StageEntity(id = id, name = id, description = null) },
        )

    private fun setTime(
        id: String,
        artistId: String,
        stageId: String? = null,
        startTime: Long,
        endTime: Long = startTime + 1_000,
    ) = SetTimeEntity(
        id = id,
        artistId = artistId,
        stageId = stageId,
        startTime = startTime,
        endTime = endTime,
        hideEndTime = false,
    )

    // ---- observeByDay boundary tests ----

    @Test
    fun `observeByDay includes a set time exactly at the start boundary`() =
        runTest {
            seedArtists("artistA")
            setTimeDao.upsertAll(listOf(setTime(id = "s1", artistId = "artistA", startTime = 1_000)))

            val result = setTimeDao.observeByDay(dayStartMillis = 1_000, dayEndMillis = 2_000).first()

            assertEquals(listOf("s1"), result.map { it.id })
        }

    @Test
    fun `observeByDay excludes a set time exactly at the end boundary`() =
        runTest {
            seedArtists("artistA")
            setTimeDao.upsertAll(listOf(setTime(id = "s1", artistId = "artistA", startTime = 2_000)))

            val result = setTimeDao.observeByDay(dayStartMillis = 1_000, dayEndMillis = 2_000).first()

            assertEquals(emptyList(), result)
        }

    @Test
    fun `observeByDay excludes a set time just before the start boundary`() =
        runTest {
            seedArtists("artistA")
            setTimeDao.upsertAll(listOf(setTime(id = "s1", artistId = "artistA", startTime = 999)))

            val result = setTimeDao.observeByDay(dayStartMillis = 1_000, dayEndMillis = 2_000).first()

            assertEquals(emptyList(), result)
        }

    @Test
    fun `observeByDay includes a set time just before the end boundary`() =
        runTest {
            seedArtists("artistA")
            setTimeDao.upsertAll(listOf(setTime(id = "s1", artistId = "artistA", startTime = 1_999)))

            val result = setTimeDao.observeByDay(dayStartMillis = 1_000, dayEndMillis = 2_000).first()

            assertEquals(listOf("s1"), result.map { it.id })
        }

    @Test
    fun `observeByDay returns only set times within the half-open range ordered by startTime`() =
        runTest {
            seedArtists("artistA")
            setTimeDao.upsertAll(
                listOf(
                    setTime(id = "before", artistId = "artistA", startTime = 500),
                    setTime(id = "atStart", artistId = "artistA", startTime = 1_000),
                    setTime(id = "middleLate", artistId = "artistA", startTime = 1_800),
                    setTime(id = "middleEarly", artistId = "artistA", startTime = 1_200),
                    setTime(id = "atEnd", artistId = "artistA", startTime = 2_000),
                    setTime(id = "after", artistId = "artistA", startTime = 2_500),
                ),
            )

            val result = setTimeDao.observeByDay(dayStartMillis = 1_000, dayEndMillis = 2_000).first()

            assertEquals(listOf("atStart", "middleEarly", "middleLate"), result.map { it.id })
        }

    // ---- observeSetTimeRange tests ----

    @Test
    fun `observeSetTimeRange on an empty table returns null bounds instead of epoch 0`() =
        runTest {
            val range = setTimeDao.observeSetTimeRange().first()

            // SQLite's MIN/MAX over zero rows yields NULL. minStart/maxStart are nullable Longs
            // specifically so Room preserves that NULL instead of coercing it to 0 (epoch millis
            // = 1 Jan 1970), which would otherwise render a bogus day tab before any sync.
            assertNull(range.minStart)
            assertNull(range.maxStart)
        }

    @Test
    fun `observeSetTimeRange returns the min and max startTime across all rows`() =
        runTest {
            seedArtists("artistA")
            setTimeDao.upsertAll(
                listOf(
                    setTime(id = "s1", artistId = "artistA", startTime = 5_000),
                    setTime(id = "s2", artistId = "artistA", startTime = 1_000),
                    setTime(id = "s3", artistId = "artistA", startTime = 3_000),
                ),
            )

            val range = setTimeDao.observeSetTimeRange().first()

            assertEquals(SetTimeDao.SetTimeRange(minStart = 1_000, maxStart = 5_000), range)
        }

    @Test
    fun `observeSetTimeRange with a single row returns its startTime as both min and max`() =
        runTest {
            seedArtists("artistA")
            setTimeDao.upsertAll(listOf(setTime(id = "s1", artistId = "artistA", startTime = 4_242)))

            val range = setTimeDao.observeSetTimeRange().first()

            assertEquals(SetTimeDao.SetTimeRange(minStart = 4_242, maxStart = 4_242), range)
        }

    // ---- observeFavorites tests ----

    @Test
    fun `observeFavorites only returns set times for favorited artists`() =
        runTest {
            seedArtists("favArtist", "notFavArtist")
            seedStages("stage1")
            database.artistDao().setFavorited(id = "favArtist", isFavorited = true)
            setTimeDao.upsertAll(
                listOf(
                    setTime(id = "s1", artistId = "favArtist", stageId = "stage1", startTime = 1_000),
                    setTime(id = "s2", artistId = "notFavArtist", stageId = "stage1", startTime = 2_000),
                ),
            )

            val result = setTimeDao.observeFavorites().first()

            assertEquals(listOf("s1"), result.map { it.id })
            assertEquals("favArtist", result.first().artistName)
            assertEquals("stage1", result.first().stageName)
        }

    @Test
    fun `observeFavorites returns empty when no artists are favorited`() =
        runTest {
            seedArtists("artistA")
            seedStages("stage1")
            setTimeDao.upsertAll(
                listOf(setTime(id = "s1", artistId = "artistA", stageId = "stage1", startTime = 1_000)),
            )

            val result = setTimeDao.observeFavorites().first()

            assertTrue(result.isEmpty())
        }

    @Test
    fun `observeFavorites includes a favorited set time with no synced stage`() =
        runTest {
            // stageId is nullable (a set can be favorited before its stage syncs, or the
            // backend can omit one) - this must not silently drop the set from My Lineup, so
            // the stage join has to be a LEFT JOIN rather than an INNER JOIN.
            seedArtists("favArtist")
            database.artistDao().setFavorited(id = "favArtist", isFavorited = true)
            setTimeDao.upsertAll(
                listOf(setTime(id = "s1", artistId = "favArtist", stageId = null, startTime = 1_000)),
            )

            val result = setTimeDao.observeFavorites().first()

            assertEquals(listOf("s1"), result.map { it.id })
            assertEquals(null, result.first().stageName)
        }

    @Test
    fun `observeFavorites returns results ordered by startTime`() =
        runTest {
            seedArtists("favArtist")
            seedStages("stage1")
            database.artistDao().setFavorited(id = "favArtist", isFavorited = true)
            setTimeDao.upsertAll(
                listOf(
                    setTime(id = "late", artistId = "favArtist", stageId = "stage1", startTime = 3_000),
                    setTime(id = "early", artistId = "favArtist", stageId = "stage1", startTime = 1_000),
                    setTime(id = "middle", artistId = "favArtist", stageId = "stage1", startTime = 2_000),
                ),
            )

            val result = setTimeDao.observeFavorites().first()

            assertEquals(listOf("early", "middle", "late"), result.map { it.id })
        }
}

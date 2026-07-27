package com.ilyne.helloszigetkmp.core.db.dao

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.ilyne.helloszigetkmp.core.db.SzigetDatabase
import com.ilyne.helloszigetkmp.core.db.entity.ArtistEntity
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

class ArtistDaoTest {
    private lateinit var database: SzigetDatabase
    private lateinit var artistDao: ArtistDao

    @BeforeTest
    fun setUp() {
        database =
            Room
                .inMemoryDatabaseBuilder<SzigetDatabase>()
                .setDriver(BundledSQLiteDriver())
                .setQueryCoroutineContext(Dispatchers.IO)
                .build()
        artistDao = database.artistDao()
    }

    @AfterTest
    fun tearDown() {
        database.close()
    }

    private fun artist(
        id: String,
        name: String,
        isFavorited: Boolean = false,
    ) = ArtistEntity(id = id, name = name, bio = null, imageUrl = null, isFavorited = isFavorited, tags = null)

    @Test
    fun `searchByName matches regardless of case`() =
        runTest {
            artistDao.upsertAll(listOf(artist("1", "ABBA")))

            assertEquals(listOf(artist("1", "ABBA")), artistDao.searchByName("abba").first())
            assertEquals(listOf(artist("1", "ABBA")), artistDao.searchByName("Abba").first())
            assertEquals(listOf(artist("1", "ABBA")), artistDao.searchByName("ABBA").first())
        }

    @Test
    fun `searchByName matches partial substrings`() =
        runTest {
            artistDao.upsertAll(listOf(artist("1", "ABBA")))

            assertEquals(listOf(artist("1", "ABBA")), artistDao.searchByName("bb").first())
            assertEquals(listOf(artist("1", "ABBA")), artistDao.searchByName("a").first())
        }

    @Test
    fun `searchByName treats percent and underscore as literal characters rather than SQL LIKE wildcards`() =
        runTest {
            artistDao.upsertAll(listOf(artist("1", "50% Off"), artist("2", "Foo_Bar"), artist("3", "FooXBar")))

            // A literal '%' must only match names containing that character, not act as a
            // wildcard matching everything.
            assertEquals(listOf(artist("1", "50% Off")), artistDao.searchByName("%").first())

            // A literal '_' must only match names containing that character, not act as a
            // single-character wildcard (which would also match "FooXBar").
            assertEquals(listOf(artist("2", "Foo_Bar")), artistDao.searchByName("_").first())
        }

    @Test
    fun `searchByName is diacritic-aware when case-folding`() =
        runTest {
            // SQL LOWER()/UPPER() are ASCII-only, so "Á" would not case-fold to match "á" via
            // SQL LIKE ... COLLATE NOCASE. Kotlin's String.lowercase() is Unicode-aware.
            artistDao.upsertAll(listOf(artist("1", "Ökrös Együttes")))

            assertEquals(listOf(artist("1", "Ökrös Együttes")), artistDao.searchByName("ökrös").first())
            assertEquals(listOf(artist("1", "Ökrös Együttes")), artistDao.searchByName("ÖKRÖS").first())
        }

    @Test
    fun `searchByName returns empty list when nothing matches`() =
        runTest {
            artistDao.upsertAll(listOf(artist("1", "ABBA"), artist("2", "Queen")))

            assertTrue(artistDao.searchByName("zzz").first().isEmpty())
        }

    @Test
    fun `searchByName returns matches ordered by name ascending`() =
        runTest {
            artistDao.upsertAll(listOf(artist("1", "The Weeknd"), artist("2", "Adele"), artist("3", "Beyonce")))

            val result = artistDao.searchByName("e").first()

            assertEquals(listOf("Adele", "Beyonce", "The Weeknd"), result.map { it.name })
        }

    @Test
    fun `upsertAll inserts new artists and observeAll returns them ordered by name`() =
        runTest {
            artistDao.upsertAll(listOf(artist("1", "Zeppelin"), artist("2", "Abba")))

            val result = artistDao.observeAll().first()

            assertEquals(listOf("Abba", "Zeppelin"), result.map { it.name })
        }

    @Test
    fun `upsertAll updates existing artist with the same id`() =
        runTest {
            artistDao.upsertAll(listOf(artist("1", "Old Name")))
            artistDao.upsertAll(listOf(artist("1", "New Name")))

            val result = artistDao.observeAll().first()

            assertEquals(listOf("New Name"), result.map { it.name })
        }

    @Test
    fun `observeById returns the matching artist`() =
        runTest {
            artistDao.upsertAll(listOf(artist("1", "Abba"), artist("2", "Queen")))

            assertEquals(artist("1", "Abba"), artistDao.observeById("1").first())
        }

    @Test
    fun `observeById returns null when artist does not exist`() =
        runTest {
            assertNull(artistDao.observeById("missing").first())
        }

    @Test
    fun `observeFavorites returns only favorited artists`() =
        runTest {
            artistDao.upsertAll(
                listOf(
                    artist("1", "Abba", isFavorited = true),
                    artist("2", "Queen", isFavorited = false),
                    artist("3", "Zeppelin", isFavorited = true),
                ),
            )

            val result = artistDao.observeFavorites().first()

            assertEquals(listOf("Abba", "Zeppelin"), result.map { it.name })
        }

    @Test
    fun `setFavorited toggles favorite state`() =
        runTest {
            artistDao.upsertAll(listOf(artist("1", "Abba", isFavorited = false)))

            artistDao.setFavorited("1", true)
            assertEquals(true, artistDao.observeById("1").first()?.isFavorited)

            artistDao.setFavorited("1", false)
            assertEquals(false, artistDao.observeById("1").first()?.isFavorited)
        }

    @Test
    fun `deleteAll removes every artist`() =
        runTest {
            artistDao.upsertAll(listOf(artist("1", "Abba"), artist("2", "Queen")))

            artistDao.deleteAll()

            assertTrue(artistDao.observeAll().first().isEmpty())
        }
}

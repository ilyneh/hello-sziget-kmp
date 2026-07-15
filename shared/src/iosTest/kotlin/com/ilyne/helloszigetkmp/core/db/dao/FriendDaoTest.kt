package com.ilyne.helloszigetkmp.core.db.dao

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.ilyne.helloszigetkmp.core.db.SzigetDatabase
import com.ilyne.helloszigetkmp.core.db.entity.ArtistEntity
import com.ilyne.helloszigetkmp.core.db.entity.ArtistFriendFavoritedEntity
import com.ilyne.helloszigetkmp.core.db.entity.UserEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class FriendDaoTest {
    private lateinit var database: SzigetDatabase
    private lateinit var friendDao: FriendDao

    @BeforeTest
    fun setUp() {
        database =
            Room
                .inMemoryDatabaseBuilder<SzigetDatabase>()
                .setDriver(BundledSQLiteDriver())
                .setQueryCoroutineContext(Dispatchers.IO)
                .build()
        friendDao = database.friendDao()
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

    private suspend fun seedUsers(vararg ids: String) =
        database.userDao().upsertAll(ids.map { id -> UserEntity(id = id, name = id, imageUrl = null) })

    private fun favorite(
        artistId: String,
        friendId: String,
    ) = ArtistFriendFavoritedEntity(artistId = artistId, friendId = friendId)

    @Test
    fun `upsert inserts new favorites`() =
        runTest {
            seedArtists("artistA")
            seedUsers("friend1", "friend2")

            friendDao.upsertAndPruneArtistFriendFavorited(
                listOf(favorite("artistA", "friend1"), favorite("artistA", "friend2")),
            )

            assertEquals(
                setOf(favorite("artistA", "friend1"), favorite("artistA", "friend2")),
                friendDao.getAllArtistFriendFavorites().toSet(),
            )
        }

    @Test
    fun `upsert is idempotent for the same pair`() =
        runTest {
            seedArtists("artistA")
            seedUsers("friend1")

            friendDao.upsertAndPruneArtistFriendFavorited(listOf(favorite("artistA", "friend1")))
            friendDao.upsertAndPruneArtistFriendFavorited(listOf(favorite("artistA", "friend1")))

            assertEquals(listOf(favorite("artistA", "friend1")), friendDao.getAllArtistFriendFavorites())
        }

    @Test
    fun `prunes a friend who is no longer in the incoming batch for an artist that is still present`() =
        runTest {
            seedArtists("artistA")
            seedUsers("friend1", "friend2")
            friendDao.upsertAndPruneArtistFriendFavorited(
                listOf(favorite("artistA", "friend1"), favorite("artistA", "friend2")),
            )

            // friend2 unfavorited artistA; only friend1 is reported as still favoriting it.
            friendDao.upsertAndPruneArtistFriendFavorited(listOf(favorite("artistA", "friend1")))

            assertEquals(listOf(favorite("artistA", "friend1")), friendDao.getAllArtistFriendFavorites())
        }

    @Test
    fun `prunes every row for an artist that drops out of the incoming batch entirely`() =
        runTest {
            seedArtists("artistA", "artistB")
            seedUsers("friend1", "friend2")
            friendDao.upsertAndPruneArtistFriendFavorited(
                listOf(favorite("artistA", "friend1"), favorite("artistB", "friend2")),
            )

            // artistA has zero friends favoriting it now, so it's absent from the refreshed batch.
            friendDao.upsertAndPruneArtistFriendFavorited(listOf(favorite("artistB", "friend2")))

            assertEquals(listOf(favorite("artistB", "friend2")), friendDao.getAllArtistFriendFavorites())
        }

    @Test
    fun `prunes everything when the incoming batch is empty`() =
        runTest {
            seedArtists("artistA", "artistB")
            seedUsers("friend1", "friend2")
            friendDao.upsertAndPruneArtistFriendFavorited(
                listOf(favorite("artistA", "friend1"), favorite("artistB", "friend2")),
            )

            friendDao.upsertAndPruneArtistFriendFavorited(emptyList())

            assertEquals(emptyList(), friendDao.getAllArtistFriendFavorites())
        }

    @Test
    fun `prunes and upserts independently across multiple artists in one call`() =
        runTest {
            seedArtists("artistA", "artistB", "artistC")
            seedUsers("friend1", "friend2", "friend3")
            friendDao.upsertAndPruneArtistFriendFavorited(
                listOf(
                    favorite("artistA", "friend1"),
                    favorite("artistA", "friend2"),
                    favorite("artistB", "friend1"),
                    favorite("artistC", "friend3"),
                ),
            )

            // artistA keeps only friend2, artistB gains friend3, artistC drops out entirely.
            friendDao.upsertAndPruneArtistFriendFavorited(
                listOf(favorite("artistA", "friend2"), favorite("artistB", "friend3")),
            )

            assertEquals(
                setOf(favorite("artistA", "friend2"), favorite("artistB", "friend3")),
                friendDao.getAllArtistFriendFavorites().toSet(),
            )
        }
}

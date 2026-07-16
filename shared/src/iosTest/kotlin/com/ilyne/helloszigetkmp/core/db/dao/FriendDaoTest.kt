package com.ilyne.helloszigetkmp.core.db.dao

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.ilyne.helloszigetkmp.core.db.SzigetDatabase
import com.ilyne.helloszigetkmp.core.db.entity.ArtistEntity
import com.ilyne.helloszigetkmp.core.db.entity.ArtistFriendFavoritedEntity
import com.ilyne.helloszigetkmp.core.db.entity.UserEntity
import com.ilyne.helloszigetkmp.core.db.entity.UserFriendEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.first
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

    private suspend fun setCurrentUser(userId: String) = database.userDao().setCurrentUser(userId)

    private fun friendship(
        userId: String,
        friendId: String,
        status: UserFriendEntity.Status,
    ) = UserFriendEntity(userId = userId, friendId = friendId, status = status)

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

    @Test
    fun `observeFriends only includes ACCEPTED friendships for the current user`() =
        runTest {
            seedUsers("me", "friend1", "friend2", "friend3")
            setCurrentUser("me")
            friendDao.upsertFriendships(
                listOf(
                    friendship("me", "friend1", UserFriendEntity.Status.ACCEPTED),
                    friendship("me", "friend2", UserFriendEntity.Status.REQUESTED),
                    friendship("me", "friend3", UserFriendEntity.Status.SENT),
                ),
            )

            val friends = friendDao.observeFriends().first()

            assertEquals(listOf(UserEntity(id = "friend1", name = "friend1", imageUrl = null)), friends)
        }

    @Test
    fun `observeFriends only includes friendships belonging to the current user`() =
        runTest {
            seedUsers("me", "otherUser", "friend1", "friend2")
            setCurrentUser("me")
            friendDao.upsertFriendships(
                listOf(
                    friendship("me", "friend1", UserFriendEntity.Status.ACCEPTED),
                    // Another user's accepted friendship should not leak into "me"'s friends list.
                    friendship("otherUser", "friend2", UserFriendEntity.Status.ACCEPTED),
                ),
            )

            val friends = friendDao.observeFriends().first()

            assertEquals(listOf(UserEntity(id = "friend1", name = "friend1", imageUrl = null)), friends)
        }

    @Test
    fun `observeFriends returns friends ordered by name ascending`() =
        runTest {
            seedUsers("me", "zed", "amy", "mike")
            setCurrentUser("me")
            friendDao.upsertFriendships(
                listOf(
                    friendship("me", "zed", UserFriendEntity.Status.ACCEPTED),
                    friendship("me", "amy", UserFriendEntity.Status.ACCEPTED),
                    friendship("me", "mike", UserFriendEntity.Status.ACCEPTED),
                ),
            )

            val friends = friendDao.observeFriends().first()

            assertEquals(listOf("amy", "mike", "zed"), friends.map { it.id })
        }

    @Test
    fun `observeFriends returns an empty list when the current user has no friendships`() =
        runTest {
            seedUsers("me")
            setCurrentUser("me")

            assertEquals(emptyList(), friendDao.observeFriends().first())
        }

    @Test
    fun `observeFriendRequests only includes REQUESTED friendships for the current user`() =
        runTest {
            seedUsers("me", "friend1", "friend2", "friend3")
            setCurrentUser("me")
            friendDao.upsertFriendships(
                listOf(
                    friendship("me", "friend1", UserFriendEntity.Status.REQUESTED),
                    friendship("me", "friend2", UserFriendEntity.Status.ACCEPTED),
                    friendship("me", "friend3", UserFriendEntity.Status.SENT),
                ),
            )

            val requests = friendDao.observeFriendRequests().first()

            assertEquals(listOf(UserEntity(id = "friend1", name = "friend1", imageUrl = null)), requests)
        }

    @Test
    fun `observeFriendRequests returns an empty list when there are no pending requests`() =
        runTest {
            seedUsers("me", "friend1")
            setCurrentUser("me")
            friendDao.upsertFriendships(listOf(friendship("me", "friend1", UserFriendEntity.Status.ACCEPTED)))

            assertEquals(emptyList(), friendDao.observeFriendRequests().first())
        }

    @Test
    fun `observeSentFriendRequests only includes SENT friendships for the current user`() =
        runTest {
            seedUsers("me", "friend1", "friend2", "friend3")
            setCurrentUser("me")
            friendDao.upsertFriendships(
                listOf(
                    friendship("me", "friend1", UserFriendEntity.Status.SENT),
                    friendship("me", "friend2", UserFriendEntity.Status.ACCEPTED),
                    friendship("me", "friend3", UserFriendEntity.Status.REQUESTED),
                ),
            )

            val sentRequests = friendDao.observeSentFriendRequests().first()

            assertEquals(listOf(UserEntity(id = "friend1", name = "friend1", imageUrl = null)), sentRequests)
        }

    @Test
    fun `observeSentFriendRequests returns an empty list when nothing has been sent`() =
        runTest {
            seedUsers("me", "friend1")
            setCurrentUser("me")
            friendDao.upsertFriendships(listOf(friendship("me", "friend1", UserFriendEntity.Status.ACCEPTED)))

            assertEquals(emptyList(), friendDao.observeSentFriendRequests().first())
        }

    @Test
    fun `observeFriendRequests and observeSentFriendRequests distinguish direction for the same pair`() =
        runTest {
            // "me" sent a request to "friend1", and separately "friend2" sent a request to "me".
            seedUsers("me", "friend1", "friend2")
            setCurrentUser("me")
            friendDao.upsertFriendships(
                listOf(
                    friendship("me", "friend1", UserFriendEntity.Status.SENT),
                    friendship("me", "friend2", UserFriendEntity.Status.REQUESTED),
                ),
            )

            assertEquals(
                listOf(UserEntity(id = "friend2", name = "friend2", imageUrl = null)),
                friendDao.observeFriendRequests().first(),
            )
            assertEquals(
                listOf(UserEntity(id = "friend1", name = "friend1", imageUrl = null)),
                friendDao.observeSentFriendRequests().first(),
            )
        }

    @Test
    fun `deleteFriendship removes only the targeted userId-friendId pair`() =
        runTest {
            seedUsers("me", "friend1", "friend2")
            setCurrentUser("me")
            friendDao.upsertFriendships(
                listOf(
                    friendship("me", "friend1", UserFriendEntity.Status.ACCEPTED),
                    friendship("me", "friend2", UserFriendEntity.Status.ACCEPTED),
                ),
            )

            friendDao.deleteFriendship(userId = "me", friendId = "friend1")

            assertEquals(
                listOf(UserEntity(id = "friend2", name = "friend2", imageUrl = null)),
                friendDao.observeFriends().first(),
            )
        }

    @Test
    fun `deleteFriendshipsNotIn keeps only friendships whose friendId is in the given list`() =
        runTest {
            seedUsers("me", "friend1", "friend2", "friend3")
            setCurrentUser("me")
            friendDao.upsertFriendships(
                listOf(
                    friendship("me", "friend1", UserFriendEntity.Status.ACCEPTED),
                    friendship("me", "friend2", UserFriendEntity.Status.ACCEPTED),
                    friendship("me", "friend3", UserFriendEntity.Status.ACCEPTED),
                ),
            )

            friendDao.deleteFriendshipsNotIn(listOf("friend2", "friend3"))

            assertEquals(
                listOf("friend2", "friend3"),
                friendDao.observeFriends().first().map { it.id },
            )
        }

    @Test
    fun `deleteFriendshipsNotIn removes everything when the keep list is empty`() =
        runTest {
            seedUsers("me", "friend1", "friend2")
            setCurrentUser("me")
            friendDao.upsertFriendships(
                listOf(
                    friendship("me", "friend1", UserFriendEntity.Status.ACCEPTED),
                    friendship("me", "friend2", UserFriendEntity.Status.ACCEPTED),
                ),
            )

            friendDao.deleteFriendshipsNotIn(emptyList())

            assertEquals(emptyList(), friendDao.observeFriends().first())
        }
}

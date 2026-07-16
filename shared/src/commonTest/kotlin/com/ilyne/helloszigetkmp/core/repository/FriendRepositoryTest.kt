package com.ilyne.helloszigetkmp.core.repository

import com.ilyne.helloszigetkmp.core.api.SzigetApiService
import com.ilyne.helloszigetkmp.core.db.dao.FriendDao
import com.ilyne.helloszigetkmp.core.db.dao.UserDao
import com.ilyne.helloszigetkmp.core.db.entity.ArtistFriendFavoritedEntity
import com.ilyne.helloszigetkmp.core.db.entity.CurrentUserEntity
import com.ilyne.helloszigetkmp.core.db.entity.UserEntity
import com.ilyne.helloszigetkmp.core.db.entity.UserFriendEntity
import com.ilyne.helloszigetkmp.core.db.entity.UserFriendEntity.Status
import com.ilyne.helloszigetkmp.core.db.model.ArtistFriendsFavoritedSummary
import com.russhwolf.settings.MapSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respondOk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * [FriendRepository.declineFriendRequest] and [FriendRepository.removeFriend] both do an optimistic local write
 * before calling the API, then roll the local write back if the API call throws (and rethrow).
 *
 * [FriendRepository.sendFriendRequest] and [FriendRepository.acceptFriendRequest]: both do an optimistic local
 * write, call the API, and roll the local write back (then rethrow) if the API call fails.
 */
class FriendRepositoryTest {
    private val currentUserId = "user-1"
    private val friendId = "friend-1"

    @Test
    fun declineFriendRequest_success_deletesLocallyAndCallsApi() =
        runTest {
            val fakeFriendDao = FakeFriendDao()
            val repository = repository(fakeFriendDao, apiSucceeds = true)

            repository.declineFriendRequest(currentUserId, friendId)

            assertEquals(
                listOf("delete($currentUserId, $friendId)"),
                fakeFriendDao.calls,
            )
        }

    @Test
    fun sendFriendRequest_success_writesLocalAndCallsApi_withoutRollback() =
        runTest {
            val fakeFriendDao = FakeFriendDao()
            val repository = repository(friendDao = fakeFriendDao, apiSucceeds = true)

            repository.sendFriendRequest(currentUserId, friendId)

            assertEquals(
                listOf(
                    "upsert($currentUserId, $friendId, ${Status.SENT})",
                ),
                fakeFriendDao.calls,
            )
        }

    @Test
    fun sendFriendRequest_apiFailure_rollsBackLocalWriteAndRethrows() =
        runTest {
            val fakeFriendDao = FakeFriendDao()
            val repository = repository(friendDao = fakeFriendDao, apiSucceeds = false)

            assertFailsWith<ApiFailureException> {
                repository.sendFriendRequest(currentUserId, friendId)
            }

            assertEquals(
                listOf(
                    "upsert($currentUserId, $friendId, ${Status.SENT})",
                    "delete($currentUserId, $friendId)",
                ),
                fakeFriendDao.calls,
            )
        }

    @Test
    fun declineFriendRequest_apiFailure_rollsBackToRequestedAndRethrows() =
        runTest {
            val fakeFriendDao = FakeFriendDao()
            val repository = repository(fakeFriendDao, apiSucceeds = false)

            assertFailsWith<Exception> {
                repository.declineFriendRequest(currentUserId, friendId)
            }

            assertEquals(
                listOf(
                    "delete($currentUserId, $friendId)",
                    "upsert($currentUserId, $friendId, ${Status.REQUESTED})",
                ),
                fakeFriendDao.calls,
            )
        }

    @Test
    fun removeFriend_success_deletesLocallyAndCallsApi() =
        runTest {
            val fakeFriendDao = FakeFriendDao()
            val repository = repository(fakeFriendDao, apiSucceeds = true)

            repository.removeFriend(currentUserId, friendId)

            assertEquals(
                listOf("delete($currentUserId, $friendId)"),
                fakeFriendDao.calls,
            )
        }

    @Test
    fun removeFriend_apiFailure_rollsBackToAcceptedAndRethrows() =
        runTest {
            val fakeFriendDao = FakeFriendDao()
            val repository = repository(fakeFriendDao, apiSucceeds = false)

            assertFailsWith<Exception> {
                repository.removeFriend(currentUserId, friendId)
            }

            assertEquals(
                listOf(
                    "delete($currentUserId, $friendId)",
                    "upsert($currentUserId, $friendId, ${Status.ACCEPTED})",
                ),
                fakeFriendDao.calls,
            )
        }

    @Test
    fun declineFriendRequest_apiFailure_lastCallIsRollbackUpsert() =
        runTest {
            val fakeFriendDao = FakeFriendDao()
            val repository = repository(fakeFriendDao, apiSucceeds = false)

            assertFailsWith<Exception> {
                repository.declineFriendRequest(currentUserId, friendId)
            }

            assertTrue(fakeFriendDao.calls.last().startsWith("upsert"))
        }

    @Test
    fun acceptFriendRequest_success_writesLocalAndCallsApi_withoutRollback() =
        runTest {
            val fakeFriendDao = FakeFriendDao()
            val repository = repository(friendDao = fakeFriendDao, apiSucceeds = true)

            repository.acceptFriendRequest(currentUserId, friendId)

            assertEquals(
                listOf(
                    "upsert($currentUserId, $friendId, ${Status.ACCEPTED})",
                ),
                fakeFriendDao.calls,
            )
        }

    @Test
    fun acceptFriendRequest_apiFailure_rollsBackToRequestedAndRethrows() =
        runTest {
            val fakeFriendDao = FakeFriendDao()
            val repository = repository(friendDao = fakeFriendDao, apiSucceeds = false)

            assertFailsWith<ApiFailureException> {
                repository.acceptFriendRequest(currentUserId, friendId)
            }

            assertEquals(
                listOf(
                    "upsert($currentUserId, $friendId, ${Status.ACCEPTED})",
                    "upsert($currentUserId, $friendId, ${Status.REQUESTED})",
                ),
                fakeFriendDao.calls,
            )
        }

    private class ApiFailureException : Exception("simulated api failure")

    private fun repository(
        friendDao: FriendDao,
        apiSucceeds: Boolean,
    ): FriendRepository {

        val engine = MockEngine { request ->
            if (apiSucceeds) {
                respondOk()
            } else {
                throw ApiFailureException()
            }
        }

        val api = SzigetApiService(
            client = HttpClient(engine),
            baseUrl = "https://unused.test",
        )

        return FriendRepository(
            api = api,
            friendDao = friendDao,
            userDao = FakeUserDao(),
            settings = MapSettings(),
        )
    }

    private class FakeFriendDao : FriendDao {
        val calls = mutableListOf<String>()

        override suspend fun upsertFriendships(friendships: List<UserFriendEntity>) {
            friendships.forEach {
                calls.add("upsert(${it.userId}, ${it.friendId}, ${it.status})")
            }
        }

        override suspend fun deleteFriendship(
            userId: String,
            friendId: String,
        ) {
            calls.add("delete($userId, $friendId)")
        }

        override suspend fun deleteFriendshipsNotIn(friendIds: List<String>) {}

        override suspend fun deleteAllFriendships() {}

        override suspend fun getArtistIdsFriendsFavorited(): List<String> = emptyList()

        override suspend fun getAllArtistFriendFavorites(): List<ArtistFriendFavoritedEntity> = emptyList()

        override suspend fun upsertArtistFriendFavorited(artistsFriendFavorited: List<ArtistFriendFavoritedEntity>) {}

        override suspend fun deleteAllArtistFriendFavorited() {}

        override suspend fun deleteStaleFavoritesForArtist(
            artistId: String,
            activeFriendIds: List<String>,
        ) {}

        override suspend fun deleteStaleArtistsFromArtistFriendFavorites(artistIds: List<String>) {}

        override suspend fun upsertAndPruneArtistFriendFavorited(artistsFriendFavorited: List<ArtistFriendFavoritedEntity>) {}

        override fun observeFriends(): Flow<List<UserEntity>> = flowOf(emptyList())

        override fun observeFriendRequests(): Flow<List<UserEntity>> = flowOf(emptyList())

        override fun observeSentFriendRequests(): Flow<List<UserEntity>> = flowOf(emptyList())

        override fun observeArtistsWithFriendsFavoritedSummary(): Flow<List<ArtistFriendsFavoritedSummary>> = flowOf(emptyList())
    }

    private class FakeUserDao : UserDao {
        override fun observeAll(): Flow<List<UserEntity>> = flowOf(emptyList())

        override fun observeById(id: String): Flow<UserEntity?> = flowOf(null)

        override suspend fun upsertAll(users: List<UserEntity>) {}

        override suspend fun setCurrentUser(currentUser: CurrentUserEntity) {}

        override suspend fun getCurrentUser(): UserEntity? = null

        override fun observeCurrentUser(): Flow<UserEntity?> = flowOf(null)

        override suspend fun clearCurrentUser() {}

        override suspend fun deleteAll() {}
    }
}

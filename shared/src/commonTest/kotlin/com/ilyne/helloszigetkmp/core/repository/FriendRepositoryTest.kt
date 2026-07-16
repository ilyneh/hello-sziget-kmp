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
 * Covers [FriendRepository.sendFriendRequest] and [FriendRepository.acceptFriendRequest]: both do an optimistic local
 * write, call the API, and roll the local write back (then rethrow) if the API call fails.
 */
class FriendRepositoryTest {
    private val currentUserId = "user-1"
    private val friendId = "friend-1"

    @Test
    fun sendFriendRequest_success_writesLocalAndCallsApi_withoutRollback() =
        runTest {
            val friendDao = FakeFriendDao()
            val repository = repository(friendDao = friendDao, apiSucceeds = true)

            repository.sendFriendRequest(currentUserId, friendId)

            assertEquals(
                listOf(UserFriendEntity(currentUserId, friendId, Status.SENT)),
                friendDao.upsertedFriendships,
            )
            assertTrue(friendDao.deletedFriendships.isEmpty())
        }

    @Test
    fun sendFriendRequest_apiFailure_rollsBackLocalWriteAndRethrows() =
        runTest {
            val friendDao = FakeFriendDao()
            val repository = repository(friendDao = friendDao, apiSucceeds = false)

            assertFailsWith<ApiFailureException> {
                repository.sendFriendRequest(currentUserId, friendId)
            }

            // Optimistic write happened first...
            assertEquals(
                listOf(UserFriendEntity(currentUserId, friendId, Status.SENT)),
                friendDao.upsertedFriendships,
            )
            // ...then rolled back via a delete on failure.
            assertEquals(
                listOf(currentUserId to friendId),
                friendDao.deletedFriendships,
            )
        }

    @Test
    fun acceptFriendRequest_success_writesLocalAndCallsApi_withoutRollback() =
        runTest {
            val friendDao = FakeFriendDao()
            val repository = repository(friendDao = friendDao, apiSucceeds = true)

            repository.acceptFriendRequest(currentUserId, friendId)

            assertEquals(
                listOf(UserFriendEntity(currentUserId, friendId, Status.ACCEPTED)),
                friendDao.upsertedFriendships,
            )
        }

    @Test
    fun acceptFriendRequest_apiFailure_rollsBackToRequestedAndRethrows() =
        runTest {
            val friendDao = FakeFriendDao()
            val repository = repository(friendDao = friendDao, apiSucceeds = false)

            assertFailsWith<ApiFailureException> {
                repository.acceptFriendRequest(currentUserId, friendId)
            }

            // Optimistic write to ACCEPTED, then rollback upsert back to REQUESTED on failure.
            assertEquals(
                listOf(
                    UserFriendEntity(currentUserId, friendId, Status.ACCEPTED),
                    UserFriendEntity(currentUserId, friendId, Status.REQUESTED),
                ),
                friendDao.upsertedFriendships,
            )
        }

    private class ApiFailureException : Exception("simulated api failure")

    private fun repository(
        friendDao: FriendDao,
        apiSucceeds: Boolean,
    ): FriendRepository {
        val userDao = object : UserDao {
            override fun observeAll(): Flow<List<UserEntity>> = flowOf(emptyList())

            override fun observeById(id: String): Flow<UserEntity?> = flowOf(null)

            override suspend fun upsertAll(users: List<UserEntity>) {}

            override suspend fun setCurrentUser(currentUser: CurrentUserEntity) {}

            override suspend fun getCurrentUser(): UserEntity? = null

            override fun observeCurrentUser(): Flow<UserEntity?> = flowOf(null)

            override suspend fun clearCurrentUser() {}

            override suspend fun deleteAll() {}
        }

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
            userDao = userDao,
            settings = MapSettings(),
        )
    }

    private class FakeFriendDao : FriendDao {
        val upsertedFriendships = mutableListOf<UserFriendEntity>()
        val deletedFriendships = mutableListOf<Pair<String, String>>()

        override suspend fun upsertFriendships(friendships: List<UserFriendEntity>) {
            upsertedFriendships += friendships
        }

        override suspend fun deleteFriendship(
            userId: String,
            friendId: String,
        ) {
            deletedFriendships += userId to friendId
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

        override fun observeFriends(): Flow<List<UserEntity>> = flowOf(emptyList())

        override fun observeFriendRequests(): Flow<List<UserEntity>> = flowOf(emptyList())

        override fun observeSentFriendRequests(): Flow<List<UserEntity>> = flowOf(emptyList())

        override fun observeArtistsWithFriendsFavoritedSummary(): Flow<List<ArtistFriendsFavoritedSummary>> = flowOf(emptyList())
    }
}

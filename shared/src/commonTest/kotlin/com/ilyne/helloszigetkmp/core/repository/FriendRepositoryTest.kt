package com.ilyne.helloszigetkmp.core.repository

import com.ilyne.helloszigetkmp.core.api.SzigetApiService
import com.ilyne.helloszigetkmp.core.api.dto.ArtistFriendsFavoritedDto
import com.ilyne.helloszigetkmp.core.api.dto.UserDto
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
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.engine.mock.respondOk
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
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
 *
 * Also covers the `artist_friend_favorites` pruning done by [FriendRepository.refresh]: stale
 * (artistId, friendId) pairs — left behind when a friend un-favorites an artist, or a friendship
 * ends — must be deleted, but only once we have fresh, complete data to safely scope that delete
 * against (mirroring the existing `deleteFriendshipsNotIn` safety gate for `users_friends`).
 */
class FriendRepositoryTest {
    private val currentUserId = "me"
    private val friendId = "friend-1"
    private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")

    @Test
    fun refresh_prunesFavoritedArtist_whenFriendUnfavoritesIt() =
        runTest {
            val friendDao = FakeFriendDao()
            val endpoints = FakeEndpoints(
                friends = listOf(userDto("f1")),
                artistsFriendsFavorited = listOf(
                    favoritedDto("a1", listOf("f1")),
                    favoritedDto("a2", listOf("f1")),
                ),
            )
            val repository = repository(friendDao, endpoints)

            repository.refresh(force = true)
            assertEquals(
                setOf("a1" to "f1", "a2" to "f1"),
                friendDao.artistFriendFavorites.map { it.artistId to it.friendId }.toSet(),
            )

            // f1 unfavorites a2 — the backend now only returns a1 for f1.
            endpoints.artistsFriendsFavorited = listOf(favoritedDto("a1", listOf("f1")))
            repository.refresh(force = true)

            assertEquals(
                setOf("a1" to "f1"),
                friendDao.artistFriendFavorites.map { it.artistId to it.friendId }.toSet(),
            )
        }

    @Test
    fun refresh_prunesFavoritedArtist_whenFriendshipEnds() =
        runTest {
            val friendDao = FakeFriendDao()
            val endpoints = FakeEndpoints(
                friends = listOf(userDto("f1"), userDto("f2")),
                artistsFriendsFavorited = listOf(favoritedDto("a1", listOf("f1", "f2"))),
            )
            val repository = repository(friendDao, endpoints)

            repository.refresh(force = true)
            assertEquals(
                setOf("a1" to "f1", "a1" to "f2"),
                friendDao.artistFriendFavorites.map { it.artistId to it.friendId }.toSet(),
            )

            // f2 is no longer a friend — the friend list and the favorites feed both drop them.
            endpoints.friends = listOf(userDto("f1"))
            endpoints.artistsFriendsFavorited = listOf(favoritedDto("a1", listOf("f1")))
            repository.refresh(force = true)

            assertEquals(
                setOf("a1" to "f1"),
                friendDao.artistFriendFavorites.map { it.artistId to it.friendId }.toSet(),
            )
        }

    @Test
    fun refresh_doesNotPruneFavorites_whenFriendListFetchFails() =
        runTest {
            val friendDao = FakeFriendDao()
            val endpoints = FakeEndpoints(
                friends = listOf(userDto("f1")),
                artistsFriendsFavorited = listOf(
                    favoritedDto("a1", listOf("f1")),
                    favoritedDto("a2", listOf("f1")),
                ),
            )
            val repository = repository(friendDao, endpoints)

            repository.refresh(force = true)
            assertEquals(2, friendDao.artistFriendFavorites.size)

            // The friend-list endpoint starts failing, but the favorites feed already looks like f1
            // un-favorited a2. Without a fresh/complete friend list to scope a prune against, refresh()
            // must not delete a2's cached row — a transient friend-list failure must not look like an
            // un-favorite.
            endpoints.friendsShouldFail = true
            endpoints.artistsFriendsFavorited = listOf(favoritedDto("a1", listOf("f1")))
            assertFailsWith<Exception> { repository.refresh(force = true) }

            assertEquals(
                setOf("a1" to "f1", "a2" to "f1"),
                friendDao.artistFriendFavorites.map { it.artistId to it.friendId }.toSet(),
            )
        }

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
    fun sendFriendRequest_localCacheWriteFails_stillCallsApiAndSucceeds() =
        runTest {
            // friendId isn't cached in the local users table yet (e.g. it came from a search
            // result before UserRepository's own sync caught up), so the optimistic local
            // write hits a simulated FK violation. That must not block the actual request from
            // being sent to the server.
            val fakeFriendDao = FakeFriendDao().apply { throwOnUpsertFor(friendId) }
            var apiRequestCount = 0
            val repository = repository(fakeFriendDao, apiSucceeds = true, onApiRequest = { apiRequestCount++ })

            repository.sendFriendRequest(currentUserId, friendId)

            assertEquals(1, apiRequestCount)
            assertTrue(fakeFriendDao.calls.isEmpty())
        }

    @Test
    fun sendFriendRequest_localCacheWriteFails_apiAlsoFails_rethrowsApiFailureWithoutRevertAttempt() =
        runTest {
            val fakeFriendDao = FakeFriendDao().apply { throwOnUpsertFor(friendId) }
            val repository = repository(fakeFriendDao, apiSucceeds = false)

            // The API failure (not the local FK violation) is what the caller should see.
            assertFailsWith<ApiFailureException> {
                repository.sendFriendRequest(currentUserId, friendId)
            }

            // Nothing was ever cached locally, so there's nothing to revert.
            assertTrue(fakeFriendDao.calls.isEmpty())
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

    @Test
    fun acceptFriendRequest_localCacheWriteFails_stillCallsApiAndSucceeds() =
        runTest {
            val fakeFriendDao = FakeFriendDao().apply { throwOnUpsertFor(friendId) }
            var apiRequestCount = 0
            val repository = repository(fakeFriendDao, apiSucceeds = true, onApiRequest = { apiRequestCount++ })

            repository.acceptFriendRequest(currentUserId, friendId)

            assertEquals(1, apiRequestCount)
            assertTrue(fakeFriendDao.calls.isEmpty())
        }

    @Test
    fun acceptFriendRequest_localCacheWriteFails_apiAlsoFails_rethrowsApiFailureWithoutRevertAttempt() =
        runTest {
            val fakeFriendDao = FakeFriendDao().apply { throwOnUpsertFor(friendId) }
            val repository = repository(fakeFriendDao, apiSucceeds = false)

            assertFailsWith<ApiFailureException> {
                repository.acceptFriendRequest(currentUserId, friendId)
            }

            assertTrue(fakeFriendDao.calls.isEmpty())
        }

    private class ApiFailureException : Exception("simulated api failure")

    /** Simulates the Room FK-constraint exception thrown when [friendId] isn't cached in the local users table yet. */
    private class FakeForeignKeyViolation(
        friendId: String,
    ) : Exception("simulated FK violation for $friendId")

    private fun userDto(id: String) = UserDto(id = id, name = id, imageUrl = null)

    private fun favoritedDto(
        artistId: String,
        friendIds: List<String>,
    ) = ArtistFriendsFavoritedDto(id = artistId, friendsFavorited = friendIds)

    private fun repository(
        friendDao: FriendDao,
        endpoints: FakeEndpoints,
    ): FriendRepository {
        val json = Json { ignoreUnknownKeys = true }
        val engine = MockEngine { request ->
            val path = request.url.encodedPath
            when {
                path.endsWith("/friends/requests") -> {
                    respond(json.encodeToString(endpoints.friendRequests), headers = jsonHeaders)
                }

                path.endsWith("/friends/sent") -> {
                    respond(json.encodeToString(endpoints.sentFriendRequests), headers = jsonHeaders)
                }

                path.endsWith("/friends/favorites") -> {
                    respond(json.encodeToString(endpoints.artistsFriendsFavorited), headers = jsonHeaders)
                }

                path.endsWith("/friends") && endpoints.friendsShouldFail -> {
                    respondError(HttpStatusCode.InternalServerError)
                }

                path.endsWith("/friends") -> {
                    respond(json.encodeToString(endpoints.friends), headers = jsonHeaders)
                }

                else -> {
                    respondError(HttpStatusCode.NotFound)
                }
            }
        }
        val client = HttpClient(engine) {
            expectSuccess = true
            install(ContentNegotiation) { json(json) }
        }
        val api = SzigetApiService(client = client, baseUrl = "https://unused.test")
        return FriendRepository(
            api = api,
            friendDao = friendDao,
            userDao = FakeUserDao(currentUserId),
            settings = MapSettings(),
        )
    }

    private fun repository(
        friendDao: FriendDao,
        apiSucceeds: Boolean,
        onApiRequest: () -> Unit = {},
    ): FriendRepository {
        val engine = MockEngine {
            onApiRequest()
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
            userDao = FakeUserDao(currentUserId),
            settings = MapSettings(),
        )
    }

    private class FakeEndpoints(
        var friends: List<UserDto>,
        var friendRequests: List<UserDto> = emptyList(),
        var sentFriendRequests: List<UserDto> = emptyList(),
        var artistsFriendsFavorited: List<ArtistFriendsFavoritedDto>,
        var friendsShouldFail: Boolean = false,
    )

    private class FakeFriendDao : FriendDao {
        val calls = mutableListOf<String>()
        val friendships = mutableListOf<UserFriendEntity>()
        val artistFriendFavorites = mutableListOf<ArtistFriendFavoritedEntity>()
        private val throwOnUpsertFor = mutableSetOf<String>()

        /** Simulates a Room foreign-key constraint violation for [friendId] not yet being cached locally. */
        fun throwOnUpsertFor(friendId: String) {
            throwOnUpsertFor.add(friendId)
        }

        override suspend fun upsertFriendships(friendships: List<UserFriendEntity>) {
            friendships.forEach { new ->
                if (new.friendId in throwOnUpsertFor) {
                    throw FakeForeignKeyViolation(new.friendId)
                }
                calls.add("upsert(${new.userId}, ${new.friendId}, ${new.status})")
                this.friendships.removeAll { it.userId == new.userId && it.friendId == new.friendId }
                this.friendships.add(new)
            }
        }

        override suspend fun deleteFriendship(
            userId: String,
            friendId: String,
        ) {
            calls.add("delete($userId, $friendId)")
            friendships.removeAll { it.userId == userId && it.friendId == friendId }
        }

        override suspend fun deleteFriendshipsNotIn(
            userId: String,
            friendIds: List<String>,
        ) {
            friendships.removeAll { it.userId == userId && it.friendId !in friendIds }
        }

        override suspend fun deleteAllFriendships() {
            friendships.clear()
        }

        override suspend fun getArtistIdsFriendsFavorited(): List<String> = artistFriendFavorites.map { it.artistId }.distinct()

        override suspend fun getAllArtistFriendFavorites(): List<ArtistFriendFavoritedEntity> = artistFriendFavorites.toList()

        override suspend fun upsertArtistFriendFavorited(artistsFriendFavorited: List<ArtistFriendFavoritedEntity>) {
            artistsFriendFavorited.forEach { new ->
                this.artistFriendFavorites.removeAll { it.artistId == new.artistId && it.friendId == new.friendId }
                this.artistFriendFavorites.add(new)
            }
        }

        override suspend fun deleteArtistFriendFavoritesForFriends(friendIds: List<String>) {
            artistFriendFavorites.removeAll { it.friendId in friendIds }
        }

        override suspend fun deleteArtistFriendFavoritesNotIn(friendIds: List<String>) {
            artistFriendFavorites.removeAll { it.friendId !in friendIds }
        }

        override suspend fun deleteAllArtistFriendFavorited() {
            artistFriendFavorites.clear()
        }

        override suspend fun deleteStaleFavoritesForArtist(
            artistId: String,
            activeFriendIds: List<String>,
        ) {
            artistFriendFavorites.removeAll { it.artistId == artistId && it.friendId !in activeFriendIds }
        }

        override suspend fun deleteStaleArtistsFromArtistFriendFavorites(artistIds: List<String>) {
            artistFriendFavorites.removeAll { it.artistId in artistIds }
        }

        override fun observeFriends(): Flow<List<UserEntity>> = throw NotImplementedError("unused in this test")

        override fun observeFriendRequests(): Flow<List<UserEntity>> = throw NotImplementedError("unused in this test")

        override fun observeSentFriendRequests(): Flow<List<UserEntity>> = throw NotImplementedError("unused in this test")

        override fun observeArtistsWithFriendsFavoritedSummary(): Flow<List<ArtistFriendsFavoritedSummary>> =
            throw NotImplementedError("unused in this test")
    }

    private class FakeUserDao(
        private val currentUserId: String,
    ) : UserDao {
        override fun observeAll(): Flow<List<UserEntity>> = flowOf(emptyList())

        override fun observeById(id: String): Flow<UserEntity?> = flowOf(null)

        override suspend fun upsertAll(users: List<UserEntity>) {
            // Not asserted on in these tests — refresh() only needs this to not throw.
        }

        override suspend fun setCurrentUser(currentUser: CurrentUserEntity) {
            // Unused in this test.
        }

        override suspend fun getCurrentUser(): UserEntity = UserEntity(id = currentUserId, name = currentUserId, imageUrl = null)

        override fun observeCurrentUser(): Flow<UserEntity?> = flowOf(null)

        override suspend fun clearCurrentUser() {
            // Unused in this test.
        }

        override suspend fun deleteAll() {
            // Unused in this test.
        }
    }
}

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
import com.ilyne.helloszigetkmp.core.db.model.ArtistFriendsFavoritedSummary
import com.russhwolf.settings.Settings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * Covers the `artist_friend_favorites` pruning added to [FriendRepository.refresh]: stale
 * (artistId, friendId) pairs — left behind when a friend un-favorites an artist, or a friendship
 * ends — must be deleted, but only once we have fresh, complete data to safely scope that delete
 * against (mirroring the existing `deleteFriendshipsNotIn` safety gate for `users_friends`).
 */
class FriendRepositoryTest {
    private val currentUserId = "me"
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
            settings = InMemorySettings(),
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
        val friendships = mutableListOf<UserFriendEntity>()
        val artistFriendFavorites = mutableListOf<ArtistFriendFavoritedEntity>()

        override suspend fun upsertFriendships(friendships: List<UserFriendEntity>) {
            friendships.forEach { new ->
                this.friendships.removeAll { it.userId == new.userId && it.friendId == new.friendId }
                this.friendships.add(new)
            }
        }

        override suspend fun deleteFriendship(
            userId: String,
            friendId: String,
        ) {
            friendships.removeAll { it.userId == userId && it.friendId == friendId }
        }

        override suspend fun deleteFriendshipsNotIn(
            userId: String,
            friendIds: List<String>,
        ) {
            friendships.removeAll { it.userId == userId && it.friendId !in friendIds }
        }

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

        override fun observeFriends(): Flow<List<UserEntity>> = throw NotImplementedError("unused in this test")

        override fun observeFriendRequests(): Flow<List<UserEntity>> = throw NotImplementedError("unused in this test")

        override fun observeSentFriendRequests(): Flow<List<UserEntity>> = throw NotImplementedError("unused in this test")

        override fun observeArtistsWithFriendsFavoritedSummary(): Flow<List<ArtistFriendsFavoritedSummary>> =
            throw NotImplementedError("unused in this test")
    }

    private class FakeUserDao(
        private val currentUserId: String,
    ) : UserDao {
        override fun observeAll(): Flow<List<UserEntity>> = throw NotImplementedError("unused in this test")

        override fun observeById(id: String): Flow<UserEntity?> = throw NotImplementedError("unused in this test")

        override suspend fun upsertAll(users: List<UserEntity>) {
            // Not asserted on in these tests — refresh() only needs this to not throw.
        }

        override suspend fun setCurrentUser(currentUser: CurrentUserEntity) {
            // Unused in this test.
        }

        override suspend fun getCurrentUser(): UserEntity = UserEntity(id = currentUserId, name = currentUserId, imageUrl = null)

        override fun observeCurrentUser(): Flow<UserEntity?> = throw NotImplementedError("unused in this test")
    }

    private class InMemorySettings : Settings {
        private val map = mutableMapOf<String, Any>()

        override val keys: Set<String> get() = map.keys
        override val size: Int get() = map.size

        override fun clear() = map.clear()

        override fun remove(key: String) {
            map.remove(key)
        }

        override fun hasKey(key: String): Boolean = map.containsKey(key)

        override fun putInt(
            key: String,
            value: Int,
        ) {
            map[key] = value
        }

        override fun getInt(
            key: String,
            defaultValue: Int,
        ): Int = map[key] as? Int ?: defaultValue

        override fun getIntOrNull(key: String): Int? = map[key] as? Int

        override fun putLong(
            key: String,
            value: Long,
        ) {
            map[key] = value
        }

        override fun getLong(
            key: String,
            defaultValue: Long,
        ): Long = map[key] as? Long ?: defaultValue

        override fun getLongOrNull(key: String): Long? = map[key] as? Long

        override fun putString(
            key: String,
            value: String,
        ) {
            map[key] = value
        }

        override fun getString(
            key: String,
            defaultValue: String,
        ): String = map[key] as? String ?: defaultValue

        override fun getStringOrNull(key: String): String? = map[key] as? String

        override fun putFloat(
            key: String,
            value: Float,
        ) {
            map[key] = value
        }

        override fun getFloat(
            key: String,
            defaultValue: Float,
        ): Float = map[key] as? Float ?: defaultValue

        override fun getFloatOrNull(key: String): Float? = map[key] as? Float

        override fun putDouble(
            key: String,
            value: Double,
        ) {
            map[key] = value
        }

        override fun getDouble(
            key: String,
            defaultValue: Double,
        ): Double = map[key] as? Double ?: defaultValue

        override fun getDoubleOrNull(key: String): Double? = map[key] as? Double

        override fun putBoolean(
            key: String,
            value: Boolean,
        ) {
            map[key] = value
        }

        override fun getBoolean(
            key: String,
            defaultValue: Boolean,
        ): Boolean = map[key] as? Boolean ?: defaultValue

        override fun getBooleanOrNull(key: String): Boolean? = map[key] as? Boolean
    }
}

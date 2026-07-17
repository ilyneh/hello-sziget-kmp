package com.ilyne.helloszigetkmp.core.repository

import com.ilyne.helloszigetkmp.core.api.SzigetApiService
import com.ilyne.helloszigetkmp.core.api.dto.ArtistFriendsFavoritedDto
import com.ilyne.helloszigetkmp.core.api.dto.UserDto
import com.ilyne.helloszigetkmp.core.db.dao.FriendDao
import com.ilyne.helloszigetkmp.core.db.dao.UserDao
import com.ilyne.helloszigetkmp.core.db.entity.ArtistFriendFavoritedEntity
import com.ilyne.helloszigetkmp.core.db.entity.UserFriendEntity
import com.ilyne.helloszigetkmp.core.db.entity.UserFriendEntity.Status
import com.ilyne.helloszigetkmp.core.db.model.ArtistFriendsFavoritedSummary
import com.ilyne.helloszigetkmp.domain.model.ArtistFriendsFavorited
import com.ilyne.helloszigetkmp.domain.model.User
import com.ilyne.helloszigetkmp.util.Logger
import com.russhwolf.settings.Settings
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.supervisorScope

class FriendRepository(
    private val api: SzigetApiService,
    private val friendDao: FriendDao,
    private val userDao: UserDao,
    settings: Settings,
) {
    private val softRefreshGate = SoftRefreshGate(settings, key = "FriendRepository")

    suspend fun sendFriendRequest(
        currentUserId: String,
        friendId: String,
    ) {
        val userFriendEntity = UserFriendEntity(
            userId = currentUserId,
            friendId = friendId,
            status = Status.SENT,
        )
        val cachedLocally = upsertFriendshipTolerantly(userFriendEntity)
        try {
            api.sendFriendRequest(friendId)
        } catch (e: Exception) {
            if (cachedLocally) friendDao.deleteFriendship(currentUserId, friendId)
            throw e
        }
    }

    suspend fun acceptFriendRequest(
        currentUserId: String,
        friendId: String,
    ) {
        val userFriendEntity = UserFriendEntity(
            userId = currentUserId,
            friendId = friendId,
            status = Status.ACCEPTED,
        )
        val cachedLocally = upsertFriendshipTolerantly(userFriendEntity)
        try {
            api.acceptFriendRequest(friendId)
        } catch (e: Exception) {
            if (cachedLocally) friendDao.upsertFriendship(userFriendEntity.copy(status = Status.REQUESTED))
            throw e
        }
    }

    suspend fun declineFriendRequest(
        currentUserId: String,
        friendId: String,
    ) {
        friendDao.deleteFriendship(currentUserId, friendId)
        try {
            api.removeFriend(friendId)
        } catch (e: Exception) {
            upsertFriendshipTolerantly(
                UserFriendEntity(
                    userId = currentUserId,
                    friendId = friendId,
                    status = Status.REQUESTED,
                ),
            )
            throw e
        }
    }

    suspend fun removeFriend(
        currentUserId: String,
        friendId: String,
    ) {
        friendDao.deleteFriendship(currentUserId, friendId)
        try {
            api.removeFriend(friendId)
        } catch (e: Exception) {
            upsertFriendshipTolerantly(
                UserFriendEntity(
                    userId = currentUserId,
                    friendId = friendId,
                    status = Status.ACCEPTED,
                ),
            )
            throw e
        }
    }

    /**
     * Best-effort local cache write: [UserFriendEntity.friendId] (and `.userId`) carry FKs to the
     * local users table, which may not have that user cached yet - e.g. this friendId came from a
     * search result or push notification that arrived before [UserRepository]'s own sync caught
     * up (see the equivalent guard in [persistArtistsFriendsFavorited]). Don't let that FK
     * violation block the actual server-side action (the caller still attempts the real API call
     * regardless of this method's result) or mask a genuine API failure thrown from the caller's
     * catch block; the next successful [refresh] upserts referenced users first and will pick this
     * relationship back up. Returns whether the write actually landed, so callers know whether a
     * subsequent revert-on-failure write has anything to revert.
     */
    private suspend fun upsertFriendshipTolerantly(entity: UserFriendEntity): Boolean =
        runCatching { friendDao.upsertFriendship(entity) }
            .onFailure { e ->
                Logger.e("FriendRepository", "upsertFriendshipTolerantly(): failed to cache ${entity.friendId} locally", e)
            }.isSuccess

    fun observeFriends(): Flow<List<User>> = friendDao.observeFriends().map { entities -> entities.map { it.toDomain() } }

    fun observeFriendRequests(): Flow<List<User>> = friendDao.observeFriendRequests().map { entities -> entities.map { it.toDomain() } }

    fun observeSentFriendRequests(): Flow<List<User>> =
        friendDao.observeSentFriendRequests().map { entities -> entities.map { it.toDomain() } }

    fun observeArtistsFriendsFavorited(): Flow<List<ArtistFriendsFavorited>> =
        friendDao.observeArtistsWithFriendsFavoritedSummary().map { entities -> entities.map { it.toDomain() } }

    suspend fun refresh(force: Boolean = false) {
        val currentUserId = userDao.getCurrentUser()?.id ?: run {
            Logger.e("FriendRepository", "refresh(): current user not found in dao")
            error("Cannot refresh friends: current user not found")
        }

        softRefreshGate.refreshIfStale(force) {
            // Run the 4 calls as independent siblings: one failing must not cancel/discard
            // the others' successful results. supervisorScope (instead of coroutineScope)
            // keeps a child's failure from cancelling its siblings, and each async body is
            // wrapped in runCatching so a failed await() doesn't itself get treated as an
            // uncaught exception that would cancel the scope.
            supervisorScope {
                val friendsDeferred = async { runCatching { api.getFriends() } }
                val friendRequestsDeferred = async { runCatching { api.getFriendRequests() } }
                val sentFriendRequestsDeferred = async { runCatching { api.getSentFriendRequests() } }
                val artistsFriendsFavoritedDeferred = async { runCatching { api.getArtistsFriendsFavorited() } }

                val friendsResult = friendsDeferred.await()
                val friendRequestsResult = friendRequestsDeferred.await()
                val sentFriendRequestsResult = sentFriendRequestsDeferred.await()
                val artistsFriendsFavoritedResult = artistsFriendsFavoritedDeferred.await()

                val friends = friendsResult.getOrNull().orEmpty()
                val friendRequests = friendRequestsResult.getOrNull().orEmpty()
                val sentFriendRequests = sentFriendRequestsResult.getOrNull().orEmpty()
                val artistsFriendsFavorited = artistsFriendsFavoritedResult.getOrNull().orEmpty()

                // Friends/requesters aren't necessarily in the local users table yet
                // (e.g. UserRepository's own sync never ran) — upsert them first so the
                // foreign keys on UserFriendEntity.userId/friendId are satisfied.
                userDao.upsertAll((friends + friendRequests + sentFriendRequests).map { it.toEntity() })

                // Only persist the slices that actually succeeded — a failed call's stale/empty
                // result must not overwrite already-cached data for that slice.
                if (friendsResult.isSuccess) {
                    friendDao.upsertFriendships(friends.toUserFriends(currentUserId, Status.ACCEPTED))
                }

                if (friendRequestsResult.isSuccess) {
                    friendDao.upsertFriendships(friendRequests.toUserFriends(currentUserId, Status.REQUESTED))
                }

                if (sentFriendRequestsResult.isSuccess) {
                    friendDao.upsertFriendships(sentFriendRequests.toUserFriends(currentUserId, Status.SENT))
                }

                if (artistsFriendsFavoritedResult.isSuccess) {
                    persistArtistsFriendsFavorited(
                        artistsFriendsFavorited = artistsFriendsFavorited,
                        friends = friends,
                        friendsFetchSucceeded = friendsResult.isSuccess,
                    )
                }

                // clean out previous friend relationships that no longer exist — only safe once
                // all three friend-list calls succeeded, otherwise a failed call's empty result
                // would wipe out relationships whose source list just didn't load this time.
                if (friendsResult.isSuccess && friendRequestsResult.isSuccess && sentFriendRequestsResult.isSuccess) {
                    pruneRemovedFriendships(currentUserId, friends, friendRequests, sentFriendRequests)
                }

                throwIfAnyFailed(
                    friendsResult,
                    friendRequestsResult,
                    sentFriendRequestsResult,
                    artistsFriendsFavoritedResult,
                )
            }
        }
    }

    /**
     * Logs every failed call from this round, then — if any failed — rethrows the first one so
     * the caller can stop spinners / show an error, even though whichever calls succeeded were
     * still persisted to the DB above.
     */
    private fun throwIfAnyFailed(
        friendsResult: Result<List<UserDto>>,
        friendRequestsResult: Result<List<UserDto>>,
        sentFriendRequestsResult: Result<List<UserDto>>,
        artistsFriendsFavoritedResult: Result<List<ArtistFriendsFavoritedDto>>,
    ) {
        val failures = listOfNotNull(
            friendsResult.exceptionOrNull()?.let { "getFriends" to it },
            friendRequestsResult.exceptionOrNull()?.let { "getFriendRequests" to it },
            sentFriendRequestsResult.exceptionOrNull()?.let { "getSentFriendRequests" to it },
            artistsFriendsFavoritedResult.exceptionOrNull()?.let { "getArtistsFriendsFavorited" to it },
        )
        failures.forEach { (name, throwable) ->
            Logger.e("FriendRepository", "refresh(): $name failed", throwable)
        }
        if (failures.isNotEmpty()) {
            throw failures.first().second
        }
    }

    /**
     * Persists this round's favorited-artist pairs, pruning stale ones when it's safe to do so.
     * Only called once [artistsFriendsFavorited] itself loaded successfully; [friendsFetchSucceeded]
     * gates whether [friends] is a complete-enough set to safely scope a prune against — see
     * [FriendDao.replaceArtistFriendFavoritesForFriends] for why a partial/stale friend list must
     * not be used to prune.
     */
    private suspend fun persistArtistsFriendsFavorited(
        artistsFriendsFavorited: List<ArtistFriendsFavoritedDto>,
        friends: List<UserDto>,
        friendsFetchSucceeded: Boolean,
    ) {
        val entities = artistsFriendsFavorited.toArtistsFriendsFavoritedEntity()
        // These rows carry a FK to the local artists table, which may not be populated yet
        // (e.g. artists haven't synced on this device). Don't let that FK violation abort the
        // rest of refresh() or crash the caller — it's just a favorited-artist row we couldn't
        // cache this round; the next successful refresh (after artists sync) will pick it up.
        runCatching {
            if (friendsFetchSucceeded) {
                // We have both a fresh favorited-artists fetch and a fresh, complete friends list,
                // so it's safe to prune: any (artist, friend) pair that was cached before but isn't
                // in this fetch means that friend actually un-favorited that artist.
                friendDao.replaceArtistFriendFavoritesForFriends(friendIds = friends.map { it.id }, artistsFriendFavorited = entities)
            } else {
                // The friend list itself failed to load this round, so we don't have a reliable
                // "current friends" set to scope a prune against — upsert only, to avoid wiping
                // cached favorites for a friend we can't currently confirm is still on the list.
                friendDao.upsertArtistFriendFavorited(entities)
            }
        }.onFailure { e ->
            Logger.e("FriendRepository", "persistArtistsFriendsFavorited(): failed to persist favorited artists", e)
        }
    }

    /**
     * Deletes friendships (and their cached favorited-artist rows) that no longer appear in any
     * of the three freshly-fetched friend lists. Only called once all three have succeeded — see
     * the call site in [refresh].
     */
    private suspend fun pruneRemovedFriendships(
        currentUserId: String,
        friends: List<UserDto>,
        friendRequests: List<UserDto>,
        sentFriendRequests: List<UserDto>,
    ) {
        val existingFriendships = (friends + friendRequests + sentFriendRequests)
            .map { it.id }
            .distinct()
        friendDao.deleteFriendshipsNotIn(userId = currentUserId, friendIds = existingFriendships)
        // A friendship ending (unfriend/decline) doesn't cascade-delete that former friend's
        // cached favorited-artist rows (the FK cascade is keyed off deleting the UserEntity
        // itself, which we never do here) — prune them explicitly so a removed friend doesn't
        // keep showing up as having favorited artists forever.
        friendDao.deleteArtistFriendFavoritesNotIn(friendIds = existingFriendships)
    }

    private fun List<UserDto>.toUserFriends(
        currentUserId: String,
        status: Status,
    ): List<UserFriendEntity> =
        map { friend ->
            UserFriendEntity(
                userId = currentUserId,
                friendId = friend.id,
                status = status,
            )
        }

    private fun List<ArtistFriendsFavoritedDto>.toArtistsFriendsFavoritedEntity(): List<ArtistFriendFavoritedEntity> =
        flatMap { artist ->
            artist.friendsFavorited.map { friendId ->
                ArtistFriendFavoritedEntity(
                    artistId = artist.id,
                    friendId = friendId,
                )
            }
        }
}

private fun ArtistFriendsFavoritedSummary.toDomain(): ArtistFriendsFavorited =
    ArtistFriendsFavorited(
        artist = artist.toDomain(),
        friendsFavorited = friends.map { it.toDomain() },
    )

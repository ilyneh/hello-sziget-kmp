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
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map


class FriendRepository(
    private val api: SzigetApiService,
    private val friendDao: FriendDao,
    private val userDao: UserDao,
) {

    suspend fun sendFriendRequest(currentUserId: String, friendId: String) {
        val userFriendEntity = UserFriendEntity(
            userId = currentUserId,
            friendId = friendId,
            status = Status.SENT
        )
        friendDao.upsertFriendship(userFriendEntity)
        try {
            api.sendFriendRequest(friendId)
        } catch (e: Exception) {
            friendDao.deleteFriendship(currentUserId, friendId)
            throw e
        }
    }

    suspend fun acceptFriendRequest(currentUserId: String, friendId: String) {
        val userFriendEntity = UserFriendEntity(
            userId = currentUserId,
            friendId = friendId,
            status = Status.ACCEPTED
        )
        friendDao.upsertFriendship(userFriendEntity)
        try {
            api.acceptFriendRequest(friendId)
        } catch (e: Exception) {
            friendDao.upsertFriendship(userFriendEntity.copy(status = Status.REQUESTED))
            throw e
        }
    }

    suspend fun declineFriendRequest(currentUserId: String, friendId: String) {
        friendDao.deleteFriendship(currentUserId, friendId)
        try {
            api.removeFriend(friendId)
        } catch (e: Exception) {
            friendDao.upsertFriendship(
                UserFriendEntity(
                    userId = currentUserId,
                    friendId = friendId,
                    status = Status.REQUESTED
                )
            )
            throw e
        }
    }

    suspend fun removeFriend(currentUserId: String, friendId: String) {
        friendDao.deleteFriendship(currentUserId, friendId)
        try {
            api.removeFriend(friendId)
        } catch (e: Exception) {
            friendDao.upsertFriendship(
                UserFriendEntity(
                    userId = currentUserId,
                    friendId = friendId,
                    status = Status.ACCEPTED
                )
            )
            throw e
        }
    }

    fun observeFriends(): Flow<List<User>> =
        friendDao.observeFriends().map { entities -> entities.map { it.toDomain() } }

    fun observeFriendRequests(): Flow<List<User>> =
        friendDao.observeFriendRequests().map { entities -> entities.map { it.toDomain() } }

    fun observeSentFriendRequests(): Flow<List<User>> =
        friendDao.observeSentFriendRequests().map { entities -> entities.map { it.toDomain() } }

    fun observeArtistsFriendsFavorited(): Flow<List<ArtistFriendsFavorited>> =
        friendDao.observeArtistsWithFriendsFavoritedSummary().map { entities -> entities.map { it.toDomain() } }

    suspend fun refresh() {
        val currentUserId = userDao.getCurrentUser()?.id ?: run {
            Logger.e("findme", "Current user not found in dao")
            return
        }

        coroutineScope {
            val friendsDeferred = async { api.getFriends() }
            val friendRequestsDeferred = async { api.getFriendRequests() }
            val sentFriendRequestsDeferred = async { api.getSentFriendRequests() }
            val artistsFriendsFavoritedDeferred = async { api.getArtistsFriendsFavorited() }

            val friends = friendsDeferred.await()
            val friendRequests = friendRequestsDeferred.await()
            val sentFriendRequests = sentFriendRequestsDeferred.await()
            val artistsFriendsFavorited = artistsFriendsFavoritedDeferred.await()

            // Friends/requesters aren't necessarily in the local users table yet
            // (e.g. UserRepository's own sync never ran) — upsert them first so the
            // foreign keys on UserFriendEntity.userId/friendId are satisfied.
            userDao.upsertAll((friends + friendRequests + sentFriendRequests).map { it.toEntity() })

            friendDao.upsertFriendships(friends.toUserFriends(currentUserId, Status.ACCEPTED))
            friendDao.upsertFriendships(friendRequests.toUserFriends(currentUserId, Status.REQUESTED))
            friendDao.upsertFriendships(sentFriendRequests.toUserFriends(currentUserId, Status.SENT))
            friendDao.upsertArtistFriendFavorited(artistsFriendsFavorited.toArtistsFriendsFavoritedEntity())

            // clean out previous friend relationships that no longer exist
            val existingFriendships = (friends + friendRequests + sentFriendRequests)
                .map { it.id }
                .distinct()
            friendDao.deleteFriendshipsNotIn(friendIds = existingFriendships)
        }
    }

    private fun List<UserDto>.toUserFriends(currentUserId: String, status: Status): List<UserFriendEntity> {
        return map { friend ->
            UserFriendEntity(
                userId = currentUserId,
                friendId = friend.id,
                status = status
            )
        }
    }

    private fun List<ArtistFriendsFavoritedDto>.toArtistsFriendsFavoritedEntity(): List<ArtistFriendFavoritedEntity> =
        flatMap { artist ->
            artist.friendsFavorited.map { friendId ->
                ArtistFriendFavoritedEntity(
                    artistId = artist.id,
                    friendId = friendId
                )
            }
        }
}

private fun ArtistFriendsFavoritedSummary.toDomain(): ArtistFriendsFavorited =
    ArtistFriendsFavorited(
        artist = artist.toDomain(),
        friendsFavorited = friends.map { it.toDomain() }
    )

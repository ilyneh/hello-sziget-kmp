package com.ilyne.helloszigetkmp.data.repository

import com.ilyne.helloszigetkmp.data.api.SzigetApiService
import com.ilyne.helloszigetkmp.data.api.dto.ArtistFriendsFavoritedDto
import com.ilyne.helloszigetkmp.data.api.dto.UserDto
import com.ilyne.helloszigetkmp.data.db.entity.UserFriendEntity
import com.ilyne.helloszigetkmp.data.db.entity.UserFriendEntity.Status
import com.ilyne.helloszigetkmp.data.db.dao.FriendDao
import com.ilyne.helloszigetkmp.data.db.dao.UserDao
import com.ilyne.helloszigetkmp.data.db.entity.ArtistFriendFavoritedEntity
import com.ilyne.helloszigetkmp.data.db.model.ArtistFriendsFavoritedSummary
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

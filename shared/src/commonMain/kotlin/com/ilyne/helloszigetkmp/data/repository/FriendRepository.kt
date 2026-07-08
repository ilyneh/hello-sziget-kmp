package com.ilyne.helloszigetkmp.data.repository

import com.ilyne.helloszigetkmp.data.api.SzigetApiService
import com.ilyne.helloszigetkmp.data.api.dto.UserDto
import com.ilyne.helloszigetkmp.data.db.entity.UserFriendEntity
import com.ilyne.helloszigetkmp.data.db.entity.UserFriendEntity.Status
import com.ilyne.helloszigetkmp.data.db.dao.FriendDao
import com.ilyne.helloszigetkmp.data.db.dao.UserDao
import com.ilyne.helloszigetkmp.domain.model.User
import com.ilyne.helloszigetkmp.util.Logger
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map


class FriendRepository(
    val api: SzigetApiService,
    val friendDao: FriendDao,
    val userDao: UserDao,
) {
    fun observeFriends(): Flow<List<User>> =
        friendDao.observeFriends().map { entities -> entities.map { it.toDomain() } }

    fun observeFriendRequests(): Flow<List<User>> =
        friendDao.observeFriendRequests().map { entities -> entities.map { it.toDomain() } }

    fun observeSentFriendRequests(): Flow<List<User>> =
        friendDao.observeSentFriendRequests().map { entities -> entities.map { it.toDomain() } }

    suspend fun refresh() {
        val currentUserId = userDao.getCurrentUser()?.id ?: run {
            Logger.e("findme", "Current user not found in dao")
            return
        }

        coroutineScope {
            val friendsDto = async { api.getFriends() }
            val friendRequestsDto = async { api.getFriendRequests() }
            val sentFriendRequestsDto = async { api.getSentFriendRequests() }

            val friends = friendsDto.await()
            val friendRequests = friendRequestsDto.await()
            val sentFriendRequests = sentFriendRequestsDto.await()

            // Friends/requesters aren't necessarily in the local users table yet
            // (e.g. UserRepository's own sync never ran) — upsert them first so the
            // foreign keys on UserFriendEntity.userId/friendId are satisfied.
            userDao.upsertAll((friends + friendRequests + sentFriendRequests).map { it.toEntity() })

            friendDao.upsertFriendships(friends.toUserFriends(currentUserId, Status.ACCEPTED))
            friendDao.upsertFriendships(friendRequests.toUserFriends(currentUserId, Status.REQUESTED))
            friendDao.upsertFriendships(sentFriendRequests.toUserFriends(currentUserId, Status.SENT))
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
}

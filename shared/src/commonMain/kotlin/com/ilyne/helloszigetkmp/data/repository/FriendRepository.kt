package com.ilyne.helloszigetkmp.data.repository

import com.ilyne.helloszigetkmp.data.api.SzigetApiService
import com.ilyne.helloszigetkmp.data.api.dto.UserDto
import com.ilyne.helloszigetkmp.data.db.UserFriendEntity
import com.ilyne.helloszigetkmp.data.db.UserFriendEntity.Status
import com.ilyne.helloszigetkmp.data.db.dao.FriendDao
import com.ilyne.helloszigetkmp.data.db.dao.UserDao
import com.ilyne.helloszigetkmp.domain.model.User
import com.ilyne.helloszigetkmp.util.Logger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map


class FriendRepository(
    val api: SzigetApiService,
    val dao: FriendDao,
    val userDao: UserDao,
) {
    fun observeFriends(): Flow<List<User>> =
        dao.observeFriends().map { entities -> entities.map { it.toDomain() } }

    fun observeFriendRequests(): Flow<List<User>> =
        dao.observeFriendRequests().map { entities -> entities.map { it.toDomain() } }

    fun observeSentFriendRequests(): Flow<List<User>> =
        dao.observeSentFriendRequests().map { entities -> entities.map { it.toDomain() } }

    suspend fun refresh() {
        val currentUserId = userDao.getCurrentUser()?.id ?: run {
            Logger.e("findme", "Current user not found in dao")
            return
        }

        val friendsDto = api.getFriends()
        val friendRequestsDto = api.getFriendRequests()
        val sentFriendRequestsDto = api.getSentFriendRequests()

        dao.upsertFriendships(
            friendsDto.toUserFriends(currentUserId, Status.ACCEPTED)
        )
        dao.upsertFriendships(
            friendRequestsDto.toUserFriends(currentUserId, Status.REQUESTED)
        )
        dao.upsertFriendships(
            sentFriendRequestsDto.toUserFriends(currentUserId, Status.SENT)
        )
    }

    private fun List<UserDto>.toUserFriends(currentUserId: String, status: UserFriendEntity.Status): List<UserFriendEntity> {
        return map { friend ->
            UserFriendEntity(
                userId = currentUserId,
                friendId = friend.id,
                status = status
            )
        }
    }
}

package com.ilyne.helloszigetkmp.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ilyne.helloszigetkmp.data.db.entity.UserEntity
import com.ilyne.helloszigetkmp.data.db.entity.UserFriendEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FriendDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFriendships(friendships: List<UserFriendEntity>)

    @Query(
        """
        SELECT users.* FROM users
        INNER JOIN users_friends ON users_friends.friendId = users.id
        WHERE users_friends.userId = (SELECT userId FROM current_user LIMIT 1)
            AND users_friends.status = 'ACCEPTED'
        ORDER BY users.name ASC
        """
    )
    fun observeFriends(): Flow<List<UserEntity>>

    @Query(
        """
        SELECT users.* FROM users
        INNER JOIN users_friends ON users_friends.friendId = users.id
        WHERE users_friends.userId = (SELECT userId FROM current_user LIMIT 1)
            AND users_friends.status = 'REQUESTED'
        ORDER BY users.name ASC
        """
    )
    fun observeFriendRequests(): Flow<List<UserEntity>>

    @Query(
        """
        SELECT users.* FROM users
        INNER JOIN users_friends ON users_friends.friendId = users.id
        WHERE users_friends.userId = (SELECT userId FROM current_user LIMIT 1)
            AND users_friends.status = 'SENT'
        ORDER BY users.name ASC
        """
    )
    fun observeSentFriendRequests(): Flow<List<UserEntity>>


}

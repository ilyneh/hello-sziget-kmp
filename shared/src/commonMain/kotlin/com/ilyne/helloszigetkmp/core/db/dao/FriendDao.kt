package com.ilyne.helloszigetkmp.core.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.ilyne.helloszigetkmp.core.db.entity.ArtistFriendFavoritedEntity
import com.ilyne.helloszigetkmp.core.db.entity.UserEntity
import com.ilyne.helloszigetkmp.core.db.entity.UserFriendEntity
import com.ilyne.helloszigetkmp.core.db.model.ArtistFriendsFavoritedSummary
import kotlinx.coroutines.flow.Flow

@Dao
interface FriendDao {

    @Upsert
    suspend fun upsertFriendships(friendships: List<UserFriendEntity>)

    suspend fun upsertFriendship(friendship: UserFriendEntity) = upsertFriendships(listOf(friendship))

    @Query("DELETE FROM users_friends WHERE userId = :userId AND friendId = :friendId")
    suspend fun deleteFriendship(userId: String, friendId: String)

    @Query("DELETE FROM users_friends WHERE friendId NOT IN (:friendIds)")
    suspend fun deleteFriendshipsNotIn(friendIds: List<String>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertArtistFriendFavorited(artistsFriendFavorited: List<ArtistFriendFavoritedEntity>)

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

    @Query(
        """
        SELECT DISTINCT artists.* FROM artists
        INNER JOIN artist_friend_favorites ON artist_friend_favorites.artistId = artists.id
        WHERE artist_friend_favorites.friendId != (SELECT userId FROM current_user LIMIT 1)
        """
    )
    fun observeArtistsWithFriendsFavoritedSummary(): Flow<List<ArtistFriendsFavoritedSummary>>
}

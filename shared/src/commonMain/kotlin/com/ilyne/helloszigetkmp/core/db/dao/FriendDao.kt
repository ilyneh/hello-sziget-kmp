package com.ilyne.helloszigetkmp.core.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
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
    suspend fun deleteFriendship(
        userId: String,
        friendId: String,
    )

    @Query("DELETE FROM users_friends WHERE userId = :userId AND friendId NOT IN (:friendIds)")
    suspend fun deleteFriendshipsNotIn(
        userId: String,
        friendIds: List<String>,
    )

    @Query("DELETE FROM users_friends")
    suspend fun deleteAllFriendships()

    @Upsert
    suspend fun upsertArtistFriendFavorited(artistsFriendFavorited: List<ArtistFriendFavoritedEntity>)

    @Query("DELETE FROM artist_friend_favorites WHERE friendId IN (:friendIds)")
    suspend fun deleteArtistFriendFavoritesForFriends(friendIds: List<String>)

    /**
     * Replaces every favorited-artist pair for [friendIds] with [artistsFriendFavorited] in one
     * transaction. Callers must only invoke this once [friendIds] is a complete, freshly-fetched
     * set of friends (not a partial/stale one) — otherwise a friend who's temporarily missing
     * from [friendIds] due to an unrelated fetch failure would have their real favorites wiped.
     */
    @Transaction
    suspend fun replaceArtistFriendFavoritesForFriends(
        friendIds: List<String>,
        artistsFriendFavorited: List<ArtistFriendFavoritedEntity>,
    ) {
        deleteArtistFriendFavoritesForFriends(friendIds)
        upsertArtistFriendFavorited(artistsFriendFavorited)
    }

    @Query("DELETE FROM artist_friend_favorites WHERE friendId NOT IN (:friendIds)")
    suspend fun deleteArtistFriendFavoritesNotIn(friendIds: List<String>)

    @Query("DELETE FROM artist_friend_favorites")
    suspend fun deleteAllArtistFriendFavorited()

    @Query(
        """
        SELECT users.* FROM users
        INNER JOIN users_friends ON users_friends.friendId = users.id
        WHERE users_friends.userId = (SELECT userId FROM current_user LIMIT 1)
            AND users_friends.status = 'ACCEPTED'
        ORDER BY users.name ASC
        """,
    )
    fun observeFriends(): Flow<List<UserEntity>>

    @Query(
        """
        SELECT users.* FROM users
        INNER JOIN users_friends ON users_friends.friendId = users.id
        WHERE users_friends.userId = (SELECT userId FROM current_user LIMIT 1)
            AND users_friends.status = 'REQUESTED'
        ORDER BY users.name ASC
        """,
    )
    fun observeFriendRequests(): Flow<List<UserEntity>>

    @Query(
        """
        SELECT users.* FROM users
        INNER JOIN users_friends ON users_friends.friendId = users.id
        WHERE users_friends.userId = (SELECT userId FROM current_user LIMIT 1)
            AND users_friends.status = 'SENT'
        ORDER BY users.name ASC
        """,
    )
    fun observeSentFriendRequests(): Flow<List<UserEntity>>

    @Query(
        """
        SELECT DISTINCT artists.* FROM artists
        INNER JOIN artist_friend_favorites ON artist_friend_favorites.artistId = artists.id
        WHERE artist_friend_favorites.friendId != (SELECT userId FROM current_user LIMIT 1)
        """,
    )
    fun observeArtistsWithFriendsFavoritedSummary(): Flow<List<ArtistFriendsFavoritedSummary>>
}

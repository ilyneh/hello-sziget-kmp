package com.ilyne.helloszigetkmp.core.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.ilyne.helloszigetkmp.core.db.entity.ArtistEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ArtistDao {
    @Query("SELECT * FROM artists ORDER BY name ASC")
    fun observeAll(): Flow<List<ArtistEntity>>

    @Query("SELECT * FROM artists WHERE id = :id")
    fun observeById(id: String): Flow<ArtistEntity?>

    @Query("SELECT * FROM artists WHERE isFavorited = 1 ORDER BY name ASC")
    fun observeFavorites(): Flow<List<ArtistEntity>>

    @Query("SELECT * FROM artists WHERE name LIKE '%' || :query || '%' COLLATE NOCASE ORDER BY name ASC")
    fun searchByName(query: String): Flow<List<ArtistEntity>>

    // Plain INSERT-OR-REPLACE would delete-then-reinsert conflicting rows on every refresh
    // (SQLite semantics), churning Room's invalidation tracker even when nothing changed.
    // Insert-or-ignore + update instead, so existing rows are updated in place (same fix as
    // UserDao.upsertAll/FriendDao.upsertFriendships, for the same reason).
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnoring(artists: List<ArtistEntity>): List<Long>

    @Update
    suspend fun updateAll(artists: List<ArtistEntity>)

    @Transaction
    suspend fun upsertAll(artists: List<ArtistEntity>) {
        if (artists.isEmpty()) return
        val insertResults = insertIgnoring(artists)
        val existing = artists.filterIndexed { index, _ -> insertResults[index] == -1L }
        if (existing.isNotEmpty()) updateAll(existing)
    }

    @Query("UPDATE artists SET isFavorited = :isFavorited WHERE id = :id")
    suspend fun setFavorited(
        id: String,
        isFavorited: Boolean,
    )
}

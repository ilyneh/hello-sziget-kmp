package com.ilyne.helloszigetkmp.core.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
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

    @Upsert
    suspend fun upsertAll(artists: List<ArtistEntity>)

    @Query("UPDATE artists SET isFavorited = :isFavorited WHERE id = :id")
    suspend fun setFavorited(
        id: String,
        isFavorited: Boolean,
    )

    @Query("DELETE FROM artists")
    suspend fun deleteAll()
}

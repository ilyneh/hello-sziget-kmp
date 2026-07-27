package com.ilyne.helloszigetkmp.core.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.ilyne.helloszigetkmp.core.db.entity.ArtistEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Dao
interface ArtistDao {
    @Query("SELECT * FROM artists ORDER BY name ASC")
    fun observeAll(): Flow<List<ArtistEntity>>

    @Query("SELECT * FROM artists WHERE id = :id")
    fun observeById(id: String): Flow<ArtistEntity?>

    @Query("SELECT * FROM artists WHERE isFavorited = 1 ORDER BY name ASC")
    fun observeFavorites(): Flow<List<ArtistEntity>>

    // SQLite's LIKE/LOWER() are ASCII-only, so they mis-fold Hungarian diacritics (e.g. "Á"
    // doesn't lower-match "á"). Kotlin's String.lowercase() is Unicode-aware, so filtering here
    // (rather than via SQL LIKE ... COLLATE NOCASE) also sidesteps needing to escape SQL LIKE's
    // '%'/'_' wildcards, since Kotlin's contains() treats them as plain characters. The artist
    // table is festival-lineup sized (hundreds of rows), so filtering the full in-memory list on
    // every emission is cheap.
    fun searchByName(query: String): Flow<List<ArtistEntity>> {
        val normalizedQuery = query.lowercase()
        return observeAll().map { artists ->
            artists.filter { it.name.lowercase().contains(normalizedQuery) }
        }
    }

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

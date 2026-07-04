package com.ilyne.helloszigetkmp.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ArtistDao {
    @Query("SELECT * FROM artists ORDER BY name ASC")
    fun observeAll(): Flow<List<ArtistEntity>>

    @Query("SELECT * FROM artists WHERE id = :id")
    fun observeById(id: String): Flow<ArtistEntity?>

    @Query("SELECT * FROM artists WHERE isFavorited = 1 ORDER BY name ASC")
    fun observeFavorites(): Flow<List<ArtistEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(artists: List<ArtistEntity>)

    @Query("UPDATE artists SET isFavorited = :isFavorited WHERE id = :id")
    suspend fun setFavorited(
        id: String,
        isFavorited: Boolean,
    )
}

@Dao
interface StageDao {
    @Query("SELECT * FROM stages")
    fun observeAll(): Flow<List<StageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(stages: List<StageEntity>)
}

@Dao
interface SetTimeDao {
    data class SetTimeRange(
        val minStart: Long,
        val maxStart: Long,
    )

    @Query("SELECT * FROM set_times ORDER BY startTime ASC")
    fun observeAll(): Flow<List<SetTimeEntity>>

    @Query("SELECT * FROM set_times WHERE startTime >= :dayStartMillis AND startTime < :dayEndMillis ORDER BY startTime ASC")
    fun observeByDay(
        dayStartMillis: Long,
        dayEndMillis: Long,
    ): Flow<List<SetTimeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(setTimes: List<SetTimeEntity>)

    @Query("SELECT MIN(startTime) AS minStart, MAX(startTime) AS maxStart FROM set_times")
    fun observeSetTimeRange(): Flow<SetTimeRange>
}

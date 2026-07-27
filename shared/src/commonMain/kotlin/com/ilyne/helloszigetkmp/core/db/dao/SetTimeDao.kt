package com.ilyne.helloszigetkmp.core.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.ilyne.helloszigetkmp.core.db.entity.SetTimeEntity
import com.ilyne.helloszigetkmp.core.db.model.SetTimeWithArtistStageSummary
import kotlinx.coroutines.flow.Flow

@Dao
interface SetTimeDao {
    // Nullable: SQL MIN()/MAX() return NULL over zero rows. If these were non-null Long, Room
    // would map that into 0 (epoch millis = 1 Jan 1970), producing a bogus single-day schedule
    // on a fresh install before any sync has happened. Nullable lets callers distinguish "no
    // data yet" from a real day at epoch 0.
    data class SetTimeRange(
        val minStart: Long?,
        val maxStart: Long?,
    )

    @Query(
        """
        SELECT set_times.*, artists.name AS artistName, stages.name AS stageName FROM set_times
        INNER JOIN artists ON set_times.artistId = artists.id
        LEFT JOIN stages ON set_times.stageId = stages.id
        WHERE artists.isFavorited = 1
        ORDER BY set_times.startTime ASC
        """,
    )
    fun observeFavorites(): Flow<List<SetTimeWithArtistStageSummary>>

    @Query("SELECT * FROM set_times WHERE startTime >= :dayStartMillis AND startTime < :dayEndMillis ORDER BY startTime ASC")
    fun observeByDay(
        dayStartMillis: Long,
        dayEndMillis: Long,
    ): Flow<List<SetTimeEntity>>

    @Query("SELECT * FROM set_times WHERE artistId = :artistId ORDER BY startTime ASC")
    fun observeByArtist(artistId: String): Flow<List<SetTimeEntity>>

    @Upsert
    suspend fun upsertAll(setTimes: List<SetTimeEntity>)

    @Query("SELECT MIN(startTime) AS minStart, MAX(startTime) AS maxStart FROM set_times")
    fun observeSetTimeRange(): Flow<SetTimeRange>
}

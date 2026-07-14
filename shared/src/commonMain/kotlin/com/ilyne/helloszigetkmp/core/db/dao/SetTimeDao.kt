package com.ilyne.helloszigetkmp.core.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.ilyne.helloszigetkmp.core.db.entity.SetTimeEntity
import com.ilyne.helloszigetkmp.core.db.model.SetTimeWithArtistStageSummary
import kotlinx.coroutines.flow.Flow

@Dao
interface SetTimeDao {
    data class SetTimeRange(
        val minStart: Long,
        val maxStart: Long,
    )

    @Query("SELECT * FROM set_times ORDER BY startTime ASC")
    fun observeAll(): Flow<List<SetTimeEntity>>

    @Query(
        """
        SELECT set_times.*, artists.name AS artistName, stages.name AS stageName FROM set_times
        INNER JOIN artists ON set_times.artistId = artists.id
        INNER JOIN stages ON set_times.stageId = stages.id
        WHERE artists.isFavorited = 1
        """
    )
    fun observeFavorites(): Flow<List<SetTimeWithArtistStageSummary>>

    @Query("SELECT * FROM set_times WHERE startTime >= :dayStartMillis AND startTime < :dayEndMillis ORDER BY startTime ASC")
    fun observeByDay(
        dayStartMillis: Long,
        dayEndMillis: Long,
    ): Flow<List<SetTimeEntity>>

    // Plain INSERT-OR-REPLACE would delete-then-reinsert conflicting rows on every refresh
    // (SQLite semantics), churning Room's invalidation tracker even when nothing changed.
    // Insert-or-ignore + update instead, so existing rows are updated in place (same fix as
    // UserDao.upsertAll/FriendDao.upsertFriendships, for the same reason).
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnoring(setTimes: List<SetTimeEntity>): List<Long>

    @Update
    suspend fun updateAll(setTimes: List<SetTimeEntity>)

    @Transaction
    suspend fun upsertAll(setTimes: List<SetTimeEntity>) {
        if (setTimes.isEmpty()) return
        val insertResults = insertIgnoring(setTimes)
        val existing = setTimes.filterIndexed { index, _ -> insertResults[index] == -1L }
        if (existing.isNotEmpty()) updateAll(existing)
    }

    @Query("SELECT MIN(startTime) AS minStart, MAX(startTime) AS maxStart FROM set_times")
    fun observeSetTimeRange(): Flow<SetTimeRange>
}

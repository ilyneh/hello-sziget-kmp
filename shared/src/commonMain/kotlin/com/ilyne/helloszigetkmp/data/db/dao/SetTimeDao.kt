package com.ilyne.helloszigetkmp.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ilyne.helloszigetkmp.data.db.entity.SetTimeEntity
import kotlinx.coroutines.flow.Flow

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

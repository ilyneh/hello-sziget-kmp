package com.ilyne.helloszigetkmp.core.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.ilyne.helloszigetkmp.core.db.entity.StageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StageDao {
    @Query("SELECT * FROM stages")
    fun observeAll(): Flow<List<StageEntity>>

    // Plain INSERT-OR-REPLACE would delete-then-reinsert conflicting rows on every refresh
    // (SQLite semantics), churning Room's invalidation tracker even when nothing changed.
    // Insert-or-ignore + update instead, so existing rows are updated in place (same fix as
    // UserDao.upsertAll/FriendDao.upsertFriendships, for the same reason).
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnoring(stages: List<StageEntity>): List<Long>

    @Update
    suspend fun updateAll(stages: List<StageEntity>)

    @Transaction
    suspend fun upsertAll(stages: List<StageEntity>) {
        if (stages.isEmpty()) return
        val insertResults = insertIgnoring(stages)
        val existing = stages.filterIndexed { index, _ -> insertResults[index] == -1L }
        if (existing.isNotEmpty()) updateAll(existing)
    }
}

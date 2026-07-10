package com.ilyne.helloszigetkmp.core.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ilyne.helloszigetkmp.core.db.entity.StageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StageDao {
    @Query("SELECT * FROM stages")
    fun observeAll(): Flow<List<StageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(stages: List<StageEntity>)
}

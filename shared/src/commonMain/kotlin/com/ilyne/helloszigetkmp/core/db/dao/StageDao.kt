package com.ilyne.helloszigetkmp.core.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.ilyne.helloszigetkmp.core.db.entity.StageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StageDao {
    @Query("SELECT * FROM stages")
    fun observeAll(): Flow<List<StageEntity>>

    @Upsert
    suspend fun upsertAll(stages: List<StageEntity>)
}

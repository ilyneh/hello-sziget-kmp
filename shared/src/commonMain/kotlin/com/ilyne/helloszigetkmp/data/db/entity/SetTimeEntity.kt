package com.ilyne.helloszigetkmp.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "set_times")
data class SetTimeEntity(
    @PrimaryKey val id: String,
    val artistId: String,
    val stageId: String?,
    val startTime: Long,
    val endTime: Long,
    val hideEndTime: Boolean,
)

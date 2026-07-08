package com.ilyne.helloszigetkmp.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "stages")
data class StageEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String?,
)

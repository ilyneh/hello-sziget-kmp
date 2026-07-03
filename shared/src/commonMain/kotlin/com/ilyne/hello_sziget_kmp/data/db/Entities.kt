package com.ilyne.hello_sziget_kmp.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "artists")
data class ArtistEntity(
    @PrimaryKey val id: String,
    val name: String,
    val bio: String?,
    val isFavorited: Boolean,
    val tags: List<String>?,
)

@Entity(tableName = "stages")
data class StageEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String?,
)

@Entity(tableName = "set_times")
data class SetTimeEntity(
    @PrimaryKey val id: String,
    val artistId: String,
    val stageId: String?,
    val startTime: Long,
    val endTime: Long,
    val hideEndTime: Boolean,
)

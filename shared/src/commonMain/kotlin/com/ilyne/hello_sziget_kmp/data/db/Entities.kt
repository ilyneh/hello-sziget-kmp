package com.ilyne.hello_sziget_kmp.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "artists")
data class ArtistEntity(
    @PrimaryKey val id: String,
    val name: String,
    val genre: String,
    val imageUrl: String?,
    val bio: String?,
    val isFavorited: Boolean,
)

@Entity(tableName = "stages")
data class StageEntity(
    @PrimaryKey val id: String,
    val name: String,
    val color: Long,
)

@Entity(tableName = "set_times")
data class SetTimeEntity(
    @PrimaryKey val id: String,
    val artistId: String,
    val stageId: String,
    val startTime: Long,
    val endTime: Long,
)

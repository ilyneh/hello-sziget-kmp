package com.ilyne.helloszigetkmp.data.db

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.room.TypeConverters
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object SzigetDatabaseConstructor : RoomDatabaseConstructor<SzigetDatabase>

@Database(
    entities = [ArtistEntity::class, StageEntity::class, SetTimeEntity::class],
    version = 1,
)
@ConstructedBy(SzigetDatabaseConstructor::class)
@TypeConverters(Converters::class)
abstract class SzigetDatabase : RoomDatabase() {
    abstract fun artistDao(): ArtistDao
    abstract fun stageDao(): StageDao
    abstract fun setTimeDao(): SetTimeDao
}

fun createDatabase(builder: RoomDatabase.Builder<SzigetDatabase>): SzigetDatabase =
    builder
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()

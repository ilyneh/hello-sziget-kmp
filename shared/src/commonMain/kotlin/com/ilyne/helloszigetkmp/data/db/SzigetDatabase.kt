package com.ilyne.helloszigetkmp.data.db

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.room.TypeConverters
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.ilyne.helloszigetkmp.data.db.dao.ArtistDao
import com.ilyne.helloszigetkmp.data.db.dao.FriendDao
import com.ilyne.helloszigetkmp.data.db.dao.SetTimeDao
import com.ilyne.helloszigetkmp.data.db.dao.StageDao
import com.ilyne.helloszigetkmp.data.db.dao.UserDao
import com.ilyne.helloszigetkmp.data.db.entity.ArtistEntity
import com.ilyne.helloszigetkmp.data.db.entity.CurrentUserEntity
import com.ilyne.helloszigetkmp.data.db.entity.SetTimeEntity
import com.ilyne.helloszigetkmp.data.db.entity.StageEntity
import com.ilyne.helloszigetkmp.data.db.entity.UserEntity
import com.ilyne.helloszigetkmp.data.db.entity.UserFriendEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object SzigetDatabaseConstructor : RoomDatabaseConstructor<SzigetDatabase>

@Database(
    entities = [
        ArtistEntity::class, StageEntity::class, SetTimeEntity::class,
        UserEntity::class, UserFriendEntity::class, CurrentUserEntity::class,
    ],
    version = 2,
)
@ConstructedBy(SzigetDatabaseConstructor::class)
@TypeConverters(Converters::class)
abstract class SzigetDatabase : RoomDatabase() {
    abstract fun artistDao(): ArtistDao

    abstract fun stageDao(): StageDao

    abstract fun setTimeDao(): SetTimeDao

    abstract fun userDao(): UserDao

    abstract fun friendDao(): FriendDao
}

fun createDatabase(builder: RoomDatabase.Builder<SzigetDatabase>): SzigetDatabase =
    builder
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .fallbackToDestructiveMigration(dropAllTables = true)
        .build()

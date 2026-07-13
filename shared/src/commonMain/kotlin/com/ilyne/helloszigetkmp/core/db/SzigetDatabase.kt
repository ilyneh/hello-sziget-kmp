package com.ilyne.helloszigetkmp.core.db

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.room.TypeConverters
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.ilyne.helloszigetkmp.core.db.dao.ArtistDao
import com.ilyne.helloszigetkmp.core.db.dao.FriendDao
import com.ilyne.helloszigetkmp.core.db.dao.SetTimeDao
import com.ilyne.helloszigetkmp.core.db.dao.StageDao
import com.ilyne.helloszigetkmp.core.db.dao.UserDao
import com.ilyne.helloszigetkmp.core.db.entity.ArtistEntity
import com.ilyne.helloszigetkmp.core.db.entity.ArtistFriendFavoritedEntity
import com.ilyne.helloszigetkmp.core.db.entity.CurrentUserEntity
import com.ilyne.helloszigetkmp.core.db.entity.SetTimeEntity
import com.ilyne.helloszigetkmp.core.db.entity.StageEntity
import com.ilyne.helloszigetkmp.core.db.entity.UserEntity
import com.ilyne.helloszigetkmp.core.db.entity.UserFriendEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object SzigetDatabaseConstructor : RoomDatabaseConstructor<SzigetDatabase>

@Database(
    entities = [
        ArtistEntity::class, StageEntity::class, SetTimeEntity::class,
        UserEntity::class, UserFriendEntity::class, CurrentUserEntity::class,
        ArtistFriendFavoritedEntity::class,
    ],
    version = 1,
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

fun createDatabase(
    builder: RoomDatabase.Builder<SzigetDatabase>,
    onDestructiveMigration: () -> Unit = {},
): SzigetDatabase =
    builder
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .addCallback(
            object : RoomDatabase.Callback() {
                override fun onDestructiveMigration(connection: SQLiteConnection) = onDestructiveMigration()
            },
        )
        .fallbackToDestructiveMigration(dropAllTables = true)
        .build()

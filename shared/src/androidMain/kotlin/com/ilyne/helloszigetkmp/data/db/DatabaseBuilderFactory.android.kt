package com.ilyne.helloszigetkmp.data.db

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

actual fun getDatabaseBuilder(): RoomDatabase.Builder<SzigetDatabase> {
    val appContext: Context by lazy {
        AndroidDatabaseContext.context
    }
    return Room.databaseBuilder<SzigetDatabase>(
        context = appContext,
        name = appContext.getDatabasePath("sziget.db").absolutePath,
    )
}

object AndroidDatabaseContext : KoinComponent {
    val context: Context by inject()
}

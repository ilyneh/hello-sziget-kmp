package com.ilyne.hello_sziget_kmp.data.db

import androidx.room.Room
import androidx.room.RoomDatabase
import platform.Foundation.NSHomeDirectory

actual fun getDatabaseBuilder(): RoomDatabase.Builder<SzigetDatabase> {
    val dbPath = NSHomeDirectory() + "/Documents/sziget.db"
    return Room.databaseBuilder<SzigetDatabase>(name = dbPath)
}

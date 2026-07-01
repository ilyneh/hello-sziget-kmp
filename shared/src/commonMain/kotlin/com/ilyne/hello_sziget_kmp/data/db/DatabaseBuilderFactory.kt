package com.ilyne.hello_sziget_kmp.data.db

import androidx.room.RoomDatabase

expect fun getDatabaseBuilder(): RoomDatabase.Builder<SzigetDatabase>

package com.ilyne.helloszigetkmp.data.db

import androidx.room.RoomDatabase

expect fun getDatabaseBuilder(): RoomDatabase.Builder<SzigetDatabase>

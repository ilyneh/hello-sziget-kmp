package com.ilyne.helloszigetkmp.core.db

import androidx.room.RoomDatabase

expect fun getDatabaseBuilder(): RoomDatabase.Builder<SzigetDatabase>

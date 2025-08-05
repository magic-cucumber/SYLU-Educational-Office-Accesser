package top.kagg886.backend.database

import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.driver.NativeSQLiteDriver

actual fun commonDatabaseBuilder(): RoomDatabase.Builder<AppDatabase> = Room.databaseBuilder<AppDatabase>(name = databasePath).setDriver(NativeSQLiteDriver())

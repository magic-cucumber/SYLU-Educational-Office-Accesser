package top.kagg886.backend.database

import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.sqlite.driver.NativeSQLiteDriver

actual fun commonDatabaseBuilder(): RoomDatabase.Builder<AppDatabase> = Room.databaseBuilder<AppDatabase>(name = databasePath).setDriver(NativeSQLiteDriver())

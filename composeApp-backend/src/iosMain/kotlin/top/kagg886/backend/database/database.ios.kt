package top.kagg886.backend.database

import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.sqlite.driver.NativeSQLiteDriver
import okio.Path

actual fun commonDatabaseBuilder(path: Path): RoomDatabase.Builder<AppDatabase> =
    Room.databaseBuilder<AppDatabase>(name = path.toString()).setDriver(NativeSQLiteDriver())

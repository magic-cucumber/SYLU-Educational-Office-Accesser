package top.kagg886.backend.database

import androidx.room3.Room
import androidx.room3.RoomDatabase
import top.kagg886.eoa.EOAApplication

actual fun commonDatabaseBuilder(): RoomDatabase.Builder<AppDatabase> {
    return Room.databaseBuilder(
        name = databasePath,
        context = EOAApplication.getApp()
    )
}

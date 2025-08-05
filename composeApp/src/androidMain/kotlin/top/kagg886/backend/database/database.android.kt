package top.kagg886.backend.database

import androidx.room.Room
import androidx.room.RoomDatabase
import top.kagg886.eoa.EOAApplication

actual fun commonDatabaseBuilder(): RoomDatabase.Builder<AppDatabase> {
    return Room.databaseBuilder(
        name = databasePath,
        context = EOAApplication.getApp()
    )
}

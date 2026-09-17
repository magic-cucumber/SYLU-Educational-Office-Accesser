package top.kagg886.backend.database

import androidx.room3.Room
import androidx.room3.RoomDatabase
import okio.Path
import top.kagg886.util.currentApplication

actual fun commonDatabaseBuilder(path: Path): RoomDatabase.Builder<AppDatabase> {
    return Room.databaseBuilder(
        name = path.toString(),
        context = currentApplication()
    )
}

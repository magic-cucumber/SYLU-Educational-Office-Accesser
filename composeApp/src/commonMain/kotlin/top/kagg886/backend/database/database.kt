package top.kagg886.backend.database

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import top.kagg886.backend.database.dao.AppLog
import top.kagg886.backend.database.dao.AppLogDao
import top.kagg886.backend.database.dao.CourseDao
import top.kagg886.backend.database.dao.CourseEntity
import top.kagg886.backend.database.dao.CourseRecordDao
import top.kagg886.backend.database.dao.CourseRecordEntity
import top.kagg886.backend.database.dao.ExamDao
import top.kagg886.backend.database.dao.ExamEntity
import top.kagg886.backend.database.dao.GPADao
import top.kagg886.backend.database.dao.GPAEntity
import top.kagg886.backend.database.dao.GPASummaryDao
import top.kagg886.backend.database.dao.GPASummaryEntity
import top.kagg886.backend.database.dao.SyncRecordEntity
import top.kagg886.backend.database.dao.SyncRecordDao
import top.kagg886.backend.database.dao.SystemNoticeDao
import top.kagg886.backend.database.dao.SystemNoticeEntity
import top.kagg886.eoa.config.BuildConfig
import top.kagg886.sylu_eoa.api.v2.bean.SystemNotice
import top.kagg886.util.absolutePath
import top.kagg886.util.dataPath

@Database(
    entities = [
        ExamEntity::class,
        GPASummaryEntity::class,
        GPAEntity::class,
        CourseEntity::class,
        CourseRecordEntity::class,
        SyncRecordEntity::class,
        AppLog::class,
        SystemNoticeEntity::class
    ],
    version = BuildConfig.DATABASE_VERSION,
)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun examDao(): ExamDao
    abstract fun gpaSummaryDao(): GPASummaryDao
    abstract fun gpaDao(): GPADao
    abstract fun courseDao(): CourseDao
    abstract fun courseRecordDao(): CourseRecordDao
    abstract fun noticeDao(): SystemNoticeDao

    abstract fun syncRecordDao(): SyncRecordDao
    abstract fun appLogDao(): AppLogDao
}

// The Room compiler generates the `actual` implementations.
@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase> {
    override fun initialize(): AppDatabase
}

val databasePath: String by lazy {
    dataPath.resolve("app.db").absolutePath().toString()
}

expect fun databaseBuilder(): RoomDatabase.Builder<AppDatabase>

package top.kagg886.backend.database

import androidx.room3.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import top.kagg886.eoa.config.BuildConfig
import top.kagg886.backend.database.dao.*
import top.kagg886.backend.database.migrate.MIGRATION_10_11
import top.kagg886.backend.database.migrate.MIGRATION_12_13
import top.kagg886.util.absolutePath
import top.kagg886.util.dataPath

@Database(
    entities = [
        ExamEntity::class,
        GPASummaryEntity::class,
        GPAEntity::class,
        CourseEntity::class,
        CourseExtendEntity::class,
        CourseRecordEntity::class,
        SyncOverviewEntity::class,
        SyncCheckpointEntity::class,
        AppLog::class,
        SystemNoticeEntity::class,
        SecondClassSummaryEntity::class,
        SecondClassDataEntity::class,
        LLMProviderEntity::class
    ],
    version = BuildConfig.DATABASE_VERSION,
    autoMigrations = [
        AutoMigration(from = 6, to = 7),
        AutoMigration(from = 7, to = 8),
        AutoMigration(from = 8, to = 9),
        AutoMigration(from = 9, to = 10),
        AutoMigration(from = 11, to = 12),
    ]
)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun examDao(): ExamDao
    abstract fun gpaSummaryDao(): GPASummaryDao
    abstract fun gpaDao(): GPADao
    abstract fun courseDao(): CourseDao
    abstract fun courseExtendDao(): CourseExtendDao
    abstract fun courseRecordDao(): CourseRecordDao
    abstract fun noticeDao(): SystemNoticeDao
    abstract fun secondClassDao(): SecondClassDao
    abstract fun llmProviderDao(): LLMProviderDao

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

expect fun commonDatabaseBuilder(): RoomDatabase.Builder<AppDatabase>

fun databaseBuilder(): RoomDatabase.Builder<AppDatabase> = commonDatabaseBuilder()
    .addMigrations(MIGRATION_10_11)
    .addMigrations(MIGRATION_12_13)
    .setQueryCoroutineContext(Dispatchers.IO)

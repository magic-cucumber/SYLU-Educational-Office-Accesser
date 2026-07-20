package top.kagg886.backend.database.dao

import androidx.paging.PagingSource
import androidx.room3.*
import androidx.room3.ColumnInfo.Companion.INTEGER
import androidx.room3.paging.PagingSourceDaoReturnTypeConverter
import co.touchlab.kermit.Severity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flow
import top.kagg886.backend.database.AppDatabase
import top.kagg886.backend.database.converters.SeverityConverter
import top.kagg886.backend.database.converters.TimeConverter
import kotlin.time.Instant

@Entity(
    tableName = "log",
    indices = [
        Index(value = ["time"]),
        Index(value = ["level", "time"])
    ]
)
@ColumnTypeConverters(TimeConverter::class, SeverityConverter::class)
data class AppLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long? = null,
    val tag: String,
    @ColumnInfo(typeAffinity = INTEGER)
    val level: Severity,
    val message: String,
    val time: Instant,
    val stacktrace: String? = null,
)

@Dao
@DaoReturnTypeConverters(PagingSourceDaoReturnTypeConverter::class)
interface AppLogDao {
    @Insert
    suspend fun insert(item: AppLog)

    @Query("DELETE FROM log")
    suspend fun clear()

    @Query("DELETE FROM log WHERE time < :before")
    suspend fun clear(before: Long): Int

    @Query("SELECT * FROM log WHERE (:level IS NULL OR level >= :level) ORDER BY time DESC")
    fun getLogsByPage(level: Int? = null): PagingSource<Int, AppLog>

    @Query("SELECT count(*) FROM log")
    suspend fun count(): Int
}

fun AppDatabase.log(level: Int? = null): Flow<AppLog> = channelFlow {
    useReaderConnection { connection ->
        connection.usePrepared("SELECT id, tag, level, message, time, stacktrace FROM log WHERE (? IS NULL OR level >= ?) ORDER BY time") { statement ->
            if (level == null) {
                statement.bindNull(1)
                statement.bindNull(2)
            } else {
                statement.bindInt(1, level)
                statement.bindInt(2, level)
            }

            while (statement.step()) {
                channel.send(
                    AppLog(
                        id = if (statement.isNull(0)) null else statement.getLong(0),
                        tag = statement.getText(1),
                        level = Severity.entries[statement.getInt(2)],
                        message = statement.getText(3),
                        time = Instant.fromEpochMilliseconds(statement.getLong(4)),
                        stacktrace = if (statement.isNull(5)) null else statement.getText(5),
                    )
                )
            }
        }
    }
}

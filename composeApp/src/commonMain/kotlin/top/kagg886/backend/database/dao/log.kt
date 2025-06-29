package top.kagg886.backend.database.dao

import androidx.paging.PagingSource
import androidx.room.*
import co.touchlab.kermit.Severity
import kotlinx.datetime.LocalDateTime
import top.kagg886.backend.database.converters.LocalDateTimeConverter

@Entity(tableName = "log")
@TypeConverters(LocalDateTimeConverter::class)
data class AppLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long? = null,
    val tag: String,
    val level: Severity,
    val message: String,
    val time: LocalDateTime,
    val stacktrace: String? = null,
)

@Dao
interface AppLogDao {
    @Insert
    suspend fun insert(item: AppLog)

    @Query("DELETE FROM log")
    suspend fun clear()

    @Query("DELETE FROM log WHERE message < :before")
    suspend fun clear(before:Long)

    @Query("SELECT * FROM log WHERE (:level IS NULL OR level = :level) ORDER BY time DESC")
    fun getLogsByPage(level: Severity? = null): PagingSource<Int, AppLog>

    @Query("SELECT * FROM log WHERE (:level IS NULL OR level = :level) ORDER BY time")
    suspend fun getLogs(level: Severity? = null): List<AppLog>
}

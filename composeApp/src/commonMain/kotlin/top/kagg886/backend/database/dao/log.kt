package top.kagg886.backend.database.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import co.touchlab.kermit.Severity

@Entity(tableName = "log")
data class AppLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long? = null,
    val tag: String,
    val level: Severity,
    val message: String,
    val time: Long,
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
    fun all(level: Severity? = null): PagingSource<Int, AppLog>
}
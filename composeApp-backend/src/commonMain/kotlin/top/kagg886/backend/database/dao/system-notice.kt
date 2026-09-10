package top.kagg886.backend.database.dao

import androidx.room3.*
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDateTime
import top.kagg886.backend.database.converters.TimeConverter

@Entity(tableName = "system_notices")
@ColumnTypeConverters(TimeConverter::class)
data class SystemNoticeEntity(
    @PrimaryKey val id: String,
    val title: String,
    val content: String,
    val time: LocalDateTime,
    val isRead: Boolean = false
)

@Dao
interface SystemNoticeDao {
    @Query("DELETE FROM system_notices")
    suspend fun clear()

    @Query("SELECT * FROM system_notices WHERE (:includeAll = 1 OR isRead = 0) ORDER BY time DESC")
    suspend fun all(includeAll: Boolean = false): List<SystemNoticeEntity>

    @Query("SELECT * FROM system_notices WHERE (:includeAll = 1 OR isRead = 0) ORDER BY time DESC")
    fun allFlow(includeAll: Boolean = false): Flow<List<SystemNoticeEntity>>

    @Query("UPDATE system_notices SET isRead = 1 WHERE id = :id")
    suspend fun markAsRead(id: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: SystemNoticeEntity)
}

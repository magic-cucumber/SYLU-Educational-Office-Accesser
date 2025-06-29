package top.kagg886.backend.database.dao

import androidx.room.*
import kotlinx.datetime.LocalDateTime
import top.kagg886.backend.database.converters.LocalDateTimeConverter

@Entity(tableName = "system_notices")
@TypeConverters(LocalDateTimeConverter::class)
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

    @Query("SELECT * FROM system_notices WHERE (:includeAll = true OR isRead = false) ORDER BY time DESC")
    suspend fun all(includeAll: Boolean = false): List<SystemNoticeEntity>

    @Query("UPDATE system_notices SET isRead = true WHERE id = :id")
    suspend fun markAsRead(id: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: SystemNoticeEntity)
}

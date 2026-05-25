package top.kagg886.backend.database.dao

import androidx.room3.*
import kotlin.time.Clock

@Entity(tableName = "sync_records")
data class SyncRecordEntity(
    @PrimaryKey(true)
    val id: Int? = null,
    val updatedStamp: Long = Clock.System.now().toEpochMilliseconds(),

    @ColumnInfo(defaultValue = "true")
    val success: Boolean = true,
)

@Dao
interface SyncRecordDao {
    @Insert
    suspend fun markSync(record: SyncRecordEntity = SyncRecordEntity())

    @Query(
        """
            SELECT updatedStamp 
            FROM sync_records 
            ORDER BY id DESC 
            LIMIT 1
        """
    )
    suspend fun getLastSyncTime(): Long?

    @Query(
        """
            SELECT success
            FROM sync_records
            ORDER BY id DESC
            LIMIT 1
        """
    )
    suspend fun getLastSyncSuccess(): Boolean?

    @Query("DELETE FROM sync_records")
    suspend fun clear()
}

package top.kagg886.backend.database.dao

import androidx.room3.ColumnInfo
import androidx.room3.Dao
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.PrimaryKey
import androidx.room3.Query
import androidx.room3.TypeConverters
import androidx.room3.Update
import kotlin.time.Clock
import kotlinx.serialization.Serializable
import top.kagg886.backend.database.converters.ExamSyncPayloadConverter
import top.kagg886.backend.database.converters.GPASyncPayloadConverter
import top.kagg886.sylu_eoa.api.v2.bean.ExamItem
import top.kagg886.sylu_eoa.api.v2.bean.GPAScoreSummary

@Entity(tableName = "sync-overviews")
data class SyncOverviewEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int? = null,
    val updatedStamp: Long = Clock.System.now().toEpochMilliseconds(),

    @ColumnInfo(defaultValue = "true")
    val success: Boolean = true,
)

@Serializable
data class ExamSyncPayload(
    val remains: List<ExamItem> = emptyList(),
    val total: Int = remains.size
)

@Serializable
data class GPASyncPayload(
    val remains: List<GPAScoreSummary> = emptyList(),
    val total: Int = remains.size
)

@Entity(
    tableName = "sync-checkpoints",
    foreignKeys = [
        ForeignKey(
            entity = SyncOverviewEntity::class,
            parentColumns = ["id"],
            childColumns = ["overviewId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["overviewId"], unique = true)
    ]
)
@TypeConverters(ExamSyncPayloadConverter::class, GPASyncPayloadConverter::class)
data class SyncCheckpointEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int? = null,
    val overviewId: Int,
    val updatedStamp: Long = Clock.System.now().toEpochMilliseconds(),

    val profileSuccess: Boolean = false,
    val calendarSuccess: Boolean = false,
    val examSuccess: Boolean = false,
    val gpaSuccess: Boolean = false,
    val noticeSuccess: Boolean = false,
    val termSuccess: Boolean = false,
    val courseSuccess: Boolean = false,

    val examPayload: ExamSyncPayload? = null,
    val gpaPayload: GPASyncPayload? = null,
)

@Dao
interface SyncRecordDao {
    @Insert
    suspend fun insertOverview(record: SyncOverviewEntity): Long

    @Update
    suspend fun updateOverview(record: SyncOverviewEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCheckpoint(record: SyncCheckpointEntity): Long

    @Update
    suspend fun updateCheckpoint(record: SyncCheckpointEntity)

    @Query(
        """
            SELECT *
            FROM `sync-overviews`
            WHERE success = 0
            ORDER BY id DESC
            LIMIT 1
        """
    )
    suspend fun getLastUnSuccessOverview(): SyncOverviewEntity?

    @Query(
        """
            SELECT *
            FROM `sync-checkpoints`
            WHERE overviewId = :overviewId
            LIMIT 1
        """
    )
    suspend fun getCheckpointByOverviewId(overviewId: Int): SyncCheckpointEntity?

    @Query(
        """
            SELECT updatedStamp 
            FROM `sync-overviews`
            ORDER BY id DESC 
            LIMIT 1
        """
    )
    suspend fun getLastSyncTime(): Long?

    @Query(
        """
            SELECT success
            FROM `sync-overviews`
            ORDER BY id DESC
            LIMIT 1
        """
    )
    suspend fun getLastSyncSuccess(): Boolean?

    @Query("DELETE FROM `sync-overviews`")
    suspend fun clear()
}

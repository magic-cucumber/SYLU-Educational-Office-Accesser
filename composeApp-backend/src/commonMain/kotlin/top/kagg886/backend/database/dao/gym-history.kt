package top.kagg886.backend.database.dao

import androidx.room3.*
import kotlinx.coroutines.flow.Flow
import top.kagg886.backend.database.converters.TimeConverter
import top.kagg886.eoa.gym.GymDetailHistory
import kotlin.time.Instant

@Entity(
    tableName = "gym_history",
    foreignKeys = [ForeignKey(
        entity = GymDetailEntity::class,
        parentColumns = ["summaryId", "id"], childColumns = ["summaryId", "detailId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index(value = ["summaryId", "detailId"])],
)
@ColumnTypeConverters(TimeConverter::class)
data class GymEntity(
    @PrimaryKey(autoGenerate = true) val id: Long? = null,
    val summaryId: String,
    val detailId: String,
    val result: String,
    val scoreTime: Instant,
    val scoreStatus: String,
    val sortOrder: Int,
)

@Dao
interface GymDao {
    @Query("DELETE FROM gym_history")
    suspend fun clear()

    @Query("DELETE FROM gym_history WHERE summaryId = :summaryId AND detailId = :detailId")
    suspend fun clear(summaryId: String, detailId: String)

    @Query("SELECT * FROM gym_history ORDER BY sortOrder")
    suspend fun all(): List<GymEntity>

    @Query("SELECT * FROM gym_history WHERE summaryId = :summaryId AND detailId = :detailId ORDER BY sortOrder")
    fun getByParentId(summaryId: String, detailId: String): Flow<List<GymEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<GymEntity>)

    @Transaction
    suspend fun replaceAll(summaryId: String, detailId: String, items: List<GymDetailHistory>) {
        clear(summaryId, detailId)
        insertAll(items.mapIndexed { index, item -> item.toEntity(summaryId, detailId, index) })
    }
}

fun GymDetailHistory.toEntity(summaryId: String, detailId: String, sortOrder: Int) =
    GymEntity(summaryId = summaryId, detailId = detailId, result = result,
        scoreTime = scoreTime, scoreStatus = scoreStatus, sortOrder = sortOrder)

fun GymEntity.toItem() = GymDetailHistory(result, scoreTime, scoreStatus)

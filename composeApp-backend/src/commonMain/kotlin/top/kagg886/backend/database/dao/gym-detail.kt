package top.kagg886.backend.database.dao

import androidx.room3.*
import kotlinx.coroutines.flow.Flow
import top.kagg886.eoa.gym.GymDetail

@Entity(
    tableName = "gym_detail",
    primaryKeys = ["summaryId", "id"],
    foreignKeys = [ForeignKey(
        entity = GymSummaryEntity::class,
        parentColumns = ["id"], childColumns = ["summaryId"],
        onDelete = ForeignKey.CASCADE,
    )],
)
data class GymDetailEntity(
    val summaryId: String,
    val id: String,
    val name: String,
    val result: String?,
    val score: Float?,
    val unit: String,
    val grade: String?,
    val ratio: String,
    val isJoin: Int,
    val sortOrder: Int,
)

@Dao
interface GymDetailDao {
    @Query("DELETE FROM gym_detail")
    suspend fun clear()

    @Query("SELECT * FROM gym_detail ORDER BY sortOrder")
    suspend fun all(): List<GymDetailEntity>

    @Query("SELECT * FROM gym_detail WHERE summaryId = :summaryId ORDER BY sortOrder")
    fun getByParentId(summaryId: String): Flow<List<GymDetailEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<GymDetailEntity>)
}

fun GymDetail.toEntity(summaryId: String, sortOrder: Int) =
    GymDetailEntity(summaryId, id, name, result, score, unit, grade, ratio, isJoin, sortOrder)

fun GymDetailEntity.toItem() = GymDetail(id, name, result, score, unit, grade, ratio, isJoin)

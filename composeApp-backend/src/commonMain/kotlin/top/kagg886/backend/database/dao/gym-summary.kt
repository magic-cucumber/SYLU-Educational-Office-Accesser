package top.kagg886.backend.database.dao

import androidx.room3.*
import top.kagg886.eoa.gym.GymSummary

@Entity(tableName = "gym_summary")
data class GymSummaryEntity(
    @PrimaryKey val id: String,
    val score: Float,
    val all: Int,
    val done: Int,
    val sortOrder: Int,
)

@Dao
interface GymSummaryDao {
    @Query("DELETE FROM gym_summary")
    suspend fun clear()

    @Query("SELECT * FROM gym_summary ORDER BY sortOrder")
    suspend fun all(): List<GymSummaryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: GymSummaryEntity)
}

fun GymSummary.toEntity(sortOrder: Int) = GymSummaryEntity(id, score, all, done, sortOrder)

fun GymSummaryEntity.toItem() = GymSummary(id, score, all, done)

package top.kagg886.backend.database.dao

import androidx.room3.*
import top.kagg886.sylu_eoa.api.v2.bean.GPAScoreSummary

@Entity(tableName = "gpa_summary")
data class GPASummaryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long? = null,
    val name: String,
    val score: Double
)

@Dao
interface GPASummaryDao {
    @Query("DELETE FROM gpa_summary")
    suspend fun clear()

    @Query("SELECT * FROM gpa_summary")
    suspend fun all(): List<GPASummaryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: GPASummaryEntity): Long
}

fun GPAScoreSummary.toEntity() = GPASummaryEntity(
    name = name,
    score = score
)

fun GPASummaryEntity.toItem() = GPAScoreSummary(
    name = name,
    score = score
)

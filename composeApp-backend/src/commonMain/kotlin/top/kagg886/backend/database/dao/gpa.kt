package top.kagg886.backend.database.dao

import androidx.room3.Dao
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.PrimaryKey
import androidx.room3.Query
import kotlinx.coroutines.flow.Flow
import top.kagg886.sylu_eoa.api.v2.bean.GPAScore

@Entity(
    tableName = "gpa_scores",
    foreignKeys = [
        ForeignKey(
            entity = GPASummaryEntity::class,
            parentColumns = ["id"],
            childColumns = ["summaryId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class GPAEntity(
    @PrimaryKey(autoGenerate = true) val id: Long? = null,
    val summaryId: Long, // Foreign key to GPASummaryEntity
    val name: String,
    val score: String
)

@Dao
interface GPADao {
    @Query("DELETE FROM gpa_scores")
    suspend fun clear()

    @Query("SELECT * FROM gpa_scores")
    suspend fun all(): List<GPAEntity>

    @Query("SELECT * FROM gpa_scores WHERE summaryId = :summaryId")
    fun getByParentId(summaryId: Long): Flow<List<GPAEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: GPAEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<GPAEntity>)
}

fun GPAScore.toEntity(summaryId: Long) = GPAEntity(
    summaryId = summaryId,
    name = name,
    score = score
)

fun GPAEntity.toItem() = GPAScore(
    name = name,
    score = score
)

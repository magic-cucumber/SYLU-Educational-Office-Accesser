package top.kagg886.backend.database.dao

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import top.kagg886.sylu_eoa.api.v2.bean.GPAScore
import top.kagg886.backend.database.dao.GPASummaryEntity

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
     fun allFlow(): Flow<List<GPAEntity>>

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


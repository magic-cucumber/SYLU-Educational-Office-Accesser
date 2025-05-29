package top.kagg886.backend.database.dao

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(
    tableName = "course_records",
    foreignKeys = [
        ForeignKey(
            entity = CourseEntity::class,
            parentColumns = ["id"],
            childColumns = ["courseId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class CourseRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long? = null,
    val courseId: Long, // Foreign key to CourseEntity
    val weekNumber: Int, // Week of the semester
    val dayOfWeek: Int, // Day of the week (1-7)
    val periodOfDay: Int, // Period of the day
    val isUserAdded: Boolean = false
)

@Dao
interface CourseRecordDao {
    @Query("DELETE FROM course_records")
    suspend fun clear()

    @Query("SELECT * FROM course_records")
    fun allFlow(): Flow<List<CourseRecordEntity>>

    @Query("SELECT * FROM course_records")
    suspend fun all(): List<CourseRecordEntity>

    @Query("SELECT * FROM course_records WHERE courseId = :courseId")
    fun getByCourseId(courseId: Long): Flow<List<CourseRecordEntity>>

    @Query("SELECT * FROM course_records WHERE weekNumber = :weekNumber AND dayOfWeek = :dayOfWeek")
    fun getTodayClassesByDateParam(weekNumber: Int, dayOfWeek: Int): List<CourseRecordEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: CourseRecordEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<CourseRecordEntity>)
}


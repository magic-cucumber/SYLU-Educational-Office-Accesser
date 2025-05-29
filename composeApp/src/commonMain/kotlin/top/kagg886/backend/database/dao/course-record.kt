package top.kagg886.backend.database.dao

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
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

data class CourseAndRecord(
    @Embedded val course: CourseEntity,

    @Embedded(prefix = "record_")
    val record: CourseRecordEntity
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

    @Query("""
    SELECT 
        c.id AS id,
        c.name AS name,
        c.teacherName AS teacherName,
        c.classroomName AS classroomName,
        c.credits AS credits,
        c.isDegreeRequired AS isDegreeRequired,
        c.isUserAdded AS isUserAdded,
        
        cr.id AS record_id,
        cr.courseId AS record_courseId,
        cr.weekNumber AS record_weekNumber,
        cr.dayOfWeek AS record_dayOfWeek,
        cr.periodOfDay AS record_periodOfDay,
        cr.isUserAdded AS record_isUserAdded
        
    FROM courses c
    JOIN course_records cr ON cr.courseId = c.id
    WHERE cr.weekNumber = :weekNumber AND cr.dayOfWeek = :dayOfWeek
    ORDER BY cr.periodOfDay
""")
    suspend fun getCoursesWithRecordInfoByDate(
        weekNumber: Int,
        dayOfWeek: Int
    ): List<CourseAndRecord>


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: CourseRecordEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<CourseRecordEntity>)
}


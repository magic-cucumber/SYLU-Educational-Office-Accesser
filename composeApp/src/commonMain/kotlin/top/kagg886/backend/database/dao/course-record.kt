package top.kagg886.backend.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query

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
    val courseId: Long? = null, // Foreign key to CourseEntity
    val weekNumber: Int, // Week of the semester (start At 1)
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
    suspend fun all(): List<CourseRecordEntity>

    @Query("SELECT * FROM course_records WHERE courseId = :courseId")
    suspend fun getByCourseId(courseId: Long): List<CourseRecordEntity>

    @Query("SELECT * FROM course_records WHERE id = :recordId")
    suspend fun getById(recordId: Long): CourseRecordEntity

    @Query(
        """
    SELECT 
        c.id AS id,
        c.name AS name,
        c.teacherName AS teacherName,
        c.classroomName AS classroomName,
        c.credits AS credits,
        c.isDegreeRequired AS isDegreeRequired,
        c.isUserAdded AS isUserAdded,
        c.yearCode AS yearCode,
        c.semesterCode AS semesterCode,
        
        cr.id AS record_id,
        cr.courseId AS record_courseId,
        cr.weekNumber AS record_weekNumber,
        cr.dayOfWeek AS record_dayOfWeek,
        cr.periodOfDay AS record_periodOfDay,
        cr.isUserAdded AS record_isUserAdded
        
    FROM courses c
    JOIN course_records cr ON cr.courseId = c.id
    WHERE cr.weekNumber = :weekNumber AND (:dayOfWeek IS NULL OR cr.dayOfWeek = :dayOfWeek) AND (:periodOfDay IS NULL OR cr.periodOfDay = :periodOfDay)
    ORDER BY cr.periodOfDay
"""
    )
    suspend fun getCoursesWithRecordInfo(
        weekNumber: Int,
        dayOfWeek: Int? = null,
        periodOfDay: Int? = null
    ): List<CourseAndRecord>


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: CourseRecordEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<CourseRecordEntity>)

    @Delete
    suspend fun delete(item: CourseRecordEntity)
}



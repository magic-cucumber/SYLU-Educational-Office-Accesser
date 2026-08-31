package top.kagg886.backend.database.dao

import androidx.room3.ColumnTypeConverters
import androidx.room3.Dao
import androidx.room3.Delete
import androidx.room3.Embedded
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.PrimaryKey
import androidx.room3.Query
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDateTime
import top.kagg886.backend.database.converters.SeverityConverter
import top.kagg886.backend.database.converters.TimeConverter

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
@ColumnTypeConverters(TimeConverter::class)
data class CourseRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long? = null,
    val courseId: Long? = null, // Foreign key to CourseEntity
    val startTime: LocalDateTime, // record start time
    val endTime: LocalDateTime, // record end time
    val isUserAdded: Boolean = false
)

data class CourseAndRecord(
    @Embedded val course: CourseEntity,

    @Embedded(prefix = "record_")
    val record: CourseRecordEntity
)

@Dao
@ColumnTypeConverters(TimeConverter::class)
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
        c.isExaminable AS isExaminable,
        c.isUserAdded AS isUserAdded,
        
        cr.id AS record_id,
        cr.courseId AS record_courseId,
        cr.startTime AS record_startTime,
        cr.endTime AS record_endTime,
        cr.isUserAdded AS record_isUserAdded
        
        FROM courses c
        JOIN course_records cr ON cr.courseId = c.id
        WHERE cr.startTime >= :start AND cr.endTime <= :end
        ORDER BY cr.startTime ASC
    """
    )
    suspend fun getCoursesWithRecordInfo(
        start: LocalDateTime,
        end: LocalDateTime,
    ): List<CourseAndRecord>

    @Query(
        """
    SELECT 
        c.id AS id,
        c.name AS name,
        c.teacherName AS teacherName,
        c.classroomName AS classroomName,
        c.credits AS credits,
        c.isDegreeRequired AS isDegreeRequired,
        c.isExaminable AS isExaminable,
        c.isUserAdded AS isUserAdded,
        
        cr.id AS record_id,
        cr.courseId AS record_courseId,
        cr.startTime AS record_startTime,
        cr.endTime AS record_endTime,
        cr.isUserAdded AS record_isUserAdded
        
        FROM courses c
        JOIN course_records cr ON cr.courseId = c.id
        WHERE cr.startTime >= :start AND cr.endTime <= :end
        ORDER BY cr.startTime ASC
    """
    )
    fun getCoursesWithRecordInfoFlow(
        start: LocalDateTime,
        end: LocalDateTime,
    ): Flow<List<CourseAndRecord>>


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: CourseRecordEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<CourseRecordEntity>)

    @Delete
    suspend fun delete(item: CourseRecordEntity)
}

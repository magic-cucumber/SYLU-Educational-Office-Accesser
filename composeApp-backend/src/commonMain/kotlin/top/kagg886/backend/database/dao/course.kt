package top.kagg886.backend.database.dao

import androidx.room3.*
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDateTime
import top.kagg886.backend.database.converters.SeverityConverter
import top.kagg886.backend.database.converters.TimeConverter

@Entity(tableName = "courses")
data class CourseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long? = null,
    val name: String,
    val teacherName: String,
    val classroomName: String,
    val credits: Float,
    val isDegreeRequired: Boolean,
    val isExaminable: Boolean,
    val isUserAdded: Boolean = false
)

@Dao
@ColumnTypeConverters(TimeConverter::class)
interface CourseDao {
    /**
     * 清理校历范围外的所有课程，以及校历范围内的非自定义课程。
     * 最终仅保留至少有一条记录位于校历范围内的自定义课程。
     */
    @Query("""
        DELETE FROM courses
        WHERE isUserAdded = 0
           OR NOT EXISTS (
            SELECT 1
            FROM course_records
            WHERE course_records.courseId = courses.id
              AND course_records.startTime >= :start
              AND course_records.endTime <= :end
        )
    """)
    suspend fun clear(start: LocalDateTime,end: LocalDateTime)

    @Query("DELETE FROM courses")
    suspend fun clearAll()

    @Query("SELECT * FROM courses WHERE (:onlyUserAdded = false OR isUserAdded = true)")
    suspend fun all(onlyUserAdded: Boolean = false): List<CourseEntity>

    @Query("SELECT * FROM courses WHERE (:onlyUserAdded = false OR isUserAdded = true)")
    fun allFlow(onlyUserAdded: Boolean = false): Flow<List<CourseEntity>>

    @Insert
    suspend fun insert(item: CourseEntity): Long

    @Update
    suspend fun update(item: CourseEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<CourseEntity>)

    @Query("SELECT * FROM courses WHERE id = :courseId")
    suspend fun getById(courseId: Long): CourseEntity

    @Delete
    suspend fun delete(item: CourseEntity)
}

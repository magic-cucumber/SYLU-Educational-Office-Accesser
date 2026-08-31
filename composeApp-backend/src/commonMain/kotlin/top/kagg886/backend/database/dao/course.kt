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
     * 此清理函数会输入校历的起点和终点（此范围下称本学期），然后会清理非本学期的所有课程。
     * 由于课程本身不涉及到时间信息（时间信息在course-record表中），因此需要联表来筛选 **全部** 不在本学期的course-record并删除。
     * 换言之，只要record有一个在本学期内，就不执行删除操作。
     */
    @Query("""
        DELETE FROM courses
        WHERE NOT EXISTS (
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

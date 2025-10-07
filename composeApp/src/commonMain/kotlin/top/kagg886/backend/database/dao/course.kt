package top.kagg886.backend.database.dao

import androidx.room.*

@Entity(tableName = "courses")
data class CourseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long? = null,
    val name: String,
    val teacherName: String,
    val classroomName: String,
    val credits: Float,
    val isDegreeRequired: Boolean,
    val isExaminable: Boolean,
    val yearCode: String,
    val semesterCode: String,

    val isUserAdded: Boolean = false
)

@Dao
interface CourseDao {
    /**
     * xnm和xqm传当前的学期，会删除以下内容：
     * 1. 学年名和学期名相同的非自定义课程
     * 2. 学年名和学期名不相同的自定义课程
     */
    @Query("""
        DELETE FROM courses
        WHERE 
        -- 情况1：非自定义且学年、学期匹配
        (isUserAdded = 0 AND yearCode = :xnm AND semesterCode = :xqm)
        OR
        -- 情况2：自定义且学年或学期不匹配
        (isUserAdded = 1 AND (yearCode != :xnm OR semesterCode != :xqm))
    """)
    suspend fun clear(xnm: String, xqm: String)

    @Query("DELETE FROM courses")
    suspend fun clearAll()

    @Query("SELECT * FROM courses WHERE (:onlyUserAdded = false OR isUserAdded = true)")
    suspend fun all(onlyUserAdded: Boolean = false): List<CourseEntity>

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


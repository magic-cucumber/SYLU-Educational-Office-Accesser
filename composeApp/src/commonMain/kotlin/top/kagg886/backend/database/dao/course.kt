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

    val yearCode: String,
    val semesterCode: String,

    val isUserAdded: Boolean = false
)

@Dao
interface CourseDao {
    @Query("DELETE FROM courses WHERE (yearCode != :xnm OR semesterCode != :xqm)")
    suspend fun cleanAndKeep(xnm: String, xqm: String)

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


package top.kagg886.backend.database.dao

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

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
    @Query("DELETE FROM courses WHERE isUserAdded = false AND yearCode = :xnm AND semesterCode = :xqm")
    suspend fun clear(xnm:String, xqm:String)

    @Query("SELECT * FROM courses")
    suspend fun all(): List<CourseEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: CourseEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<CourseEntity>)

    @Query("SELECT * FROM courses WHERE id = :courseId")
    suspend fun getById(courseId: Long): CourseEntity
}


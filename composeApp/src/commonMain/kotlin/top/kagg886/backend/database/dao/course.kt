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
    val teacherName: String,
    val classroomName: String,
    val credits: Float,
    val isDegreeRequired: Boolean,
    val isUserAdded: Boolean = false
)

@Dao
interface CourseDao {
    @Query("DELETE FROM courses WHERE isUserAdded = false")
    suspend fun clear()

    @Query("SELECT * FROM courses")
    suspend fun allFlow(): Flow<List<CourseEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: CourseEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<CourseEntity>)
}


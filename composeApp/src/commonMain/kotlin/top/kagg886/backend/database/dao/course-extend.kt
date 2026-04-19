package top.kagg886.backend.database.dao

import androidx.room.*

/**
 * ================================================
 * Author:     886kagg
 * Created on: 2025/8/5 16:43
 * ================================================
 */

@Entity("courses_extend")
data class CourseExtendEntity(
    @PrimaryKey(autoGenerate = true) val id: Long? = null,
    val name: String,
    val teacherName: String,

    val weekNumber: Int,

    val yearCode: String,
    val semesterCode: String,
)

@Dao
interface CourseExtendDao {
    @Query("DELETE FROM courses_extend WHERE yearCode = :xnm AND semesterCode = :xqm")
    suspend fun clear(xnm: String, xqm: String)

    @Query("DELETE FROM courses_extend")
    suspend fun clearAll()

    @Query("SELECT * FROM courses_extend WHERE (:weekNumber IS NULL OR weekNumber = :weekNumber)")
    suspend fun all(weekNumber: Int? = null): List<CourseExtendEntity>

    @Query("SELECT COUNT(*) FROM courses_extend WHERE (:weekNumber IS NULL OR weekNumber = :weekNumber)")
    suspend fun count(weekNumber: Int? = null): Int

    @Insert
    suspend fun insert(item: CourseExtendEntity): Long

    @Update
    suspend fun update(item: CourseExtendEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<CourseExtendEntity>)

    @Query("SELECT * FROM courses_extend WHERE id = :courseId")
    suspend fun getById(courseId: Long): CourseExtendEntity

    @Delete
    suspend fun delete(item: CourseExtendEntity)
}

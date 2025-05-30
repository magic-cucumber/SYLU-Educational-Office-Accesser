package top.kagg886.backend.database.dao

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.TypeConverters
import top.kagg886.backend.database.converters.ExamConverter
import top.kagg886.sylu_eoa.api.v2.bean.ExamItem
import top.kagg886.sylu_eoa.api.v2.bean.ExamStatus

@Entity(tableName = "exams")
@TypeConverters(ExamConverter::class)
data class ExamEntity(
    @PrimaryKey(autoGenerate = true) val id: Long? = null,
    val year: String, //学年代号
    val semester: String, //学期代号
    val detailsID: String,//详情id
    val name: String, //课程名
    val teacherName: String, //教师名
    val credit: Double, //学分
    val gradePoint: Double, //绩点
    val absoluteScore: String, //评分
    val relateScore: String, //评价

    val status: ExamStatus, //过，挂，重修
    val degree: Boolean, //是否学位,

    val detail: List<List<String>>, //详细表单
)


@Dao
interface ExamDao {
    @Query("DELETE FROM exams")
    suspend fun clear()

    @Query(
        """
            SELECT * FROM exams
            WHERE 
                (:filterPassType IS NULL OR status = :filterPassType) AND
                (:filterDegree IS NULL OR degree = :filterDegree)
        """
    )
    suspend fun all(
        filterPassType: ExamStatus? = null,
        filterDegree: Boolean? = null
    ): List<ExamEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: ExamEntity)
}

fun ExamItem.toEntity(detail: List<List<String>>) = ExamEntity(
    year = year,
    semester = semester,
    detailsID = detailsID,
    name = name,
    teacherName = teacher,
    credit = credit.toDouble(),
    gradePoint = gradePoint.toDouble(),
    absoluteScore = absoluteScore,
    relateScore = relateScore,
    status = examStatus,
    degree = degreeProgram,
    detail = detail
)

fun ExamEntity.toItem() = ExamItem(
    year = year,
    semester = semester,
    detailsID = detailsID,
    name = name,
    teacher = teacherName,
    credit = credit.toString(),
    gradePoint = gradePoint.toString(),
    crTimesGp = (credit * gradePoint).toString(),
    absoluteScore = absoluteScore,
    relateScore = relateScore,
    completionCode = "",
    _degreeProgram = "",
)

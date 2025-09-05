package top.kagg886.backend.database.dao

import androidx.room.*
import kotlinx.datetime.LocalDateTime
import top.kagg886.backend.database.converters.ExamConverter
import top.kagg886.backend.database.converters.TimeConverter
import top.kagg886.sylu_eoa.api.v2.bean.ExamItem
import top.kagg886.sylu_eoa.api.v2.bean.ExamStatus

@Entity(tableName = "exams")
@TypeConverters(ExamConverter::class, TimeConverter::class)
data class ExamEntity(
    @PrimaryKey(autoGenerate = true) val id: Long? = null,
    val year: String, //学年代号
    val semester: String, //学期代号
    val courseID: String, //课程id
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
    val submitTeacherName:  String, //提交教师名
    val submitTime: LocalDateTime, //提交时间,
)


@Dao
interface ExamDao {
    @Query("DELETE FROM exams")
    suspend fun clear()

    @Query(
        """
            SELECT * FROM exams
            WHERE 
                ((:keyword IS NULL OR name LIKE '%' || :keyword || '%') OR (:keyword IS NULL OR teacherName LIKE '%' || :keyword || '%')) AND
                (:filterPassType IS NULL OR status = :filterPassType) AND
                (:filterDegree IS NULL OR degree = :filterDegree) AND
                (:yearCode IS NULL OR year = :yearCode) AND
                (:xqmCode IS NULL OR semester = :xqmCode)
        """
    )
    suspend fun all(
        keyword: String? = null,
        filterPassType: ExamStatus? = null,
        filterDegree: Boolean? = null,
        yearCode:String? = null,
        xqmCode:String? = null
    ): List<ExamEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: ExamEntity)

    @Query("SELECT * FROM exams WHERE id = :id")
    suspend fun getById(id: Long): ExamEntity?

    @Query("SELECT * FROM exams WHERE courseID = :courseID ORDER BY submitTime ASC")
    suspend fun getTimeLineByCourseId(courseID: String): List<ExamEntity>?
}

fun ExamItem.toEntity(detail: List<List<String>>) = ExamEntity(
    year = year,
    semester = semester,
    courseID = courseID,
    detailsID = detailsID,
    name = name,
    teacherName = teacher,
    credit = credit.toDouble(),
    gradePoint = gradePoint.toDouble(),
    absoluteScore = absoluteScore,
    relateScore = relateScore,
    status = examStatus,
    degree = degreeProgram,
    detail = detail,
    submitTeacherName = recommender,
    submitTime = submitTime,
)

fun ExamEntity.toItem() = ExamItem(
    year = year,
    semester = semester,
    courseID = courseID,
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
    submitTime = submitTime,
    recommender = submitTeacherName,
)

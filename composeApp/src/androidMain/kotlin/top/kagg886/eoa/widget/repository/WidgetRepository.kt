package top.kagg886.eoa.widget.repository

import co.touchlab.kermit.Severity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.*
import kotlin.time.Clock
import top.kagg886.backend.config.AppSyncMMKV
import top.kagg886.backend.database.dao.AppLog
import top.kagg886.backend.database.databaseBuilder
import top.kagg886.util.calculateWeekNumber
import top.kagg886.util.getPeriodNumber
import top.kagg886.util.getTimeByLessonNumber

/**
 * 小组件数据仓库，封装数据库访问逻辑
 */
class WidgetRepository() {

    private val database by lazy {
        databaseBuilder().build()
    }
    private val courseRecordDao by lazy { database.courseRecordDao() }

    val logDao by lazy { database.appLogDao() }

    /**
     * 获取今日课程
     */
    suspend fun getTodayCourses(): Result<List<TodayClass>> = withContext(Dispatchers.IO) {
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())

        val (isInHoliday, isBeforeInTerm, currentWeek) = AppSyncMMKV.calender!!.calculateWeekNumber()

        if (currentWeek == -1) {
            return@withContext when {
                isInHoliday -> Result.failure(
                    IllegalStateException("享受假期吧!")
                )

                isBeforeInTerm -> Result.failure(
                    IllegalStateException("准备开学吧!")
                )

                else -> throw IllegalStateException("")
            }
        }

        //获取今天的课表计划
        val plan = courseRecordDao.getCoursesWithRecordInfo(
            weekNumber = currentWeek,
            dayOfWeek = today.dayOfWeek.isoDayNumber,
        )

        if (plan.isEmpty()) {
            return@withContext Result.failure(
                IllegalStateException("今日无课程!")
            )
        }
        //将课表计划和课表信息合并
        val period = today.time.getPeriodNumber()
        val progress = period?.let {
            val (start, end) = getTimeByLessonNumber(it)
            val all = end.toSecondOfDay() - start.toSecondOfDay()
            val current = today.time.toSecondOfDay() - start.toSecondOfDay()
            current.toFloat() / all.toFloat()
        }

        Result.success(
            plan.groupBy { it.course }.flatMap { (course, records) ->
                records.map { record ->
                    TodayClass(
                        name = course.name,
                        teacher = course.teacherName,
                        location = course.classroomName,
                        date = getTimeByLessonNumber(record.record.periodOfDay),
                        recordId = record.record.id!!,
                        courseId = course.id!!,
                        period = record.record.periodOfDay,
                        progress = if (period == record.record.periodOfDay) progress else null
                    )
                }
            }
        )
    }

    suspend fun log(severity: Severity, tag: String, msg: String, e: Throwable? = null) = logDao.insert(
        AppLog(
            tag = tag,
            level = severity,
            message = msg,
            time = Clock.System.now(),
            stacktrace = e?.stackTraceToString()
        )
    )
}

data class TodayClass(
    val recordId: Long,
    val courseId: Long,
    val name: String,
    val teacher: String,
    val location: String,
    val date: Pair<LocalTime, LocalTime>,
    val period: Int,

    val progress: Float? = null
)

package top.kagg886.eoa.widget.repository

import co.touchlab.kermit.Severity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.datetime.*
import kotlin.time.Clock
import top.kagg886.backend.config.AppSyncMMKV
import top.kagg886.backend.database.AppDatabase
import top.kagg886.backend.database.databasePath
import top.kagg886.backend.database.dao.AppLog
import top.kagg886.backend.database.databaseBuilder
import top.kagg886.util.asTaggedLogger
import top.kagg886.util.calculateWeekNumber
import top.kagg886.util.dataPath
import top.kagg886.util.getPeriodNumber
import top.kagg886.util.getTimeByLessonNumber

/**
 * 小组件数据仓库，封装数据库访问逻辑
 */
class WidgetRepository(private val database: AppDatabase) {
    private val logger = "WidgetRepository".asTaggedLogger
    private val courseRecordDao by lazy { database.courseRecordDao() }

    val logDao by lazy { database.appLogDao() }

    /**
     * 获取今日课程
     */
    @Throws(IllegalStateException::class)
    suspend fun getTodayCourses(): List<TodayClass> = withContext(Dispatchers.IO) {
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        logger.i("getTodayCourses start, now=$today, timezone=${TimeZone.currentSystemDefault()}, dataPath=$dataPath, databasePath=$databasePath")

        val profile = AppSyncMMKV.profile
        val picker = AppSyncMMKV.picker
        val calender = AppSyncMMKV.calender
        logger.i(
            "AppSync snapshot: profile=${profile?.name ?: "null"}, picker=${picker != null}, " +
                    "calender=${calender?.let { "${it.start}..${it.end}" } ?: "null"}"
        )

        val (isInHoliday, isBeforeInTerm, currentWeek) = calender?.calculateWeekNumber() ?: run {
            logger.w("AppSync calender is null, widget cannot calculate current week")
            throw IllegalStateException("请先同步数据")
        }
        logger.i("week calculation result: isInHoliday=$isInHoliday, isBeforeInTerm=$isBeforeInTerm, currentWeek=$currentWeek")

        if (currentWeek == -1) {
            when {
                isInHoliday -> {
                    logger.i("currentWeek=-1 because today is in holiday")
                    throw IllegalStateException("享受假期吧!")
                }

                isBeforeInTerm -> {
                    logger.i("currentWeek=-1 because today is before term")
                    throw IllegalStateException("准备开学吧!")
                }

                else -> {
                    logger.e("currentWeek=-1 with no known reason")
                    throw IllegalStateException("")
                }
            }
        }

        //获取今天的课表计划
        logger.i("query course plan: weekNumber=$currentWeek, dayOfWeek=${today.dayOfWeek.isoDayNumber}")
        val plan = courseRecordDao.getCoursesWithRecordInfo(
            weekNumber = currentWeek,
            dayOfWeek = today.dayOfWeek.isoDayNumber,
        )
        logger.i(
            "query course plan result: count=${plan.size}, " +
                    "records=${
                        plan.take(5).joinToString { "${it.course.name}#${it.record.id}@period${it.record.periodOfDay}" }
                    }"
        )

        if (plan.isEmpty()) {
            val allRecordCount = courseRecordDao.all().size
            logger.w("course plan is empty for today, totalCourseRecordCount=$allRecordCount")
            throw IllegalStateException("今日无课程!")
        }
        //将课表计划和课表信息合并
        val period = today.time.getPeriodNumber()
        logger.i("current period=$period, currentTime=${today.time}")
        val progress = period?.let {
            val (start, end) = getTimeByLessonNumber(it)
            val all = end.toSecondOfDay() - start.toSecondOfDay()
            val current = today.time.toSecondOfDay() - start.toSecondOfDay()
            current.toFloat() / all.toFloat()
        }

        plan
            .map { (course, record) ->
                TodayClass(
                    weekNumber = currentWeek,
                    name = course.name,
                    teacher = course.teacherName,
                    location = course.classroomName,
                    date = getTimeByLessonNumber(record.periodOfDay),
                    recordId = record.id!!,
                    courseId = course.id!!,
                    period = record.periodOfDay,
                    progress = if (period == record.periodOfDay) progress else null
                )
            }
            .groupBy { it.date }
            .map { (_, classes) ->
                if (classes.size == 1) {
                    classes.single()
                } else {
                    val sample = classes.first()
                    sample.copy(
                        name = "冲突课程 (${classes.size}) 门",
                        teacher = "",
                        location = "",
                        conflict = true
                    )
                }
            }
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
    val weekNumber: Int,
    val recordId: Long,
    val courseId: Long,
    val name: String,
    val teacher: String,
    val location: String,
    val date: Pair<LocalTime, LocalTime>,
    val period: Int,

    val progress: Float? = null,
    val conflict: Boolean = false,
)

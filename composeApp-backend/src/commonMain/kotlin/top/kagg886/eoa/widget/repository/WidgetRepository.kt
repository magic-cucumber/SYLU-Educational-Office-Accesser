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
        val timeZone = TimeZone.currentSystemDefault()
        val today = Clock.System.now().toLocalDateTime(timeZone)
        logger.i("getTodayCourses start, now=$today, timezone=$timeZone, dataPath=$dataPath, databasePath=$databasePath")

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

        // 查询今天零点至次日零点的全部课程，包含当前正在进行的课程。
        val startOfDay = today.date.atTime(0, 0)
        val endOfDay = today.date.plus(1, DateTimeUnit.DAY).atTime(0, 0)
        logger.i("query course plan: weekNumber=$currentWeek, range=$startOfDay..$endOfDay")
        val plan = courseRecordDao.getCoursesWithRecordInfo(
            start = startOfDay,
            end = endOfDay,
        )
        logger.i(
            "query course plan result: count=${plan.size}, " +
                    "records=${plan.take(5)}"
        )

        if (plan.isEmpty()) {
            val allRecordCount = courseRecordDao.all().size
            logger.w("course plan is empty for today, totalCourseRecordCount=$allRecordCount")
            throw IllegalStateException("今日无课程!")
        }

        val now = today.toInstant(timeZone).toEpochMilliseconds()

        val result = plan
            // 将课程计划转换为当天课程对象
            .map { (course, record) ->
                TodayClass(
                    name = course.name,
                    teacher = course.teacherName,
                    location = course.classroomName,
                    date = record.startTime to record.endTime,
                    recordId = record.id!!,
                    courseId = course.id!!,
                )
            }
            // 将每门课程展开为开始、结束两个扫描事件
            .flatMap { course ->
                listOf(
                    course.date.first to (EventType.START to course),
                    course.date.second to (EventType.END to course),
                )
            }
            // 将同一时间发生的扫描事件聚合到一起
            .groupBy(
                keySelector = { it.first },
                valueTransform = { it.second },
            )
            // 转为惰性序列
            .asSequence()
            // 沿时间轴扫描，并维护当前正在进行的课程
            .runningFold(SweepState()) { state, (time, events) ->
                SweepState(
                    time = time,
                    active = buildSet {
                        addAll(state.active)

                        // 移除当前时间点结束的课程
                        events
                            .asSequence()
                            .filter { it.first == EventType.END }
                            .map { it.second }
                            .forEach(::remove)

                        // 加入当前时间点开始的课程
                        events
                            .asSequence()
                            .filter { it.first == EventType.START }
                            .map { it.second }
                            .forEach(::add)
                    },
                )
            }
            // 将相邻扫描状态组合成时间区间
            .zipWithNext()
            // 转换为最终课程区间
            .mapNotNull { (previous, current) ->
                val start = previous.time ?: return@mapNotNull null
                val end = current.time ?: return@mapNotNull null

                previous.active
                    .takeIf { it.isNotEmpty() && start < end }
                    ?.let { active ->
                        val startMillis = start.toInstant(timeZone).toEpochMilliseconds()
                        val endMillis = end.toInstant(timeZone).toEpochMilliseconds()

                        val progress = now
                            .takeIf { it in startMillis..<endMillis }
                            ?.let {
                                (it - startMillis).toFloat() /
                                        (endMillis - startMillis).toFloat()
                            }

                        active.first().let { sample ->
                            if (active.size == 1) {
                                sample.copy(
                                    date = start to end,
                                    progress = progress,
                                )
                            } else {
                                sample.copy(
                                    name = "冲突课程 (${active.size}) 门",
                                    teacher = "",
                                    location = "",
                                    date = start to end,
                                    progress = progress,
                                    conflict = true,
                                )
                            }
                        }
                    }
            }
            // 收集结果
            .toList()
        result
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
    val date: Pair<LocalDateTime, LocalDateTime>,

    val progress: Float? = null,
    val conflict: Boolean = false,
)

private enum class EventType {
    START,END
}

private data class SweepState(
    val time: LocalDateTime? = null,
    val active: Set<TodayClass> = emptySet(),
)

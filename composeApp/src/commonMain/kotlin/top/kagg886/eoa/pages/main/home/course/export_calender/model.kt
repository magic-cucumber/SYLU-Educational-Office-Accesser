package top.kagg886.eoa.pages.main.home.course.export_calender

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalTime
import kotlinx.datetime.atTime
import kotlinx.datetime.plus
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.annotation.OrbitExperimental
import org.orbitmvi.orbit.viewmodel.container
import top.kagg886.backend.config.AppInitializeMMKV
import top.kagg886.backend.config.AppSyncMMKV
import top.kagg886.backend.database.AppDatabase
import top.kagg886.calendar.v2.CalendarManager
import top.kagg886.calendar.v2.state.Event
import top.kagg886.eoa.util.SnackBarType
import top.kagg886.util.calculateWeekNumber
import top.kagg886.util.getTimeByLessonNumber
import top.kagg886.util.logger
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class CourseExportCalenderModel(
    database: AppDatabase
) : ViewModel(), ContainerHost<CourseExportCalenderState, CourseExportCalenderSideEffect> {
    private val dao = database.courseRecordDao()
    override val container =
        container<CourseExportCalenderState, CourseExportCalenderSideEffect>(CourseExportCalenderState("即将开始导出..."))

    @OptIn(OrbitExperimental::class, ExperimentalUuidApi::class)
    fun exportCalender(manager: CalendarManager) = intent {
        val term = AppSyncMMKV.picker!!.default.asDisplay()
        val calendar = manager.getCalendar(AppInitializeMMKV.calendarId)
            ?: manager.createCalendar("SYLU-EOA ${term.xnm}年度${term.xqm}学期课程表")
                .apply { AppInitializeMMKV.calendarId = this.id }
        val schoolCalender = AppSyncMMKV.calender!!

        reduce {
            CourseExportCalenderState(
                message = "清空原有日程..."
            )
        }
        logger.i("开始清空日程")

        calendar.transaction {
            val events = calendar.getEvents(
                start = schoolCalender.start.atTime(LocalTime.fromSecondOfDay(0)),
                end = schoolCalender.end.plus(1, DateTimeUnit.DAY).atTime(LocalTime.fromSecondOfDay(0))
            )
            events.mapNotNull { it.id }.forEach { delete(it) }
        }

        reduce {
            CourseExportCalenderState(
                message = "解析数据库..."
            )
        }
        logger.i("开始解析数据库")

        val (isInHoliday, isBeforeInTerm, weekNumber) = schoolCalender.calculateWeekNumber()

        if (weekNumber == -1) {
            when {
                isInHoliday -> postSideEffect(CourseExportCalenderSideEffect.NavigateBack("当前正在放假，不需要导出数据"))
                isBeforeInTerm -> postSideEffect(CourseExportCalenderSideEffect.NavigateBack("请等待开学后再进行导出"))
            }
            return@intent
        }

        reduce {
            CourseExportCalenderState(
                message = "准备课程..."
            )
        }
        logger.i("准备生成日历事件")

        val days = (0 until schoolCalender.count()).flatMap { weekIdx ->
            (0 until 7).map { dayIdx ->
                weekIdx to dayIdx
            }
        }


        //异步计算减少主线程压力
        val dayCourses = days.map { (weekIdx, dayIdx) ->
            viewModelScope.async(Dispatchers.IO) {
                val startDate = schoolCalender.start
                    .plus(weekIdx, DateTimeUnit.WEEK)
                    .plus(dayIdx, DateTimeUnit.DAY)

                startDate to dao.getCoursesWithRecordInfo(
                    weekNumber = weekIdx + 1,
                    dayOfWeek = dayIdx + 1
                )
            }
        }.awaitAll()

        val events = withContext(Dispatchers.Default) {
            dayCourses.flatMap { (startDate, courses) ->
                courses.map { course ->
                    val (startTime, endTime) = getTimeByLessonNumber(course.record.periodOfDay)

                    Event(
                        id = Uuid.random().toHexString(),
                        title = course.course.name,
                        startTime = startDate.atTime(startTime),
                        endTime = startDate.atTime(endTime),
                        description = """
                            1. 任课教师: ${course.course.teacherName}
                            2. 课程属性: ${if (course.course.isDegreeRequired) "必修" else "选修"}
                            3. 学分: ${course.course.credits}
                            4. 属于系统课程: ${if (course.course.isUserAdded) "是" else "否"}
                        """.trimIndent(),
                        location = course.course.classroomName,
                    )
                }
            }
        }

        reduce {
            CourseExportCalenderState(
                message = "写入日程..."
            )
        }

        logger.i("开始写入日程")

        calendar.transaction {
            events.forEach { event ->
                create(event)
            }
        }
        postSideEffect(CourseExportCalenderSideEffect.NavigateBack("导出成功", SnackBarType.Success))
    }
}

data class CourseExportCalenderState(
    val message: String
)

sealed interface CourseExportCalenderSideEffect {
    data class NavigateBack(val msg: String, val type: SnackBarType = SnackBarType.Info) :
        CourseExportCalenderSideEffect
}

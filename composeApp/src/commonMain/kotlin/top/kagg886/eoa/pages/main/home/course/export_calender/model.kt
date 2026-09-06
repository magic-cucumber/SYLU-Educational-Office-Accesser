package top.kagg886.eoa.pages.main.home.course.export_calender

import top.kagg886.eoa.util.BaseViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalTime
import kotlinx.datetime.atTime
import kotlinx.datetime.plus
import org.orbitmvi.orbit.syntax.Syntax
import org.orbitmvi.orbit.annotation.OrbitExperimental
import top.kagg886.backend.config.AppInitializeMMKV
import top.kagg886.backend.config.AppSyncMMKV
import top.kagg886.backend.database.AppDatabase
import top.kagg886.calendar.v2.CalendarManager
import top.kagg886.calendar.v2.state.Event
import top.kagg886.eoa.util.SnackBarType
import top.kagg886.util.calculateWeekNumber
import top.kagg886.util.getTimeByLessonNumber
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class CourseExportCalenderModel(
    database: AppDatabase
) : BaseViewModel<CourseExportCalenderState, CourseExportCalenderSideEffect>(
    name = "CourseExportCalenderModel",
    initial = CourseExportCalenderState("即将开始导出...")
) {
    private val dao = database.courseRecordDao()
    override suspend fun Syntax<CourseExportCalenderState, CourseExportCalenderSideEffect>.init() =
        Unit

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
                end = schoolCalender.end.plus(1, DateTimeUnit.DAY)
                    .atTime(LocalTime.fromSecondOfDay(0))
            )
            events.mapNotNull { it.id }.forEach { delete(it) }
        }

        reduce {
            CourseExportCalenderState(
                message = "解析数据库..."
            )
        }
        logger.i("开始解析数据库")

        reduce {
            CourseExportCalenderState(
                message = "准备课程..."
            )
        }
        logger.i("准备生成日历事件")

        val courses = dao.getCoursesWithRecordInfo(
            start = schoolCalender.start.atTime(0, 0),
            end = schoolCalender.end.plus(1, DateTimeUnit.DAY).atTime(0, 0)
        )

        val events = withContext(Dispatchers.Default) {
            courses.map { course ->
                val startTime = course.record.startTime
                val endTime = course.record.endTime

                Event(
                    id = Uuid.random().toHexString(),
                    title = course.course.name,
                    startTime = startTime,
                    endTime = endTime,
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
        postSideEffect(
            CourseExportCalenderSideEffect.NavigateBack(
                "导出成功",
                SnackBarType.Success
            )
        )
    }
}

data class CourseExportCalenderState(
    val message: String
)

sealed interface CourseExportCalenderSideEffect {
    data class NavigateBack(val msg: String, val type: SnackBarType = SnackBarType.Info) :
        CourseExportCalenderSideEffect
}

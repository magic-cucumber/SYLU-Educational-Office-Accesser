package top.kagg886.eoa.pages.main.home.course.export_calender

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.atTime
import kotlinx.datetime.plus
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.annotation.OrbitExperimental
import org.orbitmvi.orbit.viewmodel.container
import top.kagg886.backend.config.AppSyncMMKV
import top.kagg886.backend.database.AppDatabase
import top.kagg886.calender.data.Event
import top.kagg886.eoa.util.SnackBarType
import top.kagg886.util.getTimeByLessonNumber
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class CourseExportCalenderModel(
    database: AppDatabase
) : ViewModel(), ContainerHost<CourseExportCalenderState, CourseExportCalenderSideEffect> {
    private val dao = database.courseRecordDao()
    override val container = container<CourseExportCalenderState, CourseExportCalenderSideEffect>(CourseExportCalenderState("即将开始导出..."))

    @OptIn(OrbitExperimental::class, ExperimentalUuidApi::class)
    fun exportCalender(events: MutableList<Event>) = intent {
        reduce {
            CourseExportCalenderState(
                message = "清空原有日程..."
            )
        }
        events.clear()

        reduce {
            CourseExportCalenderState(
                message = "解析数据库..."
            )
        }

        val calendar = AppSyncMMKV.calender!!

        if (calendar.currentWeek() == -1) {
            postSideEffect(CourseExportCalenderSideEffect.NavigateBack("当前未处于学期内，请等待开学后再进行导出"))
            return@intent
        }

        val map = (1..calendar.count()).map { weekNumber ->
            viewModelScope.async {
                (1..7).map { dayOfWeek ->
                    async {
                        dao.getCoursesWithRecordInfo(
                            weekNumber = weekNumber,
                            dayOfWeek = dayOfWeek
                        )
                    }
                }.awaitAll()
            }
        }.awaitAll()

        reduce {
            CourseExportCalenderState(
                message = "写入日程..."
            )
        }
        val count = map.flatten().flatten().size
        var index = 0
        for ((weekIdx, weekCourses) in map.withIndex()) {
            for ((dayOfWeek, dayCourses) in weekCourses.withIndex()) {
                val startDate = calendar.start
                    .plus(weekIdx, DateTimeUnit.WEEK)
                    .plus(dayOfWeek, DateTimeUnit.DAY)

                for (course in dayCourses) {
                    val (startTime, endTime) = getTimeByLessonNumber(course.record.periodOfDay)

                    events.add(
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
                    )

                    reduce {
                        CourseExportCalenderState(
                            message = "写入课程... ${++index} / $count"
                        )
                    }
                }
            }
        }
    }
}

data class CourseExportCalenderState(
    val message: String
)

sealed interface CourseExportCalenderSideEffect {
    data class NavigateBack(val msg: String, val type: SnackBarType = SnackBarType.Info) :
        CourseExportCalenderSideEffect
}

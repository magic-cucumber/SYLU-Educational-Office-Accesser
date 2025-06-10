package top.kagg886.eoa.pages.main.home.course.export

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.openFileSaver
import io.github.vinceglb.filekit.writeString
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.atTime
import kotlinx.datetime.plus
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.annotation.OrbitExperimental
import org.orbitmvi.orbit.viewmodel.container
import top.kagg886.backend.config.AppSyncMMKV
import top.kagg886.backend.database.AppDatabase
import top.kagg886.eoa.util.SnackBarType
import top.kagg886.ics.data.AlarmAction
import top.kagg886.ics.ics
import top.kagg886.util.getTimeByLessonNumber
import kotlin.coroutines.resume
import kotlin.time.Duration.Companion.minutes

class CourseExportModel(
    database: AppDatabase
) : ViewModel(), ContainerHost<CourseExportState, CourseExportSideEffect> {
    private val dao = database.courseRecordDao()
    override val container =
        container<CourseExportState, CourseExportSideEffect>(CourseExportState("正在导出...")) {
            exportICS().join()
        }

    @OptIn(OrbitExperimental::class)
    fun exportICS() = intent {
        reduce {
            CourseExportState("正在获取数据库...")
        }
        val calendar = AppSyncMMKV.calender!!

        if (calendar.currentWeek() == -1) {
            postSideEffect(CourseExportSideEffect.NavigateBack("当前未处于学期内，请等待开学后再进行导出"))
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
            CourseExportState("正在构建ICS文件...")
        }

        val ics = suspendCancellableCoroutine { continuation ->
            ics {
                for ((weekIdx, weekCourses) in map.withIndex()) {
                    for ((dayOfWeek, dayCourses) in weekCourses.withIndex()) {
                        val startDate = calendar.start
                            .plus(weekIdx, DateTimeUnit.WEEK)
                            .plus(dayOfWeek, DateTimeUnit.DAY)

                        for (course in dayCourses) {
                            val (startTime, endTime) = getTimeByLessonNumber(course.record.periodOfDay)

                            event {
                                summary(course.course.name)
                                description(course.course.classroomName)

                                startTime(startDate.atTime(startTime))
                                endTime(startDate.atTime(endTime))

                                alarm {
                                    action(AlarmAction.AUDIO)
                                    trigger(30.minutes)
                                    description("${course.course.name} 即将开始")
                                }
                            }
                        }
                    }
                }

                writeTo {
                    continuation.resume(it)
                }
            }
        }

        val file = FileKit.openFileSaver(
            suggestedName = "课程表 - ${calendar.start} to ${calendar.end}",
            extension = "ics",
        )
        if (file == null) {
            postSideEffect(CourseExportSideEffect.NavigateBack("用户取消导出"))
            return@intent
        }
        file.writeString(ics)
        postSideEffect(CourseExportSideEffect.NavigateBack("导出成功"))
    }
}

data class CourseExportState(
    val message: String
)

sealed interface CourseExportSideEffect {
    data class NavigateBack(val msg: String, val type: SnackBarType = SnackBarType.Info) :
        CourseExportSideEffect
}
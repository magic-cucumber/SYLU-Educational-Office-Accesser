package top.kagg886.eoa.pages.main.home.course.export_ics

import top.kagg886.eoa.util.BaseViewModel
import androidx.lifecycle.viewModelScope
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.openFileSaver
import io.github.vinceglb.filekit.writeString
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.atTime
import kotlinx.datetime.plus
import org.orbitmvi.orbit.syntax.Syntax
import org.orbitmvi.orbit.annotation.OrbitExperimental
import top.kagg886.backend.config.AppSyncMMKV
import top.kagg886.backend.database.AppDatabase
import top.kagg886.eoa.util.SnackBarType
import top.kagg886.ics.data.AlarmAction
import top.kagg886.ics.ics
import top.kagg886.util.calculateWeekNumber
import top.kagg886.util.getTimeByLessonNumber
import kotlin.coroutines.resume
import kotlin.time.Duration.Companion.minutes

class CourseExportIcsModel(
    database: AppDatabase
) : BaseViewModel<CourseExportIcsState, CourseIcsExportSideEffect>(
    name = "CourseExportIcsModel",
    initial = CourseExportIcsState("正在导出...")
) {
    private val dao = database.courseRecordDao()
    override suspend fun Syntax<CourseExportIcsState, CourseIcsExportSideEffect>.init() {
        exportICS().join()
    }

    @OptIn(OrbitExperimental::class)
    fun exportICS() = intent {
        reduce {
            CourseExportIcsState("正在获取数据库...")
        }
        val calendar = AppSyncMMKV.calender!!

        val map =  dao.getCoursesWithRecordInfo(
            start = calendar.start.atTime(0, 0),
            end = calendar.end.plus(1, DateTimeUnit.DAY).atTime(0, 0)
        )

        reduce {
            CourseExportIcsState("正在构建ICS文件...")
        }

        val ics = suspendCancellableCoroutine { continuation ->
            ics {
                for (course in map) {
                    val startTime = course.record.startTime
                    val endTime = course.record.endTime

                    event {
                        summary(course.course.name)
                        location(course.course.classroomName)
                        description(
                            """
                                        1. 任课教师: ${course.course.teacherName}
                                        2. 课程属性: ${if (course.course.isDegreeRequired) "必修" else "选修"}
                                        3. 学分: ${course.course.credits}
                                        4. 属于系统课程: ${if (course.course.isUserAdded) "是" else "否"}
                                    """.trimIndent()
                        )

                        startTime(startTime)
                        endTime(endTime)

                        alarm {
                            action(AlarmAction.AUDIO)
                            trigger(30.minutes)
                            description("${course.course.name} 即将开始")
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
            defaultExtension = "ics",
        )
        if (file == null) {
            postSideEffect(CourseIcsExportSideEffect.NavigateBack("用户取消导出"))
            return@intent
        }
        file.writeString(ics)
        postSideEffect(CourseIcsExportSideEffect.NavigateBack("导出成功"))
    }
}

data class CourseExportIcsState(
    val message: String
)

sealed interface CourseIcsExportSideEffect {
    data class NavigateBack(val msg: String, val type: SnackBarType = SnackBarType.Info) :
        CourseIcsExportSideEffect
}

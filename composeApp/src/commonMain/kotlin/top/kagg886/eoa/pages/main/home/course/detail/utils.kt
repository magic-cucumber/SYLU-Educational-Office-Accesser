package top.kagg886.eoa.pages.main.home.course.detail

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

data class CourseRecordAndProgress(
    val id: Long,
    val courseId: Long,
    val weekNumber: Int,
    val dayOfWeek: Int,
    val periodOfDay: Int,
    val isUserAdded: Boolean,

    val progressStatus: ProgressStatus,
    val date: LocalDate,
    val start: LocalTime,
    val end:LocalTime
) {
    enum class ProgressStatus {
        NotStarted,
        InProgress,
        Completed,
    }
}
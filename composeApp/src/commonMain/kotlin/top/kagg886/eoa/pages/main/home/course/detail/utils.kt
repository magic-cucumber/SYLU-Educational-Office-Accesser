package top.kagg886.eoa.pages.main.home.course.detail

import kotlinx.datetime.LocalDateTime

data class CourseRecordAndProgress(
    val id: Long,
    val courseId: Long,
    val isUserAdded: Boolean,

    val progressStatus: ProgressStatus,
    val start: LocalDateTime,
    val end:LocalDateTime
) {
    enum class ProgressStatus {
        NotStarted,
        InProgress,
        Completed,
    }
}
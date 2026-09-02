package top.kagg886.eoa.pages.main.home.summary

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime

sealed interface TodayClass {
    val date: Pair<LocalDateTime, LocalDateTime>
    val progress: Flow<Float?>

    data class Single(
        val recordId: Long,
        val courseId: Long,
        val name: String,
        val teacher: String,
        val location: String,
        val isDegreeProgram: Boolean,
        val isExamine: Boolean,

        val fullDate: Pair<LocalDateTime, LocalDateTime>,
        override val date: Pair<LocalDateTime, LocalDateTime>,
        override val progress: Flow<Float?> = MutableStateFlow(null),
    ) : TodayClass

    data class Conflict(
        override val date: Pair<LocalDateTime, LocalDateTime>,
        override val progress: Flow<Float?> = MutableStateFlow(null),

        val data: List<Single>
    ) : TodayClass
}


val TodayClass.Single.hasCourseConflict: Boolean
    get() = date != fullDate
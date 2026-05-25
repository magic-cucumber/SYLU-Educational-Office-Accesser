package top.kagg886.eoa.pages.main.home.summary

import kotlinx.datetime.LocalTime

sealed interface TodayClass {
    val date: Pair<LocalTime, LocalTime>
    val progress: Float?
    data class Single(
        val recordId: Long,
        val courseId: Long,
        val name: String,
        val teacher: String,
        val location: String,
        val isDegreeProgram: Boolean,
        val isExamine: Boolean,

        override val date: Pair<LocalTime, LocalTime>,
        override val progress: Float? = null,
    ): TodayClass

    data class Conflict(
        override val date: Pair<LocalTime, LocalTime>,
        override val progress: Float? = null,

        val data: List<Single>
    ): TodayClass
}

package top.kagg886.eoa.pages.main.home.summary

import kotlinx.datetime.LocalTime

data class TodayClass(
    val recordId: Long,
    val courseId: Long,
    val name: String,
    val teacher: String,
    val location: String,
    val date: Pair<LocalTime, LocalTime>,

    val progress: Float? = null
)

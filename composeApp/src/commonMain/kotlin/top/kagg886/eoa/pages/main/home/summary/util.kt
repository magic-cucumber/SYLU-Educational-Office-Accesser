package top.kagg886.eoa.pages.main.home.summary

import kotlinx.datetime.LocalTime

data class TodayClass(
    val name: String,
    val teacher: String,
    val date: Pair<LocalTime, LocalTime>
)

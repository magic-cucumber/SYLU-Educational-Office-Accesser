package top.kagg886.util

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import top.kagg886.sylu_eoa.api.v2.bean.SchoolCalender

fun getTimeByLessonNumber(dt:Int) : Pair<LocalTime, LocalTime> {
    return when (dt) {
        1 -> LocalTime.parse("08:00") to LocalTime.parse("09:40")
        2 -> LocalTime.parse("10:00") to LocalTime.parse("11:40")
        3 -> LocalTime.parse("13:00") to LocalTime.parse("14:40")
        4 -> LocalTime.parse("14:50") to LocalTime.parse("16:30")
        5 -> LocalTime.parse("16:40") to LocalTime.parse("18:20")
        6 -> LocalTime.parse("18:30") to LocalTime.parse("21:10")
        else -> throw IllegalStateException("no this class")
    }
}

fun SchoolCalender.calculateWeekNumber(date: LocalDate): Int {
    if (date > end) {
        throw IllegalStateException("放假中")
    }
    if (date < start) {
        throw IllegalStateException("开学之前")
    }
    return (date.toEpochDays() - start.toEpochDays()) / 7 + 1
}

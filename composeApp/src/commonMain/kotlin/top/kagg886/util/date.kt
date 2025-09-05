package top.kagg886.util

import kotlinx.datetime.*
import kotlinx.datetime.format.char
import top.kagg886.sylu_eoa.api.v2.bean.SchoolCalender
import kotlinx.datetime.Clock
import kotlinx.datetime.format.DateTimeComponents
import kotlin.time.ExperimentalTime

fun getTimeByLessonNumber(dt: Int): Pair<LocalTime, LocalTime> {
    return when (dt) {
        1 -> LocalTime.parse("08:00") to LocalTime.parse("08:45")
        2 -> LocalTime.parse("08:55") to LocalTime.parse("09:40")
        3 -> LocalTime.parse("10:00") to LocalTime.parse("10:45")
        4 -> LocalTime.parse("10:55") to LocalTime.parse("11:40")
        5 -> LocalTime.parse("13:00") to LocalTime.parse("13:45")
        6 -> LocalTime.parse("13:55") to LocalTime.parse("14:40")
        7 -> LocalTime.parse("14:50") to LocalTime.parse("15:35")
        8 -> LocalTime.parse("15:45") to LocalTime.parse("16:30")
        9 -> LocalTime.parse("16:40") to LocalTime.parse("17:25")
        10 -> LocalTime.parse("17:35") to LocalTime.parse("18:20")
        11 -> LocalTime.parse("19:30") to LocalTime.parse("20:15")
        12 -> LocalTime.parse("20:25") to LocalTime.parse("21:10")
        else -> throw IllegalStateException("no this class")
    }
}

fun LocalTime.getPeriodNumber(): Int? = when (this) {
    in LocalTime.parse("08:00")..LocalTime.parse("08:45") -> 1
    in LocalTime.parse("08:55")..LocalTime.parse("09:40") -> 2
    in LocalTime.parse("10:00")..LocalTime.parse("10:45") -> 3
    in LocalTime.parse("10:55")..LocalTime.parse("11:40") -> 4
    in LocalTime.parse("13:00")..LocalTime.parse("13:45") -> 5
    in LocalTime.parse("13:55")..LocalTime.parse("14:40") -> 6
    in LocalTime.parse("14:50")..LocalTime.parse("15:35") -> 7
    in LocalTime.parse("15:45")..LocalTime.parse("16:30") -> 8
    in LocalTime.parse("16:40")..LocalTime.parse("17:25") -> 9
    in LocalTime.parse("17:35")..LocalTime.parse("18:20") -> 10
    in LocalTime.parse("19:30")..LocalTime.parse("20:15") -> 11
    in LocalTime.parse("20:25")..LocalTime.parse("21:10") -> 12
    else -> null
}

/**
 * 计算当前周数，返回元组。
 *
 * first：是否在放假
 * second：是否开学前
 * third：当前周数
 */
@OptIn(ExperimentalTime::class)
fun SchoolCalender.calculateWeekNumber(date: LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault())): Triple<Boolean, Boolean, Int> {
    if (date > end) {
        return Triple(true, false, -1)
    }
    if (date < start) {
        return Triple(false, true, -1)
    }

    return Triple(false, false, ((date.toEpochDays() - start.toEpochDays()) / 7 + 1).toInt())
}

val ChinaDateFormater = LocalDateTime.Format {
    year()
    char('-')
    monthNumber()
    char('-')
    dayOfMonth()
    char(' ')
    hour()
    char(':')
    minute()
}


val ChinaTimeFormater = DateTimeComponents.Format {
    year()
    char('-')
    monthNumber()
    char('-')
    dayOfMonth()
    char(' ')
    hour()
    char(':')
    minute()
    char(':')
    second()
    char('.')
    secondFraction(3)
}

package top.kagg886.sylu_eoa.api.v2.bean

import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.until
import kotlinx.serialization.Serializable

@Serializable
data class SchoolCalender(
    val start: LocalDate,
    val end: LocalDate
) {

    fun currentWeek(
        now: LocalDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    ): Int {
        if (now < start || now > end) {
            return -1
        }

        var start = start
        var i = 0
        while (start <= now) {
            i++
            start = start.plus(7, DateTimeUnit.DAY)
        }
        return i
    }

    fun count(): Int = start.until(end, DateTimeUnit.WEEK)
}
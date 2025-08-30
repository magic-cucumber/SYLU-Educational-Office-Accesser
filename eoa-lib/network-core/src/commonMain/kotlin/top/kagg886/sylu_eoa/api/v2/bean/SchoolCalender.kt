package top.kagg886.sylu_eoa.api.v2.bean

import kotlinx.datetime.*
import kotlinx.serialization.Serializable
import kotlinx.datetime.Clock
import kotlin.time.ExperimentalTime

@Serializable
data class SchoolCalender(
    val start: LocalDate,
    val end: LocalDate
) {

    @OptIn(ExperimentalTime::class)
    fun currentWeek(
        now: LocalDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    ): Int {
        if (now < start) {
            return -1
        }

        if (now > end) {
            return -2
        }

        var start = start
        var i = 0
        while (start <= now) {
            i++
            start = start.plus(7, DateTimeUnit.DAY)
        }
        return i
    }

    fun count(): Int = (start.until(end, DateTimeUnit.WEEK) + 1).toInt()
}

package top.kagg886.eoa

import kotlinx.datetime.LocalDate
import top.kagg886.sylu_eoa.api.v2.bean.SchoolCalender
import top.kagg886.util.calculateWeekNumber
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CalenderTest {

    @Test
    fun testCalender() {
        val calender = SchoolCalender(
            LocalDate.parse("2025-02-24"),
            LocalDate.parse("2025-07-13"),
        )

        assertEquals(1,calender.calculateWeekNumber(LocalDate.parse("2025-02-24")))
        assertEquals(1,calender.calculateWeekNumber(LocalDate.parse("2025-02-25")))
        assertEquals(1,calender.calculateWeekNumber(LocalDate.parse("2025-03-02")))
        assertEquals(2,calender.calculateWeekNumber(LocalDate.parse("2025-03-03")))
        assertEquals(20,calender.calculateWeekNumber(LocalDate.parse("2025-07-13")))

        assertFailsWith<IllegalStateException>{
            calender.calculateWeekNumber(LocalDate.parse("2025-07-14"))
        }

        assertFailsWith<IllegalStateException> {
            calender.calculateWeekNumber(LocalDate.parse("2024-02-23"))
        }
    }
}

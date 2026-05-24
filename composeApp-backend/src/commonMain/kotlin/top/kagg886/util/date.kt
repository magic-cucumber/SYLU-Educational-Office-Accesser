@file:JvmName("ComposeAppBackendDateKt")
package top.kagg886.util

import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import top.kagg886.sylu_eoa.api.v2.bean.SchoolCalender
import kotlin.jvm.JvmName
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * ================================================
 * Author:     886kagg
 * Created on: 2026/5/24 10:02
 * ================================================
 */
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

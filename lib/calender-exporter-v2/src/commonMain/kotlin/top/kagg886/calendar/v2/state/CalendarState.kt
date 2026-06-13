package top.kagg886.calendar.v2.state

import top.kagg886.calendar.v2.CalendarManager

/**
 * ================================================
 * Author:     886kagg
 * Created on: 2026/6/13 13:02
 * ================================================
 */
sealed interface CalendarState {
    data object Waiting : CalendarState
    data object Processing : CalendarState
    data class Granted(val manager: CalendarManager) : CalendarState
    data class Denied(val permanent: Boolean) : CalendarState
    data object NotSupported : CalendarState
}

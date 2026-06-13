package top.kagg886.calendar.v2

import androidx.compose.runtime.Composable
import top.kagg886.calendar.v2.state.CalendarState

/**
 * ================================================
 * Author:     886kagg
 * Created on: 2026/6/13 13:01
 * ================================================
 */

@Composable
expect fun rememberCalendarManagerState(): CalendarState

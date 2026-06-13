package top.kagg886.calendar.v2

import top.kagg886.calendar.v2.state.CalendarState

@androidx.compose.runtime.Composable
actual fun rememberCalendarManagerState(): CalendarState = CalendarState.NotSupported

package top.kagg886.calender.util

import androidx.compose.runtime.*
import platform.EventKit.EKEntityType
import top.kagg886.calender.EKCalenderManager
import top.kagg886.calender.NativeCalenderManager
import top.kagg886.calender.data.CalenderPermissionGrantType

@Composable
internal actual fun rememberCalenderPermissionRequester(manager: NativeCalenderManager): State<CalenderPermissionGrantType> {
    val state = remember {
        mutableStateOf(CalenderPermissionGrantType.WAIT)
    }

    LaunchedEffect(Unit) {
        val manager = manager as EKCalenderManager
        manager.eventStore.requestAccessToEntityType(EKEntityType.EKEntityTypeEvent) { granted, _ ->

            state.value = when {
                granted -> {
                    manager.initializeCalendar()
                    CalenderPermissionGrantType.ALL_GRANTED
                }
                else -> CalenderPermissionGrantType.DENY_PERMANENT
            }
        }
    }

    return state
}

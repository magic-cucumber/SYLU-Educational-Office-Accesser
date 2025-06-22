package top.kagg886.calender.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
        (manager as EKCalenderManager).eventStore.requestAccessToEntityType(EKEntityType.EKEntityTypeEvent) { granted, _ ->
            state.value = when {
                granted -> CalenderPermissionGrantType.ALL_GRANTED
                else -> CalenderPermissionGrantType.DENY_PERMANENT
            }
        }
    }

    return state
}

package top.kagg886.calender.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import top.kagg886.calender.NativeCalenderManager
import top.kagg886.calender.data.CalenderPermissionGrantType

@Composable
actual fun rememberCalenderPermissionRequester(manager: NativeCalenderManager): State<CalenderPermissionGrantType> {
    return remember {
        mutableStateOf(CalenderPermissionGrantType.NOT_SUPPORTED)
    }
}
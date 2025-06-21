package top.kagg886.calender.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import top.kagg886.calender.NativeCalenderManager
import top.kagg886.calender.data.CalenderPermissionGrantType

@Composable
expect fun rememberCalenderPermissionRequester(manager: NativeCalenderManager): State<CalenderPermissionGrantType>
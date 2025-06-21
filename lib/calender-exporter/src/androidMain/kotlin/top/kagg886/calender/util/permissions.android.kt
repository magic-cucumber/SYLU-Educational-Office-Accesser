package top.kagg886.calender.util

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import top.kagg886.calender.NativeCalenderManager
import top.kagg886.calender.data.CalenderPermissionGrantType

private val CALENDER_PERMISSION = arrayOf(
    android.Manifest.permission.READ_CALENDAR,
    android.Manifest.permission.WRITE_CALENDAR
)

@Composable
actual fun rememberCalenderPermissionRequester(manager: NativeCalenderManager): State<CalenderPermissionGrantType> {
    val ctx = manager.ctx as Activity

    val state = remember {
        mutableStateOf(CalenderPermissionGrantType.WAIT)
    }

    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) callback@{ result ->
            when {
                result.values.all { it } -> CalenderPermissionGrantType.ALL_GRANTED //全部为true则通过检查
                result.values.any { it } -> when {
                    result.any {
                        ActivityCompat.shouldShowRequestPermissionRationale(
                            ctx,
                            it.key
                        )
                    } -> CalenderPermissionGrantType.DENY_PERMANENT //有权限被永久拒绝则升级整个状态为永久拒绝
                    else -> CalenderPermissionGrantType.DENY_ONCE //否则可以重新申请
                }
            }
        }

    LaunchedEffect(Unit) {
        state.value = CalenderPermissionGrantType.PROCESSING
        launcher.launch(CALENDER_PERMISSION)
    }

    return state
}
package top.kagg886.calender.util

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.flow.MutableStateFlow
import top.kagg886.calender.AndroidCalenderManager
import top.kagg886.calender.NativeCalenderManager
import top.kagg886.calender.data.CalenderPermissionGrantType

private val CALENDER_PERMISSION = arrayOf(
    android.Manifest.permission.READ_CALENDAR,
    android.Manifest.permission.WRITE_CALENDAR
)

@Composable
internal actual fun rememberCalenderPermissionRequester(manager: NativeCalenderManager): State<CalenderPermissionGrantType> {
    val ctx = (manager as AndroidCalenderManager).ctx as Activity

    //使用flow避免漏监听
    val state = remember {
        MutableStateFlow(CalenderPermissionGrantType.WAIT)
    }

    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) callback@{ result ->
            state.value = when {
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

                else -> CalenderPermissionGrantType.DENY_PERMANENT
            }
        }

    LaunchedEffect(Unit) {
        logger.i("start permission granted")
        state.value = CalenderPermissionGrantType.PROCESSING
        launcher.launch(CALENDER_PERMISSION)
    }

    return state.collectAsState()
}

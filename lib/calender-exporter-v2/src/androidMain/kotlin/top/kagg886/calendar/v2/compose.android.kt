package top.kagg886.calendar.v2

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import top.kagg886.calendar.v2.state.CalendarState
import top.kagg886.util.findActivity

@Composable
actual fun rememberCalendarManagerState(): CalendarState {
    var state by remember {
        mutableStateOf<CalendarState>(CalendarState.Waiting)
    }

    val ctx = LocalContext.current
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) callback@{ result ->
            state = when {
                result.values.all { it } -> CalendarState.Granted(AndroidCalendarManager(ctx)) //全部为true则通过检查
                result.values.any { it } -> CalendarState.Denied(
                    permanent = result.any {
                        ActivityCompat.shouldShowRequestPermissionRationale(
                            ctx.findActivity()!!,
                            it.key
                        )
                    }
                )

                else -> error("dead code")
            }
        }

    LaunchedEffect(Unit) {
        logger.i("start permission granted")
        state = CalendarState.Processing
        launcher.launch(
            arrayOf(
                android.Manifest.permission.READ_CALENDAR,
                android.Manifest.permission.WRITE_CALENDAR
            )
        )
    }

    return state
}

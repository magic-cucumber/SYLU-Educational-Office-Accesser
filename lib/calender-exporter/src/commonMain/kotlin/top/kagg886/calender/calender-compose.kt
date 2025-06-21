package top.kagg886.calender

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import top.kagg886.calender.data.CalenderPermissionGrantType
import top.kagg886.calender.data.Event
import top.kagg886.calender.util.ListChange
import top.kagg886.calender.util.ObservableMutableList
import top.kagg886.calender.util.rememberCalenderPermissionRequester

internal interface NativeCalenderManager {
    fun getEvents(): List<Event>
    fun clearEvents()
    fun insertEvent(event: Event)
    fun deleteEvent(event: Event)
    fun updateEvent(event: Event)
}

class CalenderState internal constructor(
    private val nativeCalenderManager: NativeCalenderManager
) {
    var permission by mutableStateOf(CalenderPermissionGrantType.WAIT)
        internal set

    val events = ObservableMutableList<Event>(mutableStateListOf())

    init {
        events.addAll(nativeCalenderManager.getEvents())
        events.addObserver {
            when(it) {
                is ListChange.Added -> {
                    nativeCalenderManager.insertEvent(it.item)
                }
                is ListChange.Removed -> {
                    nativeCalenderManager.deleteEvent(it.item)
                }
                is ListChange.Updated -> {
                    nativeCalenderManager.updateEvent(it.newItem)
                }
            }
        }
    }
}

/**
 * 创建一个日历状态，会自动申请权限。
 *
 * 由于会存储ctx和EventKit，所以请不要在多个地方创建多个日历状态。
 *
 * 最好的实践是创建一个单例，然后使用[staticCompositionLocalOf]暴露它
 */
@Composable
fun rememberCalenderState(vararg key: Any? = arrayOf()): CalenderState {
    val nativeCalenderManager = rememberNativeCalenderManager()

    val permission = rememberCalenderPermissionRequester(nativeCalenderManager)

    val state = remember(*key, nativeCalenderManager) {
        CalenderState(nativeCalenderManager)
    }

    LaunchedEffect(permission) {
        state.permission = permission.value
    }

    return state
}

@Composable
internal expect fun rememberNativeCalenderManager(): NativeCalenderManager
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
import top.kagg886.calender.util.logger
import top.kagg886.calender.util.rememberCalenderPermissionRequester

internal interface NativeCalenderManager {
    fun getEvents(account: String): List<Event>
    fun clearEvents(account: String)
    fun insertEvent(account: String, event: Event)
    fun deleteEvent(account: String, event: Event)
    fun updateEvent(account: String, event: Event)
}

class CalenderState internal constructor(
    private val nativeCalenderManager: NativeCalenderManager,
    private val name: String
) {
    var permission by mutableStateOf(CalenderPermissionGrantType.WAIT)
        internal set

    val events = ObservableMutableList<Event>(mutableStateListOf())
}

/**
 * 创建一个日历状态，会自动申请权限。
 *
 * 由于会存储ctx和EventKit，所以请不要在多个地方创建多个日历状态。
 *
 * 最好的实践是创建一个单例，然后使用[staticCompositionLocalOf]暴露它
 */
@Composable
fun rememberCalenderState(name: String = "default", vararg key: Any? = arrayOf()): CalenderState {
    val nativeCalenderManager = rememberNativeCalenderManager()

    val permission by rememberCalenderPermissionRequester(nativeCalenderManager)

    val state = remember(*key, nativeCalenderManager) {
        CalenderState(nativeCalenderManager, name)
    }

    LaunchedEffect(permission) {
        logger.d("Permission changed to $permission")
        state.permission = permission
    }

    LaunchedEffect(state.permission) {
        if (state.permission != CalenderPermissionGrantType.ALL_GRANTED) {
            return@LaunchedEffect
        }
        with(state) {
            events.addAll(nativeCalenderManager.getEvents(name))
            logger.i("Loaded ${events.size} events")
            events.addObserver {
                when (it) {
                    is ListChange.Added -> {
                        logger.d("Event added: ${it.item}")
                        nativeCalenderManager.insertEvent(name, it.item)
                    }

                    is ListChange.Removed -> {
                        logger.d("Event removed: ${it.item}")
                        nativeCalenderManager.deleteEvent(name, it.item)
                    }

                    is ListChange.Updated -> {
                        logger.d("Event updated: ${it.oldItem} -> ${it.newItem}")
                        nativeCalenderManager.updateEvent(name, it.newItem)
                    }
                }
            }
        }
    }
    return state
}

@Composable
internal expect fun rememberNativeCalenderManager(): NativeCalenderManager

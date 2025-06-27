package top.kagg886.calender

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import top.kagg886.calender.data.CalenderPermissionGrantType
import top.kagg886.calender.data.Event
import top.kagg886.calender.util.ListChange
import top.kagg886.calender.util.ObservableMutableList
import top.kagg886.calender.util.logger
import top.kagg886.calender.util.rememberCalenderPermissionRequester

internal interface NativeCalenderManager {
    fun getEvents(): List<Event>
    fun clearEvents()
    fun insertEvent(event: Event)
    fun deleteEvent(event: Event)
    fun updateEvent(event: Event)
}

class CalenderState internal constructor() {
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
fun rememberCalenderState(name: String = "default"): CalenderState {
    val nativeCalenderManager = rememberNativeCalenderManager(name)
    val permission by rememberCalenderPermissionRequester(nativeCalenderManager)

    val state = remember {
        CalenderState()
    }

    LaunchedEffect(permission) {
        val data = permission
        state.permission = data

        logger.i("calender permission changed: $data")

        if (data != CalenderPermissionGrantType.ALL_GRANTED) {
            return@LaunchedEffect
        }

        state.events.addAll(
            nativeCalenderManager.getEvents().apply {
                logger.i("Loaded $size events")
            }
        )

        state.events.addObserver {
            //为了防止其他数据不被提前终止，这里需要设计为阻塞线程。
            //TODO 这里不写Dispatchers.Main的话无法在iOS侧插入日志，待解决
            runBlocking(Dispatchers.Main) {
                when (it) {
                    is ListChange.Added -> {
                        nativeCalenderManager.insertEvent(it.item)
                        logger.d("Event added: ${it.item}")
                    }

                    is ListChange.Removed -> {
                        nativeCalenderManager.deleteEvent(it.item)
                        logger.d("Event removed: ${it.item}")
                    }

                    is ListChange.Updated -> {
                        nativeCalenderManager.updateEvent(it.newItem)
                        logger.d("Event updated: ${it.oldItem} -> ${it.newItem}")
                    }

                    is ListChange.Clear -> {
                        nativeCalenderManager.clearEvents()
                        logger.d("Event cleared")
                    }
                }
            }
        }
    }
    return state
}

@Composable
internal expect fun rememberNativeCalenderManager(name: String): NativeCalenderManager

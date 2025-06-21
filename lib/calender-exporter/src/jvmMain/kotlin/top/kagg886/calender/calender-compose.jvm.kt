package top.kagg886.calender

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import top.kagg886.calender.data.Event

internal class JvmCalenderManager: NativeCalenderManager {
    override fun getEvents(): List<Event> {
        return listOf()
    }

    override fun clearEvents() {}

    override fun insertEvent(event: Event) {}
    override fun deleteEvent(event: Event) {}

    override fun updateEvent(event: Event) {}
}

@Composable
internal actual fun rememberNativeCalenderManager(): NativeCalenderManager {
    return remember {
        JvmCalenderManager()
    }
}
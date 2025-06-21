package top.kagg886.calender

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import top.kagg886.calender.data.Event

internal class JvmCalenderManager: NativeCalenderManager {
    override fun getEvents(account:String): List<Event> {
        return listOf()
    }

    override fun clearEvents(account:String) {}

    override fun insertEvent(account:String,event: Event) {}
    override fun deleteEvent(account:String,event: Event) {}

    override fun updateEvent(account:String,event: Event) {}
}

@Composable
internal actual fun rememberNativeCalenderManager(): NativeCalenderManager {
    return remember {
        JvmCalenderManager()
    }
}
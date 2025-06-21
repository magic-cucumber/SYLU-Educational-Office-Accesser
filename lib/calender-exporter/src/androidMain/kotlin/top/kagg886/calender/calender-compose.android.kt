package top.kagg886.calender

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import top.kagg886.calender.data.Event

internal class AndroidCalenderManager internal constructor(val ctx: Context):NativeCalenderManager {
    override fun getEvents(): List<Event> {
        return listOf()
    }

    override fun clearEvents() {

    }

    override fun insertEvent(event: Event) {
    }

    override fun deleteEvent(event: Event) {
    }

    override fun updateEvent(event: Event) {
    }
}

@Composable
internal actual fun rememberNativeCalenderManager(): NativeCalenderManager {
    val ctx = LocalContext.current
    return remember {
        AndroidCalenderManager(ctx)
    }
}
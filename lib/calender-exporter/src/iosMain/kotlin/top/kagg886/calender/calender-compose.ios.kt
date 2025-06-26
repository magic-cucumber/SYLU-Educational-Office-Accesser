package top.kagg886.calender

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import platform.EventKit.EKCalendar
import platform.EventKit.EKEntityType
import platform.EventKit.EKEvent
import platform.EventKit.EKEventStore
import platform.EventKit.EKSpan
import platform.Foundation.NSDate
import platform.Foundation.NSError
import platform.Foundation.dateWithTimeIntervalSince1970
import platform.Foundation.distantFuture
import platform.Foundation.distantPast
import platform.Foundation.timeIntervalSince1970
import top.kagg886.calender.data.Event

@OptIn(ExperimentalForeignApi::class)
internal class EKCalenderManager(account: String) : NativeCalenderManager {
    val eventStore = EKEventStore()

    val calendar = getCalendar(account)

    override fun getEvents(): List<Event> {
        return memScoped {
            val predicate = eventStore.predicateForEventsWithStartDate(
                startDate = NSDate.distantPast,
                endDate = NSDate.distantFuture,
                calendars = listOf(calendar)
            )

            // Fetch events
            val events = eventStore.eventsMatchingPredicate(predicate) as List<EKEvent>

            // Convert to our domain model
            events.map { ekEvent ->
                Event(
                    id = ekEvent.eventIdentifier!!,
                    title = ekEvent.title ?: "",
                    startTime = ekEvent.startDate!!.toLocalDateTime(),
                    endTime = ekEvent.endDate!!.toLocalDateTime(),
                    description = ekEvent.notes ?: "",
                    location = ekEvent.location ?: ""
                )
            }
        }
    }

    override fun clearEvents() {
        memScoped {
            val events = getEvents()
            events.forEach { event ->
                val ekEvent = eventStore.eventWithIdentifier(event.id) ?: return@forEach
                eventStore.removeEvent(ekEvent, span = EKSpan.EKSpanThisEvent, error = null)
            }
        }
    }

    @OptIn(BetaInteropApi::class)
    override fun insertEvent(event: Event) {
        memScoped {
            val errorVar = alloc<ObjCObjectVar<NSError?>>().apply { value = null }

            val ekEvent = EKEvent.eventWithEventStore(eventStore).apply {
                calendar = this@EKCalenderManager.calendar
                title = event.title
                notes = event.description
                location = event.location
                startDate = event.startTime.toNSDate()
                endDate = event.endTime.toNSDate()
            }

            eventStore.saveEvent(ekEvent, span = EKSpan.EKSpanThisEvent, error = errorVar.ptr)

            errorVar.value?.let { error ->
                throw Exception("Failed to insert event: ${error.localizedDescription}")
            }
        }
    }

    @OptIn(BetaInteropApi::class)
    override fun deleteEvent(event: Event) {
        memScoped {
            val errorVar = alloc<ObjCObjectVar<NSError?>>().apply { value = null }

            val ekEvent = eventStore.eventWithIdentifier(event.id) ?: return
            eventStore.removeEvent(ekEvent, EKSpan.EKSpanThisEvent, error = errorVar.ptr)

            errorVar.value?.let { error ->
                throw Exception("Failed to delete event: ${error.localizedDescription}")
            }
        }
    }

    @OptIn(BetaInteropApi::class)
    override fun updateEvent(event: Event) {
        memScoped {
            val errorVar = alloc<ObjCObjectVar<NSError?>>().apply { value = null }

            val ekEvent = eventStore.eventWithIdentifier(event.id) ?: return
            ekEvent.apply {
                calendar = this@EKCalenderManager.calendar
                title = event.title
                notes = event.description
                location = event.location
                startDate = event.startTime.toNSDate()
                endDate = event.endTime.toNSDate()
            }

            eventStore.saveEvent(ekEvent, span = EKSpan.EKSpanThisEvent, error = errorVar.ptr)

            errorVar.value?.let { error ->
                throw Exception("Failed to update event: ${error.localizedDescription}")
            }
        }
    }
}

@Composable
internal actual fun rememberNativeCalenderManager(name: String): NativeCalenderManager {
    return remember(name) {
        EKCalenderManager(name)
    }
}

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
private fun EKCalenderManager.getCalendar(accountName: String): EKCalendar {
    // 先尝试找到已有的指定名称的日历
    val calendars =
        eventStore.calendarsForEntityType(EKEntityType.EKEntityTypeEvent) as List<EKCalendar>
    val existing = calendars.firstOrNull { it.title == accountName }
    if (existing != null) return existing

    // 没找到则创建新的日历
    val newCalendar = EKCalendar.calendarWithEventStore(eventStore).apply {
        title = accountName

        // 这里尽量使用默认账户的source，否则需要自己选择合适source
        source = (eventStore.defaultCalendarForNewEvents?.source ?: calendars.firstOrNull()?.source)
            ?: throw Exception("No calendar source found")
    }

    memScoped {
        val errorVar = alloc<ObjCObjectVar<NSError?>>().apply {
            value = null
        }

        val success = eventStore.saveCalendar(newCalendar, true, errorVar.ptr)
        if (!success) {
            throw Exception("创建日历失败: ${errorVar.value?.localizedDescription ?: "未知错误"}")
        }
    }


    return newCalendar
}

private fun LocalDateTime.toNSDate(): NSDate {
    val instant = this.toInstant(TimeZone.currentSystemDefault())
    return NSDate.dateWithTimeIntervalSince1970(instant.epochSeconds.toDouble())
}

private fun NSDate.toLocalDateTime(): LocalDateTime {
    val instant = Instant.fromEpochSeconds(this.timeIntervalSince1970.toLong())
    return instant.toLocalDateTime(TimeZone.currentSystemDefault())
}

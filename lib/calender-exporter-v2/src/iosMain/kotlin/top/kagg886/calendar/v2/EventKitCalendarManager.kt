@file:OptIn(
    kotlin.time.ExperimentalTime::class,
    BetaInteropApi::class,
    ExperimentalForeignApi::class
)

package top.kagg886.calendar.v2

import kotlinx.cinterop.*
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import platform.EventKit.*
import platform.Foundation.NSDate
import platform.Foundation.NSError
import platform.Foundation.dateWithTimeIntervalSince1970
import platform.Foundation.timeIntervalSince1970
import platform.darwin.dispatch_async
import platform.darwin.dispatch_queue_create
import top.kagg886.calendar.v2.state.Event
import kotlin.coroutines.resume
import kotlin.time.Instant

/**
 * ================================================
 * Author:     886kagg
 * Created on: 2026/6/13 14:45
 * ================================================
 */

class EventKitCalendarManager(private val api: EKEventStore) : CalendarManager {
    override suspend fun getCalendar(id: String): Calendar? = eventKit {
        logger.d("getCalendar called: id=$id")
        val calendar = api.calendarWithIdentifier(id) ?: run {
            logger.d("getCalendar missed: id=$id")
            return@eventKit null
        }

        val result = calendar.toCalendar(api)
        logger.d("getCalendar found: id=${result.id}, title=${result.title}")
        result
    }

    override suspend fun createCalendar(title: String): Calendar = eventKit {
        val displayTitle = title.ifBlank { "Calendar" }
        logger.d("createCalendar called: title=$displayTitle")

        val calendar = EKCalendar.calendarForEntityType(EKEntityType.EKEntityTypeEvent, api)
        calendar.title = displayTitle
        calendar.source = api.defaultCalendarSource()

        api.saveCalendarChecked(calendar, commit = false)
        api.commitChecked()

        val id = requireNotNull(calendar.calendarIdentifier) {
            "EventKit returned calendar without identifier"
        }
        logger.d("createCalendar success: id=$id, title=$displayTitle")
        EventKitCalendar(api, id, displayTitle)
    }

    override suspend fun deleteCalendar(id: String): Unit = eventKit {
        logger.d("deleteCalendar called: id=$id")
        val calendar = api.calendarWithIdentifier(id) ?: run {
            logger.d("deleteCalendar skipped: calendar missed id=$id")
            return@eventKit
        }

        api.removeCalendarChecked(calendar, commit = false)
        api.commitChecked()
        logger.d("deleteCalendar finished: id=$id")
    }
}

class EventKitCalendar internal constructor(
    private val api: EKEventStore,
    override val id: String,
    override val title: String
) : Calendar {

    override suspend fun getEvents(
        start: LocalDateTime,
        end: LocalDateTime
    ): List<Event> = eventKit {
        logger.d("getEvents called: calendarId=$id, start=$start, end=$end")
        val calendar = api.calendarWithIdentifier(id) ?: run {
            logger.d("getEvents skipped: calendar missed id=$id")
            return@eventKit emptyList()
        }

        val predicate = api.predicateForEventsWithStartDate(
            startDate = start.toNSDate(),
            endDate = end.toNSDate(),
            calendars = listOf(calendar)
        )

        val events = api.eventsMatchingPredicate(predicate)
            .filterIsInstance<EKEvent>()
            .filter { it.calendar?.calendarIdentifier == id }
            .map { it.toEvent() }

        logger.d("getEvents finished: calendarId=$id, count=${events.size}")
        events
    }

    override suspend fun getEvent(id: String): Event? = eventKit {
        logger.d("getEvent called: calendarId=${this.id}, eventId=$id")
        val event = api.findEventById(id, this.id)?.toEvent()
        logger.d("getEvent finished: calendarId=${this.id}, eventId=$id, found=${event != null}")
        event
    }

    override suspend fun transaction(block: suspend TransactionScope.() -> Unit) {
        logger.d("transaction started: calendarId=$id")

        val scope = eventKit {
            val transactionApi = EKEventStore()
            val calendar = transactionApi.calendarWithIdentifier(id) ?: error("EventKit calendar not found: $id")
            EventKitCalendarTransactionScope(
                api = transactionApi,
                calendar = calendar,
                calendarIdString = id
            )
        }

        scope.block()
        if (scope.operationCount > 0) {
            logger.d("transaction committing: calendarId=$id, operations=${scope.operationCount}")
            scope.commit()
            logger.d("transaction committed: calendarId=$id, operations=${scope.operationCount}")
        } else {
            logger.d("transaction skipped commit: calendarId=$id, operations=0")
        }
    }

    override suspend fun create(event: Event): Event = eventKit {
        logger.d("createEvent called: calendarId=$id, title=${event.title}, start=${event.startTime}, end=${event.endTime}")
        val calendar = api.calendarWithIdentifier(id) ?: error("EventKit calendar not found: $id")
        val ekEvent = event.toEKEvent(api, calendar)

        api.saveEventChecked(ekEvent, commit = false)
        api.commitChecked()

        val eventId = requireNotNull(ekEvent.eventIdentifier) {
            "EventKit returned event without identifier"
        }
        logger.d("createEvent success: calendarId=$id, eventId=$eventId")
        event.copy(id = eventId)
    }

    override suspend fun update(event: Event): Boolean = eventKit {
        logger.d("updateEvent called: calendarId=$id, eventId=${event.id}, title=${event.title}")
        val eventId = event.id ?: run {
            logger.d("updateEvent skipped: invalid eventId=${event.id}")
            return@eventKit false
        }
        val ekEvent = api.findEventById(eventId, id) ?: run {
            logger.d("updateEvent skipped: event missed eventId=$eventId")
            return@eventKit false
        }

        ekEvent.applyEvent(event)
        api.saveEventChecked(ekEvent, commit = false)
        api.commitChecked()

        logger.d("updateEvent finished: calendarId=$id, eventId=$eventId")
        true
    }

    override suspend fun delete(id: String): Boolean = eventKit {
        logger.d("deleteEvent called: calendarId=${this.id}, eventId=$id")
        val ekEvent = api.findEventById(id, this.id) ?: run {
            logger.d("deleteEvent skipped: event missed eventId=$id")
            return@eventKit false
        }

        api.removeEventChecked(ekEvent, commit = false)
        api.commitChecked()

        logger.d("deleteEvent finished: calendarId=${this.id}, eventId=$id")
        true
    }

}

class EventKitCalendarTransactionScope internal constructor(
    private val api: EKEventStore,
    private val calendar: EKCalendar,
    private val calendarIdString: String
) : TransactionScope {
    internal var operationCount: Int = 0
        private set

    internal suspend fun commit() = eventKit {
        api.commitChecked()
    }

    override suspend fun create(event: Event): Event = eventKit {
        logger.d("transaction create queued: calendarId=$calendarIdString, title=${event.title}")
        val ekEvent = event.toEKEvent(api, calendar)
        api.saveEventChecked(ekEvent, commit = false)
        operationCount++
        logger.d("transaction operation count: calendarId=$calendarIdString, operations=$operationCount")

        // EventKit assigns the stable event id after commit.
        event
    }

    override suspend fun update(event: Event): Boolean = eventKit {
        logger.d("transaction update queued: calendarId=$calendarIdString, eventId=${event.id}, title=${event.title}")
        val eventId = event.id ?: run {
            logger.d("transaction update skipped: invalid eventId=${event.id}")
            return@eventKit false
        }
        val ekEvent = api.findEventById(eventId, calendarIdString) ?: run {
            logger.d("transaction update skipped: event missed eventId=$eventId")
            return@eventKit false
        }

        ekEvent.applyEvent(event)
        api.saveEventChecked(ekEvent, commit = false)
        operationCount++
        logger.d("transaction operation count: calendarId=$calendarIdString, operations=$operationCount")
        true
    }

    override suspend fun delete(id: String): Boolean = eventKit {
        logger.d("transaction delete queued: calendarId=$calendarIdString, eventId=$id")
        val ekEvent = api.findEventById(id, calendarIdString) ?: run {
            logger.d("transaction delete skipped: event missed eventId=$id")
            return@eventKit false
        }

        api.removeEventChecked(ekEvent, commit = false)
        operationCount++
        logger.d("transaction operation count: calendarId=$calendarIdString, operations=$operationCount")
        true
    }

}

private fun EKCalendar.toCalendar(api: EKEventStore): EventKitCalendar =
    EventKitCalendar(
        api = api,
        id = requireNotNull(calendarIdentifier) { "EventKit returned calendar without identifier" },
        title = title.orEmpty()
    )

private fun EKEventStore.defaultCalendarSource(): EKSource {
    val sources = sources
    return sources.firstOrNull { source ->
        (source as? EKSource)?.sourceType == EKSourceType.EKSourceTypeLocal
    } as? EKSource
        ?: sources.firstOrNull() as? EKSource
        ?: error("No EventKit source available")
}

private fun EKEventStore.findEventById(eventId: String, calendarId: String): EKEvent? {
    val event = eventWithIdentifier(eventId) ?: return null
    return event.takeIf { it.calendar?.calendarIdentifier == calendarId }
}

private fun Event.toEKEvent(api: EKEventStore, calendar: EKCalendar): EKEvent =
    EKEvent.eventWithEventStore(api).also { ekEvent ->
        ekEvent.calendar = calendar
        ekEvent.applyEvent(this)
    }

private fun EKEvent.applyEvent(event: Event) {
    title = event.title
    notes = event.description
    location = event.location
    startDate = event.startTime.toNSDate()
    endDate = event.endTime.toNSDate()
}

private fun EKEvent.toEvent(): Event =
    Event(
        id = eventIdentifier,
        title = title.orEmpty(),
        startTime = startDate?.toLocalDateTime() ?: LocalDateTime(2000, 1, 1, 0, 0),
        endTime = endDate?.toLocalDateTime() ?: LocalDateTime(2000, 1, 1, 1, 0),
        description = notes.orEmpty(),
        location = location.orEmpty()
    )

private fun EKEventStore.saveCalendarChecked(calendar: EKCalendar, commit: Boolean) {
    memScoped {
        val error = alloc<ObjCObjectVar<NSError?>>()
        val success = saveCalendar(calendar, commit = commit, error = error.ptr)
        if (!success) {
            error("Failed to save EventKit calendar: ${error.value?.localizedDescription}")
        }
    }
}

private fun EKEventStore.removeCalendarChecked(calendar: EKCalendar, commit: Boolean) {
    memScoped {
        val error = alloc<ObjCObjectVar<NSError?>>()
        val success = removeCalendar(calendar, commit = commit, error = error.ptr)
        if (!success) {
            error("Failed to remove EventKit calendar: ${error.value?.localizedDescription}")
        }
    }
}

private fun EKEventStore.saveEventChecked(event: EKEvent, commit: Boolean) {
    memScoped {
        val error = alloc<ObjCObjectVar<NSError?>>()
        val success = saveEvent(event, span = EKSpan.EKSpanThisEvent, commit = commit, error = error.ptr)
        if (!success) {
            error("Failed to save EventKit event: ${error.value?.localizedDescription}")
        }
    }
}

private fun EKEventStore.removeEventChecked(event: EKEvent, commit: Boolean) {
    memScoped {
        val error = alloc<ObjCObjectVar<NSError?>>()
        val success = removeEvent(event, span = EKSpan.EKSpanThisEvent, commit = commit, error = error.ptr)
        if (!success) {
            error("Failed to remove EventKit event: ${error.value?.localizedDescription}")
        }
    }
}

private fun EKEventStore.commitChecked() {
    memScoped {
        val error = alloc<ObjCObjectVar<NSError?>>()
        val success = commit(error.ptr)
        if (!success) {
            error("Failed to commit EventKit changes: ${error.value?.localizedDescription}")
        }
    }
}

private fun LocalDateTime.toNSDate(): NSDate {
    val millis = toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()
    return NSDate.dateWithTimeIntervalSince1970(millis.toDouble() / 1000.0)
}

private fun NSDate.toLocalDateTime(): LocalDateTime {
    val millis = (timeIntervalSince1970 * 1000).toLong()
    return Instant.fromEpochMilliseconds(millis).toLocalDateTime(TimeZone.currentSystemDefault())
}

private val eventKitQueue = dispatch_queue_create("top.kagg886.calendar.v2.eventkit", null)

private suspend fun <T> eventKit(block: () -> T): T =
    suspendCancellableCoroutine { continuation ->
        dispatch_async(eventKitQueue) {
            try {
                continuation.resume(block())
            } catch (throwable: Throwable) {
                continuation.cancel(throwable)
            }
        }
    }

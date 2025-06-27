@file:OptIn(BetaInteropApi::class)

package top.kagg886.calender

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.cinterop.*
import kotlinx.datetime.*
import platform.EventKit.*
import platform.Foundation.*
import top.kagg886.calender.data.Event
import top.kagg886.util.asTaggedLogger

@OptIn(ExperimentalForeignApi::class)
internal class EKCalenderManager(private val accountName: String) : NativeCalenderManager {
    private val logger = "EKCalenderManager".asTaggedLogger
    val eventStore = EKEventStore()
    private var calendar: EKCalendar? = null

    init {
        initializeCalendar()
    }

    private fun initializeCalendar() {
        val userDefaults = NSUserDefaults.standardUserDefaults
        val calendarIdKey = "calendar_id_$accountName"
        val storedCalendarId = userDefaults.stringForKey(calendarIdKey)

        // 如果有存储的日历ID，尝试根据ID查找日历
        if (storedCalendarId != null) {
            calendar = eventStore.calendarWithIdentifier(storedCalendarId)
        }

        // 如果没有找到日历，创建新的日历
        if (calendar == null) {
            createNewCalendar()?.let { newCalendar ->
                calendar = newCalendar
                // 存储新日历的ID到NSUserDefaults
                userDefaults.setObject(newCalendar.calendarIdentifier, calendarIdKey)
                userDefaults.synchronize()
            } ?: throw IllegalStateException("calender init failed")
        }
    }

    private fun createNewCalendar(): EKCalendar? {
        // 检查权限
        val authStatus = EKEventStore.authorizationStatusForEntityType(EKEntityType.EKEntityTypeEvent)
        if (authStatus != EKAuthorizationStatusAuthorized) {
            throw IllegalStateException("Calendar permission not authorized when creating calendar. Status: $authStatus")
        }

        return memScoped {
            try {
                val error = alloc<ObjCObjectVar<NSError?>>()
                val newCalendar = EKCalendar.calendarForEntityType(EKEntityType.EKEntityTypeEvent, eventStore)
                newCalendar.title = accountName

                // 查找合适的数据源（通常使用本地数据源）
                val sources = eventStore.sources
                val localSource = sources.firstOrNull { source ->
                    (source as? EKSource)?.sourceType == EKSourceType.EKSourceTypeLocal
                } as? EKSource ?: sources.firstOrNull() as? EKSource

                if (localSource != null) {
                    newCalendar.source = localSource

                    val success = eventStore.saveCalendar(newCalendar, commit = true, error = error.ptr)

                    if (success) {
                        logger.d("Successfully created calendar: ${newCalendar.calendarIdentifier}")
                        newCalendar
                    } else {
                        logger.e("Failed to create calendar: ${error.value?.localizedDescription}")
                        null
                    }
                } else {
                    logger.e("No suitable source found for calendar")
                    null
                }
            } catch (e: Exception) {
                logger.e("Exception creating calendar: ${e.message}")
                null
            }
        }
    }

    private fun getEventsOrigin(): List<EKEvent> {
        val currentCalendar = calendar ?: throw IllegalStateException("calender not initialized")

        // 检查权限
        val authStatus = EKEventStore.authorizationStatusForEntityType(EKEntityType.EKEntityTypeEvent)
        when (authStatus) {
            EKAuthorizationStatusDenied -> {
                throw IllegalStateException("Calendar permission denied")
            }
            EKAuthorizationStatusRestricted -> {
                throw IllegalStateException("Calendar permission restricted")
            }
            EKAuthorizationStatusNotDetermined -> {
                throw IllegalStateException("Calendar permission not determined")
            }
            EKAuthorizationStatusAuthorized -> {
                logger.d("Calendar permission authorized")
            }
            else -> {
                throw IllegalStateException("Unknown calendar permission status: $authStatus")
            }
        }

        return try {
            logger.d("Getting events from calendar: ${currentCalendar.title} (${currentCalendar.calendarIdentifier})")

            // 使用更合理的时间范围：过去1年到未来1年
            val now = NSDate()
            val calendar = platform.Foundation.NSCalendar.currentCalendar
            val startDate = calendar.dateByAddingUnit(
                unit = NSCalendarUnitYear,
                value = -1,
                toDate = now,
                options = 0u
            ) ?: NSDate.distantPast

            val endDate = calendar.dateByAddingUnit(
                unit = NSCalendarUnitYear,
                value = 1,
                toDate = now,
                options = 0u
            ) ?: NSDate.distantFuture

            logger.d("Querying events from ${startDate} to ${endDate}")

            val predicate = eventStore.predicateForEventsWithStartDate(
                startDate = startDate,
                endDate = endDate,
                calendars = listOf(currentCalendar)
            )

            val events = eventStore.eventsMatchingPredicate(predicate)
            logger.d("Found ${events.size} raw events")
            events as List<EKEvent>
        } catch (e: Exception) {
            logger.e("Error getting events: ${e.message}")
            emptyList()
        }
    }

    override fun getEvents(): List<Event> = getEventsOrigin().mapNotNull { ekEvent ->
        val event = ekEvent as? EKEvent
        event?.let {
            logger.d("Processing event: ${it.title} (${it.eventIdentifier})")
            Event(
                id = it.eventIdentifier ?: "",
                title = it.title ?: "",
                startTime = it.startDate?.let { date ->
                    Instant.fromEpochSeconds(date.timeIntervalSince1970.toLong())
                        .toLocalDateTime(TimeZone.currentSystemDefault())
                } ?: LocalDateTime(2000, 1, 1, 0, 0),
                endTime = it.endDate?.let { date ->
                    Instant.fromEpochSeconds(date.timeIntervalSince1970.toLong())
                        .toLocalDateTime(TimeZone.currentSystemDefault())
                } ?: LocalDateTime(2000, 1, 1, 1, 0),
                description = it.notes ?: "",
                location = it.location ?: ""
            )
        }
    }

    override fun clearEvents() {
        memScoped {
            try {
               val events = getEventsOrigin()

                // 批量删除事件，不立即提交
                events.forEach { event ->
                    val error = alloc<ObjCObjectVar<NSError?>>().apply { value = null }
                    eventStore.removeEvent(event, span = EKSpan.EKSpanThisEvent, commit = false, error = error.ptr)

                    if (error.value != null) {
                        throw IllegalStateException("remove failed: ${error.value?.localizedDescription}")
                    }
                }

                // 最后一次性提交所有更改
                val error = alloc<ObjCObjectVar<NSError?>>().apply { value = null }
                val success = eventStore.commit(error.ptr)
                if (!success) {
                    logger.e("Failed to clear events: ${error.value?.localizedDescription}")
                }
            } catch (e: Exception) {
                logger.e("Error clearing events: ${e.message}")
            }
        }
    }

    override fun insertEvent(event: Event) {
        val currentCalendar = calendar ?: throw IllegalStateException("calender not initialized")

        // 检查权限
        val authStatus = EKEventStore.authorizationStatusForEntityType(EKEntityType.EKEntityTypeEvent)
        if (authStatus != EKAuthorizationStatusAuthorized) {
            throw IllegalStateException("Calendar permission not authorized when inserting event. Status: $authStatus")
        }

        memScoped {
            try {
                val error = alloc<ObjCObjectVar<NSError?>>()
                val ekEvent = EKEvent.eventWithEventStore(eventStore)
                ekEvent.title = event.title
                ekEvent.notes = event.description
                ekEvent.location = event.location
                ekEvent.calendar = currentCalendar

                // 转换时间
                val startInstant = event.startTime.toInstant(TimeZone.currentSystemDefault())
                val endInstant = event.endTime.toInstant(TimeZone.currentSystemDefault())

                ekEvent.startDate = NSDate.dateWithTimeIntervalSince1970(startInstant.epochSeconds.toDouble())
                ekEvent.endDate = NSDate.dateWithTimeIntervalSince1970(endInstant.epochSeconds.toDouble())

                logger.d("Inserting event: ${event.title} from ${event.startTime} to ${event.endTime}")

                val success =
                    eventStore.saveEvent(ekEvent, span = EKSpan.EKSpanThisEvent, commit = true, error = error.ptr)

                if (success) {
                    logger.d("Successfully inserted event: ${ekEvent.eventIdentifier}")
                } else {
                    logger.e("Failed to insert event: ${error.value?.localizedDescription}")
                }
            } catch (e: Exception) {
                logger.e("Error inserting event: ${e.message}")
            }
        }
    }

    override fun deleteEvent(event: Event) {
        val currentCalendar = calendar ?: throw IllegalStateException("calender not initialized")

        memScoped {
            try {
                val error = alloc<ObjCObjectVar<NSError?>>()
                val ekEvent = findEventById(event.id, currentCalendar)
                ekEvent?.let {
                    val success =
                        eventStore.removeEvent(it, span = EKSpan.EKSpanThisEvent, commit = true, error = error.ptr)

                    if (!success) {
                        logger.e("Failed to delete event: ${error.value?.localizedDescription}")
                    }
                }
            } catch (e: Exception) {
                logger.e("Error deleting event: ${e.message}")
            }
        }
    }

    override fun updateEvent(event: Event) {
        val currentCalendar = calendar ?: throw IllegalStateException("calender not initialized")

        memScoped {
            try {
                val error = alloc<ObjCObjectVar<NSError?>>()
                val ekEvent = findEventById(event.id, currentCalendar)
                ekEvent?.let {
                    it.title = event.title
                    it.notes = event.description
                    it.location = event.location

                    // 转换时间
                    val startInstant = event.startTime.toInstant(TimeZone.currentSystemDefault())
                    val endInstant = event.endTime.toInstant(TimeZone.currentSystemDefault())

                    it.startDate = NSDate.dateWithTimeIntervalSince1970(startInstant.epochSeconds.toDouble())
                    it.endDate = NSDate.dateWithTimeIntervalSince1970(endInstant.epochSeconds.toDouble())

                    val success =
                        eventStore.saveEvent(it, span = EKSpan.EKSpanThisEvent, commit = true, error = error.ptr)

                    if (!success) {
                        logger.e("Failed to update event: ${error.value?.localizedDescription}")
                    }
                }
            } catch (e: Exception) {
                logger.e("Error updating event: ${e.message}")
            }
        }
    }

    private fun findEventById(eventId: String, calendar: EKCalendar): EKEvent? {
        return try {
            val startDate = NSDate.distantPast
            val endDate = NSDate.distantFuture

            val predicate = eventStore.predicateForEventsWithStartDate(
                startDate = startDate,
                endDate = endDate,
                calendars = listOf(calendar)
            )

            val events = eventStore.eventsMatchingPredicate(predicate)

            events.firstOrNull { ekEvent ->
                val event = ekEvent as? EKEvent
                event?.eventIdentifier == eventId
            } as? EKEvent
        } catch (e: Exception) {
            logger.e("Error finding event by ID: ${e.message}")
            null
        }
    }
}

@Composable
internal actual fun rememberNativeCalenderManager(name: String): NativeCalenderManager {
    return remember(name) {
        EKCalenderManager(name)
    }
}

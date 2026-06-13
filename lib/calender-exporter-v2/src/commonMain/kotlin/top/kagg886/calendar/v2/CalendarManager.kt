package top.kagg886.calendar.v2

import kotlinx.datetime.LocalDateTime
import top.kagg886.calendar.v2.state.Event

interface CalendarManager {
    /**
     * Returns a calendar created by this library, or null when the id is unknown
     * or belongs to another application.
     */
    suspend fun getCalendar(id: String): Calendar?

    /**
     * Creates a new calendar owned by this library.
     */
    suspend fun createCalendar(title: String): Calendar

    /**
     * Deletes a calendar owned by this library.
     */
    suspend fun deleteCalendar(id: String)
}

interface Calendar : EventWritable {
    val id: String
    val title: String

    /**
     * Queries events intersecting the given time range.
     *
     * Recurring events are returned as their original event objects and are not
     * expanded into individual occurrences.
     */
    suspend fun getEvents(start: LocalDateTime, end: LocalDateTime): List<Event>

    /**
     * Returns the original event object by id, without expanding recurrences.
     */
    suspend fun getEvent(id: String): Event?

    /**
     * Executes event writes in a platform transaction when supported.
     *
     * iOS implementations may execute the block directly without rollback
     * support.
     */
    suspend fun transaction(block: suspend TransactionScope.() -> Unit)
}

interface EventWritable {
    suspend fun create(event: Event): Event
    suspend fun update(event: Event): Boolean
    suspend fun delete(id: String): Boolean
}

interface TransactionScope : EventWritable

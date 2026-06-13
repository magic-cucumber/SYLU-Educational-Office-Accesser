@file:OptIn(kotlin.time.ExperimentalTime::class)

package top.kagg886.calendar.v2

import android.content.ContentProviderOperation
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.CalendarContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import top.kagg886.calendar.v2.state.Event
import kotlin.time.Instant

/**
 * ================================================
 * Author:     886kagg
 * Created on: 2026/6/13 13:36
 * ================================================
 */
class AndroidCalendarManager(context: Context) : CalendarManager {
    private val context = context.applicationContext

    override suspend fun getCalendar(id: String): Calendar? = withContext(Dispatchers.IO) {
        logger.d("getCalendar called: id=$id")
        val calendarId = id.toLongOrNull() ?: run {
            logger.d("getCalendar skipped: invalid id=$id")
            return@withContext null
        }
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME
        )

        context.contentResolver.query(
            calendarUri(calendarId),
            projection,
            "${CalendarContract.Calendars.ACCOUNT_NAME} = ? AND ${CalendarContract.Calendars.ACCOUNT_TYPE} = ?",
            arrayOf(ACCOUNT_NAME, CalendarContract.ACCOUNT_TYPE_LOCAL),
            null
        )?.use { cursor ->
            if (!cursor.moveToFirst()) {
                logger.d("getCalendar missed: id=$id")
                return@withContext null
            }

            val calendar = AndroidCalendar(
                context = context,
                id = cursor.getLongValue(CalendarContract.Calendars._ID).toString(),
                title = cursor.getStringValue(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME)
            )
            logger.d("getCalendar found: id=${calendar.id}, title=${calendar.title}")
            return@withContext calendar
        }

        logger.d("getCalendar missed: id=$id")
        null
    }

    override suspend fun createCalendar(title: String): Calendar = withContext(Dispatchers.IO) {
        val displayTitle = title.ifBlank { "Calendar" }
        logger.d("createCalendar called: title=$displayTitle")
        val name = "$ACCOUNT_NAME.${System.currentTimeMillis()}"
        val timeZone = TimeZone.currentSystemDefault().id
        val values = ContentValues().apply {
            put(CalendarContract.Calendars.ACCOUNT_NAME, ACCOUNT_NAME)
            put(CalendarContract.Calendars.ACCOUNT_TYPE, CalendarContract.ACCOUNT_TYPE_LOCAL)
            put(CalendarContract.Calendars.NAME, name)
            put(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME, displayTitle)
            put(CalendarContract.Calendars.CALENDAR_COLOR, CALENDAR_COLOR)
            put(
                CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL,
                CalendarContract.Calendars.CAL_ACCESS_OWNER
            )
            put(CalendarContract.Calendars.OWNER_ACCOUNT, ACCOUNT_NAME)
            put(CalendarContract.Calendars.CALENDAR_TIME_ZONE, timeZone)
            put(CalendarContract.Calendars.SYNC_EVENTS, 1)
            put(CalendarContract.Calendars.VISIBLE, 1)
        }

        val uri = context.contentResolver.insert(calendarSyncAdapterUri(), values)
            ?: error("Failed to create Android calendar")
        val id = requireNotNull(uri.lastPathSegment?.toLongOrNull()) {
            "Calendar provider returned invalid uri: $uri"
        }

        logger.d("createCalendar success: id=$id, title=$displayTitle")
        AndroidCalendar(context, id.toString(), displayTitle)
    }

    override suspend fun deleteCalendar(id: String): Unit = withContext(Dispatchers.IO) {
        logger.d("deleteCalendar called: id=$id")
        val calendar = getCalendar(id) ?: return@withContext
        val rows = context.contentResolver.delete(calendarSyncAdapterUri(calendar.id.toLong()), null, null)
        logger.d("deleteCalendar finished: id=${calendar.id}, rows=$rows")
    }
}

class AndroidCalendar internal constructor(
    private val context: Context,
    override val id: String,
    override val title: String
) : Calendar {
    private val calendarId: Long = requireNotNull(id.toLongOrNull()) {
        "Android calendar id must be a long: $id"
    }

    override suspend fun getEvents(
        start: LocalDateTime,
        end: LocalDateTime
    ): List<Event> = withContext(Dispatchers.IO) {
        logger.d("getEvents called: calendarId=$id, start=$start, end=$end")
        val startMillis = start.toEpochMillis()
        val endMillis = end.toEpochMillis()
        val selection = """
            ${CalendarContract.Events.CALENDAR_ID} = ? AND (
                (${CalendarContract.Events.DTEND} IS NULL AND ${CalendarContract.Events.DTSTART} >= ? AND ${CalendarContract.Events.DTSTART} < ?)
                OR
                (${CalendarContract.Events.DTEND} IS NOT NULL AND ${CalendarContract.Events.DTSTART} < ? AND ${CalendarContract.Events.DTEND} > ?)
            )
        """.trimIndent()
        val selectionArgs = arrayOf(
            id,
            startMillis.toString(),
            endMillis.toString(),
            endMillis.toString(),
            startMillis.toString()
        )

        val events = queryEvents(
            selection = selection,
            selectionArgs = selectionArgs,
            sortOrder = "${CalendarContract.Events.DTSTART} ASC"
        )
        logger.d("getEvents finished: calendarId=$id, count=${events.size}")
        events
    }

    override suspend fun getEvent(id: String): Event? = withContext(Dispatchers.IO) {
        logger.d("getEvent called: calendarId=${this@AndroidCalendar.id}, eventId=$id")
        val eventId = id.toLongOrNull() ?: run {
            logger.d("getEvent skipped: invalid eventId=$id")
            return@withContext null
        }
        val event = queryEvents(
            uri = eventId.eventUri(),
            selection = "${CalendarContract.Events.CALENDAR_ID} = ?",
            selectionArgs = arrayOf(this@AndroidCalendar.id),
            sortOrder = null
        ).firstOrNull()
        logger.d("getEvent finished: calendarId=${this@AndroidCalendar.id}, eventId=$id, found=${event != null}")
        event
    }

    override suspend fun transaction(block: suspend TransactionScope.() -> Unit) {
        logger.d("transaction started: calendarId=$id")
        val operations = arrayListOf<ContentProviderOperation>()
        val scope = AndroidCalendarTransactionScope(
            operations = operations,
            calendarId = calendarId,
            calendarIdString = id
        )
        scope.block()
        if (operations.isNotEmpty()) {
            logger.d("transaction committing: calendarId=$id, operations=${operations.size}")
            withContext(Dispatchers.IO) {
                context.contentResolver.applyBatch(CalendarContract.AUTHORITY, operations)
            }
            logger.d("transaction committed: calendarId=$id, operations=${operations.size}")
        } else {
            logger.d("transaction skipped commit: calendarId=$id, operations=0")
        }
    }

    override suspend fun create(event: Event): Event = withContext(Dispatchers.IO) {
        logger.d("createEvent called: calendarId=$id, title=${event.title}, start=${event.startTime}, end=${event.endTime}")
        val uri = context.contentResolver.insert(
            CalendarContract.Events.CONTENT_URI,
            event.toContentValues(calendarId)
        ) ?: error("Failed to create Android calendar event")

        val eventId = requireNotNull(uri.lastPathSegment) {
            "Calendar provider returned invalid uri: $uri"
        }
        logger.d("createEvent success: calendarId=$id, eventId=$eventId")
        event.copy(id = eventId)
    }

    override suspend fun update(event: Event): Boolean = withContext(Dispatchers.IO) {
        logger.d("updateEvent called: calendarId=$id, eventId=${event.id}, title=${event.title}")
        val eventId = event.id?.toLongOrNull() ?: run {
            logger.d("updateEvent skipped: invalid eventId=${event.id}")
            return@withContext false
        }
        val rows = context.contentResolver.update(
            eventId.eventUri(),
            event.toContentValues(calendarId = null),
            null,
            null
        )
        logger.d("updateEvent finished: calendarId=$id, eventId=$eventId, rows=$rows")
        rows > 0
    }

    override suspend fun delete(id: String): Boolean = withContext(Dispatchers.IO) {
        logger.d("deleteEvent called: calendarId=${this@AndroidCalendar.id}, eventId=$id")
        val eventId = id.toLongOrNull() ?: run {
            logger.d("deleteEvent skipped: invalid eventId=$id")
            return@withContext false
        }
        val rows = context.contentResolver.delete(
            eventId.eventUri(),
            null,
            null
        )
        logger.d("deleteEvent finished: calendarId=${this@AndroidCalendar.id}, eventId=$eventId, rows=$rows")
        rows > 0
    }

    private fun queryEvents(
        uri: Uri = CalendarContract.Events.CONTENT_URI,
        selection: String?,
        selectionArgs: Array<String>?,
        sortOrder: String?
    ): List<Event> {
        val projection = arrayOf(
            CalendarContract.Events._ID,
            CalendarContract.Events.TITLE,
            CalendarContract.Events.DESCRIPTION,
            CalendarContract.Events.EVENT_LOCATION,
            CalendarContract.Events.DTSTART,
            CalendarContract.Events.DTEND
        )

        val events = mutableListOf<Event>()
        context.contentResolver.query(uri, projection, selection, selectionArgs, sortOrder)?.use { cursor ->
            while (cursor.moveToNext()) {
                events += cursor.toEvent()
            }
        }
        return events
    }
}

class AndroidCalendarTransactionScope internal constructor(
    private val operations: MutableList<ContentProviderOperation>,
    private val calendarId: Long,
    private val calendarIdString: String
) : TransactionScope {
    override suspend fun create(event: Event): Event {
        logger.d("transaction create queued: calendarId=$calendarIdString, title=${event.title}")
        operations += ContentProviderOperation.newInsert(CalendarContract.Events.CONTENT_URI)
            .withValues(event.toContentValues(calendarId))
            .build()
        logger.d("transaction operation count: calendarId=$calendarIdString, operations=${operations.size}")

        // The provider id is only available after applyBatch commits.
        return event
    }

    override suspend fun update(event: Event): Boolean {
        logger.d("transaction update queued: calendarId=$calendarIdString, eventId=${event.id}, title=${event.title}")
        val eventId = event.id?.toLongOrNull() ?: run {
            logger.d("transaction update skipped: invalid eventId=${event.id}")
            return false
        }
        operations += ContentProviderOperation.newUpdate(eventId.eventUri())
            .withValues(event.toContentValues(calendarId = null))
            .build()
        logger.d("transaction operation count: calendarId=$calendarIdString, operations=${operations.size}")
        return true
    }

    override suspend fun delete(id: String): Boolean {
        logger.d("transaction delete queued: calendarId=$calendarIdString, eventId=$id")
        val eventId = id.toLongOrNull() ?: run {
            logger.d("transaction delete skipped: invalid eventId=$id")
            return false
        }
        operations += ContentProviderOperation.newDelete(eventId.eventUri())
            .build()
        logger.d("transaction operation count: calendarId=$calendarIdString, operations=${operations.size}")
        return true
    }
}

private const val ACCOUNT_NAME = "top.kagg886.calendar.v2"
private const val CALENDAR_COLOR = 0xEA8561

private fun calendarUri(id: Long): Uri =
    ContentUris.withAppendedId(CalendarContract.Calendars.CONTENT_URI, id)

private fun calendarSyncAdapterUri(id: Long? = null): Uri {
    val base = id?.let(::calendarUri) ?: CalendarContract.Calendars.CONTENT_URI
    return base.buildUpon()
        .appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, "true")
        .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_NAME, ACCOUNT_NAME)
        .appendQueryParameter(
            CalendarContract.Calendars.ACCOUNT_TYPE,
            CalendarContract.ACCOUNT_TYPE_LOCAL
        )
        .build()
}

private fun Long.eventUri(): Uri =
    ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, this)

private fun Event.toContentValues(calendarId: Long?): ContentValues = ContentValues().apply {
    calendarId?.let { put(CalendarContract.Events.CALENDAR_ID, it) }
    put(CalendarContract.Events.TITLE, title)
    put(CalendarContract.Events.DESCRIPTION, description)
    put(CalendarContract.Events.DTSTART, startTime.toEpochMillis())
    put(CalendarContract.Events.DTEND, endTime.toEpochMillis())
    put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.currentSystemDefault().id)
    put(CalendarContract.Events.EVENT_LOCATION, location)
}

private fun Cursor.toEvent(): Event {
    val startMillis = getLongValue(CalendarContract.Events.DTSTART)
    val endMillis = getNullableLongValue(CalendarContract.Events.DTEND) ?: startMillis

    return Event(
        id = getLongValue(CalendarContract.Events._ID).toString(),
        title = getStringValue(CalendarContract.Events.TITLE),
        startTime = startMillis.toLocalDateTime(),
        endTime = endMillis.toLocalDateTime(),
        description = getStringValue(CalendarContract.Events.DESCRIPTION),
        location = getStringValue(CalendarContract.Events.EVENT_LOCATION)
    )
}

private fun LocalDateTime.toEpochMillis(): Long =
    toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()

private fun Long.toLocalDateTime(): LocalDateTime =
    Instant.fromEpochMilliseconds(this).toLocalDateTime(TimeZone.currentSystemDefault())

private fun Cursor.getStringValue(column: String): String {
    val index = getColumnIndexOrThrow(column)
    return if (isNull(index)) "" else getString(index).orEmpty()
}

private fun Cursor.getLongValue(column: String): Long =
    getLong(getColumnIndexOrThrow(column))

private fun Cursor.getNullableLongValue(column: String): Long? {
    val index = getColumnIndexOrThrow(column)
    return if (isNull(index)) null else getLong(index)
}

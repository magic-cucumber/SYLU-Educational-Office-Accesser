package top.kagg886.calender

import android.app.Activity
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.ContextWrapper
import android.database.Cursor
import android.net.Uri
import android.provider.CalendarContract
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import top.kagg886.calender.data.Event

internal class AndroidCalenderManager internal constructor(val ctx: Context, account: String) :
    NativeCalenderManager {
    val accountId = getCalenderAccount(account)

    override fun getEvents(): List<Event> {

        val events = mutableListOf<Event>()

        val projection = arrayOf(
            CalendarContract.Events._ID,
            CalendarContract.Events.TITLE,
            CalendarContract.Events.DESCRIPTION,
            CalendarContract.Events.EVENT_LOCATION,
            CalendarContract.Events.DTSTART,
            CalendarContract.Events.DTEND
        )

        val selection = "${CalendarContract.Events.CALENDAR_ID} = ?"
        val selectionArgs = arrayOf(accountId.toString())

        val cursor = ctx.contentResolver.query(
            CalendarContract.Events.CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            "${CalendarContract.Events.DTSTART} ASC" // 排序：按开始时间升序
        )

        cursor?.use {
            while (it.moveToNext()) {

                val id = try {
                    it.getLong(it.getColumnIndexOrThrow(CalendarContract.Events._ID))
                } catch (e: Exception) {
                    -1
                }
                val title = try {
                    it.getString(it.getColumnIndexOrThrow(CalendarContract.Events.TITLE))
                } catch (e: Exception) {
                    ""
                }
                val start = try {
                    it.getLong(it.getColumnIndexOrThrow(CalendarContract.Events.DTSTART))
                } catch (e: Exception) {
                    0
                }
                val end = try {
                    it.getLong(it.getColumnIndexOrThrow(CalendarContract.Events.DTEND))
                } catch (e: Exception) {
                    0
                }
                val description = try {
                    it.getString(it.getColumnIndexOrThrow(CalendarContract.Events.DESCRIPTION))
                } catch (e: Exception) {
                    ""
                }

                val location = try {
                    it.getString(it.getColumnIndexOrThrow(CalendarContract.Events.EVENT_LOCATION))
                } catch (e: Exception) {
                    ""
                }
                events.add(
                    Event(
                        id = id.toString(),
                        title = title,
                        startTime = Instant.fromEpochMilliseconds(start)
                            .toLocalDateTime(TimeZone.currentSystemDefault()),
                        endTime = Instant.fromEpochMilliseconds(end)
                            .toLocalDateTime(TimeZone.currentSystemDefault()),
                        location = location,
                        description = description
                    )
                )
            }
        }

        return events
    }

    override fun clearEvents() {
        // 构建用于删除事件的选择条件：指定只删除属于特定日历ID的事件
        val selection = "${CalendarContract.Events.CALENDAR_ID} = ?"
        val selectionArgs = arrayOf(accountId.toString())

        // 执行删除操作，并返回被删除的事件数量
        ctx.contentResolver.delete(
            CalendarContract.Events.CONTENT_URI,   // 事件内容URI
            selection,            // 选择条件
            selectionArgs         // 选择条件参数
        )
    }

    override fun insertEvent(event: Event) {
        val values = ContentValues().apply {
            put(CalendarContract.Events.CALENDAR_ID, accountId) //账户id
            put(CalendarContract.Events.TITLE, event.title) //标题
            put(CalendarContract.Events.DESCRIPTION, event.description) //描述
            put(CalendarContract.Events.DTSTART, event.startTime.toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()) //开始时间
            put(CalendarContract.Events.DTEND, event.endTime.toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()) //停止时间
            put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.currentSystemDefault().id) //时区
            put(CalendarContract.Events.EVENT_LOCATION,event.location) //位置
            put(CalendarContract.Events.HAS_ALARM,1) //闹钟提醒
        }

        ctx.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
    }

    override fun deleteEvent(event: Event) {
        val deleteUri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, event.id.toLong())
        ctx.contentResolver.delete(deleteUri, null, null)
    }

    override fun updateEvent(event: Event) {
        val values = ContentValues().apply {
            put(CalendarContract.Events.TITLE, event.title)
            put(CalendarContract.Events.DESCRIPTION, event.description)
            put(CalendarContract.Events.DTSTART, event.startTime.toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds())
            put(CalendarContract.Events.DTEND, event.endTime.toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds())
            put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.currentSystemDefault().id)
            put(CalendarContract.Events.EVENT_LOCATION, event.location)
            put(CalendarContract.Events.HAS_ALARM, 1)
        }

        val updateUri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, event.id.toLong())

        ctx.contentResolver.update(updateUri, values, null, null)
    }
}

@Composable
internal actual fun rememberNativeCalenderManager(name:String): NativeCalenderManager {
    val ctx = LocalContext.current
    return remember(name) {
        AndroidCalenderManager(ctx.findActivity()!!,name)
    }
}

private fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}


private fun AndroidCalenderManager.getCalenderAccount(accountName: String): Long {
    val timeZone = TimeZone.currentSystemDefault().id
    val projection =
        arrayOf(CalendarContract.Calendars._ID, CalendarContract.Calendars.ACCOUNT_NAME)
    val selection = "${CalendarContract.Calendars.ACCOUNT_NAME} = ?"
    val selectionArgs = arrayOf(accountName)

    // 查询现有的日历账户
    val cursor: Cursor? = ctx.contentResolver.query(
        CalendarContract.Calendars.CONTENT_URI, projection, selection, selectionArgs, null
    )

    if (cursor != null && cursor.moveToFirst()) {
        // 如果账户存在
        val calId = cursor.getLong(0)
        cursor.close()
        return calId
    } else {
        // 创建新的日历账户
        val values = ContentValues().apply {
            put(CalendarContract.Calendars.ACCOUNT_NAME, accountName)
            put(CalendarContract.Calendars.ACCOUNT_TYPE, CalendarContract.ACCOUNT_TYPE_LOCAL)
            put(CalendarContract.Calendars.NAME, accountName)
            put(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME, accountName)
            put(CalendarContract.Calendars.CALENDAR_COLOR, 0xEA8561)
            put(
                CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL,
                CalendarContract.Calendars.CAL_ACCESS_OWNER
            )
            put(CalendarContract.Calendars.OWNER_ACCOUNT, accountName)
            put(CalendarContract.Calendars.CALENDAR_TIME_ZONE, timeZone)
            put(CalendarContract.Calendars.SYNC_EVENTS, 1)
            put(CalendarContract.Calendars.VISIBLE, 1)
        }

        val uri: Uri? = ctx.contentResolver.insert(
            CalendarContract.Calendars.CONTENT_URI.buildUpon()
                .appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, "true")
                .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_NAME, accountName)
                .appendQueryParameter(
                    CalendarContract.Calendars.ACCOUNT_TYPE,
                    CalendarContract.ACCOUNT_TYPE_LOCAL
                ).build(), values
        )

        return uri?.lastPathSegment?.toLong() ?: -1
    }
}

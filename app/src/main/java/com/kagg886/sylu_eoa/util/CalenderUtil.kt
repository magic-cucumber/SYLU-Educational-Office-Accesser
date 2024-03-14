package com.kagg886.sylu_eoa.util

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.CalendarContract
import com.kagg886.sylu_eoa.getApp


private val app by lazy {
    getApp()
}

data class Event(
    val id: Long = -1,
    val title: String?,
    val description: String?,
    val location: String?,
    val startDate: Long,
    val endDate: Long,
)

private const val TIME_ZONE = "Asia/Shanghai"

data class Calender(private val name:String) {
    private val id = getCalenderAccount(accountName = name)

    fun deleteAccount() {
        val selection = "${CalendarContract.Calendars._ID} = ?" // 使用where字句来指定要删除的日历ID
        app.contentResolver.delete(CalendarContract.Calendars.CONTENT_URI, selection, arrayOf(id.toString()))
    }

    fun clearEvents(): Int {
        // 构建用于删除事件的选择条件：指定只删除属于特定日历ID的事件
        val selection = "${CalendarContract.Events.CALENDAR_ID} = ?"
        val selectionArgs = arrayOf(id.toString())

        // 执行删除操作，并返回被删除的事件数量
        return app.contentResolver.delete(
            CalendarContract.Events.CONTENT_URI,   // 事件内容URI
            selection,            // 选择条件
            selectionArgs         // 选择条件参数
        )
    }

    fun insertEvents(events: List<Event>,alarm: Int): List<Uri?> {
        val results = mutableListOf<Uri?>()

        for (event in events) {
            val values = ContentValues().apply {
                put(CalendarContract.Events.CALENDAR_ID, id) //账户id
                put(CalendarContract.Events.TITLE, event.title) //标题
                put(CalendarContract.Events.DESCRIPTION, event.description) //描述
                put(CalendarContract.Events.DTSTART, event.startDate) //开始时间
                put(CalendarContract.Events.DTEND, event.endDate) //停止时间
                put(CalendarContract.Events.EVENT_TIMEZONE, TIME_ZONE) //市区
                put(CalendarContract.Events.HAS_ALARM,1) //闹钟提醒
            }

            val uri = app.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
            results.add(uri)

            uri?.lastPathSegment?.let { eventId ->
                val reminderValues = ContentValues().apply {
                    put(CalendarContract.Reminders.EVENT_ID, eventId.toLong())
                    put(CalendarContract.Reminders.MINUTES, alarm) // 提醒时间为事件开始前30分钟
                    put(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT)
                }

                app.contentResolver.insert(CalendarContract.Reminders.CONTENT_URI, reminderValues)
            }
        }
        return results
    }

    fun getEvents(context: Context): List<Event> {
        val eventsList = mutableListOf<Event>()
        val projection = arrayOf(
            CalendarContract.Events._ID,
            CalendarContract.Events.TITLE,
            CalendarContract.Events.DESCRIPTION,
            CalendarContract.Events.EVENT_LOCATION,
            CalendarContract.Events.DTSTART,
            CalendarContract.Events.DTEND,
            CalendarContract.Events.CALENDAR_ID
        )

        // 使用日历ID来限定查询范围，找到特定账户下的事件
        val selection = "${CalendarContract.Events.CALENDAR_ID} = ?"
        val selectionArgs = arrayOf("$id")

        val cursor = context.contentResolver.query(
            CalendarContract.Events.CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            null
        )

        cursor?.use {
            while (it.moveToNext()) {
                val id = it.getLong(0)
                val title = it.getString(1)
                val description = it.getString(2)
                val location = it.getString(3)
                val startDate = it.getLong(4)
                val endDate = it.getLong(5)

                eventsList.add(Event(id, title, description, location, startDate, endDate))
            }
        }

        return eventsList
    }
}

private fun getCalenderAccount(accountName: String): Long {
    val projection = arrayOf(CalendarContract.Calendars._ID, CalendarContract.Calendars.ACCOUNT_NAME)
    val selection = "${CalendarContract.Calendars.ACCOUNT_NAME} = ?"
    val selectionArgs = arrayOf(accountName)

    // 查询现有的日历账户
    val cursor: Cursor? = app.contentResolver.query(
        CalendarContract.Calendars.CONTENT_URI, projection, selection, selectionArgs, null)

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
            put(CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL, CalendarContract.Calendars.CAL_ACCESS_OWNER)
            put(CalendarContract.Calendars.OWNER_ACCOUNT, accountName)
            put(CalendarContract.Calendars.CALENDAR_TIME_ZONE, TIME_ZONE)
            put(CalendarContract.Calendars.SYNC_EVENTS, 1)
            put(CalendarContract.Calendars.VISIBLE, 1)
        }

        val uri: Uri? = app.contentResolver.insert(CalendarContract.Calendars.CONTENT_URI.buildUpon().appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, "true")
            .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_NAME, accountName)
            .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_TYPE, CalendarContract.ACCOUNT_TYPE_LOCAL).build(), values)

        return uri?.lastPathSegment?.toLong() ?: -1
    }
}
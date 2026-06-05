package com.memoriabox.utils

import android.content.ContentValues
import android.content.Context
import android.provider.CalendarContract
import android.util.Log
import com.memoriabox.data.model.Event
import java.util.TimeZone

class SystemCalendarHelper(private val context: Context) {
    fun insertEvent(event: Event): Boolean {
        return runCatching {
            if (findExistingEventId(event) != null) return true
            val calendarId = getWritableCalendarId() ?: return false
            val startMillis = event.date
            val endMillis = startMillis + 60 * 60 * 1000L
            val values = ContentValues().apply {
                put(CalendarContract.Events.CALENDAR_ID, calendarId)
                put(CalendarContract.Events.TITLE, event.name)
                put(CalendarContract.Events.DESCRIPTION, event.note)
                put(CalendarContract.Events.DTSTART, startMillis)
                put(CalendarContract.Events.DTEND, endMillis)
                put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
                put(CalendarContract.Events.HAS_ALARM, if (event.reminderEnabled) 1 else 0)
            }
            val uri = context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values) ?: return false
            val eventId = uri.lastPathSegment?.toLongOrNull() ?: return true
            if (event.reminderEnabled) {
                val reminderValues = ContentValues().apply {
                    put(CalendarContract.Reminders.EVENT_ID, eventId)
                    put(CalendarContract.Reminders.MINUTES, event.reminderDays.coerceAtLeast(0) * 24 * 60)
                    put(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT)
                }
                context.contentResolver.insert(CalendarContract.Reminders.CONTENT_URI, reminderValues)
            }
            true
        }.onFailure {
            Log.e("SystemCalendarHelper", "Insert calendar event failed", it)
        }.getOrDefault(false)
    }

    private fun findExistingEventId(event: Event): Long? {
        val projection = arrayOf(CalendarContract.Events._ID)
        val selection = "${CalendarContract.Events.TITLE}=? AND ${CalendarContract.Events.DTSTART}=?"
        val args = arrayOf(event.name, event.date.toString())
        context.contentResolver.query(
            CalendarContract.Events.CONTENT_URI,
            projection,
            selection,
            args,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                return cursor.getLong(cursor.getColumnIndexOrThrow(CalendarContract.Events._ID))
            }
        }
        return null
    }

    private fun getWritableCalendarId(): Long? {
        val projection = arrayOf(CalendarContract.Calendars._ID, CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL)
        context.contentResolver.query(CalendarContract.Calendars.CONTENT_URI, projection, null, null, null)?.use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(CalendarContract.Calendars._ID)
            val accessIndex = cursor.getColumnIndexOrThrow(CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL)
            while (cursor.moveToNext()) {
                val access = cursor.getInt(accessIndex)
                if (access >= CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR) {
                    return cursor.getLong(idIndex)
                }
            }
        }
        return null
    }
}

package com.memoriabox.utils

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.provider.CalendarContract
import android.util.Log
import com.memoriabox.data.model.Event
import java.util.TimeZone

class SystemCalendarHelper(private val context: Context) {

    private val prefs by lazy {
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * 把 App 事件写入系统日历。
     *
     * 去重不能再只靠「标题 + 开始时间」：用户改个名字或改个日期就匹配不上，
     * 会不断插入新事件，旧的还带着闹钟留在系统日历里变成幽灵提醒。
     * 因此以 App 事件 id 为键保存映射，命中映射时走更新而不是插入。
     */
    fun insertEvent(event: Event): Boolean {
        return runCatching {
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

            val existingId = findExistingEventId(event)
            val systemEventId = if (existingId != null) {
                val uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, existingId)
                context.contentResolver.update(uri, values, null, null)
                // 提醒先清空再重建，避免同一条事件累积多个闹钟
                context.contentResolver.delete(
                    CalendarContract.Reminders.CONTENT_URI,
                    "${CalendarContract.Reminders.EVENT_ID}=?",
                    arrayOf(existingId.toString())
                )
                existingId
            } else {
                val uri = context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
                    ?: return false
                uri.lastPathSegment?.toLongOrNull() ?: return true
            }

            if (event.reminderEnabled) {
                val reminderValues = ContentValues().apply {
                    put(CalendarContract.Reminders.EVENT_ID, systemEventId)
                    // 多数 ROM 对提醒提前量有上限（通常一周），
                    // 提前 30 天会算成 43200 分钟被系统截断或直接忽略，这里夹到上限内。
                    put(
                        CalendarContract.Reminders.MINUTES,
                        (event.reminderDays.coerceAtLeast(0) * 24 * 60).coerceAtMost(MAX_REMINDER_MINUTES)
                    )
                    put(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT)
                }
                context.contentResolver.insert(CalendarContract.Reminders.CONTENT_URI, reminderValues)
            }
            rememberMapping(event.id, systemEventId)
            true
        }.onFailure {
            Log.e(TAG, "Insert calendar event failed", it)
        }.getOrDefault(false)
    }

    /** App 事件被删除时同步清理系统日历事件，否则通知会一直残留。 */
    fun deleteEvent(appEventId: String) {
        val systemEventId = prefs.getLong(mappingKey(appEventId), -1L)
        if (systemEventId <= 0) return
        runCatching {
            context.contentResolver.delete(
                CalendarContract.Events.CONTENT_URI,
                "${CalendarContract.Events._ID}=?",
                arrayOf(systemEventId.toString())
            )
        }.onFailure {
            Log.e(TAG, "Delete calendar event failed", it)
        }
        prefs.edit().remove(mappingKey(appEventId)).apply()
    }

    private fun rememberMapping(appEventId: String, systemEventId: Long) {
        prefs.edit().putLong(mappingKey(appEventId), systemEventId).apply()
    }

    private fun mappingKey(appEventId: String) = "event_$appEventId"

    private fun findExistingEventId(event: Event): Long? {
        // 1) 映射优先：改名、改日期后依然能找到同一条系统事件
        val mapped = prefs.getLong(mappingKey(event.id), -1L)
        if (mapped > 0 && existsInCalendar(mapped)) return mapped
        // 2) 回退：兼容早期版本插入过、但没有记录映射的事件
        val selection = "${CalendarContract.Events.TITLE}=? AND ${CalendarContract.Events.DTSTART}=?"
        val args = arrayOf(event.name, event.date.toString())
        context.contentResolver.query(
            CalendarContract.Events.CONTENT_URI,
            arrayOf(CalendarContract.Events._ID),
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

    private fun existsInCalendar(systemEventId: Long): Boolean {
        val uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, systemEventId)
        return runCatching {
            context.contentResolver.query(uri, arrayOf(CalendarContract.Events._ID), null, null, null)
                ?.use { it.moveToFirst() } ?: false
        }.getOrDefault(false)
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

    private companion object {
        const val TAG = "SystemCalendarHelper"
        const val PREFS_NAME = "system_calendar_map"
        /** 一周，覆盖多数 ROM 允许的最大提前提醒量。 */
        const val MAX_REMINDER_MINUTES = 7 * 24 * 60
    }
}

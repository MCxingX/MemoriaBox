package com.memoriabox.utils

import android.app.AlarmManager
import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.memoriabox.MainActivity
import com.memoriabox.data.model.Event
import com.memoriabox.data.model.RepeatMode
import com.memoriabox.receiver.ReminderReceiver
import kotlinx.coroutines.*
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.Calendar

class NotificationHelper(private val context: Context) {
    
    private val TAG = "NotificationHelper"
    
    private val alarmManager: AlarmManager?
    private val prefs = context.getSharedPreferences("pushplus_config", Context.MODE_PRIVATE)
    
    companion object {
        private const val CHANNEL_ID = "memoriabox_reminders"
    }

    init {
        alarmManager = try {
            context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get AlarmManager", e)
            null
        }
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val manager = context.getSystemService(NotificationManager::class.java)
                
                val existingChannel = manager.getNotificationChannel(CHANNEL_ID)
                if (existingChannel == null) {
                    val channel = NotificationChannel(
                        CHANNEL_ID,
                        "纪念日提醒",
                        NotificationManager.IMPORTANCE_DEFAULT
                    ).apply {
                        description = "用于纪念日倒计时提醒"
                        setShowBadge(true)
                        enableVibration(true)
                        enableLights(false)
                    }
                    manager.createNotificationChannel(channel)
                    Log.d(TAG, "Notification channel created")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create notification channel", e)
        }
    }

    fun scheduleReminder(event: Event) {
        if (alarmManager == null) {
            Log.w(TAG, "AlarmManager not available")
            return
        }
        
        try {
            val occurrenceDate = nextOccurrenceDate(event) ?: return
            reminderOffsets(event).forEach { offsetDays ->
                scheduleSingleReminder(event, occurrenceDate, offsetDays)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in scheduleReminder", e)
        }
    }

    private fun scheduleSingleReminder(event: Event, occurrenceDate: Long, offsetDays: Int) {
        val alarmMgr = alarmManager ?: return
        try {
            val calendar = Calendar.getInstance().apply {
                timeInMillis = occurrenceDate
                add(Calendar.DAY_OF_YEAR, -offsetDays)
                set(Calendar.HOUR_OF_DAY, event.alarmTime.substring(0, 2).toInt())
                set(Calendar.MINUTE, event.alarmTime.substring(3, 5).toInt())
                set(Calendar.SECOND, 0)
            }

            val triggerTime = calendar.timeInMillis
            if (triggerTime < System.currentTimeMillis()) {
                Log.d(TAG, "Reminder time already passed: ${event.name}")
                return
            }

            val intent = Intent(context, ReminderReceiver::class.java).apply {
                putExtra("event_id", event.id)
                putExtra("event_title", event.name)
                putExtra("event_note", event.note)
                putExtra("event_date", event.date)
                putExtra("event_type", event.type.name)
                putExtra("event_lunar", event.lunar)
                putExtra("reminder_days", offsetDays)
                putExtra("reminder_offsets", event.reminderOffsets)
                putExtra("alarm_time", event.alarmTime)
                putExtra("pushplus_enabled", event.pushPlusEnabled)
                putExtra("repeat_mode", effectiveRepeatMode(event).name)
                putExtra("repeat_interval", event.repeatInterval.coerceAtLeast(1))
                putExtra("repeat_end_date", event.repeatEndDate ?: 0L)
                putExtra("repeat_count", event.repeatCount)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                event.id.hashCode() + offsetDays,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        if (alarmMgr.canScheduleExactAlarms()) {
                            alarmMgr.setExactAndAllowWhileIdle(
                                AlarmManager.RTC_WAKEUP,
                                triggerTime,
                                pendingIntent
                            )
                            Log.d(TAG, "Exact alarm scheduled: ${event.name}")
                        } else {
                            Log.w(TAG, "Exact alarm permission not granted, using inexact")
                            alarmMgr.setAndAllowWhileIdle(
                                AlarmManager.RTC_WAKEUP,
                                triggerTime,
                                pendingIntent
                            )
                        }
                    } else {
                        alarmMgr.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            triggerTime,
                            pendingIntent
                        )
                    }
                } else {
                    alarmMgr.set(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                }
            } catch (e: SecurityException) {
                Log.e(TAG, "SecurityException scheduling alarm", e)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to schedule reminder", e)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in scheduleSingleReminder", e)
        }
    }

    private fun nextOccurrenceDate(event: Event): Long? {
        if (event.type == com.memoriabox.data.model.EventType.BIRTHDAY && !event.lunar.isNullOrBlank()) {
            return LunarDateUtils.nextOccurrenceMillis(event.lunar)
        }
        val mode = effectiveRepeatMode(event)
        if (mode == RepeatMode.NONE) return event.date

        val now = System.currentTimeMillis()
        val candidate = Calendar.getInstance().apply { timeInMillis = event.date }
        var occurrenceIndex = 1
        while (reminderTriggerTime(candidate.timeInMillis, event) <= now) {
            occurrenceIndex++
            when (mode) {
                RepeatMode.YEARLY -> candidate.add(Calendar.YEAR, event.repeatInterval.coerceAtLeast(1))
                RepeatMode.MONTHLY -> candidate.add(Calendar.MONTH, event.repeatInterval.coerceAtLeast(1))
                RepeatMode.CUSTOM_DAYS -> candidate.add(Calendar.DAY_OF_YEAR, event.repeatInterval.coerceAtLeast(1))
                RepeatMode.CUSTOM_WEEKS -> candidate.add(Calendar.WEEK_OF_YEAR, event.repeatInterval.coerceAtLeast(1))
                RepeatMode.CUSTOM_MONTHS -> candidate.add(Calendar.MONTH, event.repeatInterval.coerceAtLeast(1))
                RepeatMode.NONE -> return event.date
            }
            if (event.repeatEndDate != null && candidate.timeInMillis > event.repeatEndDate) return null
            if (event.repeatCount > 0 && occurrenceIndex > event.repeatCount) return null
        }
        return candidate.timeInMillis
    }

    private fun reminderTriggerTime(date: Long, event: Event): Long {
        return Calendar.getInstance().apply {
            timeInMillis = date
            add(Calendar.DAY_OF_YEAR, -event.reminderDays)
            set(Calendar.HOUR_OF_DAY, event.alarmTime.substring(0, 2).toInt())
            set(Calendar.MINUTE, event.alarmTime.substring(3, 5).toInt())
            set(Calendar.SECOND, 0)
        }.timeInMillis
    }

    private fun effectiveRepeatMode(event: Event): RepeatMode {
        return when {
            event.repeatMode != RepeatMode.NONE -> event.repeatMode
            event.repeatYearly -> RepeatMode.YEARLY
            event.type == com.memoriabox.data.model.EventType.BIRTHDAY -> RepeatMode.YEARLY
            else -> RepeatMode.NONE
        }
    }

    private fun reminderOffsets(event: Event): List<Int> {
        val offsets = event.reminderOffsets.split(",").mapNotNull { it.trim().toIntOrNull() }.filter { it in 0..365 }.distinct()
        return offsets.ifEmpty { listOf(event.reminderDays.coerceIn(0, 365)) }
    }

    fun cancelReminder(event: Event) {
        val alarmMgr = alarmManager ?: return
        try {
            reminderOffsets(event).forEach { offsetDays ->
                val intent = Intent(context, ReminderReceiver::class.java)
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    event.id.hashCode() + offsetDays,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                alarmMgr.cancel(pendingIntent)
            }
            Log.d(TAG, "Reminder cancelled: ${event.name}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cancel reminder", e)
        }
    }

    fun showMonthlySummaryNotification(monthStart: Long) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w(TAG, "Post notifications permission not granted")
            return
        }

        val monthText = java.text.SimpleDateFormat("yyyy年M月", java.util.Locale.getDefault()).format(java.util.Date(monthStart))
        val notificationIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(MainActivity.EXTRA_OPEN_MONTHLY_SUMMARY, true)
            putExtra(MainActivity.EXTRA_MONTHLY_SUMMARY_MONTH_START, monthStart)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            MonthlySummaryHelper.monthKey(monthStart).hashCode(),
            notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("$monthText 月度总结已准备好")
            .setContentText("查看上个月的日记与照片回顾。")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify("monthly_summary_${MonthlySummaryHelper.monthKey(monthStart)}".hashCode(), notification)
    }

    fun getPushPlusToken(): String {
        return prefs.getString("pushplus_token", "") ?: ""
    }

    fun setPushPlusToken(token: String) {
        prefs.edit().putString("pushplus_token", token).apply()
    }

    fun isPushPlusEnabled(): Boolean {
        return prefs.getBoolean("pushplus_enabled", false)
    }

    fun setPushPlusEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("pushplus_enabled", enabled).apply()
    }

    fun getPushPlusChannel(): String {
        return prefs.getString("pushplus_channel", "wechat") ?: "wechat"
    }

    fun setPushPlusChannel(channel: String) {
        prefs.edit().putString("pushplus_channel", channel).apply()
    }

    fun sendPushPlusNotification(title: String, content: String) {
        if (!isPushPlusEnabled()) return
        
        val token = getPushPlusToken()
        if (token.isEmpty()) return

        val channel = getPushPlusChannel()
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL("https://www.pushplus.plus/send")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true

                val jsonPayload = """
                    {
                        "token": "$token",
                        "title": "$title",
                        "content": "$content",
                        "template": "html",
                        "channel": "$channel"
                    }
                """.trimIndent()

                OutputStreamWriter(connection.outputStream).use {
                    it.write(jsonPayload)
                    it.flush()
                }

                val responseCode = connection.responseCode
                if (responseCode == 200) {
                    android.util.Log.d("PushPlus", "Notification sent successfully")
                } else {
                    android.util.Log.e("PushPlus", "Failed to send notification: $responseCode")
                }
            } catch (e: Exception) {
                android.util.Log.e("PushPlus", "Error sending PushPlus notification", e)
            }
        }
    }
}

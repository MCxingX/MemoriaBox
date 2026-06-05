package com.memoriabox.receiver

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.memoriabox.MainActivity
import com.memoriabox.R
import com.memoriabox.data.model.Event
import com.memoriabox.data.model.EventType
import com.memoriabox.data.model.RepeatMode
import com.memoriabox.utils.NotificationHelper

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val eventId = intent.getStringExtra("event_id") ?: return
        val eventTitle = intent.getStringExtra("event_title") ?: "纪念日提醒"
        val eventNote = intent.getStringExtra("event_note") ?: ""
        val pushPlusEnabled = intent.getBooleanExtra("pushplus_enabled", false)
        val repeatMode = runCatching { RepeatMode.valueOf(intent.getStringExtra("repeat_mode") ?: "NONE") }.getOrDefault(RepeatMode.NONE)

        val notificationManager = NotificationManagerCompat.from(context)
        
        val notificationIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("navigate_to_event", eventId)
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            eventId.hashCode(),
            notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = "memoriabox_reminders"
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(eventTitle)
            .setContentText(eventNote.takeIf { it.isNotEmpty() } ?: "您的纪念日即将到来！")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(eventId.hashCode(), notification)

        if (pushPlusEnabled) {
            NotificationHelper(context).sendPushPlusNotification(
                title = eventTitle,
                content = eventNote.takeIf { it.isNotEmpty() } ?: "您的纪念日即将到来！"
            )
        }

        if (repeatMode != RepeatMode.NONE) {
            val helper = NotificationHelper(context)
            helper.scheduleReminder(
                Event(
                    id = eventId,
                    boxId = "",
                    name = eventTitle,
                    date = intent.getLongExtra("event_date", System.currentTimeMillis()),
                    lunar = intent.getStringExtra("event_lunar"),
                    type = runCatching { EventType.valueOf(intent.getStringExtra("event_type") ?: "COUNTDOWN") }.getOrDefault(EventType.COUNTDOWN),
                    note = eventNote,
                    reminderEnabled = true,
                    reminderDays = intent.getIntExtra("reminder_days", 1),
                    reminderOffsets = intent.getStringExtra("reminder_offsets") ?: intent.getIntExtra("reminder_days", 1).toString(),
                    alarmTime = intent.getStringExtra("alarm_time") ?: "09:00",
                    pushPlusEnabled = pushPlusEnabled,
                    repeatMode = repeatMode,
                    repeatInterval = intent.getIntExtra("repeat_interval", 1).coerceAtLeast(1),
                    repeatEndDate = intent.getLongExtra("repeat_end_date", 0L).takeIf { it > 0L },
                    repeatCount = intent.getIntExtra("repeat_count", 0)
                )
            )
        }
    }
}

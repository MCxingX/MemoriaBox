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
import com.memoriabox.utils.NotificationHelper

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val eventId = intent.getStringExtra("event_id") ?: return
        val eventTitle = intent.getStringExtra("event_title") ?: "纪念日提醒"
        val eventNote = intent.getStringExtra("event_note") ?: ""

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

        NotificationHelper(context).sendPushPlusNotification(
            title = eventTitle,
            content = eventNote.takeIf { it.isNotEmpty() } ?: "您的纪念日即将到来！"
        )
    }
}

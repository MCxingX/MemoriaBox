package com.memoriabox.receiver

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.memoriabox.MainActivity
import com.memoriabox.R
import com.memoriabox.utils.AppSettings
import com.memoriabox.utils.HolidayUtils
import java.util.Calendar

class HolidayReminderReceiver : BroadcastReceiver() {

    companion object {
        private const val CHANNEL_ID = "memoriabox_holidays"
        const val ACTION_CHECK = "com.memoriabox.action.CHECK_HOLIDAY"
        private const val REQUEST_CODE = 9101
        private const val DEFAULT_HOUR = 8
        private const val DEFAULT_MINUTE = 0

        private fun ensureChannel(context: Context) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
            val manager = context.getSystemService(NotificationManager::class.java) ?: return
            if (manager.getNotificationChannel(CHANNEL_ID) == null) {
                manager.createNotificationChannel(
                    NotificationChannel(CHANNEL_ID, "节日提醒", NotificationManager.IMPORTANCE_HIGH).apply {
                        description = "用于节日当天提醒"
                    }
                )
            }
        }

        fun schedule(context: Context) {
            ensureChannel(context)
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val intent = Intent(context, HolidayReminderReceiver::class.java).apply {
                action = ACTION_CHECK
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val triggerAt = nextCheckTime()
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
                    } else {
                        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
                    }
                } else {
                    alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
                }
            } catch (e: Exception) {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            }
        }

        fun cancel(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val intent = Intent(context, HolidayReminderReceiver::class.java).apply {
                action = ACTION_CHECK
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
        }

        private fun nextCheckTime(): Long {
            val now = System.currentTimeMillis()
            val next = Calendar.getInstance().apply {
                timeInMillis = now
                set(Calendar.HOUR_OF_DAY, DEFAULT_HOUR)
                set(Calendar.MINUTE, DEFAULT_MINUTE)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                if (timeInMillis <= now) add(Calendar.DAY_OF_YEAR, 1)
            }
            return next.timeInMillis
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_CHECK) return

        if (!AppSettings.getHolidayReminderEnabled(context)) {
            schedule(context)
            return
        }

        val holiday = HolidayUtils.holidayForDay(System.currentTimeMillis())
        if (holiday != null) {
            sendHolidayNotification(context, holiday)
        }
        schedule(context)
    }

    private fun sendHolidayNotification(context: Context, holiday: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        ensureChannel(context)
        try {
            val notificationIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                REQUEST_CODE,
                notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("今天是 $holiday")
                .setContentText("祝您${holiday}快乐，记得记录这份美好。")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()
            NotificationManagerCompat.from(context).notify(REQUEST_CODE, notification)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

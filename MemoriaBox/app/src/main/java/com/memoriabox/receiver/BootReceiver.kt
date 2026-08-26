package com.memoriabox.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.memoriabox.database.AppDatabase
import com.memoriabox.utils.AppSettings
import com.memoriabox.utils.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        if (AppSettings.getHolidayReminderEnabled(context)) {
            HolidayReminderReceiver.schedule(context)
        }
        val appContext = context.applicationContext
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getDatabase(appContext)
                val events = db.eventDao().getAllEventsOnce().filter { it.reminderEnabled }
                val helper = NotificationHelper(appContext)
                events.forEach { event -> helper.scheduleReminder(event) }
            } finally {
                pendingResult.finish()
            }
        }
    }
}

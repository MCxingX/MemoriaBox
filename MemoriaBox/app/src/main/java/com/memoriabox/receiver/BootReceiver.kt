package com.memoriabox.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.memoriabox.utils.AppSettings

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        if (AppSettings.getHolidayReminderEnabled(context)) {
            HolidayReminderReceiver.schedule(context)
        }
    }
}

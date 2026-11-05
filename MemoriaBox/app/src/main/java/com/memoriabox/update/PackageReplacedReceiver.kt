package com.memoriabox.update

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.memoriabox.MainActivity
import com.memoriabox.R
import kotlinx.coroutines.launch

/**
 * 应用覆盖安装完成后触发的收尾处理。
 *
 * 注意：Android 在覆盖安装时会自动终止旧进程并清除 AlarmManager 状态，
 * 所以这里只负责两件事：
 * 1. 清理已下载的安装包；
 * 2. 重新注册所有事件提醒。
 *
 * 更新完成后只发一条通知，不再主动拉起界面——主动拉起会与通知同时打扰用户，
 * 用户点击通知时才会打开应用。
 */
class PackageReplacedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        UpdateManager.removeInstalledApk(context)

        // 重新注册所有事件提醒（应用更新会清除 AlarmManager 状态）
        val appContext = context.applicationContext
        val pendingReschedule = goAsync()
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            try {
                val db = com.memoriabox.database.AppDatabase.getDatabase(appContext)
                val events = db.eventDao().getAllEventsOnce().filter { it.reminderEnabled }
                val helper = com.memoriabox.utils.NotificationHelper(appContext)
                events.forEach { event -> helper.scheduleReminder(event) }
            } finally {
                pendingReschedule.finish()
            }
        }

        notifyUpdateFinished(context)
    }

    private fun notifyUpdateFinished(context: Context) {
        // Android 13+ 未授予通知权限时 notify 会静默失效，先做检查
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val manager = context.getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (manager.getNotificationChannel(CHANNEL_ID) == null) {
                manager.createNotificationChannel(
                    NotificationChannel(CHANNEL_ID, "应用更新", NotificationManager.IMPORTANCE_DEFAULT)
                )
            }
        }

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("念记已更新完成")
            .setContentText("点击打开新版本，查看最新功能")
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()
        runCatching { NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification) }
    }

    companion object {
        private const val CHANNEL_ID = "app_updates"
        private const val NOTIFICATION_ID = 3400
    }
}

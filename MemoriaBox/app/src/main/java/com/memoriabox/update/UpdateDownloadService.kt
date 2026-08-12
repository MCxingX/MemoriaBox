package com.memoriabox.update

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.memoriabox.MainActivity
import com.memoriabox.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class UpdateDownloadService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var runningInfo: UpdateInfo? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val info = intent?.getStringExtra(EXTRA_INFO)?.let { UpdateInfoJson.fromJson(it) }
        if (info == null) {
            stopForegroundCompat()
            stopSelf()
            return START_NOT_STICKY
        }
        if (runningInfo != null && runningInfo?.versionName == info.versionName) {
            return START_NOT_STICKY
        }
        runningInfo = info
        val notification = buildNotification(info, 0, downloading = true)
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            notification,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            } else {
                0
            }
        )
        serviceScope.launch {
            UpdateManager.state.collectLatest { state ->
                when (state) {
                    is UpdateState.Downloading -> {
                        if (canPostNotifications()) {
                            val manager = getSystemService(NotificationManager::class.java)
                            manager.notify(NOTIFICATION_ID, buildNotification(state.info, state.progress, downloading = true))
                        }
                    }
                    is UpdateState.Ready -> {
                        showFinishedNotification(state.info, success = true)
                        finish()
                    }
                    is UpdateState.Error -> {
                        showFinishedNotification(state.info ?: info, success = false)
                        finish()
                    }
                    else -> Unit
                }
            }
        }
        serviceScope.launch {
            UpdateDownloadWorker.enqueue(applicationContext, info)
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    private fun buildNotification(info: UpdateInfo, progress: Int, downloading: Boolean): Notification {
        val contentIntent = PendingIntent.getActivity(
            this,
            NOTIFICATION_REQUEST_CODE,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("正在下载更新 v${info.versionName}")
            .setContentText(if (downloading) "$progress% · 后台下载，息屏不中断" else "准备安装")
            .setOngoing(downloading)
            .setOnlyAlertOnce(true)
            .setContentIntent(contentIntent)
        if (downloading) {
            builder.setProgress(100, progress, false)
        }
        return builder.build()
    }

    private fun showFinishedNotification(info: UpdateInfo, success: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) !=
            android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val contentIntent = PendingIntent.getActivity(
            this,
            NOTIFICATION_REQUEST_CODE,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(MainActivity.EXTRA_OPEN_UPDATE, true)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(if (success) "更新包 v${info.versionName} 已就绪" else "更新下载失败")
            .setContentText(if (success) "点击打开应用并安装更新" else "点击重试或检查网络")
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .build()
        NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, notification)
    }

    private fun canPostNotifications(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED

    private fun finish() {
        stopForegroundCompat()
        runningInfo = null
        stopSelf()
    }

    private fun stopForegroundCompat() {
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_DETACH)
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "更新下载",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "展示版本更新下载进度"
                setShowBadge(false)
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "update_download"
        const val NOTIFICATION_ID = 1001
        private const val NOTIFICATION_REQUEST_CODE = 2001
        private const val EXTRA_INFO = "extra_info"

        fun start(context: Context, info: UpdateInfo) {
            val intent = Intent(context, UpdateDownloadService::class.java).apply {
                putExtra(EXTRA_INFO, UpdateInfoJson.toJson(info))
            }
            ContextCompat.startForegroundService(context, intent)
        }

    }
}

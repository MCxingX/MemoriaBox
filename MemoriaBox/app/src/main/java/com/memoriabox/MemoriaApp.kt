package com.memoriabox

import android.app.Application
import android.content.Context
import android.os.Process
import android.util.Log
import com.memoriabox.database.AppDatabase
import com.memoriabox.receiver.HolidayReminderReceiver
import com.memoriabox.utils.AppSettings
import com.memoriabox.utils.BackupManager

class MemoriaApp : Application() {
    
    private val TAG = "MemoriaApp"

    val database by lazy { 
        try {
            Log.d(TAG, "Initializing database...")
            AppDatabase.getDatabase(this)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize database", e)
            throw e
        }
    }
    
    val backupManager by lazy {
        try {
            Log.d(TAG, "Initializing backup manager...")
            BackupManager(this, database)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize backup manager", e)
            throw e
        }
    }

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        PROCESS_ATTACHED_AT = System.currentTimeMillis()
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Application onCreate")
        
        killIfRunningOldBinary()

        try {
            backupManager.initialize()
            Log.d(TAG, "Backup manager initialized")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize backup manager", e)
        }

        try {
            if (AppSettings.getHolidayReminderEnabled(this)) {
                HolidayReminderReceiver.schedule(this)
                Log.d(TAG, "Holiday reminder scheduled")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule holiday reminder", e)
        }
    }

    private fun killIfRunningOldBinary() {
        runCatching {
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            val lastUpdateTime = packageInfo.lastUpdateTime
            val processStartTime = PROCESS_ATTACHED_AT
            if (lastUpdateTime > processStartTime) {
                Log.w(TAG, "Detected stale process from before last package update. Restarting.")
                Process.killProcess(Process.myPid())
            }
        }
    }
    
    override fun onTerminate() {
        super.onTerminate()
        Log.d(TAG, "Application onTerminate")
    }
    
    override fun onLowMemory() {
        super.onLowMemory()
        Log.w(TAG, "Low memory warning")
    }
    
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        Log.d(TAG, "Trim memory: $level")
    }

    companion object {
        @Volatile
        private var PROCESS_ATTACHED_AT: Long = 0L
    }
}

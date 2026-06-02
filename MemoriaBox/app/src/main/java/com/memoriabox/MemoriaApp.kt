package com.memoriabox

import android.app.Application
import android.util.Log
import com.memoriabox.database.AppDatabase
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

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Application onCreate")
        
        try {
            backupManager.initialize()
            Log.d(TAG, "Backup manager initialized")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize backup manager", e)
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
}

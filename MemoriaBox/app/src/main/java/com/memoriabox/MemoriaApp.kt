package com.memoriabox

import android.app.Application
import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.Process
import android.provider.MediaStore
import android.util.Log
import com.memoriabox.database.AppDatabase
import com.memoriabox.receiver.HolidayReminderReceiver
import com.memoriabox.utils.AppSettings
import com.memoriabox.utils.BackupManager
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
        installCrashHandler(base)
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

        private const val MAX_LOG_FILES = 5
        private const val RELATIVE_PATH = "com.memoriabox/files/crash_logs"

        private fun installCrashHandler(context: Context) {
            val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
            Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
                try {
                    writeCrashLog(context, thread, throwable)
                } catch (_: Throwable) { }
                previousHandler?.uncaughtException(thread, throwable)
            }
        }

        private fun writeCrashLog(context: Context, thread: Thread, throwable: Throwable) {
            val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault()).format(Date())
            val fileName = "crash_$timestamp.txt"

            val sw = StringWriter()
            val pw = PrintWriter(sw)
            pw.println("=== MemoriaBox Crash Report ===")
            pw.println("Time: $timestamp")
            pw.println("Thread: ${thread.name} (id=${thread.id})")
            pw.println("Process: ${android.os.Process.myPid()}")
            pw.println()
            pw.println("Exception: ${throwable.javaClass.name}")
            pw.println("Message: ${throwable.message}")
            pw.println()
            pw.println("Stack Trace:")
            throwable.printStackTrace(pw)
            pw.println()
            pw.println("=== End of Report ===")
            pw.flush()
            val content = sw.toString()

            val written = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                writeViaMediaStore(context, fileName, content)
            } else {
                writeViaEnvironment(fileName, content)
            }

            if (!written) {
                val fallbackDir = File(context.getExternalFilesDir(null) ?: context.filesDir, "crash_logs")
                if (!fallbackDir.exists()) fallbackDir.mkdirs()
                File(fallbackDir, fileName).writeText(content)
            }
        }

        private fun writeViaMediaStore(context: Context, fileName: String, content: String): Boolean {
            return runCatching {
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "text/plain")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/$RELATIVE_PATH")
                }
                val collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI
                val uri = context.contentResolver.insert(collection, values) ?: return false
                context.contentResolver.openOutputStream(uri)?.use { os ->
                    os.write(content.toByteArray())
                } ?: return false
                true
            }.getOrDefault(false)
        }

        private fun writeViaEnvironment(fileName: String, content: String): Boolean {
            return runCatching {
                val dir = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    RELATIVE_PATH
                )
                if (!dir.exists()) dir.mkdirs()
                File(dir, fileName).writeText(content)
                true
            }.getOrDefault(false)
        }
    }
}

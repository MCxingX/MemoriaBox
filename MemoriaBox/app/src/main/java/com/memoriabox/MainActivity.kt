package com.memoriabox

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.memoriabox.ui.theme.AppThemeMode
import com.memoriabox.ui.theme.MemoriaBoxTheme
import com.memoriabox.ui.screen.MainScreen

class MainActivity : ComponentActivity() {
    
    private val TAG = "MainActivity"
    private var pendingMonthlySummaryMonth by mutableStateOf<Long?>(null)

    companion object {
        const val EXTRA_OPEN_MONTHLY_SUMMARY = "open_monthly_summary"
        const val EXTRA_MONTHLY_SUMMARY_MONTH_START = "monthly_summary_month_start"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        Log.d(TAG, "onCreate called")
        pendingMonthlySummaryMonth = extractMonthlySummaryMonth(intent)
        
        try {
            requestRequiredPermissions()
        } catch (e: Exception) {
            Log.e(TAG, "Permission request failed", e)
        }
        
        try {
            setContent {
                val prefs = remember { getSharedPreferences("ui_settings", MODE_PRIVATE) }
                var themeMode by remember {
                    mutableStateOf(
                        AppThemeMode.entries.firstOrNull { it.id == prefs.getString("theme_mode", AppThemeMode.BLUE_WHITE.id) } ?: AppThemeMode.BLUE_WHITE
                    )
                }
                MemoriaBoxTheme(themeMode = themeMode) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        MainScreen(
                            application = application,
                            initialMonthlySummaryMonth = pendingMonthlySummaryMonth,
                            onMonthlySummaryIntentConsumed = { pendingMonthlySummaryMonth = null },
                            currentThemeMode = themeMode,
                            onThemeModeChange = { mode ->
                                themeMode = mode
                                prefs.edit().putString("theme_mode", mode.id).apply()
                            }
                        )
                    }
                }
            }
            Log.d(TAG, "setContent completed")
        } catch (e: Exception) {
            Log.e(TAG, "setContent failed", e)
            throw e
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingMonthlySummaryMonth = extractMonthlySummaryMonth(intent)
    }
    
    private fun requestRequiredPermissions() {
        val permissionsToRequest = mutableListOf<String>()
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        listOf(
            Manifest.permission.READ_CALENDAR,
            Manifest.permission.WRITE_CALENDAR
        ).forEach { permission ->
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(permission)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            try {
                permissionLauncher.launch(permissionsToRequest.toTypedArray())
                Log.d(TAG, "Permission request launched for: ${permissionsToRequest.joinToString()}")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to launch permission request", e)
            }
        } else {
            Log.d(TAG, "All permissions already granted")
        }
    }
    
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.filter { it.value }.keys
        val denied = permissions.filter { !it.value }.keys
        
        Log.d(TAG, "Permissions granted: ${granted.joinToString()}")
        if (denied.isNotEmpty()) {
            Log.w(TAG, "Permissions denied: ${denied.joinToString()}")
        }
    }
    
    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume called")
    }
    
    override fun onPause() {
        super.onPause()
        Log.d(TAG, "onPause called")
    }

    private fun extractMonthlySummaryMonth(intent: Intent?): Long? {
        if (intent?.getBooleanExtra(EXTRA_OPEN_MONTHLY_SUMMARY, false) != true) return null
        return intent.getLongExtra(EXTRA_MONTHLY_SUMMARY_MONTH_START, 0L).takeIf { it > 0L }
    }
}

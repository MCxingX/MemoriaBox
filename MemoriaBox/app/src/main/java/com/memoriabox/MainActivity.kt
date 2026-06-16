package com.memoriabox

import android.Manifest
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.memoriabox.ui.theme.AppThemeMode
import com.memoriabox.ui.theme.NianJiTheme
import com.memoriabox.ui.screen.MainScreen
import com.memoriabox.widget.CalendarWidget
import com.memoriabox.widget.CountdownWidget
import com.memoriabox.widget.MemoriaBoxWidget

class MainActivity : ComponentActivity() {
    
    private val TAG = "MainActivity"
    private var pendingMonthlySummaryMonth by mutableStateOf<Long?>(null)

    companion object {
        const val EXTRA_OPEN_MONTHLY_SUMMARY = "open_monthly_summary"
        const val EXTRA_MONTHLY_SUMMARY_MONTH_START = "monthly_summary_month_start"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applySystemBarStyle(AppThemeMode.BLUE_WHITE)
        
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
                SideEffect {
                    applySystemBarStyle(themeMode)
                }
                NianJiTheme(themeMode = themeMode) {
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
                                refreshWidgetsForThemeChange()
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

    private fun applySystemBarStyle(themeMode: AppThemeMode) {
        val background = when (themeMode) {
            AppThemeMode.DARK -> 0xFF17121A.toInt()
            AppThemeMode.EYE_CARE -> 0xFFFAFCF4.toInt()
            AppThemeMode.PLAYFUL -> 0xFFFFFBFF.toInt()
            AppThemeMode.WARM -> 0xFFFFF8EF.toInt()
            AppThemeMode.CREAM -> 0xFFFFF8EC.toInt()
            AppThemeMode.MINT -> 0xFFF7FFFC.toInt()
            AppThemeMode.LAVENDER -> 0xFFFCF8FF.toInt()
            AppThemeMode.BLUE_WHITE -> 0xFFFFFFFF.toInt()
        }
        val isDarkTheme = themeMode == AppThemeMode.DARK
        val statusBarStyle = if (isDarkTheme) {
            SystemBarStyle.dark(background)
        } else {
            SystemBarStyle.light(background, 0xFF000000.toInt())
        }
        val navigationBarStyle = if (isDarkTheme) {
            SystemBarStyle.dark(background)
        } else {
            SystemBarStyle.light(background, 0xFF000000.toInt())
        }
        enableEdgeToEdge(statusBarStyle = statusBarStyle, navigationBarStyle = navigationBarStyle)
    }

    private fun refreshWidgetsForThemeChange() {
        val manager = AppWidgetManager.getInstance(this)
        listOf(
            MemoriaBoxWidget::class.java,
            CountdownWidget::class.java,
            CalendarWidget::class.java
        ).forEach { widgetClass ->
            val ids = manager.getAppWidgetIds(ComponentName(this, widgetClass))
            if (ids.isNotEmpty()) {
                sendBroadcast(Intent(this, widgetClass).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                })
            }
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

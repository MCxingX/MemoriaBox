package com.memoriabox.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.core.view.WindowCompat
import androidx.compose.ui.unit.dp

enum class AppThemeMode(val id: String, val label: String, val description: String) {
    BLUE_WHITE("blue_white", "蓝白默认", "干净、克制、适合长期使用"),
    DARK("dark", "深色模式", "夜间使用更舒服"),
    EYE_CARE("eye_care", "护眼绿", "柔和低刺激"),
    PLAYFUL("playful", "活泼彩色", "更明亮、更调皮"),
    WARM("warm", "暖橙小米感", "温暖、轻快、有活力")
}

private val BlueWhiteColorScheme = lightColorScheme(
    primary = androidx.compose.ui.graphics.Color(0xFF1677FF),
    onPrimary = androidx.compose.ui.graphics.Color.White,
    primaryContainer = androidx.compose.ui.graphics.Color(0xFFE8F2FF),
    onPrimaryContainer = androidx.compose.ui.graphics.Color(0xFF062A5C),
    secondary = androidx.compose.ui.graphics.Color(0xFF4A90E2),
    onSecondary = androidx.compose.ui.graphics.Color.White,
    secondaryContainer = androidx.compose.ui.graphics.Color(0xFFEAF4FF),
    onSecondaryContainer = androidx.compose.ui.graphics.Color(0xFF0B3058),
    tertiary = androidx.compose.ui.graphics.Color(0xFF13C2C2),
    onTertiary = androidx.compose.ui.graphics.Color.White,
    tertiaryContainer = androidx.compose.ui.graphics.Color(0xFFE6FFFB),
    onTertiaryContainer = androidx.compose.ui.graphics.Color(0xFF003A3A),
    background = androidx.compose.ui.graphics.Color(0xFFF6FAFF),
    surface = androidx.compose.ui.graphics.Color(0xFFFFFFFF),
    surfaceVariant = androidx.compose.ui.graphics.Color(0xFFEFF6FF),
    outline = androidx.compose.ui.graphics.Color(0xFFB7C9DD),
    onBackground = androidx.compose.ui.graphics.Color(0xFF111827),
    onSurface = androidx.compose.ui.graphics.Color(0xFF111827),
    onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFF52677D),
)

private val EyeCareColorScheme = lightColorScheme(
    primary = androidx.compose.ui.graphics.Color(0xFF2E7D32),
    onPrimary = androidx.compose.ui.graphics.Color.White,
    primaryContainer = androidx.compose.ui.graphics.Color(0xFFE7F5E8),
    onPrimaryContainer = androidx.compose.ui.graphics.Color(0xFF0B3D12),
    secondary = androidx.compose.ui.graphics.Color(0xFF6B8E23),
    onSecondary = androidx.compose.ui.graphics.Color.White,
    secondaryContainer = androidx.compose.ui.graphics.Color(0xFFF2F8E5),
    tertiary = androidx.compose.ui.graphics.Color(0xFF4DB6AC),
    background = androidx.compose.ui.graphics.Color(0xFFFAFCF4),
    surface = androidx.compose.ui.graphics.Color(0xFFFFFFFF),
    surfaceVariant = androidx.compose.ui.graphics.Color(0xFFF0F7EA),
    onBackground = androidx.compose.ui.graphics.Color(0xFF172117),
    onSurface = androidx.compose.ui.graphics.Color(0xFF172117),
    onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFF536352),
)

private val PlayfulColorScheme = lightColorScheme(
    primary = androidx.compose.ui.graphics.Color(0xFFFF6B6B),
    onPrimary = androidx.compose.ui.graphics.Color.White,
    primaryContainer = androidx.compose.ui.graphics.Color(0xFFFFE1E4),
    onPrimaryContainer = androidx.compose.ui.graphics.Color(0xFF5E1620),
    secondary = androidx.compose.ui.graphics.Color(0xFF7C5CFF),
    onSecondary = androidx.compose.ui.graphics.Color.White,
    secondaryContainer = androidx.compose.ui.graphics.Color(0xFFE7E0FF),
    tertiary = androidx.compose.ui.graphics.Color(0xFF00B8D9),
    background = androidx.compose.ui.graphics.Color(0xFFFFFBF7),
    surface = androidx.compose.ui.graphics.Color(0xFFFFFFFF),
    surfaceVariant = androidx.compose.ui.graphics.Color(0xFFFFF0F5),
    onBackground = androidx.compose.ui.graphics.Color(0xFF221A22),
    onSurface = androidx.compose.ui.graphics.Color(0xFF221A22),
    onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFF6F5965),
)

private val WarmColorScheme = lightColorScheme(
    primary = androidx.compose.ui.graphics.Color(0xFFFF7A00),
    onPrimary = androidx.compose.ui.graphics.Color.White,
    primaryContainer = androidx.compose.ui.graphics.Color(0xFFFFF0E0),
    onPrimaryContainer = androidx.compose.ui.graphics.Color(0xFF5A2800),
    secondary = androidx.compose.ui.graphics.Color(0xFFFFB020),
    onSecondary = androidx.compose.ui.graphics.Color(0xFF3F2400),
    secondaryContainer = androidx.compose.ui.graphics.Color(0xFFFFF6D8),
    tertiary = androidx.compose.ui.graphics.Color(0xFF1677FF),
    background = androidx.compose.ui.graphics.Color(0xFFFFFBF6),
    surface = androidx.compose.ui.graphics.Color(0xFFFFFFFF),
    surfaceVariant = androidx.compose.ui.graphics.Color(0xFFFFF3E8),
    onBackground = androidx.compose.ui.graphics.Color(0xFF241A10),
    onSurface = androidx.compose.ui.graphics.Color(0xFF241A10),
    onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFF6E5A47),
)

private val DarkColorScheme = darkColorScheme(
    primary = androidx.compose.ui.graphics.Color(0xFFFF8A9A),
    secondary = androidx.compose.ui.graphics.Color(0xFFB8A6FF),
    tertiary = androidx.compose.ui.graphics.Color(0xFF69E8FF),
    background = androidx.compose.ui.graphics.Color(0xFF17121A),
    surface = androidx.compose.ui.graphics.Color(0xFF221B28),
    surfaceVariant = androidx.compose.ui.graphics.Color(0xFF342637),
    onPrimary = androidx.compose.ui.graphics.Color(0xFF3B0710),
    onSecondary = androidx.compose.ui.graphics.Color(0xFF150736),
    onBackground = androidx.compose.ui.graphics.Color.White,
    onSurface = androidx.compose.ui.graphics.Color.White,
    onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFFE5D6E0),
)

private val MemoriaShapes = Shapes(
    extraSmall = RoundedCornerShape(12.dp),
    small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(22.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(34.dp)
)

@Composable
fun MemoriaBoxTheme(
    themeMode: AppThemeMode = AppThemeMode.BLUE_WHITE,
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        themeMode == AppThemeMode.DARK -> DarkColorScheme
        themeMode == AppThemeMode.EYE_CARE -> EyeCareColorScheme
        themeMode == AppThemeMode.PLAYFUL -> PlayfulColorScheme
        themeMode == AppThemeMode.WARM -> WarmColorScheme
        else -> BlueWhiteColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.surface.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = MemoriaShapes,
        content = content
    )
}

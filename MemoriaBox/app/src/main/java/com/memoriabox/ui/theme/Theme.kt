package com.memoriabox.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

enum class AppThemeMode(val id: String, val label: String, val description: String) {
    BLUE_WHITE("blue_white", "蓝白默认", "干净、克制、适合长期使用"),
    DARK("dark", "深色模式", "夜间使用更舒服"),
    EYE_CARE("eye_care", "护眼绿", "柔和低刺激"),
    PLAYFUL("playful", "活泼彩色", "更明亮、更调皮"),
    WARM("warm", "暖橙小米感", "温暖、轻快、有活力"),
    CREAM("cream", "奶油米白", "轻松、温柔、像纸质手账"),
    MINT("mint", "薄荷清晨", "清爽、安静、适合日常记录"),
    LAVENDER("lavender", "薰衣草雾", "柔软、舒缓、带一点浪漫")
}

enum class AppThemeGroup(val label: String) {
    RECOMMENDED("推荐"), EYE_CARE("护眼"), PLAYFUL("个性"), DARK("暗色")
}

val AppThemeMode.group: AppThemeGroup
    get() = when (this) {
        AppThemeMode.DARK -> AppThemeGroup.DARK
        AppThemeMode.EYE_CARE -> AppThemeGroup.EYE_CARE
        AppThemeMode.PLAYFUL -> AppThemeGroup.PLAYFUL
        AppThemeMode.BLUE_WHITE, AppThemeMode.WARM, AppThemeMode.CREAM, AppThemeMode.MINT, AppThemeMode.LAVENDER -> AppThemeGroup.RECOMMENDED
    }

@Immutable
data class MemoriaThemeTokens(
    val calendarBackground: Color,
    val calendarCard: Color,
    val calendarToday: Color,
    val calendarSelected: Color,
    val calendarSelectedContent: Color,
    val diaryMarker: Color,
    val anniversaryMarker: Color,
    val festivalMarker: Color,
    val todoMarker: Color,
    val heatLow: Color,
    val heatHigh: Color,
    val gentleWarning: Color,
    val success: Color
)

val LocalMemoriaThemeTokens = staticCompositionLocalOf {
    MemoriaThemeTokens(
        calendarBackground = Color(0xFFF6FAFF),
        calendarCard = Color.White,
        calendarToday = Color(0xFF1677FF),
        calendarSelected = Color(0xFF062A5C),
        calendarSelectedContent = Color.White,
        diaryMarker = Color(0xFF1677FF),
        anniversaryMarker = Color(0xFFFF7A00),
        festivalMarker = Color(0xFFD946EF),
        todoMarker = Color(0xFF0F9F8E),
        heatLow = Color(0xFFE8F2FF),
        heatHigh = Color(0xFF1677FF),
        gentleWarning = Color(0xFFFFB020),
        success = Color(0xFF2E7D32)
    )
}

private fun memoriaThemeTokens(themeMode: AppThemeMode, scheme: androidx.compose.material3.ColorScheme): MemoriaThemeTokens = when (themeMode) {
    AppThemeMode.DARK -> MemoriaThemeTokens(
        calendarBackground = Color(0xFF17121A),
        calendarCard = Color(0xFF221B28),
        calendarToday = Color(0xFFFFC2CC),
        calendarSelected = Color(0xFFFF8A9A),
        calendarSelectedContent = Color(0xFF3B0710),
        diaryMarker = Color(0xFF91D7FF),
        anniversaryMarker = Color(0xFFFFC078),
        festivalMarker = Color(0xFFE6B8FF),
        todoMarker = Color(0xFF83E6C8),
        heatLow = Color(0xFF342637),
        heatHigh = Color(0xFFFF8A9A),
        gentleWarning = Color(0xFFFFC078),
        success = Color(0xFF83E6C8)
    )
    AppThemeMode.EYE_CARE -> MemoriaThemeTokens(
        calendarBackground = Color(0xFFFAFCF4),
        calendarCard = Color.White,
        calendarToday = Color(0xFF2E7D32),
        calendarSelected = Color(0xFF0B3D12),
        calendarSelectedContent = Color.White,
        diaryMarker = Color(0xFF2E7D32),
        anniversaryMarker = Color(0xFFB7791F),
        festivalMarker = Color(0xFF4DB6AC),
        todoMarker = Color(0xFF6B8E23),
        heatLow = Color(0xFFE7F5E8),
        heatHigh = Color(0xFF2E7D32),
        gentleWarning = Color(0xFFFFB020),
        success = Color(0xFF2E7D32)
    )
    AppThemeMode.PLAYFUL -> MemoriaThemeTokens(
        calendarBackground = Color(0xFFFFFBF7),
        calendarCard = Color.White,
        calendarToday = Color(0xFFFF6B6B),
        calendarSelected = Color(0xFF7C5CFF),
        calendarSelectedContent = Color.White,
        diaryMarker = Color(0xFF00B8D9),
        anniversaryMarker = Color(0xFFFF6B6B),
        festivalMarker = Color(0xFF7C5CFF),
        todoMarker = Color(0xFF2ECA8B),
        heatLow = Color(0xFFFFE1E4),
        heatHigh = Color(0xFFFF6B6B),
        gentleWarning = Color(0xFFFFB020),
        success = Color(0xFF2ECA8B)
    )
    else -> MemoriaThemeTokens(
        calendarBackground = scheme.background,
        calendarCard = scheme.surface,
        calendarToday = scheme.primary,
        calendarSelected = scheme.primaryContainer,
        calendarSelectedContent = scheme.onPrimaryContainer,
        diaryMarker = scheme.primary,
        anniversaryMarker = scheme.secondary,
        festivalMarker = scheme.tertiary,
        todoMarker = Color(0xFF0F9F8E),
        heatLow = scheme.primaryContainer.copy(alpha = 0.42f),
        heatHigh = scheme.primary,
        gentleWarning = Color(0xFFFFB020),
        success = Color(0xFF2E7D32)
    )
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

private val CreamColorScheme = lightColorScheme(
    primary = androidx.compose.ui.graphics.Color(0xFFD97706),
    onPrimary = androidx.compose.ui.graphics.Color.White,
    primaryContainer = androidx.compose.ui.graphics.Color(0xFFFFE8C2),
    onPrimaryContainer = androidx.compose.ui.graphics.Color(0xFF4B2500),
    secondary = androidx.compose.ui.graphics.Color(0xFFB7791F),
    onSecondary = androidx.compose.ui.graphics.Color.White,
    secondaryContainer = androidx.compose.ui.graphics.Color(0xFFFFF2D8),
    tertiary = androidx.compose.ui.graphics.Color(0xFFE88C6D),
    background = androidx.compose.ui.graphics.Color(0xFFFFFCF5),
    surface = androidx.compose.ui.graphics.Color(0xFFFFF8EC),
    surfaceVariant = androidx.compose.ui.graphics.Color(0xFFF7EAD8),
    onBackground = androidx.compose.ui.graphics.Color(0xFF241A10),
    onSurface = androidx.compose.ui.graphics.Color(0xFF241A10),
    onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFF6C5841),
)

private val MintColorScheme = lightColorScheme(
    primary = androidx.compose.ui.graphics.Color(0xFF0F9F8E),
    onPrimary = androidx.compose.ui.graphics.Color.White,
    primaryContainer = androidx.compose.ui.graphics.Color(0xFFD9F7F2),
    onPrimaryContainer = androidx.compose.ui.graphics.Color(0xFF003C35),
    secondary = androidx.compose.ui.graphics.Color(0xFF5BAE8C),
    onSecondary = androidx.compose.ui.graphics.Color.White,
    secondaryContainer = androidx.compose.ui.graphics.Color(0xFFE8F8EF),
    tertiary = androidx.compose.ui.graphics.Color(0xFF5B8DEF),
    background = androidx.compose.ui.graphics.Color(0xFFF7FFFC),
    surface = androidx.compose.ui.graphics.Color(0xFFFFFFFF),
    surfaceVariant = androidx.compose.ui.graphics.Color(0xFFE8F7F3),
    onBackground = androidx.compose.ui.graphics.Color(0xFF10201D),
    onSurface = androidx.compose.ui.graphics.Color(0xFF10201D),
    onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFF4C6861),
)

private val LavenderColorScheme = lightColorScheme(
    primary = androidx.compose.ui.graphics.Color(0xFF8B5CF6),
    onPrimary = androidx.compose.ui.graphics.Color.White,
    primaryContainer = androidx.compose.ui.graphics.Color(0xFFEDE7FF),
    onPrimaryContainer = androidx.compose.ui.graphics.Color(0xFF2D145C),
    secondary = androidx.compose.ui.graphics.Color(0xFFB56CC7),
    onSecondary = androidx.compose.ui.graphics.Color.White,
    secondaryContainer = androidx.compose.ui.graphics.Color(0xFFF7E8FB),
    tertiary = androidx.compose.ui.graphics.Color(0xFFFF8FB3),
    background = androidx.compose.ui.graphics.Color(0xFFFCF8FF),
    surface = androidx.compose.ui.graphics.Color(0xFFFFFFFF),
    surfaceVariant = androidx.compose.ui.graphics.Color(0xFFF3ECFA),
    onBackground = androidx.compose.ui.graphics.Color(0xFF211827),
    onSurface = androidx.compose.ui.graphics.Color(0xFF211827),
    onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFF65566E),
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
        themeMode == AppThemeMode.CREAM -> CreamColorScheme
        themeMode == AppThemeMode.MINT -> MintColorScheme
        themeMode == AppThemeMode.LAVENDER -> LavenderColorScheme
        else -> BlueWhiteColorScheme
    }

    CompositionLocalProvider(LocalMemoriaThemeTokens provides memoriaThemeTokens(themeMode, colorScheme)) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = MemoriaShapes,
            content = content
        )
    }
}

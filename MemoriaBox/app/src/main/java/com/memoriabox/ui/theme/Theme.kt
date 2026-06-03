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

private val LightColorScheme = lightColorScheme(
    primary = androidx.compose.ui.graphics.Color(0xFFFF6B6B),
    onPrimary = androidx.compose.ui.graphics.Color.White,
    primaryContainer = androidx.compose.ui.graphics.Color(0xFFFFE1E4),
    onPrimaryContainer = androidx.compose.ui.graphics.Color(0xFF5E1620),
    secondary = androidx.compose.ui.graphics.Color(0xFF7C5CFF),
    onSecondary = androidx.compose.ui.graphics.Color.White,
    secondaryContainer = androidx.compose.ui.graphics.Color(0xFFE7E0FF),
    onSecondaryContainer = androidx.compose.ui.graphics.Color(0xFF241258),
    tertiary = androidx.compose.ui.graphics.Color(0xFF00B8D9),
    onTertiary = androidx.compose.ui.graphics.Color.White,
    tertiaryContainer = androidx.compose.ui.graphics.Color(0xFFC6F6FF),
    onTertiaryContainer = androidx.compose.ui.graphics.Color(0xFF00363F),
    background = androidx.compose.ui.graphics.Color(0xFFFFFBF7),
    surface = androidx.compose.ui.graphics.Color(0xFFFFFFFF),
    surfaceVariant = androidx.compose.ui.graphics.Color(0xFFFFF0F5),
    outline = androidx.compose.ui.graphics.Color(0xFFD8C6D0),
    onBackground = androidx.compose.ui.graphics.Color(0xFF221A22),
    onSurface = androidx.compose.ui.graphics.Color(0xFF221A22),
    onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFF6F5965),
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
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
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

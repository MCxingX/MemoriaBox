package com.memoriabox.ui.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class AdaptiveUiSize(
    val compact: Boolean,
    val roomy: Boolean,
    val screenPadding: Dp,
    val sectionSpacing: Dp,
    val topBarHeight: Dp,
    val buttonHeight: Dp,
    val heroMinHeight: Dp,
    val cardRadius: Dp
)

@Composable
fun rememberAdaptiveUiSize(): AdaptiveUiSize {
    val configuration = LocalConfiguration.current
    val shortest = minOf(configuration.screenWidthDp, configuration.screenHeightDp)
    val tallest = maxOf(configuration.screenWidthDp, configuration.screenHeightDp)
    return remember(shortest, tallest) {
        val compact = shortest < 375 || tallest < 720
        val roomy = shortest > 414 || shortest >= 600 || tallest >= 1000
        AdaptiveUiSize(
            compact = compact,
            roomy = roomy,
            screenPadding = when {
                compact -> 16.dp
                roomy -> 24.dp
                else -> 16.dp
            },
            sectionSpacing = when {
                compact -> 8.dp
                roomy -> 16.dp
                else -> 8.dp
            },
            topBarHeight = when {
                compact -> 48.dp
                roomy -> 56.dp
                else -> 48.dp
            },
            buttonHeight = 48.dp,
            heroMinHeight = when {
                compact -> 128.dp
                roomy -> 176.dp
                else -> 144.dp
            },
            cardRadius = if (compact) 16.dp else 24.dp
        )
    }
}

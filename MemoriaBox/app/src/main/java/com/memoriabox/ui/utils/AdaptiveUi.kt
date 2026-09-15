package com.memoriabox.ui.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class AdaptiveUiSize(
    val compact: Boolean,
    val roomy: Boolean,
    val screenPadding: Dp,
    val sectionSpacing: Dp,
    val tightSpacing: Dp,
    val contentSpacing: Dp,
    val topBarHeight: Dp,
    val buttonHeight: Dp,
    val chipHeight: Dp,
    val cardRadius: Dp,
    val cardPadding: Dp,
    val listItemMinHeight: Dp,
    val homeRowMinHeight: Dp,
    val iconSmall: Dp,
    val iconMedium: Dp,
    val iconLarge: Dp,
    val logoMarkSize: Dp,
    val filterMinWidth: Dp,
    val filterMaxWidth: Dp,
    val maxContentWidth: Dp
)

@Composable
fun rememberAdaptiveUiSize(): AdaptiveUiSize {
    val configuration = LocalConfiguration.current
    val fontScale = LocalDensity.current.fontScale
    val width = configuration.screenWidthDp
    val height = configuration.screenHeightDp
    val shortest = minOf(configuration.screenWidthDp, configuration.screenHeightDp)
    val tallest = maxOf(configuration.screenWidthDp, configuration.screenHeightDp)
    return remember(width, height, shortest, tallest, fontScale) {
        val compact = shortest < 360 || tallest < 680 || fontScale >= 1.2f
        val tablet = shortest >= 600
        val roomy = tablet || width >= 430 || tallest >= 960
        AdaptiveUiSize(
            compact = compact,
            roomy = roomy,
            screenPadding = when {
                tablet -> 28.dp
                compact -> 14.dp
                roomy -> 20.dp
                else -> 16.dp
            },
            sectionSpacing = when {
                tablet -> 18.dp
                compact -> 8.dp
                roomy -> 12.dp
                else -> 10.dp
            },
            tightSpacing = when {
                compact -> 4.dp
                roomy -> 8.dp
                else -> 6.dp
            },
            contentSpacing = when {
                tablet -> 12.dp
                compact -> 8.dp
                roomy -> 12.dp
                else -> 10.dp
            },
            topBarHeight = when {
                compact -> 48.dp
                roomy -> 56.dp
                else -> 48.dp
            },
            buttonHeight = if (compact) 44.dp else 48.dp,
            chipHeight = when {
                compact -> 36.dp
                roomy -> 44.dp
                else -> 40.dp
            },
            cardRadius = when {
                compact -> 14.dp
                tablet -> 20.dp
                else -> 16.dp
            },
            cardPadding = when {
                compact -> 12.dp
                tablet -> 18.dp
                else -> 14.dp
            },
            listItemMinHeight = if (compact) 92.dp else 104.dp,
            homeRowMinHeight = when {
                compact -> 72.dp
                roomy -> 88.dp
                else -> 80.dp
            },
            iconSmall = if (compact) 16.dp else 20.dp,
            iconMedium = if (compact) 20.dp else 24.dp,
            iconLarge = if (compact) 28.dp else 32.dp,
            logoMarkSize = if (compact) 28.dp else 32.dp,
            filterMinWidth = if (compact) 84.dp else 96.dp,
            filterMaxWidth = if (compact) 128.dp else if (roomy) 180.dp else 150.dp,
            maxContentWidth = if (tablet) 760.dp else 640.dp
        )
    }
}

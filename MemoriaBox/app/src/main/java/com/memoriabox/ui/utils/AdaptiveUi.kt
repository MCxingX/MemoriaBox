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
    val topBarHeight: Dp,
    val buttonHeight: Dp,
    val cardRadius: Dp,
    val cardPadding: Dp,
    val listItemMinHeight: Dp,
    val maxContentWidth: Dp,
    /** 是否折叠首页每日一言卡片：只按屏幕高度决定，不因系统字号变大而隐藏功能。 */
    val hideHeroCard: Boolean = false
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
        // 三个字段统一使用 tablet > compact > roomy > 默认 的优先级，避免大屏大字号下留白与间距比例失衡。
        val textScale = fontScale.coerceIn(1f, 1.6f)
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
            topBarHeight = when {
                tablet -> 56.dp
                compact -> 48.dp
                roomy -> 56.dp
                else -> 48.dp
            },
            // 按钮与列表项随系统字号放大，避免 1.5x/2.0x 字号下文字被裁切。
            buttonHeight = (48f * textScale).dp,
            cardRadius = 16.dp,
            cardPadding = when {
                tablet -> 18.dp
                compact -> 12.dp
                else -> 14.dp
            },
            listItemMinHeight = ((if (compact) 92f else 104f) * textScale).dp,
            maxContentWidth = if (tablet) 760.dp else 640.dp,
            // 仅在屏幕高度确实不足时折叠卡片，与字号无关。
            hideHeroCard = tallest < 640 && height < 640
        )
    }
}

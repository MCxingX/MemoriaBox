package com.memoriabox.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class MemoriaSpacing(
    val xs: Dp = 6.dp,
    val sm: Dp = 10.dp,
    val md: Dp = 14.dp,
    val lg: Dp = 20.dp,
    val xl: Dp = 28.dp,
    val cardPadding: Dp = 14.dp
)

object MemoriaDesign {
    val spacing = MemoriaSpacing()
    val cardRadius = 16.dp
    val compactCardRadius = 14.dp
    val sheetRadius = 22.dp
    val softShadow = 3.dp
    val liftedShadow = 6.dp
    val maxContentWidth = 640.dp
    val wideContentWidth = 760.dp
}

@Composable
fun MemoriaBoxLogoMark(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.primary
) {
    val accentColor = MaterialTheme.colorScheme.tertiary
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size / 3))
            .background(
                Brush.linearGradient(
                    colors = listOf(containerColor, MaterialTheme.colorScheme.surface),
                    start = Offset.Zero,
                    end = Offset(120f, 120f)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.foundation.Canvas(modifier = Modifier.size(size * 0.68f)) {
            val markWidth = this.size.width
            val markHeight = this.size.height
            val strokeWidth = markWidth * 0.055f
            val boxPath = Path().apply {
                moveTo(markWidth * 0.12f, markHeight * 0.34f)
                lineTo(markWidth * 0.50f, markHeight * 0.14f)
                lineTo(markWidth * 0.88f, markHeight * 0.34f)
                lineTo(markWidth * 0.88f, markHeight * 0.78f)
                lineTo(markWidth * 0.50f, markHeight * 0.96f)
                lineTo(markWidth * 0.12f, markHeight * 0.78f)
                close()
            }
            drawPath(boxPath, color = contentColor.copy(alpha = 0.18f))
            drawPath(boxPath, color = contentColor, style = Stroke(width = strokeWidth))
            drawRoundRect(
                color = contentColor.copy(alpha = 0.92f),
                topLeft = Offset(markWidth * 0.30f, markHeight * 0.40f),
                size = androidx.compose.ui.geometry.Size(markWidth * 0.24f, markHeight * 0.26f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(markWidth * 0.04f)
            )
            drawCircle(
                color = accentColor,
                radius = markWidth * 0.075f,
                center = Offset(markWidth * 0.68f, markHeight * 0.58f)
            )
        }
    }
}

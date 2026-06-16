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
fun NianJiLogoMark(
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
        androidx.compose.foundation.Canvas(modifier = Modifier.size(size * 0.64f)) {
            val w = this.size.width
            val h = this.size.height
            val sw = w * 0.045f

            // Envelope body
            val bodyPath = Path().apply {
                moveTo(w * 0.15f, h * 0.44f)
                lineTo(w * 0.85f, h * 0.44f)
                lineTo(w * 0.85f, h * 0.82f)
                lineTo(w * 0.80f, h * 0.88f)
                lineTo(w * 0.20f, h * 0.88f)
                lineTo(w * 0.15f, h * 0.82f)
                close()
            }
            drawPath(bodyPath, color = contentColor.copy(alpha = 0.25f))
            drawPath(bodyPath, color = contentColor, style = Stroke(width = sw))

            // Envelope flap
            val flapPath = Path().apply {
                moveTo(w * 0.15f, h * 0.44f)
                lineTo(w * 0.50f, h * 0.16f)
                lineTo(w * 0.85f, h * 0.44f)
                close()
            }
            drawPath(flapPath, color = contentColor.copy(alpha = 0.12f))
            drawPath(flapPath, color = contentColor, style = Stroke(width = sw))

            // Heart seal on flap
            val cx = w * 0.50f
            val cy = h * 0.40f
            val heartR = w * 0.14f
            val heartPath = Path().apply {
                moveTo(cx, cy + heartR * 0.3f)
                cubicTo(cx - heartR, cy - heartR * 0.5f, cx - heartR, cy + heartR * 0.2f, cx, cy + heartR)
                cubicTo(cx + heartR, cy + heartR * 0.2f, cx + heartR, cy - heartR * 0.5f, cx, cy + heartR * 0.3f)
            }
            drawPath(heartPath, color = accentColor)
        }
    }
}

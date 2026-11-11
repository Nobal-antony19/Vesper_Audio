package com.vesper.audio.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.vesper.audio.ui.theme.VesperBurgundyAccent
import com.vesper.audio.ui.theme.VesperTextPrimary
import com.vesper.audio.ui.theme.VesperTextSecondary

/**
 * Premium Vesper Cosmic Crescent Logo Composable.
 * Features:
 * - Luminous crescent moon with gradient fill (cream -> amber -> burgundy)
 * - 5 vertical frequency waveform bars with rounded caps
 * - Right pulse dot
 * - 4-pointed radiant cosmic star at top right
 * - Optional animated breathing pulse for splash / hero surfaces
 */
@Composable
fun VesperLogo(
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
    animated: Boolean = false
) {
    val infiniteTransition = rememberInfiniteTransition(label = "vesper_logo_anim")
    val pulseScale by if (animated) {
        infiniteTransition.animateFloat(
            initialValue = 0.95f,
            targetValue = 1.05f,
            animationSpec = infiniteRepeatable(
                animation = tween(1400, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulse_scale"
        )
    } else {
        rememberInfiniteTransition(label = "static").animateFloat(
            initialValue = 1f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(1000)),
            label = "static_scale"
        )
    }

    Box(modifier = modifier.size(size)) {
        Canvas(modifier = Modifier.size(size)) {
            val w = this.size.width * pulseScale
            val h = this.size.height * pulseScale
            val offsetX = (this.size.width - w) / 2f
            val offsetY = (this.size.height - h) / 2f

            // Scaling from 108x108 viewBox
            fun sx(x: Float): Float = offsetX + (x / 108f) * w
            fun sy(y: Float): Float = offsetY + (y / 108f) * h

            // 1. Crescent Moon Gradient
            val crescentBrush = Brush.linearGradient(
                colors = listOf(
                    Color(0xFFFFF1D5), // Cream
                    Color(0xFFE8B07A), // Radiant Amber
                    Color(0xFF722C42)  // Burgundy Accent
                ),
                start = Offset(sx(27f), sy(26f)),
                end = Offset(sx(64f), sy(82f))
            )

            // Crescent Moon Path
            // M 64,26 C 41,26 27,38 27,54 C 27,70 41,82 64,82 C 55,77 47,67 47,54 C 47,41 55,31 64,26 Z
            val crescentPath = Path().apply {
                moveTo(sx(64f), sy(26f))
                cubicTo(sx(41f), sy(26f), sx(27f), sy(38f), sx(27f), sy(54f))
                cubicTo(sx(27f), sy(70f), sx(41f), sy(82f), sx(64f), sy(82f))
                cubicTo(sx(55f), sy(77f), sx(47f), sy(67f), sx(47f), sy(54f))
                cubicTo(sx(47f), sy(41f), sx(55f), sy(31f), sx(64f), sy(26f))
                close()
            }
            drawPath(path = crescentPath, brush = crescentBrush, style = Fill)

            // 2. Waveform Bars
            val barStrokeWidth = (2.9f / 108f) * w

            // Bar 1 (Leftmost small bar)
            drawLine(
                color = Color(0xFF722C42),
                start = Offset(sx(50f), sy(51f)),
                end = Offset(sx(50f), sy(57f)),
                strokeWidth = barStrokeWidth,
                cap = StrokeCap.Round
            )

            // Bar 2 (Medium)
            drawLine(
                color = Color(0xFFA84462),
                start = Offset(sx(55f), sy(44f)),
                end = Offset(sx(55f), sy(64f)),
                strokeWidth = barStrokeWidth * 1.05f,
                cap = StrokeCap.Round
            )

            // Bar 3 (Center High Bar with Luminous Gradient)
            val centerBarBrush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFFFFF1D5),
                    Color(0xFFF5E7CC),
                    Color(0xFF722C42)
                ),
                startY = sy(37f),
                endY = sy(71f)
            )
            drawLine(
                brush = centerBarBrush,
                start = Offset(sx(61f), sy(37f)),
                end = Offset(sx(61f), sy(71f)),
                strokeWidth = barStrokeWidth * 1.15f,
                cap = StrokeCap.Round
            )

            // Bar 4 (Secondary High Bar)
            drawLine(
                color = Color(0xFFD4869C),
                start = Offset(sx(67f), sy(43f)),
                end = Offset(sx(67f), sy(65f)),
                strokeWidth = barStrokeWidth * 1.05f,
                cap = StrokeCap.Round
            )

            // Bar 5 (Medium-Low)
            drawLine(
                color = Color(0xFF722C42),
                start = Offset(sx(72f), sy(48f)),
                end = Offset(sx(72f), sy(60f)),
                strokeWidth = barStrokeWidth,
                cap = StrokeCap.Round
            )

            // 3. Dot Pulse 6
            drawCircle(
                color = Color(0xFFFFF1D5),
                radius = barStrokeWidth * 0.55f,
                center = Offset(sx(77f), sy(54f))
            )

            // 4. 4-pointed Radiant Star at Top Right
            // M 70,22 Q 70,26 66,26 Q 70,26 70,30 Q 70,26 74,26 Q 70,26 70,22 Z
            val starPath = Path().apply {
                moveTo(sx(70f), sy(22f))
                quadraticTo(sx(70f), sy(26f), sx(66f), sy(26f))
                quadraticTo(sx(70f), sy(26f), sx(70f), sy(30f))
                quadraticTo(sx(70f), sy(26f), sx(74f), sy(26f))
                quadraticTo(sx(70f), sy(26f), sx(70f), sy(22f))
                close()
            }
            drawPath(path = starPath, color = Color(0xFFFFF1D5), style = Fill)
        }
    }
}

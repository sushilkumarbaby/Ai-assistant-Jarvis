package com.jarvis.assistant.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jarvis.assistant.ui.theme.CyanGlow
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ArcReactorView(
    modifier: Modifier = Modifier,
    size: Dp = 120.dp,
    isActive: Boolean = false,
    coreColor: Color = CyanGlow
) {
    val infiniteTransition = rememberInfiniteTransition(label = "reactor")

    val rotationCW by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isActive) 4000 else 12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotationCW"
    )

    val rotationCCW by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isActive) 3000 else 8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotationCCW"
    )

    val corePulse by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isActive) 600 else 1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "corePulse"
    )

    Canvas(modifier = modifier.size(size)) {
        val center = Offset(this.size.width / 2f, this.size.height / 2f)
        val maxRadius = this.size.minDimension / 2f

        // Outer Ring
        drawCircle(
            color = coreColor.copy(alpha = 0.4f),
            radius = maxRadius * 0.95f,
            center = center,
            style = Stroke(width = 3f)
        )

        // Dashed Middle Ring (Rotating Clockwise)
        drawCircle(
            color = coreColor,
            radius = maxRadius * 0.78f,
            center = center,
            style = Stroke(
                width = 2.5f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 10f), rotationCW)
            )
        )

        // Inner Dashed Ring (Rotating Counter-Clockwise)
        drawCircle(
            color = Color.White.copy(alpha = 0.8f),
            radius = maxRadius * 0.58f,
            center = center,
            style = Stroke(
                width = 2f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), rotationCCW)
            )
        )

        // Spokes / Segments
        val spokeCount = 10
        val innerR = maxRadius * 0.40f
        val outerR = maxRadius * 0.72f
        for (i in 0 until spokeCount) {
            val angle = Math.toRadians((360.0 / spokeCount) * i + rotationCW).toFloat()
            val start = Offset(center.x + innerR * cos(angle), center.y + innerR * sin(angle))
            val end = Offset(center.x + outerR * cos(angle), center.y + outerR * sin(angle))
            drawLine(
                color = coreColor.copy(alpha = 0.7f),
                start = start,
                end = end,
                strokeWidth = 3f
            )
        }

        // Glowing Center Core
        drawCircle(
            color = coreColor.copy(alpha = 0.3f),
            radius = maxRadius * 0.35f * corePulse,
            center = center
        )
        drawCircle(
            color = coreColor,
            radius = maxRadius * 0.22f * corePulse,
            center = center
        )
        drawCircle(
            color = Color.White,
            radius = maxRadius * 0.12f * corePulse,
            center = center
        )
    }
}

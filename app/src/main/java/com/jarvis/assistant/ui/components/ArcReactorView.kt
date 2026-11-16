package com.jarvis.assistant.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jarvis.assistant.ui.theme.*
import kotlin.math.cos
import kotlin.math.sin

/**
 * Tony Stark Interactive Arc Reactor Central HUD Button.
 * Tapping toggles JARVIS between ACTIVE (Listening/Speaking) and STANDBY.
 */
@Composable
fun ArcReactorButton(
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 230.dp
) {
    val interactionSource = remember { MutableInteractionSource() }

    // Pulsing, rotating animations
    val infiniteTransition = rememberInfiniteTransition(label = "stark_arc_reactor")

    // Rotation Speeds: revs up dramatically when active
    val rotationCW by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isActive) 3500 else 16000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotCW"
    )

    val rotationCCW by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isActive) 2500 else 11000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotCCW"
    )

    // Breathing Glow / Pulse
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = if (isActive) 0.94f else 0.98f,
        targetValue = if (isActive) 1.06f else 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isActive) 650 else 2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val activeGlowColor by animateColorAsState(
        targetValue = if (isActive) CyanGlow else CyanBorder,
        animationSpec = tween(500),
        label = "glowColor"
    )

    Box(
        modifier = modifier
            .size(size)
            .scale(pulseScale)
            .clip(CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        // High-tech Arc Reactor Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val maxRadius = this.size.minDimension / 2f

            // 1. Outermost Ambient Halo Glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        activeGlowColor.copy(alpha = if (isActive) 0.35f else 0.08f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = maxRadius
                ),
                radius = maxRadius,
                center = center
            )

            // 2. Heavy Armored Bezel Ring
            drawCircle(
                color = BgCard,
                radius = maxRadius * 0.94f,
                center = center,
                style = Stroke(width = 10f)
            )
            drawCircle(
                color = activeGlowColor.copy(alpha = 0.5f),
                radius = maxRadius * 0.94f,
                center = center,
                style = Stroke(width = 2f)
            )

            // 3. Segmented Mechanical Cog Track (Rotates CW)
            val teethCount = 16
            val innerTrack = maxRadius * 0.82f
            val outerTrack = maxRadius * 0.90f
            for (i in 0 until teethCount) {
                val angle = Math.toRadians((360.0 / teethCount) * i + rotationCW).toFloat()
                val start = Offset(center.x + innerTrack * cos(angle), center.y + innerTrack * sin(angle))
                val end = Offset(center.x + outerTrack * cos(angle), center.y + outerTrack * sin(angle))
                drawLine(
                    color = if (isActive) CyanBright else activeGlowColor.copy(alpha = 0.6f),
                    start = start,
                    end = end,
                    strokeWidth = if (isActive) 4f else 2.5f,
                    cap = StrokeCap.Round
                )
            }

            // 4. Energy Induction Coil Ring (Rotates CCW)
            drawCircle(
                color = if (isActive) CyanBright else activeGlowColor,
                radius = maxRadius * 0.76f,
                center = center,
                style = Stroke(
                    width = 3.5f,
                    pathEffect = PathEffect.dashPathEffect(
                        floatArrayOf(24f, 14f, 6f, 14f),
                        rotationCCW
                    )
                )
            )

            // 5. 10 Core Flux Brackets (Iconic Tony Stark Arc Reactor Triangle Coils)
            val bracketCount = 10
            val bInner = maxRadius * 0.52f
            val bOuter = maxRadius * 0.72f
            for (i in 0 until bracketCount) {
                val angle = Math.toRadians((360.0 / bracketCount) * i + rotationCW).toFloat()
                val start = Offset(center.x + bInner * cos(angle), center.y + bInner * sin(angle))
                val end = Offset(center.x + bOuter * cos(angle), center.y + bOuter * sin(angle))
                drawLine(
                    color = if (isActive) CoreWhite.copy(alpha = 0.9f) else activeGlowColor.copy(alpha = 0.7f),
                    start = start,
                    end = end,
                    strokeWidth = if (isActive) 5f else 3.5f,
                    cap = StrokeCap.Square
                )
            }

            // 6. Inner Confinement Ring
            drawCircle(
                color = activeGlowColor,
                radius = maxRadius * 0.50f,
                center = center,
                style = Stroke(width = 2.5f)
            )

            // 7. Center Arc Fusion Well
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        (if (isActive) CyanBright else BgCard).copy(alpha = 0.95f),
                        BgDark
                    ),
                    center = center,
                    radius = maxRadius * 0.48f
                ),
                radius = maxRadius * 0.48f,
                center = center
            )
        }

        // Center Typography: "JARVIS" & Status
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            Text(
                text = "JARVIS",
                color = if (isActive) CoreWhite else CyanGlow,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 4.sp,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (isActive) "ONLINE • LISTENING" else "STANDBY • TAP",
                color = if (isActive) CyanBright else TextDim,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
        }
    }
}

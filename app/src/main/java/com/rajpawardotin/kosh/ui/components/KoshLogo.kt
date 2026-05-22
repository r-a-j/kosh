package com.rajpawardotin.kosh.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun KoshLogo(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "kosh_logo_animations")

    // Pulsing of the central core
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    // Rotation of inner orbit (clockwise)
    val innerRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "innerRotation"
    )

    // Rotation of outer orbit (counter-clockwise)
    val outerRotation by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(9000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "outerRotation"
    )

    // Line alpha pulse to create neural signal flashes
    val lineAlphaSignal by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "lineAlphaSignal"
    )

    // Star sparkle animation factor
    val sparkleFactor by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sparkleFactor"
    )

    Canvas(
        modifier = modifier
            .size(160.dp)
            .aspectRatio(1f)
    ) {
        val width = size.width
        val height = size.height
        val center = this.center

        // 1. Ambient Background Glow Behind Logo
        val ambientBrush = Brush.radialGradient(
            colors = listOf(
                Color(0xFF8B5CF6).copy(alpha = 0.15f * pulseScale),
                Color(0xFF00E5FF).copy(alpha = 0.08f * pulseScale),
                Color.Transparent
            ),
            center = center,
            radius = width * 0.5f
        )
        drawCircle(brush = ambientBrush, radius = width * 0.5f, center = center)

        // 2. Concentration Rings (Orbits)
        val innerOrbitRadius = width * 0.24f
        val outerOrbitRadius = width * 0.38f

        // Draw Inner Orbit (Dashed)
        drawCircle(
            color = Color(0xFF00E5FF).copy(alpha = 0.12f),
            radius = innerOrbitRadius,
            center = center,
            style = Stroke(
                width = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 16f))
            )
        )

        // Draw Outer Orbit (Dashed, different pattern)
        drawCircle(
            color = Color(0xFF8B5CF6).copy(alpha = 0.08f),
            radius = outerOrbitRadius,
            center = center,
            style = Stroke(
                width = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 12f))
            )
        )

        // 3. Neural Synapses (Connecting Lines to the central core)
        // Inner Node Coordinates
        val innerAngleRad = Math.toRadians(innerRotation.toDouble()).toFloat()
        val innerNodeX = center.x + innerOrbitRadius * cos(innerAngleRad)
        val innerNodeY = center.y + innerOrbitRadius * sin(innerAngleRad)

        // Outer Node 1 Coordinates
        val outerAngleRad1 = Math.toRadians(outerRotation.toDouble()).toFloat()
        val outerNodeX1 = center.x + outerOrbitRadius * cos(outerAngleRad1)
        val outerNodeY1 = center.y + outerOrbitRadius * sin(outerAngleRad1)

        // Outer Node 2 Coordinates (180 deg offset)
        val outerAngleRad2 = Math.toRadians((outerRotation + 180f).toDouble()).toFloat()
        val outerNodeX2 = center.x + outerOrbitRadius * cos(outerAngleRad2)
        val outerNodeY2 = center.y + outerOrbitRadius * sin(outerAngleRad2)

        // Draw lines from center to nodes
        drawLine(
            brush = Brush.linearGradient(listOf(Color(0xFF00E5FF).copy(alpha = lineAlphaSignal), Color(0xFF00E5FF).copy(alpha = 0.05f))),
            start = center,
            end = Offset(innerNodeX, innerNodeY),
            strokeWidth = 1.5.dp.toPx()
        )

        drawLine(
            brush = Brush.linearGradient(listOf(Color(0xFFEC4899).copy(alpha = (1f - lineAlphaSignal) * 0.8f), Color(0xFFEC4899).copy(alpha = 0.05f))),
            start = center,
            end = Offset(outerNodeX1, outerNodeY1),
            strokeWidth = 1.2.dp.toPx()
        )

        drawLine(
            brush = Brush.linearGradient(listOf(Color(0xFF8B5CF6).copy(alpha = lineAlphaSignal * 0.7f), Color(0xFF8B5CF6).copy(alpha = 0.05f))),
            start = center,
            end = Offset(outerNodeX2, outerNodeY2),
            strokeWidth = 1.2.dp.toPx()
        )

        // 4. Fixed Sparkling Neural Junctions (Small ambient sparkles)
        val fixedJunctions = listOf(
            Pair(width * 0.32f, 45f),
            Pair(width * 0.32f, 135f),
            Pair(width * 0.32f, 220f),
            Pair(width * 0.32f, 305f)
        )
        fixedJunctions.forEachIndexed { index, (r, angleDeg) ->
            val angle = Math.toRadians(angleDeg.toDouble()).toFloat()
            val x = center.x + r * cos(angle)
            val y = center.y + r * sin(angle)
            // Pulse individual opacity out of phase using index offset
            val individualAlpha = (sin(sparkleFactor + index) * 0.4f + 0.6f) * 0.3f
            drawCircle(
                color = Color(0xFF00E5FF).copy(alpha = individualAlpha),
                radius = 3.dp.toPx(),
                center = Offset(x, y)
            )
        }

        // 5. Draw Orbiting Nodes
        // Inner Node
        drawCircle(
            color = Color(0xFF00E5FF).copy(alpha = 0.2f),
            radius = 10.dp.toPx(),
            center = Offset(innerNodeX, innerNodeY)
        )
        drawCircle(
            color = Color(0xFF00E5FF),
            radius = 4.5.dp.toPx(),
            center = Offset(innerNodeX, innerNodeY)
        )

        // Outer Node 1
        drawCircle(
            color = Color(0xFFEC4899).copy(alpha = 0.2f),
            radius = 9.dp.toPx(),
            center = Offset(outerNodeX1, outerNodeY1)
        )
        drawCircle(
            color = Color(0xFFEC4899),
            radius = 4.dp.toPx(),
            center = Offset(outerNodeX1, outerNodeY1)
        )

        // Outer Node 2
        drawCircle(
            color = Color(0xFF8B5CF6).copy(alpha = 0.2f),
            radius = 9.dp.toPx(),
            center = Offset(outerNodeX2, outerNodeY2)
        )
        drawCircle(
            color = Color(0xFF8B5CF6),
            radius = 4.dp.toPx(),
            center = Offset(outerNodeX2, outerNodeY2)
        )

        // 6. Glowing Central core (Neural Hub)
        val coreRadius = width * 0.13f * pulseScale
        val coreBrush = Brush.radialGradient(
            colors = listOf(
                Color(0xFF00E5FF),
                Color(0xFF8B5CF6),
                Color(0xFF0C0C0F)
            ),
            center = center,
            radius = coreRadius
        )
        drawCircle(
            brush = coreBrush,
            radius = coreRadius,
            center = center
        )

        // Subtle core outline
        drawCircle(
            color = Color(0xFF00E5FF).copy(alpha = 0.5f),
            radius = coreRadius,
            center = center,
            style = Stroke(width = 1.dp.toPx())
        )

        // 7. Geometric Logo Symbol "K" inside core
        val kSize = coreRadius * 0.8f
        val kPath = Path().apply {
            // Draw a high-tech geometric K
            // Left stem
            moveTo(center.x - kSize * 0.35f, center.y - kSize * 0.5f)
            lineTo(center.x - kSize * 0.35f, center.y + kSize * 0.5f)

            // Diagonal top
            moveTo(center.x - kSize * 0.35f, center.y - kSize * 0.05f)
            lineTo(center.x + kSize * 0.35f, center.y - kSize * 0.5f)

            // Diagonal bottom
            moveTo(center.x - kSize * 0.15f, center.y - kSize * 0.18f)
            lineTo(center.x + kSize * 0.35f, center.y + kSize * 0.5f)
        }

        drawPath(
            path = kPath,
            color = Color.White.copy(alpha = 0.95f),
            style = Stroke(
                width = 3.dp.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
    }
}

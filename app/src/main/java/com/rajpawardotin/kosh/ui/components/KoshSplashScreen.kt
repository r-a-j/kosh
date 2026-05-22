package com.rajpawardotin.kosh.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun KoshSplashScreen(
    isInitializing: Boolean,
    isCopyingModel: Boolean,
    modifier: Modifier = Modifier
) {
    // Logo scale transition on entry
    val scale = remember { Animatable(0.5f) }
    // Text entrance animations
    val textAlpha = remember { Animatable(0f) }
    val textOffsetY = remember { Animatable(30f) }

    LaunchedEffect(Unit) {
        // Logo pops in with bouncy spring
        scale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
    }

    LaunchedEffect(Unit) {
        // Text fades and slides up shortly after
        delay(400)
        textAlpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(800, easing = EaseOutCubic)
        )
    }

    LaunchedEffect(Unit) {
        delay(400)
        textOffsetY.animateTo(
            targetValue = 0f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0C0C0F)),
        contentAlignment = Alignment.Center
    ) {
        // Ambient background glows (Premium Aesthetics)
        // Top-left radial glow
        Box(
            modifier = Modifier
                .size(400.dp)
                .align(Alignment.TopStart)
                .offset(x = (-100).dp, y = (-100).dp)
                .background(Brush.radialGradient(listOf(Color(0xFF0F766E).copy(alpha = 0.25f), Color.Transparent)))
                .blur(80.dp)
        )
        // Bottom-right radial glow
        Box(
            modifier = Modifier
                .size(450.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 100.dp, y = 100.dp)
                .background(Brush.radialGradient(listOf(Color(0xFF312E81).copy(alpha = 0.28f), Color.Transparent)))
                .blur(80.dp)
        )

        // Main Content (Centered)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Animated Logo
            KoshLogo(
                modifier = Modifier
                    .graphicsLayer(
                        scaleX = scale.value,
                        scaleY = scale.value
                    )
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Animated Welcome Title & Tagline
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .graphicsLayer(
                        alpha = textAlpha.value,
                        translationY = textOffsetY.value
                    )
            ) {
                // Wide, modern typography with a beautiful text gradient
                Text(
                    text = "K O S H",
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 10.sp,
                        fontSize = 38.sp,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF00E5FF),
                                Color(0xFF8B5CF6),
                                Color(0xFFEC4899)
                            )
                        )
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "NEURAL COGNITION SYSTEM",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 3.sp,
                        fontSize = 10.sp
                    ),
                    color = Color.White.copy(alpha = 0.5f)
                )
            }
        }

        // Bottom Loading Indicators (Smooth and premium)
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 64.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val statusText = when {
                isCopyingModel -> "ALLOCATING COGNITIVE CHANNELS..."
                isInitializing -> "SYNCHRONIZING NEURAL CORE..."
                else -> "IGNITING CHANNELS..."
            }

            Text(
                text = statusText,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    fontSize = 10.sp
                ),
                color = Color(0xFF00E5FF).copy(alpha = 0.8f)
            )

            // Minimal neon blue/cyan tracking progress line
            LinearProgressIndicator(
                modifier = Modifier
                    .width(160.dp)
                    .height(2.dp)
                    .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(1.dp)),
                color = Color(0xFF00E5FF),
                trackColor = Color.Transparent
            )
        }
    }
}

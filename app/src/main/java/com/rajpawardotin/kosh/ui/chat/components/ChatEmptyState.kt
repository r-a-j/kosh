package com.rajpawardotin.kosh.ui.chat.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ChatEmptyState(
    isTemporarySession: Boolean
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(32.dp)
    ) {
        Icon(
            imageVector = Icons.Default.AutoAwesome,
            contentDescription = null,
            modifier = Modifier
                .size(80.dp)
                .graphicsLayer(alpha = 0.95f)
                .drawWithContent {
                    drawContent()
                    drawRect(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF8B5CF6), // Violet
                                Color(0xFF06B6D4), // Cyan
                                Color(0xFFEC4899)  // Pink
                            )
                        ),
                        blendMode = BlendMode.SrcAtop
                    )
                },
            tint = Color.Unspecified
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = if (isTemporarySession) "Temporary Vault: History is disabled." else "What should we focus on?",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Medium,
                letterSpacing = (-0.5).sp,
                fontSize = 26.sp
            ),
            color = if (isTemporarySession) Color(0xFFFF9100).copy(alpha = 0.8f) else Color.White.copy(alpha = 0.9f),
            textAlign = TextAlign.Center
        )
    }
}

package com.rajpawardotin.kosh.ui.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.AutoAwesome

@Composable
fun ChatEmptyState(
    isTemporarySession: Boolean,
    onSuggestionClick: (String) -> Unit,
    onExitTemporaryClick: (() -> Unit)? = null
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Icon(
            imageVector = Icons.Default.AutoAwesome,
            contentDescription = null,
            tint = Color(0xFF03DAC5),
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (isTemporarySession) "Temporary Vault" else "Kosh Neural Core",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.5).sp,
                fontSize = 24.sp
            ),
            color = if (isTemporarySession) Color(0xFFFF9100).copy(alpha = 0.8f) else Color.White.copy(alpha = 0.95f),
            textAlign = TextAlign.Center
        )
        Text(
            text = if (isTemporarySession) "History is disabled. Session keys will be shredded upon lock." else "Your secure local AI brainstorm companion.",
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 14.sp
            ),
            color = Color.White.copy(alpha = 0.5f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
        )

        if (isTemporarySession && onExitTemporaryClick != null) {
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = onExitTemporaryClick,
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF9100).copy(alpha = 0.4f)),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color(0xFFFF9100)
                )
            ) {
                Text(
                    text = "EXIT TEMPORARY VAULT",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                )
            }
        }
        
        Spacer(modifier = Modifier.height(28.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SuggestionCard(
                title = "Write & Refactor",
                desc = "Solve coding problems or debug logic",
                icon = Icons.Default.Code,
                gradientColors = listOf(Color(0xFF8B5CF6), Color(0xFF6366F1)),
                modifier = Modifier.weight(1f),
                onClick = { onSuggestionClick("Write a Kotlin class to handle binary search recursively.") }
            )
            SuggestionCard(
                title = "Web Search",
                desc = "Find real-time news, tech trends, or weather",
                icon = Icons.Default.Language,
                gradientColors = listOf(Color(0xFF06B6D4), Color(0xFF3B82F6)),
                modifier = Modifier.weight(1f),
                onClick = { onSuggestionClick("Search online for the latest news on on-device LLM trends today.") }
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SuggestionCard(
                title = "Analyze Docs",
                desc = "Summarize or extract facts from attachments",
                icon = Icons.Default.Description,
                gradientColors = listOf(Color(0xFF10B981), Color(0xFF059669)),
                modifier = Modifier.weight(1f),
                onClick = { onSuggestionClick("Summarize the key findings and details of the attached document.") }
            )
            SuggestionCard(
                title = "Brainstorm Ideas",
                desc = "Generate features, names, or outlines",
                icon = Icons.Default.Lightbulb,
                gradientColors = listOf(Color(0xFFF59E0B), Color(0xFFD97706)),
                modifier = Modifier.weight(1f),
                onClick = { onSuggestionClick("Brainstorm 5 unique features for an immersive local AI notes app.") }
            )
        }
    }
}

@Composable
private fun SuggestionCard(
    title: String,
    desc: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    gradientColors: List<Color>,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161618).copy(alpha = 0.7f)),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            Brush.linearGradient(
                listOf(
                    Color.White.copy(alpha = 0.12f),
                    Color.White.copy(alpha = 0.02f)
                )
            )
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Brush.linearGradient(gradientColors)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                ),
                color = Color.White
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = desc,
                style = MaterialTheme.typography.bodySmall.copy(
                    lineHeight = 16.sp,
                    fontSize = 12.sp
                ),
                color = Color.White.copy(alpha = 0.6f)
            )
        }
    }
}

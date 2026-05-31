package com.rajpawardotin.kosh.ui.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.Dp

import androidx.compose.foundation.ScrollState

@Composable
fun ChatEmptyState(
    isTemporarySession: Boolean,
    onSuggestionClick: (String) -> Unit,
    onExitTemporaryClick: (() -> Unit)? = null,
    bottomPadding: Dp = 100.dp,
    scrollState: ScrollState = rememberScrollState()
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(
                start = 20.dp,
                end = 20.dp,
                top = 24.dp,
                bottom = 16.dp
            )
    ) {
        Spacer(modifier = Modifier.height(8.dp))
        
        // Header Icon with subtle ambient gradient
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF03DAC5).copy(alpha = 0.15f),
                            Color(0xFF8B5CF6).copy(alpha = 0.05f)
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF03DAC5).copy(alpha = 0.4f),
                            Color.Transparent
                        )
                    ),
                    shape = RoundedCornerShape(14.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = Color(0xFF03DAC5),
                modifier = Modifier.size(28.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(14.dp))
        
        Text(
            text = if (isTemporarySession) "Temporary Session" else "Kosh Private Chat",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.5).sp,
                fontSize = 22.sp
            ),
            color = if (isTemporarySession) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        
        Text(
            text = if (isTemporarySession) 
                "History is disabled. Session keys will be deleted when locked." 
            else 
                "Secure local chat running entirely on your device.",
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 13.sp,
                lineHeight = 18.sp
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
        )

        if (isTemporarySession && onExitTemporaryClick != null) {
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedButton(
                onClick = onExitTemporaryClick,
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f)),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                ),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "EXIT TEMPORARY SESSION",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp,
                        fontSize = 11.sp
                    )
                )
            }
        }
        
        Spacer(modifier = Modifier.height(20.dp))

        // Status Indicators Row (2026 Sleek Dashboard Chips)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatusChip(label = "Engine", value = "LiteRT", color = Color(0xFF10B981))
            StatusChip(label = "Encryption", value = "AES-GCM", color = Color(0xFF06B6D4))
            StatusChip(label = "Hardware", value = "NPU", color = Color(0xFF8B5CF6))
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Dashboard Section Title
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "CONNECTED TOOLS",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    fontSize = 11.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(0.5.dp)
                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Grid of Connected Skills (Dashboard Showcase)
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SuggestionCard(
                    title = "Device Flashlight",
                    desc = "Toggle hardware torch",
                    promptText = "Turn on the flashlight",
                    icon = Icons.Default.Settings,
                    gradientColors = listOf(Color(0xFF8B5CF6), Color(0xFF6D28D9)),
                    modifier = Modifier.weight(1f),
                    onClick = onSuggestionClick
                )
                SuggestionCard(
                    title = "Audio Manager",
                    desc = "Set music & media volume",
                    promptText = "Set media volume to 50%",
                    icon = Icons.AutoMirrored.Filled.VolumeUp,
                    gradientColors = listOf(Color(0xFF3B82F6), Color(0xFF1D4ED8)),
                    modifier = Modifier.weight(1f),
                    onClick = onSuggestionClick
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SuggestionCard(
                    title = "Calendar Planner",
                    desc = "Check device schedules",
                    promptText = "List my calendar events for next week",
                    icon = Icons.Default.CalendarToday,
                    gradientColors = listOf(Color(0xFF10B981), Color(0xFF047857)),
                    modifier = Modifier.weight(1f),
                    onClick = onSuggestionClick
                )
                SuggestionCard(
                    title = "Web Knowledge",
                    desc = "Forced or smart hybrid search",
                    promptText = "Search online for the latest news on local LLM trends today",
                    icon = Icons.Default.Language,
                    gradientColors = listOf(Color(0xFF06B6D4), Color(0xFF0891B2)),
                    modifier = Modifier.weight(1f),
                    onClick = onSuggestionClick
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SuggestionCard(
                    title = "Doc Intellect",
                    desc = "Local PDF / TXT RAG parser",
                    promptText = "Summarize the key findings of the attached document",
                    icon = Icons.Default.Description,
                    gradientColors = listOf(Color(0xFFEC4899), Color(0xFFBE185D)),
                    modifier = Modifier.weight(1f),
                    onClick = onSuggestionClick
                )
                SuggestionCard(
                    title = "Script Sandbox",
                    desc = "Run custom JS plugins",
                    promptText = "Run the custom JS script to parse this query",
                    icon = Icons.Default.Code,
                    gradientColors = listOf(Color(0xFFF59E0B), Color(0xFFD97706)),
                    modifier = Modifier.weight(1f),
                    onClick = onSuggestionClick
                )
            }

            // A final full-width featured card for general coding/logic assistant
            SuggestionCardFullWidth(
                title = "Code & Logic Helper",
                desc = "Write classes, debug logic, or run local brainstorming routines",
                promptText = "Write a Kotlin class to handle binary search recursively.",
                icon = Icons.Default.Lightbulb,
                gradientColors = listOf(Color(0xFFEF4444), Color(0xFFB91C1C)),
                onClick = onSuggestionClick
            )
        }
        
        Spacer(modifier = Modifier.height(bottomPadding + 16.dp))
    }
}

@Composable
private fun StatusChip(label: String, value: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .border(
                width = 0.5.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(androidx.compose.foundation.shape.CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "$label: ",
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun SuggestionCard(
    title: String,
    desc: String,
    promptText: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    gradientColors: List<Color>,
    modifier: Modifier = Modifier,
    onClick: (String) -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick(promptText) },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        border = androidx.compose.foundation.BorderStroke(
            0.8.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(Brush.linearGradient(gradientColors)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = desc,
                style = MaterialTheme.typography.bodySmall.copy(
                    lineHeight = 14.sp,
                    fontSize = 11.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SuggestionCardFullWidth(
    title: String,
    desc: String,
    promptText: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    gradientColors: List<Color>,
    onClick: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick(promptText) },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        border = androidx.compose.foundation.BorderStroke(
            0.8.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(Brush.linearGradient(gradientColors)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = desc,
                    style = MaterialTheme.typography.bodySmall.copy(
                        lineHeight = 14.sp,
                        fontSize = 11.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

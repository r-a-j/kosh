package com.rajpawardotin.kosh.ui.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rajpawardotin.kosh.domain.model.ChatSession
import com.rajpawardotin.kosh.ui.chat.ChatViewModel
import java.io.File

@Composable
fun ChatTopBar(
    viewModel: ChatViewModel,
    onMenuClick: () -> Unit,
    onCoreSelectorClick: () -> Unit,
    onLockSettingsClick: (ChatSession) -> Unit,
    onManageLockClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(top = 12.dp, start = 16.dp, end = 16.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Left: 2-line Hamburger Menu
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.05f))
                .clickable { onMenuClick() },
            contentAlignment = Alignment.Center
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalAlignment = Alignment.Start,
                modifier = Modifier.width(18.dp)
            ) {
                Box(modifier = Modifier.width(18.dp).height(2.dp).background(Color.White))
                Box(modifier = Modifier.width(12.dp).height(2.dp).background(Color.White))
            }
        }

        // Center: Dropdown Selector
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White.copy(alpha = 0.05f))
                .clickable { onCoreSelectorClick() }
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = if (viewModel.isEngineReady) {
                    viewModel.modelPath?.let {
                        File(it).name
                            .replace(".litertlm", "")
                            .replace(".bin", "")
                            .replace("model", "Neural Core")
                            .uppercase()
                    } ?: "NEURAL CORE"
                } else {
                    "SELECT CORE"
                },
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    fontSize = 13.sp
                ),
                color = Color.White
            )
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = "Select Core",
                tint = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.size(16.dp)
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Chat Lock Management Button
            if (viewModel.currentSessionId != null && !viewModel.isTemporarySession) {
                val currentSession = viewModel.savedSessions.find { it.id == viewModel.currentSessionId }
                if (currentSession != null) {
                    val isEncrypted = currentSession.encryptedKeyPassword != null
                    val isUnlocked = viewModel.activeSessionKeys.containsKey(currentSession.id)

                    IconButton(
                        onClick = {
                            if (!isEncrypted) {
                                onLockSettingsClick(currentSession)
                            } else if (isUnlocked) {
                                onManageLockClick()
                            }
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(
                                if (isEncrypted) Color(0xFF03DAC5).copy(alpha = 0.15f)
                                else Color.White.copy(alpha = 0.05f)
                            )
                    ) {
                        Icon(
                            imageVector = if (isEncrypted) Icons.Default.Lock else Icons.Default.LockOpen,
                            contentDescription = "Vault Lock Settings",
                            tint = if (isEncrypted) Color(0xFF03DAC5) else Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Temporary Chat Button
            IconButton(
                onClick = {
                    viewModel.startNewChat(isTemporary = true)
                },
                enabled = !viewModel.isGenerating,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (viewModel.isTemporarySession) Color(0xFFFF9100).copy(alpha = 0.15f)
                        else Color.White.copy(alpha = 0.05f)
                    )
                    .graphicsLayer(alpha = if (viewModel.isTemporarySession) 1f else 0.6f)
            ) {
                Icon(
                    imageVector = Icons.Default.VisibilityOff,
                    contentDescription = "Temporary Chat",
                    tint = if (viewModel.isTemporarySession) Color(0xFFFF9100) else Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }

            // New Saved Chat Button
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.05f))
                    .clickable(enabled = !viewModel.isGenerating) {
                        viewModel.startNewChat(isTemporary = false)
                    }
                    .drawWithContent {
                        drawContent()
                        drawCircle(
                            brush = Brush.linearGradient(
                                colors = listOf(Color(0xFF03DAC5), Color(0xFF6200EE))
                            ),
                            radius = size.minDimension / 2f,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5.dp.toPx())
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "New Saved Chat",
                    tint = Color(0xFF03DAC5),
                    modifier = Modifier.size(18.dp)
                )
            }

            // Settings & Core Configuration Button
            IconButton(
                onClick = onSettingsClick,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.05f))
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

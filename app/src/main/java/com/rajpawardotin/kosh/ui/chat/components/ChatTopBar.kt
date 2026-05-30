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
import androidx.compose.material.icons.filled.Visibility
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rajpawardotin.kosh.domain.model.ChatSession
import com.rajpawardotin.kosh.ui.chat.ChatViewModel
import java.io.File

@Composable
fun ChatTopBar(
    isEngineReady: Boolean,
    modelPath: String?,
    currentSession: ChatSession?,
    isCurrentSessionUnlocked: Boolean,
    isTemporarySession: Boolean,
    isGenerating: Boolean,
    onMenuClick: () -> Unit,
    onCoreSelectorClick: () -> Unit,
    onLockSettingsClick: (ChatSession) -> Unit,
    onManageLockClick: () -> Unit,
    onNewChatClick: (isTemporary: Boolean) -> Unit,
    onSettingsClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(top = 12.dp, start = 16.dp, end = 16.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
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

        // Safety margin between left menu and center selector
        Spacer(modifier = Modifier.width(12.dp))

        // Center: Dropdown Selector (occupies remaining space, centering its content)
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White.copy(alpha = 0.05f))
                    .clickable { onCoreSelectorClick() }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = if (isEngineReady) {
                        modelPath?.let {
                            File(it).name
                                .replace(".litertlm", "")
                                .replace(".bin", "")
                                .replace("model", "Neural Core")
                                .uppercase()
                                .replace("_QUALCOMM_SM8750", " (NPU)")
                                .replace("_QUALCOMM", " (NPU)")
                                .replace("_SM8750", " (NPU)")
                                .replace("-IT", "")
                        } ?: "NEURAL CORE"
                    } else {
                        "SELECT CORE"
                    },
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        fontSize = 13.sp
                    ),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Select Core",
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // Safety margin between center selector and right actions
        Spacer(modifier = Modifier.width(12.dp))

        // Right: Action Buttons Row
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Chat Lock Management Button
            if (currentSession != null && !isTemporarySession) {
                val isEncrypted = currentSession.encryptedKeyPassword != null
                val isUnlocked = isCurrentSessionUnlocked

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

            // Temporary Chat Button
            IconButton(
                onClick = {
                    onNewChatClick(!isTemporarySession)
                },
                enabled = !isGenerating,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (isTemporarySession) Color(0xFFFF9100).copy(alpha = 0.15f)
                        else Color.White.copy(alpha = 0.05f)
                    )
                    .graphicsLayer(alpha = if (isTemporarySession) 1f else 0.6f)
            ) {
                Icon(
                    imageVector = if (isTemporarySession) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = if (isTemporarySession) "Exit Temporary Chat" else "Enter Temporary Chat",
                    tint = if (isTemporarySession) Color(0xFFFF9100) else Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }

            // New Saved Chat Button
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.05f))
                    .clickable(enabled = !isGenerating) {
                        onNewChatClick(false)
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

package com.rajpawardotin.kosh.ui.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.drawBehind
import com.rajpawardotin.kosh.domain.model.ChatSession
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
    onSettingsClick: () -> Unit,
    scrollProgress: () -> Float = { 0f }
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(top = 12.dp, start = 16.dp, end = 16.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: 2-line Hamburger Menu (40.dp glassmorphic circle)
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.03f))
                .border(1.dp, Color.White.copy(alpha = 0.06f), CircleShape)
                .clickable { onMenuClick() },
            contentAlignment = Alignment.Center
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalAlignment = Alignment.Start,
                modifier = Modifier.width(18.dp)
            ) {
                Box(modifier = Modifier.width(18.dp).height(1.5.dp).background(Color.White.copy(alpha = 0.9f)))
                Box(modifier = Modifier.width(12.dp).height(1.5.dp).background(Color.White.copy(alpha = 0.9f)))
            }
        }

        // Safety margin between left menu and center selector
        Spacer(modifier = Modifier.width(10.dp))

        // Center: Dropdown Selector (40.dp height, centering content)
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .height(40.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White.copy(alpha = 0.03f))
                    .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(20.dp))
                    .clickable { onCoreSelectorClick() }
                    .padding(horizontal = 14.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = Color(0xFF03DAC5),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
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
                        letterSpacing = 0.8.sp,
                        fontSize = 11.5.sp
                    ),
                    color = Color.White.copy(alpha = 0.9f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Select Core",
                    tint = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        // Safety margin between center selector and right actions
        Spacer(modifier = Modifier.width(10.dp))

        // Right: Unified Actions Capsule (40.dp height, 34.dp buttons)
        Row(
            modifier = Modifier
                .height(40.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White.copy(alpha = 0.03f))
                .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(20.dp))
                .padding(horizontal = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Action 1: Chat Lock Management Button
            if (currentSession != null && !isTemporarySession) {
                val isEncrypted = currentSession.encryptedKeyPassword != null
                val isUnlocked = isCurrentSessionUnlocked

                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(
                            if (isEncrypted) {
                                if (isUnlocked) Color(0xFF03DAC5).copy(alpha = 0.15f)
                                else Color(0xFFEF5350).copy(alpha = 0.15f)
                            } else Color.Transparent
                        )
                        .clickable {
                            if (!isEncrypted) {
                                onLockSettingsClick(currentSession)
                            } else if (isUnlocked) {
                                onManageLockClick()
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isEncrypted) Icons.Default.Lock else Icons.Default.LockOpen,
                        contentDescription = "Vault Lock Settings",
                        tint = if (isEncrypted) {
                            if (isUnlocked) Color(0xFF03DAC5) else Color(0xFFEF5350)
                        } else Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Action 2: Temporary Chat Button
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(
                        if (isTemporarySession) Color(0xFFFF9100).copy(alpha = 0.15f)
                        else Color.Transparent
                    )
                    .clickable(enabled = !isGenerating) {
                        onNewChatClick(!isTemporarySession)
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isTemporarySession) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = if (isTemporarySession) "Exit Temporary Chat" else "Enter Temporary Chat",
                    tint = if (isTemporarySession) Color(0xFFFF9100) else Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.size(16.dp)
                )
            }

            // Action 3: New Saved Chat Button
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.05f))
                    .border(
                        width = 1.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(Color(0xFF03DAC5), Color(0xFF6200EE).copy(alpha = 0.6f))
                        ),
                        shape = CircleShape
                    )
                    .clickable(enabled = !isGenerating) {
                        onNewChatClick(false)
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "New Saved Chat",
                    tint = Color(0xFF03DAC5),
                    modifier = Modifier.size(16.dp)
                )
            }

            // Action 4: Settings & Core Configuration Button
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .clickable { onSettingsClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

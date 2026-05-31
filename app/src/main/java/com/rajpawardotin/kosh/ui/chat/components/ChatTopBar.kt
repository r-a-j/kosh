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
    val barColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
    val outlineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceMuted = MaterialTheme.colorScheme.onSurfaceVariant
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val errorColor = MaterialTheme.colorScheme.error
    val warningColor = MaterialTheme.colorScheme.tertiary

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(64.dp)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: 2-line Hamburger Menu M3 styled button
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                .border(1.dp, outlineColor, CircleShape)
                .clickable { onMenuClick() },
            contentAlignment = Alignment.Center
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(3.5.dp),
                horizontalAlignment = Alignment.Start,
                modifier = Modifier.width(18.dp)
            ) {
                Box(modifier = Modifier.width(18.dp).height(2.dp).background(onSurface.copy(alpha = 0.8f)))
                Box(modifier = Modifier.width(12.dp).height(2.dp).background(onSurface.copy(alpha = 0.8f)))
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Center: Dropdown Selector (Model Selector)
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .height(40.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                    .border(1.dp, outlineColor, RoundedCornerShape(20.dp))
                    .clickable { onCoreSelectorClick() }
                    .padding(horizontal = 12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = primary,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isEngineReady) {
                        modelPath?.let {
                            File(it).name
                                .replace(".litertlm", "")
                                .replace(".bin", "")
                                .replace("model", "Model")
                                .uppercase()
                                .replace("_QUALCOMM_SM8750", " (NPU)")
                                .replace("_QUALCOMM", " (NPU)")
                                .replace("_SM8750", " (NPU)")
                                .replace("-IT", "")
                        } ?: "LOCAL MODEL"
                    } else {
                        "SELECT MODEL"
                    },
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.6.sp,
                        fontSize = 12.sp
                    ),
                    color = onSurface.copy(alpha = 0.9f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Select Core",
                    tint = onSurfaceMuted,
                    modifier = Modifier.size(15.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Right: Unified Actions Capsule (Material 3 Compliant)
        Row(
            modifier = Modifier
                .height(40.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                .border(1.dp, outlineColor, RoundedCornerShape(20.dp))
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Action 1: Chat Lock Management Button
            if (currentSession != null && !isTemporarySession) {
                val isEncrypted = currentSession.encryptedKeyPassword != null
                val isUnlocked = isCurrentSessionUnlocked

                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(
                            if (isEncrypted) {
                                if (isUnlocked) primary.copy(alpha = 0.15f)
                                else errorColor.copy(alpha = 0.15f)
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
                        contentDescription = "Chat Lock Settings",
                        tint = if (isEncrypted) {
                            if (isUnlocked) primary else errorColor
                        } else onSurfaceMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Action 2: Temporary Chat Button
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(
                        if (isTemporarySession) warningColor.copy(alpha = 0.15f)
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
                    tint = if (isTemporarySession) warningColor else onSurfaceMuted,
                    modifier = Modifier.size(16.dp)
                )
            }

            // Action 3: New Saved Chat Button
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                    .border(
                        width = 1.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(primary, secondary)
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
                    tint = primary,
                    modifier = Modifier.size(16.dp)
                )
            }

            // Action 4: Settings & Core Configuration Button
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .clickable { onSettingsClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = onSurfaceMuted,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

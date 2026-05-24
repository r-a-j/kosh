package com.rajpawardotin.kosh.ui.chat.components

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rajpawardotin.kosh.domain.model.ChatSession
import com.rajpawardotin.kosh.ui.chat.ChatViewModel
import com.rajpawardotin.kosh.ui.components.KoshLogo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun ChatDrawerContent(
    viewModel: ChatViewModel,
    drawerState: DrawerState,
    scope: CoroutineScope,
    context: Context,
    onLockSession: (ChatSession) -> Unit
) {
    ModalDrawerSheet(
        drawerContainerColor = Color(0xFF0F0F12),
        drawerContentColor = Color.White,
        modifier = Modifier.width(320.dp).fillMaxHeight()
    ) {
        var sessionToDelete by remember { mutableStateOf<ChatSession?>(null) }
        var sessionToRename by remember { mutableStateOf<ChatSession?>(null) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Title / Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            ) {
                Box(modifier = Modifier.size(36.dp)) {
                    KoshLogo(
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "COGNITIVE VAULTS",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp,
                        fontSize = 14.sp
                    ),
                    color = Color.White
                )
            }

            HorizontalDivider(
                color = Color.White.copy(alpha = 0.1f),
                thickness = 1.dp,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // New Brainstorm button
            Button(
                onClick = {
                    viewModel.startNewChat()
                    scope.launch { drawerState.close() }
                },
                enabled = !viewModel.isGenerating,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White.copy(alpha = 0.05f),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(
                    1.dp,
                    Brush.linearGradient(listOf(Color(0xFF03DAC5), Color(0xFF6200EE)))
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = Color(0xFF03DAC5),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "New Brainstorm",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Scrollable List of Saved Chats
            Text(
                text = "HISTORY",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = Color.Gray,
                    letterSpacing = 1.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
            )

            if (viewModel.savedSessions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No cognitive vaults yet.",
                        color = Color.Gray,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(viewModel.savedSessions) { session ->
                        val isActive = viewModel.currentSessionId == session.id
                        val relativeTime = remember(session.lastActive) {
                            val now = System.currentTimeMillis()
                            val diff = now - session.lastActive
                            when {
                                diff < 60_000 -> "Just now"
                                diff < 3600_000 -> "${diff / 60_000}m ago"
                                diff < 86400_000 -> "${diff / 3600_000}h ago"
                                else -> "${diff / 86400_000}d ago"
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (isActive) Color.White.copy(alpha = 0.08f)
                                    else Color.Transparent
                                )
                                .clickable(enabled = !viewModel.isGenerating) {
                                    viewModel.loadSession(session.id)
                                    scope.launch { drawerState.close() }
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val isEncrypted = session.encryptedKeyPassword != null
                            val isUnlocked = viewModel.activeSessionKeys.containsKey(session.id)
                            Icon(
                                imageVector = if (isEncrypted) {
                                    if (isUnlocked) Icons.Default.LockOpen else Icons.Default.Lock
                                } else {
                                    Icons.AutoMirrored.Filled.Chat
                                },
                                contentDescription = null,
                                tint = if (isActive) Color(0xFF03DAC5) else if (isEncrypted) Color(0xFF03DAC5).copy(alpha = 0.7f) else Color.Gray,
                                modifier = Modifier.size(18.dp)
                            )

                            Spacer(modifier = Modifier.width(10.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = session.title,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
                                    ),
                                    color = if (isActive) Color.White else Color.White.copy(alpha = 0.7f),
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                                Text(
                                    text = relativeTime,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.Gray
                                )
                            }

                            IconButton(
                                onClick = { sessionToRename = session },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Rename",
                                    tint = Color.Gray,
                                    modifier = Modifier.size(14.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(4.dp))

                            IconButton(
                                onClick = {
                                    if (!isEncrypted) {
                                        onLockSession(session)
                                    } else if (isUnlocked) {
                                        viewModel.activeSessionKeys.remove(session.id)
                                        if (viewModel.currentSessionId == session.id) {
                                            viewModel.loadSession(session.id)
                                        }
                                        Toast.makeText(context, "Vault Locked", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = if (isEncrypted) Icons.Default.Lock else Icons.Default.LockOpen,
                                    contentDescription = "Lock Status",
                                    tint = if (isEncrypted) Color(0xFF03DAC5) else Color.Gray,
                                    modifier = Modifier.size(14.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(4.dp))

                            IconButton(
                                onClick = { sessionToDelete = session },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = Color.Gray,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        if (sessionToDelete != null) {
            com.rajpawardotin.kosh.ui.chat.dialogs.DeleteSessionDialog(
                session = sessionToDelete!!,
                viewModel = viewModel,
                context = context,
                onDismiss = { sessionToDelete = null }
            )
        }

        if (sessionToRename != null) {
            com.rajpawardotin.kosh.ui.chat.dialogs.RenameSessionDialog(
                session = sessionToRename!!,
                viewModel = viewModel,
                onDismiss = { sessionToRename = null }
            )
        }
    }
}

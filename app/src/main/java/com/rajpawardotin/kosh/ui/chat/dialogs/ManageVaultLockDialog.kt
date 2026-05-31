package com.rajpawardotin.kosh.ui.chat.dialogs

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rajpawardotin.kosh.ui.chat.ChatViewModel

@Composable
fun ManageVaultLockDialog(
    sessionId: String,
    viewModel: ChatViewModel,
    onDismiss: () -> Unit
) {
    var isProcessing by remember { mutableStateOf(false) }
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = { if (!isProcessing) onDismiss() },
        containerColor = Color(0xFF1E1E22),
        titleContentColor = Color.White,
        textContentColor = Color.White.copy(alpha = 0.8f),
        title = { Text("Chat Security Settings", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Manage encryption options for this chat.", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                
                // Option 1: Lock Now
                Button(
                    onClick = {
                        viewModel.activeSessionKeys.remove(sessionId)
                        viewModel.loadSession(sessionId)
                        onDismiss()
                        Toast.makeText(context, "Chat Locked", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White.copy(alpha = 0.05f),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color(0xFF03DAC5))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Lock Chat Now", fontWeight = FontWeight.Bold)
                }
                
                // Option 2: Remove Password
                Button(
                    onClick = {
                        isProcessing = true
                        viewModel.removeSessionLock(sessionId) { success ->
                            isProcessing = false
                            if (success) {
                                Toast.makeText(context, "Password Lock Removed", Toast.LENGTH_SHORT).show()
                                onDismiss()
                            } else {
                                Toast.makeText(context, "Failed to remove lock", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    enabled = !isProcessing,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFEF4444).copy(alpha = 0.15f),
                        contentColor = Color(0xFFEF4444)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color(0xFFEF4444), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color(0xFFEF4444))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Remove Password Lock", fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !isProcessing,
                onClick = onDismiss
            ) {
                Text("Close", color = Color.White.copy(alpha = 0.6f))
            }
        }
    )
}

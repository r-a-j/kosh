package com.rajpawardotin.kosh.ui.chat.dialogs

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.rajpawardotin.kosh.domain.model.ChatSession
import com.rajpawardotin.kosh.ui.chat.ChatViewModel

@Composable
fun RenameSessionDialog(
    session: ChatSession,
    viewModel: ChatViewModel,
    onDismiss: () -> Unit
) {
    var newTitle by remember { mutableStateOf(session.title) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1E1E22),
        titleContentColor = Color.White,
        title = { Text("Rename Vault", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) },
        text = {
            OutlinedTextField(
                value = newTitle,
                onValueChange = { newTitle = it },
                label = { Text("Title") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedLabelColor = Color(0xFF03DAC5),
                    unfocusedLabelColor = Color.White.copy(alpha = 0.6f),
                    focusedBorderColor = Color(0xFF03DAC5),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                    cursorColor = Color(0xFF03DAC5)
                ),
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    viewModel.renameSession(session.id, newTitle)
                    onDismiss()
                }
            ) {
                Text("Save", color = Color(0xFF03DAC5), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.White.copy(alpha = 0.6f))
            }
        }
    )
}

package com.rajpawardotin.kosh.ui.chat.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun ImportPasswordDialog(
    onDismiss: () -> Unit,
    onImport: (String, () -> Unit, (String) -> Unit) -> Unit
) {
    var password by remember { mutableStateOf("") }
    var passwordVisibility by remember { mutableStateOf(false) }
    var isProcessing by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { if (!isProcessing) onDismiss() },
        containerColor = Color(0xFF1E1E22),
        titleContentColor = Color.White,
        title = { Text("Restore Password", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Enter the password that was used to encrypt this backup file.", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    singleLine = true,
                    enabled = !isProcessing,
                    visualTransformation = if (passwordVisibility) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisibility = !passwordVisibility }) {
                            Icon(
                                imageVector = if (passwordVisibility) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = null,
                                tint = Color.Gray
                            )
                        }
                    },
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
            }
        },
        confirmButton = {
            TextButton(
                enabled = password.isNotEmpty() && !isProcessing,
                onClick = {
                    isProcessing = true
                    onImport(
                        password,
                        { isProcessing = false }, // onSuccess
                        { err -> isProcessing = false } // onError
                    )
                }
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color(0xFF03DAC5), strokeWidth = 2.dp)
                } else {
                    Text("Restore", color = Color(0xFF03DAC5), fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(
                enabled = !isProcessing,
                onClick = onDismiss
            ) {
                Text("Cancel", color = Color.White.copy(alpha = 0.6f))
            }
        }
    )
}

package com.rajpawardotin.kosh.ui.chat.dialogs

import android.content.Context
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.rajpawardotin.kosh.domain.model.ChatSession
import com.rajpawardotin.kosh.ui.chat.ChatViewModel

@Composable
fun DeleteSessionDialog(
    session: ChatSession,
    viewModel: ChatViewModel,
    context: Context,
    onDismiss: () -> Unit
) {
    val isEncrypted = session.encryptedKeyPassword != null
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisibility by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var isDeleting by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.padding(16.dp).clip(RoundedCornerShape(28.dp)).border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(28.dp)),
        containerColor = Color(0xFF16161A),
        titleContentColor = Color.White,
        textContentColor = Color.White.copy(alpha = 0.8f),
        title = { Text("Delete Cognitive Vault", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Are you sure you want to permanently delete this chat history?")

                if (isEncrypted) {
                    Text(
                        text = "This vault is locked. Provide your passcode to authorize deletion.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = {
                            confirmPassword = it
                            errorMsg = null
                        },
                        label = { Text("Passcode") },
                        singleLine = true,
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

                    if (errorMsg != null) {
                        Text(
                            text = errorMsg!!,
                            color = Color(0xFFCF6679),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    if (session.encryptedKeyBiometric != null) {
                        Button(
                            onClick = {
                                viewModel.unlockSessionWithBiometrics(session.id, context) { success ->
                                    if (success) {
                                        viewModel.deleteSession(session.id)
                                        onDismiss()
                                    } else {
                                        errorMsg = "Biometric authentication failed"
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF03DAC5).copy(alpha = 0.1f),
                                contentColor = Color(0xFF03DAC5)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(imageVector = Icons.Default.Fingerprint, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Verify with Fingerprint")
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (isEncrypted) {
                        isDeleting = true
                        viewModel.verifySessionPassword(session.id, confirmPassword) { success ->
                            isDeleting = false
                            if (success) {
                                viewModel.deleteSession(session.id)
                                onDismiss()
                            } else {
                                errorMsg = "Incorrect passcode"
                            }
                        }
                    } else {
                        viewModel.deleteSession(session.id)
                        onDismiss()
                    }
                },
                enabled = !isDeleting && (!isEncrypted || confirmPassword.isNotEmpty())
            ) {
                Text("Delete", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isDeleting
            ) {
                Text("Cancel", color = Color.White.copy(alpha = 0.6f))
            }
        }
    )
}

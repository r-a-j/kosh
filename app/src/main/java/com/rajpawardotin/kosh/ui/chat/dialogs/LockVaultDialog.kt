package com.rajpawardotin.kosh.ui.chat.dialogs

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rajpawardotin.kosh.domain.model.ChatSession
import com.rajpawardotin.kosh.ui.chat.ChatViewModel

@Composable
fun LockVaultDialog(
    session: ChatSession,
    viewModel: ChatViewModel,
    context: Context,
    onDismiss: () -> Unit,
    onSuccess: (String) -> Unit
) {
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var enableBiometric by remember { mutableStateOf(false) }
    var passwordVisibility by remember { mutableStateOf(false) }
    var isProcessing by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { if (!isProcessing) onDismiss() },
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clip(RoundedCornerShape(28.dp))
            .border(1.dp, Color(0xFF03DAC5).copy(alpha = 0.2f), RoundedCornerShape(28.dp)),
        containerColor = Color(0xFF16161A),
        titleContentColor = Color.White,
        textContentColor = Color.White.copy(alpha = 0.7f),
        title = { 
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Security, contentDescription = null, tint = Color(0xFF03DAC5), modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text("Lock Vault", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black, letterSpacing = 0.5.sp))
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .padding(top = 8.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text("Secure this conversation with AES-256 encryption. This will mask the title and content in history.", style = MaterialTheme.typography.bodyMedium)
                
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Set Passcode") },
                    singleLine = true,
                    enabled = !isProcessing,
                    shape = RoundedCornerShape(16.dp),
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
                        unfocusedLabelColor = Color.Gray,
                        focusedBorderColor = Color(0xFF03DAC5),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                        cursorColor = Color(0xFF03DAC5)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text("Confirm Passcode") },
                    singleLine = true,
                    enabled = !isProcessing,
                    shape = RoundedCornerShape(16.dp),
                    visualTransformation = PasswordVisualTransformation(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedLabelColor = Color(0xFF03DAC5),
                        unfocusedLabelColor = Color.Gray,
                        focusedBorderColor = Color(0xFF03DAC5),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                        cursorColor = Color(0xFF03DAC5)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Surface(
                    onClick = { if (!isProcessing) enableBiometric = !enableBiometric },
                    shape = RoundedCornerShape(16.dp),
                    color = if (enableBiometric) Color(0xFF03DAC5).copy(alpha = 0.1f) else Color.White.copy(alpha = 0.05f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp, 
                        if (enableBiometric) Color(0xFF03DAC5).copy(alpha = 0.5f) else Color.Transparent
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(12.dp).fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = enableBiometric,
                            onCheckedChange = { enableBiometric = it },
                            enabled = !isProcessing,
                            colors = CheckboxDefaults.colors(
                                checkedColor = Color(0xFF03DAC5),
                                checkmarkColor = Color.Black,
                                uncheckedColor = Color.White.copy(alpha = 0.4f)
                            )
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Column {
                            Text("Biometric Unlock", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold), color = Color.White)
                            Text("Allow fingerprint to bypass passcode", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = password.isNotEmpty() && password == confirmPassword && !isProcessing,
                onClick = {
                    isProcessing = true
                    viewModel.lockSession(session.id, password, enableBiometric, context) { success, mnemonic ->
                        isProcessing = false
                        if (success && mnemonic != null) {
                            onSuccess(mnemonic)
                        } else {
                            Toast.makeText(context, "Locking failed", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF03DAC5),
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.Black, strokeWidth = 2.dp)
                } else {
                    Text("Seal Vault", fontWeight = FontWeight.Black)
                }
            }
        },
        dismissButton = {
            TextButton(
                enabled = !isProcessing,
                onClick = onDismiss
            ) {
                Text("Cancel", color = Color.White.copy(alpha = 0.4f))
            }
        }
    )
}

package com.rajpawardotin.kosh.ui.chat.dialogs

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CrashRecoveryDialog(
    onTryAgain: () -> Unit,
    onDisableModel: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { /* Force explicit choice */ },
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clip(RoundedCornerShape(28.dp))
            .border(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f), RoundedCornerShape(28.dp)),
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Warning, contentDescription = "Warning", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Critical Error Detected", fontWeight = FontWeight.Bold, fontSize = 20.sp)
            }
        },
        text = {
            Text("Kosh shut down unexpectedly while loading this model. If this happens repeatedly, the model file may be corrupted or unsupported by your device.", fontSize = 16.sp)
        },
        confirmButton = {
            TextButton(onClick = onTryAgain) {
                Text("Try Again", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDisableModel) {
                Text("Disable Model", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
            }
        }
    )
}

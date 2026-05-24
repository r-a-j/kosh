package com.rajpawardotin.kosh.ui.chat.dialogs

import android.widget.Toast
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun RecoveryPhraseDialog(
    mnemonic: String,
    onDismiss: () -> Unit
) {
    var confirmedWritten by remember { mutableStateOf(false) }
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = { }, // Force user to acknowledge
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clip(RoundedCornerShape(28.dp))
            .border(1.dp, Color(0xFF03DAC5).copy(alpha = 0.2f), RoundedCornerShape(28.dp)),
        containerColor = Color(0xFF16161A),
        titleContentColor = Color.White,
        textContentColor = Color.White.copy(alpha = 0.8f),
        title = { 
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFF03DAC5), modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text("Recovery Secret", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black, letterSpacing = 0.5.sp))
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                Surface(
                    color = Color(0xFFFF9100).copy(alpha = 0.1f),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF9100).copy(alpha = 0.2f))
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = Color(0xFFFF9100), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            "If you lose your passcode, this phrase is the ONLY way to recover your data. Store it safely offline.",
                            color = Color(0xFFFF9100).copy(alpha = 0.9f),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.Black.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = mnemonic,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.2.sp,
                                lineHeight = 28.sp,
                                fontFamily = FontFamily.Monospace
                            ),
                            color = Color(0xFF03DAC5),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp)
                        )
                        
                        Button(
                            onClick = { 
                                clipboardManager.setText(AnnotatedString(mnemonic))
                                Toast.makeText(context, "Mnemonic Copied", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF03DAC5).copy(alpha = 0.1f),
                                contentColor = Color(0xFF03DAC5)
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF03DAC5).copy(alpha = 0.25f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().height(44.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "COPY RECOVERY PHRASE",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.sp
                                )
                            )
                        }
                    }
                }

                Surface(
                    onClick = { confirmedWritten = !confirmedWritten },
                    color = Color.Transparent,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Checkbox(
                            checked = confirmedWritten,
                            onCheckedChange = { confirmedWritten = it },
                            colors = CheckboxDefaults.colors(checkedColor = Color(0xFF03DAC5))
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("I have secured my 12-word phrase", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.6f))
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = confirmedWritten,
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF03DAC5),
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Finalize Encryption", fontWeight = FontWeight.Black)
            }
        }
    )
}

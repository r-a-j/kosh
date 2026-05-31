package com.rajpawardotin.kosh.ui.chat.components

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.ThumbDown
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ResponseActionsRow(
    textToCopy: String,
    isCurrentlySpeaking: Boolean = false,
    onPlayTts: () -> Unit = {},
    onStopTts: () -> Unit = {},
    feedback: Int = 0,
    onFeedbackChanged: (Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    val isThumbUpSelected = feedback == 1
    val isThumbDownSelected = feedback == -1
    var isCopied by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }
    var showRawTextDialog by remember { mutableStateOf(false) }

    val defaultIconTint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
    val activeGoodColor = MaterialTheme.colorScheme.primary
    val activeBadColor = MaterialTheme.colorScheme.error

    // Android SAF document export launcher
    val exportFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain"),
        onResult = { uri ->
            uri?.let {
                scope.launch(Dispatchers.IO) {
                    try {
                        context.contentResolver.openOutputStream(it)?.use { outputStream ->
                            outputStream.write(textToCopy.toByteArray())
                        }
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Saved to text file!", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Failed to save: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. Thumbs Up
        IconButton(
            onClick = { 
                val nextFeedback = if (isThumbUpSelected) 0 else 1
                onFeedbackChanged(nextFeedback)
                if (nextFeedback == 1) {
                    Toast.makeText(context, "Feedback recorded: Helpful", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = if (isThumbUpSelected) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp,
                contentDescription = "Good response",
                tint = if (isThumbUpSelected) activeGoodColor else defaultIconTint,
                modifier = Modifier.size(16.dp)
            )
        }

        // 2. Thumbs Down
        IconButton(
            onClick = { 
                val nextFeedback = if (isThumbDownSelected) 0 else -1
                onFeedbackChanged(nextFeedback)
                if (nextFeedback == -1) {
                    Toast.makeText(context, "Feedback recorded: Unhelpful", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = if (isThumbDownSelected) Icons.Filled.ThumbDown else Icons.Outlined.ThumbDown,
                contentDescription = "Bad response",
                tint = if (isThumbDownSelected) activeBadColor else defaultIconTint,
                modifier = Modifier.size(16.dp)
            )
        }

        // 3. Copy Text
        IconButton(
            onClick = {
                clipboardManager.setText(AnnotatedString(textToCopy))
                isCopied = true
                scope.launch {
                    delay(2000)
                    isCopied = false
                }
            },
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = if (isCopied) Icons.Filled.Check else Icons.Outlined.ContentCopy,
                contentDescription = "Copy text",
                tint = if (isCopied) activeGoodColor else defaultIconTint,
                modifier = Modifier.size(16.dp)
            )
        }

        // 4. Share Text (System Chooser)
        IconButton(
            onClick = {
                try {
                    val shareIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, textToCopy)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Share AI Response"))
                } catch (e: Exception) {
                    Toast.makeText(context, "Cannot share: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Share,
                contentDescription = "Share",
                tint = defaultIconTint,
                modifier = Modifier.size(16.dp)
            )
        }

        // 5. Read Aloud / Stop (Functional TTS)
        IconButton(
            onClick = {
                if (isCurrentlySpeaking) {
                    onStopTts()
                } else {
                    onPlayTts()
                }
            },
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = if (isCurrentlySpeaking) Icons.Filled.Stop else Icons.Outlined.VolumeUp,
                contentDescription = if (isCurrentlySpeaking) "Stop speaking" else "Read aloud",
                tint = if (isCurrentlySpeaking) activeGoodColor else defaultIconTint,
                modifier = Modifier.size(16.dp)
            )
        }

        // 6. More Options Menu
        Box {
            IconButton(
                onClick = { showMoreMenu = true },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.MoreVert,
                    contentDescription = "More options",
                    tint = defaultIconTint,
                    modifier = Modifier.size(16.dp)
                )
            }

            DropdownMenu(
                expanded = showMoreMenu,
                onDismissRequest = { showMoreMenu = false },
                modifier = Modifier.background(MaterialTheme.colorScheme.surface)
            ) {
                DropdownMenuItem(
                    text = { Text("View Raw Source") },
                    leadingIcon = { Icon(Icons.Outlined.Code, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    onClick = {
                        showMoreMenu = false
                        showRawTextDialog = true
                    }
                )
                DropdownMenuItem(
                    text = { Text("Google Search") },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    onClick = {
                        showMoreMenu = false
                        try {
                            val searchUrl = "https://www.google.com/search?q=" + Uri.encode(textToCopy.take(150))
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(searchUrl))
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Cannot search: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
                DropdownMenuItem(
                    text = { Text("Export as .txt") },
                    leadingIcon = { Icon(Icons.Outlined.Save, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    onClick = {
                        showMoreMenu = false
                        try {
                            exportFileLauncher.launch("kosh_response.txt")
                        } catch (e: Exception) {
                            Toast.makeText(context, "Export error: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
        }
    }

    // View Raw Markdown Source Dialog
    if (showRawTextDialog) {
        Dialog(onDismissRequest = { showRawTextDialog = false }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 450.dp)
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Raw Response Source",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text(
                                text = textToCopy,
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(textToCopy))
                                Toast.makeText(context, "Copied source to clipboard!", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Text("Copy Source", fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { showRawTextDialog = false },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Close")
                        }
                    }
                }
            }
        }
    }
}

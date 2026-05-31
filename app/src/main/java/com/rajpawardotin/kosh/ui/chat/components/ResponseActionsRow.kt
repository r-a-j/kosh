package com.rajpawardotin.kosh.ui.chat.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.ThumbDown
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ResponseActionsRow(
    textToCopy: String,
    isCurrentlySpeaking: Boolean = false,
    onPlayTts: () -> Unit = {},
    onStopTts: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val clipboardManager = LocalClipboardManager.current
    var isThumbUpSelected by remember { mutableStateOf(false) }
    var isThumbDownSelected by remember { mutableStateOf(false) }
    var isCopied by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val defaultIconTint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
    val activeGoodColor = MaterialTheme.colorScheme.primary
    val activeBadColor = MaterialTheme.colorScheme.error

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = { 
                isThumbUpSelected = !isThumbUpSelected
                if (isThumbUpSelected) isThumbDownSelected = false
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

        IconButton(
            onClick = { 
                isThumbDownSelected = !isThumbDownSelected
                if (isThumbDownSelected) isThumbUpSelected = false
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

        IconButton(
            onClick = { /* Share */ },
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Share,
                contentDescription = "Share",
                tint = defaultIconTint,
                modifier = Modifier.size(16.dp)
            )
        }

        IconButton(
            onClick = { /* Speak */ },
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.VolumeUp,
                contentDescription = "Speak",
                tint = defaultIconTint,
                modifier = Modifier.size(16.dp)
            )
        }

        IconButton(
            onClick = { /* More */ },
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.MoreVert,
                contentDescription = "More",
                tint = defaultIconTint,
                modifier = Modifier.size(16.dp)
            )
        }

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
                imageVector = if (isCurrentlySpeaking) Icons.Filled.Stop else Icons.Filled.VolumeUp,
                contentDescription = if (isCurrentlySpeaking) "Stop speaking" else "Read aloud",
                tint = if (isCurrentlySpeaking) activeGoodColor else defaultIconTint,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

package com.rajpawardotin.kosh.ui.components
 
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rajpawardotin.kosh.domain.model.AttachedFile

@Composable
fun ChatInput(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    onStop: () -> Unit,
    onVoiceClick: () -> Unit,
    onAttachClick: () -> Unit,
    attachedFiles: List<AttachedFile>,
    onDetachFile: (AttachedFile) -> Unit,
    enabled: Boolean,
    isGenerating: Boolean,
    isInternetEnabled: Boolean,
    isSearchForced: Boolean,
    onToggleSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Horizontal Attachment Preview Bar
        AnimatedVisibility(
            visible = attachedFiles.isNotEmpty(),
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(attachedFiles) { file ->
                    AttachmentBadge(file = file, onDetach = { onDetachFile(file) })
                }
            }
        }

        // Input Box Bubble
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .shadow(
                    elevation = if (enabled) 4.dp else 2.dp, 
                    shape = CircleShape
                ),
            shape = CircleShape,
            color = Color(0xFF1E1E22),
            border = androidx.compose.foundation.BorderStroke(
                width = 1.dp,
                brush = Brush.horizontalGradient(
                    colors = if (isGenerating) {
                        listOf(Color(0xFF03DAC5), Color(0xFF6200EE))
                    } else {
                        listOf(Color.White.copy(alpha = 0.1f), Color.White.copy(alpha = 0.1f))
                    }
                )
            )
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Paperclip button (left side)
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onAttachClick()
                    },
                    enabled = enabled && !isGenerating,
                    modifier = Modifier
                        .padding(start = 4.dp)
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.05f))
                ) {
                    Icon(
                        imageVector = Icons.Default.AttachFile,
                        contentDescription = "Attach Document",
                        tint = Color(0xFF03DAC5)
                    )
                }
 
                // Web Search Toggle Button
                AnimatedVisibility(visible = isInternetEnabled) {
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onToggleSearch()
                        },
                        enabled = enabled && !isGenerating,
                        modifier = Modifier
                            .padding(start = 4.dp)
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSearchForced) {
                                    Color(0xFF00E5FF).copy(alpha = 0.15f)
                                } else {
                                    Color.White.copy(alpha = 0.05f)
                                }
                            )
                            .border(
                                width = 1.dp,
                                color = if (isSearchForced) Color(0xFF00E5FF).copy(alpha = 0.4f) else Color.Transparent,
                                shape = CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Public,
                            contentDescription = "Force Web Search",
                            tint = if (isSearchForced) Color(0xFF00E5FF) else Color.Gray
                        )
                    }
                }

                TextField(
                    value = value,
                    onValueChange = onValueChange,
                    placeholder = { 
                        Text(
                            if (enabled) "Neural Command..." else "Neural Core Offline...", 
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.Gray.copy(alpha = if (enabled) 1f else 0.5f)
                        ) 
                    },
                    modifier = Modifier.weight(1f),
                    enabled = enabled && !isGenerating,
                    maxLines = 4,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color.White),
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Send,
                        capitalization = KeyboardCapitalization.Sentences
                    ),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (value.isNotBlank() || attachedFiles.isNotEmpty()) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onSend()
                            }
                        }
                    ),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent,
                        cursorColor = Color(0xFF03DAC5)
                    )
                )

                // Voice Mic button (right side)
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onVoiceClick()
                    },
                    enabled = enabled && !isGenerating,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.05f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Voice Input",
                        tint = Color(0xFF03DAC5)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Send / Stop button
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        if (isGenerating) {
                            onStop()
                        } else {
                            onSend()
                        }
                    },
                    enabled = enabled && (isGenerating || value.isNotBlank() || attachedFiles.isNotEmpty()),
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            if (!enabled) {
                                Brush.linearGradient(listOf(Color(0xFF222222), Color(0xFF222222)))
                            } else if (isGenerating) {
                                Brush.linearGradient(listOf(Color(0xFFFF5252), Color(0xFFFF1744)))
                            } else if (value.isNotBlank() || attachedFiles.isNotEmpty()) {
                                Brush.linearGradient(listOf(Color(0xFF03DAC5), Color(0xFF6200EE)))
                            } else {
                                Brush.linearGradient(listOf(Color(0xFF222222), Color(0xFF222222)))
                            }
                        )
                ) {
                    if (isGenerating) {
                        Icon(
                            imageVector = Icons.Default.Stop,
                            contentDescription = "Stop",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AttachmentBadge(
    file: AttachedFile,
    onDetach: () -> Unit,
    modifier: Modifier = Modifier
) {
    val extensionColor = when (file.fileType.lowercase()) {
        "pdf" -> Color(0xFFEF5350)     // Soft Red for PDF
        "md" -> Color(0xFF8B5CF6)      // Violet for Markdown
        else -> Color(0xFF03DAC5)      // Cyan for TXT/others
    }

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .border(
                width = 1.dp,
                brush = Brush.horizontalGradient(
                    listOf(extensionColor.copy(alpha = 0.5f), Color.White.copy(alpha = 0.05f))
                ),
                shape = RoundedCornerShape(16.dp)
            ),
        color = Color(0xFF151518).copy(alpha = 0.9f),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Extension tag capsule
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(extensionColor.copy(alpha = 0.15f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = file.fileType.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = 9.sp,
                        letterSpacing = 0.5.sp
                    ),
                    color = extensionColor
                )
            }

            // File Name & Size
            Column {
                Text(
                    text = file.fileName,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 120.dp)
                )
                Text(
                    text = formatFileSize(file.fileSize),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = Color.Gray
                )
            }

            // Detach button
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.08f))
                    .clickable { onDetach() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Remove",
                    tint = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}

private fun formatFileSize(size: Long): String {
    if (size <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB")
    val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
    return String.format("%.1f %s", size / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
}

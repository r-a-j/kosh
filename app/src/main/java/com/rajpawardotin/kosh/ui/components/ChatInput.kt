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
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
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

    // Adaptive corner radius: morphs from a capsule (26.dp) to a rounded rect (18.dp) when multi-line
    val lines = value.count { it == '\n' } + 1
    val isMultiLine = lines > 1 || value.length > 45
    val cornerRadius by animateDpAsState(
        targetValue = if (isMultiLine) 18.dp else 26.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "cornerRadius"
    )
    val inputShape = RoundedCornerShape(cornerRadius)

    // Breathing border animation for active generation - ONLY runs when generating
    val borderAlpha = if (isGenerating) {
        val generatingBorderTransition = rememberInfiniteTransition(label = "generating_border")
        generatingBorderTransition.animateFloat(
            initialValue = 0.3f,
            targetValue = 0.8f,
            animationSpec = infiniteRepeatable(
                animation = tween(1500, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "borderAlpha"
        ).value
    } else {
        0.08f // Static border when idle
    }

    // Breathing shimmer alpha for generation placeholder - ONLY runs when generating
    val generatingAlpha = if (isGenerating) {
        val generatingTextTransition = rememberInfiniteTransition(label = "generating_text")
        generatingTextTransition.animateFloat(
            initialValue = 0.4f,
            targetValue = 0.8f,
            animationSpec = infiniteRepeatable(
                animation = tween(1200, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "generatingAlpha"
        ).value
    } else {
        0.6f // Static placeholder when idle
    }

    // Double-ended dynamic padding so text does not clash with the capsule borders or icons
    val startPadding by animateDpAsState(
        targetValue = if (isGenerating) 16.dp else 8.dp, // Comfort spacing when icons are visible
        animationSpec = spring(stiffness = 1200f),
        label = "textFieldStartPadding"
    )

    val showMic = value.isEmpty() && attachedFiles.isEmpty() && !isGenerating
    val endPadding by animateDpAsState(
        targetValue = if (showMic) 8.dp else 16.dp, // Comfort spacing when Mic icon is visible
        animationSpec = spring(stiffness = 1200f),
        label = "textFieldEndPadding"
    )

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

        // Horizontal Row containing the Input Bubble and the External floating Send Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically // Vertically center the bubble and the Send button
        ) {
            // Input Box Bubble (expands to fill all space except the Send button)
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 56.dp) // Standard Material 3 height
                    .shadow(
                        elevation = if (enabled && !isGenerating) 4.dp else 0.dp,
                        shape = inputShape
                    ),
                shape = inputShape,
                color = if (isGenerating) Color(0xFF161619).copy(alpha = 0.95f) else Color(0xFF1E1E22).copy(alpha = 0.9f),
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.dp,
                    brush = Brush.horizontalGradient(
                        colors = if (isGenerating) {
                            listOf(
                                Color(0xFF03DAC5).copy(alpha = borderAlpha),
                                Color(0xFF6200EE).copy(alpha = borderAlpha)
                            )
                        } else {
                            listOf(
                                Color.White.copy(alpha = 0.08f),
                                Color.White.copy(alpha = 0.08f)
                            )
                        }
                    )
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp), // Symmetric 6.dp padding top/bottom
                    verticalAlignment = Alignment.CenterVertically // Center all elements vertically for absolute symmetry
                ) {
                    // Paperclip button (left side) - borderless, no background
                    AnimatedVisibility(
                        visible = !isGenerating,
                        enter = fadeIn(animationSpec = spring(stiffness = 1200f)) + expandHorizontally(animationSpec = spring(stiffness = 1200f)),
                        exit = fadeOut(animationSpec = spring(stiffness = 1200f)) + shrinkHorizontally(animationSpec = spring(stiffness = 1200f))
                    ) {
                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onAttachClick()
                            },
                            enabled = enabled,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AttachFile,
                                contentDescription = "Attach Document",
                                tint = Color(0xFF03DAC5)
                            )
                        }
                    }

                    // Web Search Toggle Button - borderless, no background
                    AnimatedVisibility(
                        visible = isInternetEnabled && !isGenerating,
                        enter = fadeIn(animationSpec = spring(stiffness = 1200f)) + expandHorizontally(animationSpec = spring(stiffness = 1200f)),
                        exit = fadeOut(animationSpec = spring(stiffness = 1200f)) + shrinkHorizontally(animationSpec = spring(stiffness = 1200f))
                    ) {
                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onToggleSearch()
                            },
                            enabled = enabled,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Public,
                                contentDescription = "Force Web Search",
                                tint = if (isSearchForced) Color(0xFF00E5FF) else Color.White.copy(alpha = 0.5f)
                            )
                        }
                    }

                    // Custom BasicTextField for polished padding and alignment
                    val placeholderText = when {
                        !enabled -> "Neural Core Offline..."
                        isGenerating -> "Kosh is composing..."
                        else -> "Neural Command..."
                    }
                    val placeholderColor = when {
                        !enabled -> Color.Gray.copy(alpha = 0.3f)
                        isGenerating -> Color(0xFF03DAC5).copy(alpha = generatingAlpha)
                        else -> Color.Gray.copy(alpha = 0.6f)
                    }

                    BasicTextField(
                        value = value,
                        onValueChange = onValueChange,
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = startPadding, end = endPadding, top = 12.dp, bottom = 12.dp),
                        enabled = enabled && !isGenerating,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            color = Color.White,
                            platformStyle = androidx.compose.ui.text.PlatformTextStyle(includeFontPadding = false)
                        ),
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Default, // Carriage Return for multi-line support
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
                        maxLines = 5,
                        cursorBrush = SolidColor(Color(0xFF03DAC5)),
                        decorationBox = { innerTextField ->
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                if (value.isEmpty()) {
                                    Text(
                                        text = placeholderText,
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            platformStyle = androidx.compose.ui.text.PlatformTextStyle(includeFontPadding = false)
                                        ),
                                        color = placeholderColor
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )

                    // Voice Mic button (right side) - borderless, no background. Hidden when typing or generating.
                    AnimatedVisibility(
                        visible = showMic,
                        enter = fadeIn(animationSpec = spring(stiffness = 1200f)) + expandHorizontally(animationSpec = spring(stiffness = 1200f)),
                        exit = fadeOut(animationSpec = spring(stiffness = 1200f)) + shrinkHorizontally(animationSpec = spring(stiffness = 1200f))
                    ) {
                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onVoiceClick()
                            },
                            enabled = enabled,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Voice Input",
                                tint = Color(0xFF03DAC5)
                            )
                        }
                    }
                }
            }

            // Dynamic Spacer between input bubble and Send/Stop button (only when Send button is active)
            val showSendStop = isGenerating || value.isNotEmpty() || attachedFiles.isNotEmpty()
            AnimatedVisibility(
                visible = showSendStop,
                enter = fadeIn(animationSpec = spring(stiffness = 1200f)) + expandHorizontally(animationSpec = spring(stiffness = 1200f)),
                exit = fadeOut(animationSpec = spring(stiffness = 1200f)) + shrinkHorizontally(animationSpec = spring(stiffness = 1200f))
            ) {
                Spacer(modifier = Modifier.width(8.dp))
            }

            // External Floating Send / Stop Action Button (Telegram style)
            AnimatedVisibility(
                visible = showSendStop,
                enter = scaleIn(animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = 1200f)) + 
                        fadeIn(animationSpec = spring(stiffness = 1200f)) + 
                        expandHorizontally(animationSpec = spring(stiffness = 1200f)),
                exit = scaleOut(animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = 1200f)) + 
                       fadeOut(animationSpec = spring(stiffness = 1200f)) + 
                       shrinkHorizontally(animationSpec = spring(stiffness = 1200f))
            ) {
                val stopPulseTransition = rememberInfiniteTransition(label = "stop_button_pulse")
                val stopPulseScale by stopPulseTransition.animateFloat(
                    initialValue = 1f,
                    targetValue = 1.08f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1000, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "stopPulseScale"
                )

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
                        .size(48.dp) // Removed padding to align bottom bounds perfectly
                        .shadow(
                            elevation = if (enabled) 4.dp else 0.dp,
                            shape = CircleShape
                        )
                        .graphicsLayer {
                            if (isGenerating) {
                                scaleX = stopPulseScale
                                scaleY = stopPulseScale
                            }
                        }
                        .clip(CircleShape)
                        .background(
                            if (!enabled) {
                                Brush.linearGradient(listOf(Color(0xFF222222), Color(0xFF222222)))
                            } else if (isGenerating) {
                                Brush.linearGradient(listOf(Color(0xFFFF5252), Color(0xFFFF1744)))
                            } else {
                                Brush.linearGradient(listOf(Color(0xFF03DAC5), Color(0xFF6200EE)))
                            }
                        )
                ) {
                    if (isGenerating) {
                        Icon(
                            imageVector = Icons.Default.Stop,
                            contentDescription = "Stop",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
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

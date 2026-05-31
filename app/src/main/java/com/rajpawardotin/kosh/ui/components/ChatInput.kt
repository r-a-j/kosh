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
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val outline = MaterialTheme.colorScheme.outline
    val surfaceColor = MaterialTheme.colorScheme.surface
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val errorColor = MaterialTheme.colorScheme.error
    val errorContainer = MaterialTheme.colorScheme.errorContainer

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
        0.12f
    }

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
        0.6f
    }

    val startPadding by animateDpAsState(
        targetValue = if (isGenerating) 16.dp else 8.dp,
        animationSpec = spring(stiffness = 1200f),
        label = "textFieldStartPadding"
    )

    val showMic = value.isEmpty() && attachedFiles.isEmpty() && !isGenerating
    val endPadding by animateDpAsState(
        targetValue = if (showMic) 8.dp else 16.dp,
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

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 56.dp)
                    .shadow(
                        elevation = if (enabled && !isGenerating) 4.dp else 0.dp,
                        shape = inputShape
                    ),
                shape = inputShape,
                color = if (isGenerating) surfaceColor else surfaceVariant,
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.dp,
                    brush = Brush.horizontalGradient(
                        colors = if (isGenerating) {
                            listOf(
                                primary.copy(alpha = borderAlpha),
                                secondary.copy(alpha = borderAlpha)
                            )
                        } else {
                            listOf(
                                outline.copy(alpha = 0.12f),
                                outline.copy(alpha = 0.12f)
                            )
                        }
                    )
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Paperclip button
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
                                tint = primary
                            )
                        }
                    }

                    // Web Search Toggle Button
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
                                tint = if (isSearchForced) primary else onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        }
                    }

                    val placeholderText = when {
                        !enabled -> "Model Offline..."
                        isGenerating -> "Kosh is composing..."
                        else -> "Ask Kosh..."
                    }
                    val placeholderColor = when {
                        !enabled -> onSurfaceVariant.copy(alpha = 0.3f)
                        isGenerating -> primary.copy(alpha = generatingAlpha)
                        else -> onSurfaceVariant.copy(alpha = 0.6f)
                    }

                    BasicTextField(
                        value = value,
                        onValueChange = onValueChange,
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = startPadding, end = endPadding, top = 12.dp, bottom = 12.dp),
                        enabled = enabled && !isGenerating,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            color = onSurface,
                            platformStyle = androidx.compose.ui.text.PlatformTextStyle(includeFontPadding = false)
                        ),
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Default,
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
                        cursorBrush = SolidColor(primary),
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

                    // Voice Mic button
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
                                tint = primary
                            )
                        }
                    }
                }
            }

            val showSendStop = isGenerating || value.isNotEmpty() || attachedFiles.isNotEmpty()
            AnimatedVisibility(
                visible = showSendStop,
                enter = fadeIn(animationSpec = spring(stiffness = 1200f)) + expandHorizontally(animationSpec = spring(stiffness = 1200f)),
                exit = fadeOut(animationSpec = spring(stiffness = 1200f)) + shrinkHorizontally(animationSpec = spring(stiffness = 1200f))
            ) {
                Spacer(modifier = Modifier.width(8.dp))
            }

            // External Floating Send / Stop Action Button
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

                val buttonEnabled = enabled && (isGenerating || value.isNotBlank() || attachedFiles.isNotEmpty())
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        if (isGenerating) {
                            onStop()
                        } else {
                            onSend()
                        }
                    },
                    enabled = buttonEnabled,
                    modifier = Modifier
                        .size(48.dp)
                        .shadow(
                            elevation = if (buttonEnabled) 4.dp else 0.dp,
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
                            if (!buttonEnabled) {
                                Brush.linearGradient(listOf(
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                                ))
                            } else if (isGenerating) {
                                SolidColor(errorColor)
                            } else {
                                Brush.linearGradient(listOf(primary, secondary))
                            }
                        )
                ) {
                    val iconTint = if (!buttonEnabled) {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    } else {
                        Color.White
                    }
                    if (isGenerating) {
                        Icon(
                            imageVector = Icons.Default.Stop,
                            contentDescription = "Stop",
                            tint = iconTint,
                            modifier = Modifier.size(22.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = iconTint,
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
        "pdf" -> MaterialTheme.colorScheme.error
        "md" -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.primary
    }

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .border(
                width = 1.dp,
                brush = Brush.horizontalGradient(
                    listOf(extensionColor.copy(alpha = 0.5f), MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                ),
                shape = RoundedCornerShape(16.dp)
            ),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
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

            Column {
                Text(
                    text = file.fileName,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 120.dp)
                )
                Text(
                    text = formatFileSize(file.fileSize),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                    .clickable { onDetach() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Remove",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
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

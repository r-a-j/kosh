package com.rajpawardotin.kosh.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rajpawardotin.kosh.ui.chat.components.MarkdownText
import com.rajpawardotin.kosh.domain.model.ChatMessage
import com.rajpawardotin.kosh.ui.chat.ChatContentBlock
import com.rajpawardotin.kosh.ui.chat.ResponseParser
import com.rajpawardotin.kosh.ui.chat.ReferenceParser
import com.rajpawardotin.kosh.ui.chat.components.*
import com.rajpawardotin.kosh.ui.chat.ThinkingIndicator

@Composable
fun ChatBubble(
    message: ChatMessage,
    currentlySpeakingMessageId: String? = null,
    onPlayTts: ((String, String) -> Unit)? = null,
    onStopTts: (() -> Unit)? = null,
    checkedItems: Map<String, Boolean>,
    onToggleChecklistItem: (Int, Boolean) -> Unit,
    onFeedbackChanged: ((Int) -> Unit)? = null,
    onManageTagsClick: (() -> Unit)? = null,
    isSearchingInternet: Boolean,
    isGenerating: Boolean,
    agenticStateLabel: String,
    modifier: Modifier = Modifier
) {
    val alignment = if (message.isUser) Alignment.End else Alignment.Start
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val onBackground = MaterialTheme.colorScheme.onBackground
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalAlignment = alignment
    ) {
        if (!message.isUser) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier
                        .size(16.dp)
                        .graphicsLayer(alpha = 0.9f)
                        .drawWithContent {
                            drawContent()
                            drawRect(
                                brush = Brush.linearGradient(
                                    colors = listOf(primary, secondary)
                                ),
                                blendMode = BlendMode.SrcAtop
                            )
                        },
                    tint = Color.Unspecified
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (message.isSystemMessage) "System" else "Assistant",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    ),
                    color = onBackground.copy(alpha = 0.6f)
                )
                
                // Sleek pulsing breathing dot next to Assistant header
                if (isGenerating && message.isStreaming) {
                    Spacer(modifier = Modifier.width(8.dp))
                    val isThinking = agenticStateLabel.contains("Thinking") || (message.text.contains("<thinking>") && !message.text.contains("</thinking>"))
                    val statusColor = when {
                        isSearchingInternet && message.text.isEmpty() && !isThinking -> MaterialTheme.colorScheme.tertiary
                        isThinking -> MaterialTheme.colorScheme.secondary
                        else -> MaterialTheme.colorScheme.primary
                    }
                    
                    val infiniteTransition = rememberInfiniteTransition(label = "glowing_indicator")
                    val pulseAlpha by infiniteTransition.animateFloat(
                        initialValue = 0.8f,
                        targetValue = 0.0f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1500, easing = LinearEasing),
                            repeatMode = RepeatMode.Restart
                        ),
                        label = "pulseAlpha"
                    )
                    val pulseScale by infiniteTransition.animateFloat(
                        initialValue = 1.0f,
                        targetValue = 2.4f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1500, easing = LinearEasing),
                            repeatMode = RepeatMode.Restart
                        ),
                        label = "pulseScale"
                    )
                    
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(16.dp)
                    ) {
                        // Outer breathing/rippling glow ring
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .graphicsLayer {
                                    scaleX = pulseScale
                                    scaleY = pulseScale
                                    alpha = pulseAlpha
                                }
                                .clip(CircleShape)
                                .background(statusColor)
                        )
                        
                        // Inner solid core dot
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(statusColor)
                        )
                    }
                }
            }
        }

        if (message.isUser) {
            Column(horizontalAlignment = Alignment.End) {
                Box(
                    modifier = Modifier
                        .widthIn(max = 280.dp)
                        .clip(RoundedCornerShape(
                            topStart = 20.dp,
                            topEnd = 20.dp,
                            bottomStart = 20.dp,
                            bottomEnd = 4.dp // Asymmetric Material 3 bubble corner for user
                        ))
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = message.text,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            lineHeight = 22.sp,
                            letterSpacing = 0.2.sp,
                            fontSize = 15.sp
                        ),
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
                
                val parsedRefs = remember(message.sourceDocuments) {
                    ReferenceParser.parseReferences(message.sourceDocuments)
                }
                val docsList = parsedRefs.first
                if (docsList.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    DocumentSourcesView(sourceDocuments = docsList.joinToString(", "))
                }
            }
        } else {
            if (message.isStreaming) {
                val textToShow = if (message.text.isNotEmpty()) message.text else agenticStateLabel
                ThinkingIndicator(
                    text = textToShow,
                    isSearchingInternet = isSearchingInternet,
                    isGenerating = isGenerating,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp)
                )
            } else {
                val blocks = remember(message.text) { ResponseParser.parse(message.text) }
                
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp)
                ) {
                    blocks.forEachIndexed { index, block ->
                        key("${message.id}_$index") {
                            when (block) {
                                is ChatContentBlock.Text -> {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                    ) {
                                        MarkdownText(
                                            content = block.content
                                        )
                                    }
                                }
                                is ChatContentBlock.Checklist -> {
                                    ChecklistCard(
                                        items = block.items,
                                        messageKey = message.id,
                                        checkedItems = checkedItems,
                                        onToggleChecklistItem = { itemIndex, checked ->
                                            onToggleChecklistItem(itemIndex, checked)
                                        }
                                    )
                                }
                                is ChatContentBlock.CodeBlock -> {
                                    CodeBlockCard(
                                        language = block.language,
                                        code = block.code
                                    )
                                }
                                is ChatContentBlock.Sources -> {
                                    SourcesCarousel(items = block.items)
                                }
                                is ChatContentBlock.MathBlock -> {
                                    MathFormulaCard(formula = block.formula)
                                }
                                is ChatContentBlock.Thinking -> {
                                    ThinkingBlockCard(content = block.content)
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }

                    if (!message.isSystemMessage) {
                        val parsedRefs = remember(message.sourceDocuments) {
                            ReferenceParser.parseReferences(message.sourceDocuments)
                        }
                        val docsList = parsedRefs.first
                        val webList = parsedRefs.second

                        if (docsList.isNotEmpty()) {
                            DocumentSourcesView(sourceDocuments = docsList.joinToString(", "))
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                        if (webList.isNotEmpty()) {
                            SourcesCarousel(items = webList)
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                        val showPermissionSettingsButton = remember(message.text) {
                            val lowerText = message.text.lowercase()
                            (lowerText.contains("permission") || lowerText.contains("denied")) &&
                            (lowerText.contains("kosh") || 
                             lowerText.contains("microphone") || 
                             lowerText.contains("voice input") ||
                             lowerText.contains("speech") ||
                             lowerText.contains("record audio") ||
                             lowerText.contains("permission denied"))
                        }
                        if (showPermissionSettingsButton) {
                            val context = androidx.compose.ui.platform.LocalContext.current
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    try {
                                        val intent = android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                            data = android.net.Uri.fromParts("package", context.packageName, null)
                                        }
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.padding(horizontal = 8.dp)
                            ) {
                                Text("Open App Permissions Settings", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        val isCurrentlySpeaking = currentlySpeakingMessageId == message.id
                        ResponseActionsRow(
                            textToCopy = message.text,
                            isCurrentlySpeaking = isCurrentlySpeaking,
                            onPlayTts = { onPlayTts?.invoke(message.id, message.text) },
                            onStopTts = { onStopTts?.invoke() },
                            feedback = message.feedback,
                            onFeedbackChanged = { nextFeedback -> onFeedbackChanged?.invoke(nextFeedback) },
                            onManageTagsClick = onManageTagsClick
                        )
                        Text(
                            text = "Kosh may make mistakes. Verify important info.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 11.sp,
                                color = onBackground.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier.padding(start = 12.dp, top = 2.dp, bottom = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

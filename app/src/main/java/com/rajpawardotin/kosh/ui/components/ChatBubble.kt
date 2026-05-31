package com.rajpawardotin.kosh.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
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
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownColor
import com.mikepenz.markdown.m3.markdownTypography
import com.rajpawardotin.kosh.domain.model.ChatMessage
import com.rajpawardotin.kosh.ui.chat.ChatContentBlock
import com.rajpawardotin.kosh.ui.chat.ResponseParser
import com.rajpawardotin.kosh.ui.chat.ReferenceParser
import com.rajpawardotin.kosh.ui.chat.components.*

@Composable
fun ChatBubble(
    message: ChatMessage,
    currentlySpeakingMessageId: String? = null,
    onPlayTts: ((String, String) -> Unit)? = null,
    onStopTts: (() -> Unit)? = null,
    checkedItems: Map<String, Boolean>,
    onToggleChecklistItem: (Int, Boolean) -> Unit
) {
    val alignment = if (message.isUser) Alignment.End else Alignment.Start

    Column(
        modifier = Modifier
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
                                    colors = listOf(
                                        Color(0xFF8B5CF6),
                                        Color(0xFF06B6D4)
                                    )
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
                    color = Color.White.copy(alpha = 0.6f)
                )
            }
        }

        if (message.isUser) {
            Column(horizontalAlignment = Alignment.End) {
                Box(
                    modifier = Modifier
                        .widthIn(max = 280.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF2F3033)) // Sleek dark charcoal capsule
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = message.text,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            lineHeight = 22.sp,
                            letterSpacing = 0.2.sp,
                            fontSize = 15.sp
                        ),
                        color = Color.White
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
            // Render blocks
            val blocks = remember(message.text) { ResponseParser.parse(message.text) }
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp)
                    .animateContentSize(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioLowBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    )
            ) {
                blocks.forEach { block ->
                    when (block) {
                        is ChatContentBlock.Text -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Markdown(
                                    content = block.content,
                                    colors = markdownColor(
                                        text = Color(0xFFE4E4E7),
                                        codeBackground = Color(0xFF1E1E20),
                                        inlineCodeBackground = Color(0xFF1E1E20)
                                    ),
                                    typography = markdownTypography(
                                        text = MaterialTheme.typography.bodyLarge.copy(
                                            lineHeight = 26.sp,
                                            fontSize = 16.sp
                                        )
                                    )
                                )
                            }
                        }
                        is ChatContentBlock.Checklist -> {
                            ChecklistCard(
                                items = block.items,
                                messageKey = message.id,
                                checkedItems = checkedItems,
                                onToggleChecklistItem = { index, checked ->
                                    onToggleChecklistItem(index, checked)
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
                    }
                    Spacer(modifier = Modifier.height(4.dp))
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
                        message.text.contains("permission", ignoreCase = true) ||
                        message.text.contains("denied", ignoreCase = true)
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
                                containerColor = Color(0xFF03DAC5),
                                contentColor = Color.Black
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
                        onStopTts = { onStopTts?.invoke() }
                    )
                    Text(
                        text = "Kosh may make mistakes. Verify important info.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.padding(start = 12.dp, top = 2.dp, bottom = 8.dp)
                    )
                }
            }
        }
    }
}

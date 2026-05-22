package com.rajpawardotin.kosh.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material.icons.outlined.ThumbDown
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material.icons.outlined.MoreVert
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownColor
import com.mikepenz.markdown.m3.markdownTypography
import com.rajpawardotin.kosh.domain.model.ChatMessage
import com.rajpawardotin.kosh.ui.chat.ChatContentBlock
import com.rajpawardotin.kosh.ui.chat.ChecklistItem
import com.rajpawardotin.kosh.ui.chat.ResponseParser
import com.rajpawardotin.kosh.ui.chat.SourceItem
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ChatBubble(
    message: ChatMessage,
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
                    text = if (message.isSystemMessage) "System Core" else "Neural AI",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    ),
                    color = Color.White.copy(alpha = 0.6f)
                )
            }
        }

        if (message.isUser) {
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
                                messageKey = message.hashCode().toString(),
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
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }

                if (!message.isSystemMessage) {
                    ResponseActionsRow(textToCopy = message.text)
                    Text(
                        text = "Neural OS is an AI and can make mistakes. Verify important info.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier.padding(start = 12.dp, top = 2.dp, bottom = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ResponseActionsRow(
    textToCopy: String,
    modifier: Modifier = Modifier
) {
    val clipboardManager = LocalClipboardManager.current
    var isThumbUpSelected by remember { mutableStateOf(false) }
    var isThumbDownSelected by remember { mutableStateOf(false) }
    var isCopied by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

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
                tint = if (isThumbUpSelected) Color(0xFF03DAC5) else Color.White.copy(alpha = 0.5f),
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
                tint = if (isThumbDownSelected) Color(0xFFCF6679) else Color.White.copy(alpha = 0.5f),
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
                tint = if (isCopied) Color(0xFF03DAC5) else Color.White.copy(alpha = 0.5f),
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
                tint = Color.White.copy(alpha = 0.5f),
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
                tint = Color.White.copy(alpha = 0.5f),
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
                tint = Color.White.copy(alpha = 0.5f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun CodeBlockCard(language: String, code: String) {
    val clipboardManager = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F0F0F)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1E1E1E))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = language.ifEmpty { "CODE" }.uppercase(),
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    color = Color.Gray
                )
                
                Row(
                    modifier = Modifier.clickable {
                        clipboardManager.setText(AnnotatedString(code))
                        copied = true
                        scope.launch {
                            delay(2000)
                            copied = false
                        }
                    },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = if (copied) Icons.Default.Check else Icons.Default.ContentCopy,
                        contentDescription = "Copy",
                        tint = if (copied) Color(0xFF03DAC5) else Color.Gray,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = if (copied) "Copied" else "Copy",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (copied) Color(0xFF03DAC5) else Color.Gray
                    )
                }
            }
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = code,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        fontSize = 13.sp,
                        color = Color(0xFF80CBC4),
                        lineHeight = 18.sp
                    ),
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                )
            }
        }
    }
}

@Composable
fun ChecklistCard(
    items: List<ChecklistItem>,
    messageKey: String,
    checkedItems: Map<String, Boolean>,
    onToggleChecklistItem: (Int, Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161616).copy(alpha = 0.8f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF03DAC5).copy(alpha = 0.15f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF03DAC5))
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "ACTION TIMELINE",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    color = Color(0xFF03DAC5)
                )
            }
            
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items.forEach { item ->
                    val isChecked = checkedItems["${messageKey}_${item.index}"] ?: item.initiallyChecked
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onToggleChecklistItem(item.index, !isChecked) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isChecked,
                            onCheckedChange = { onToggleChecklistItem(item.index, it) },
                            colors = CheckboxDefaults.colors(
                                checkedColor = Color(0xFF03DAC5),
                                uncheckedColor = Color.Gray,
                                checkmarkColor = Color.Black
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = item.text,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                textDecoration = if (isChecked) androidx.compose.ui.text.style.TextDecoration.LineThrough else null,
                                color = if (isChecked) Color.Gray else Color.White
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SourcesCarousel(items: List<SourceItem>) {
    val uriHandler = LocalUriHandler.current
    
    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 12.dp, bottom = 8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Link,
                contentDescription = null,
                tint = Color(0xFFBB86FC),
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "REFERENCED KNOWLEDGE",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                ),
                color = Color(0xFFBB86FC)
            )
        }
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(horizontal = 4.dp)
        ) {
            items(items) { item ->
                Card(
                    modifier = Modifier
                        .width(220.dp)
                        .height(80.dp)
                        .clickable { 
                            try {
                                uriHandler.openUri(item.url)
                            } catch (e: Exception) {
                                // Ignore
                            }
                        },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E).copy(alpha = 0.9f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val initialLetter = item.title.firstOrNull()?.uppercaseChar()?.toString() ?: "W"
                        val domainColors = listOf(Color(0xFF03DAC5), Color(0xFFBB86FC), Color(0xFF6200EE))
                        val circleColor = domainColors[item.title.length % domainColors.size]
                        
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(circleColor.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = initialLetter,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = circleColor
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(10.dp))
                        
                        Column(verticalArrangement = Arrangement.Center) {
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = Color.White,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                            Text(
                                text = item.url.replace("https://", "").replace("http://", ""),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}


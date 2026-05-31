package com.rajpawardotin.kosh.ui.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rajpawardotin.kosh.ui.chat.ChecklistItem

@Composable
fun ChecklistCard(
    items: List<ChecklistItem>,
    messageKey: String,
    checkedItems: Map<String, Boolean>,
    onToggleChecklistItem: (Int, Boolean) -> Unit
) {
    val primary = MaterialTheme.colorScheme.primary
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, primary.copy(alpha = 0.15f))
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
                        .background(primary)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "ACTION TIMELINE",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    color = primary
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
                                checkedColor = primary,
                                uncheckedColor = onSurfaceVariant.copy(alpha = 0.6f),
                                checkmarkColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = parseMarkdownToAnnotatedString(item.text),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                textDecoration = if (isChecked) androidx.compose.ui.text.style.TextDecoration.LineThrough else null,
                                color = if (isChecked) onSurfaceVariant.copy(alpha = 0.6f) else onSurface
                            )
                        )
                    }
                }
            }
        }
    }
}

fun parseMarkdownToAnnotatedString(text: String): AnnotatedString {
    val builder = AnnotatedString.Builder()
    val pattern = """(\*\*\*.*?\*\*\*|\*\*.*?\*\*|\*.*?\*|_.*?_|~~.*?~~)""".toRegex()
    val matches = pattern.findAll(text)
    
    var lastIndex = 0
    for (match in matches) {
        if (match.range.first > lastIndex) {
            builder.append(text.substring(lastIndex, match.range.first))
        }
        
        val token = match.value
        when {
            token.startsWith("***") && token.endsWith("***") -> {
                builder.pushStyle(androidx.compose.ui.text.SpanStyle(fontWeight = FontWeight.Bold, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic))
                builder.append(token.substring(3, token.length - 3))
                builder.pop()
            }
            token.startsWith("**") && token.endsWith("**") -> {
                builder.pushStyle(androidx.compose.ui.text.SpanStyle(fontWeight = FontWeight.Bold))
                builder.append(token.substring(2, token.length - 2))
                builder.pop()
            }
            token.startsWith("*") && token.endsWith("*") -> {
                builder.pushStyle(androidx.compose.ui.text.SpanStyle(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic))
                builder.append(token.substring(1, token.length - 1))
                builder.pop()
            }
            token.startsWith("_") && token.endsWith("_") -> {
                builder.pushStyle(androidx.compose.ui.text.SpanStyle(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic))
                builder.append(token.substring(1, token.length - 1))
                builder.pop()
            }
            token.startsWith("~~") && token.endsWith("~~") -> {
                builder.pushStyle(androidx.compose.ui.text.SpanStyle(textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough))
                builder.append(token.substring(2, token.length - 2))
                builder.pop()
            }
            else -> {
                builder.append(token)
            }
        }
        lastIndex = match.range.last + 1
    }
    
    if (lastIndex < text.length) {
        builder.append(text.substring(lastIndex))
    }
    
    return builder.toAnnotatedString()
}

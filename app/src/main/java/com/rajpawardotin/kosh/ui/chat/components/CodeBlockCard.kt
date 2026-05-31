package com.rajpawardotin.kosh.ui.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
                        tint = if (copied) MaterialTheme.colorScheme.primary else Color.Gray,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = if (copied) "Copied" else "Copy",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (copied) MaterialTheme.colorScheme.primary else Color.Gray
                    )
                }
            }
            
            val commentRegex = "(//.*|/\\*[\\s\\S]*?\\*/|#.*)"
            val stringRegex = "(\"(?:\\\\.|[^\"\\\\])*\"|'(?:\\\\.|[^'\\\\])*')"
            val numberRegex = "(\\b\\d+(?:\\.\\d+)?\\b)"
            val keywordRegex = "(\\b(?:val|var|fun|class|interface|object|import|package|return|if|else|for|while|when|true|false|null|private|protected|public|internal|override|this|super|throw|try|catch|finally|def|from|as|elif|in|is|not|and|or|except|with|lambda|None|True|False|let|const|function|export|default|switch|case|break|continue|undefined|async|await|yield)\\b)"
            val annotationRegex = "(@[a-zA-Z_]\\w*)"
            val combinedRegex = remember { Regex("$commentRegex|$stringRegex|$numberRegex|$keywordRegex|$annotationRegex") }

            val highlightedCode = remember(code, language) {
                val builder = AnnotatedString.Builder(code)
                val commentColor = Color(0xFF6A9955)
                val stringColor = Color(0xFFCE9178)
                val numberColor = Color(0xFFB5CEA8)
                val keywordColor = Color(0xFF569CD6)
                val annotationColor = Color(0xFFC586C0)

                combinedRegex.findAll(code).forEach { matchResult ->
                    val range = matchResult.range
                    val style = when {
                        matchResult.groups[1] != null -> androidx.compose.ui.text.SpanStyle(color = commentColor)
                        matchResult.groups[2] != null -> androidx.compose.ui.text.SpanStyle(color = stringColor)
                        matchResult.groups[3] != null -> androidx.compose.ui.text.SpanStyle(color = numberColor)
                        matchResult.groups[4] != null -> androidx.compose.ui.text.SpanStyle(color = keywordColor, fontWeight = FontWeight.Bold)
                        matchResult.groups[5] != null -> androidx.compose.ui.text.SpanStyle(color = annotationColor)
                        else -> null
                    }
                    if (style != null) {
                        builder.addStyle(style, range.first, range.last + 1)
                    }
                }
                builder.toAnnotatedString()
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = highlightedCode,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        fontSize = 13.sp,
                        color = Color(0xFFD4D4D4),
                        lineHeight = 18.sp
                    ),
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                )
            }
        }
    }
}

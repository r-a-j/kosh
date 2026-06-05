package com.rajpawardotin.kosh.ui.chat.components

import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp

@Composable
fun MarkdownText(
    content: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onBackground,
    primaryColor: Color = MaterialTheme.colorScheme.primary,
    codeBackgroundColor: Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
) {
    val annotatedText = remember(content, primaryColor, codeBackgroundColor) {
        parseMarkdownToAnnotatedString(content, primaryColor, codeBackgroundColor)
    }
    
    SelectionContainer {
        Text(
            text = annotatedText,
            style = MaterialTheme.typography.bodyLarge.copy(
                lineHeight = 26.sp,
                fontSize = 16.sp
            ),
            color = color,
            modifier = modifier
        )
    }
}

private fun parseMarkdownToAnnotatedString(
    text: String,
    primaryColor: Color,
    codeBackgroundColor: Color
): AnnotatedString {
    val lines = text.lines()
    return buildAnnotatedString {
        lines.forEachIndexed { index, line ->
            val trimmedLine = line.trimStart()
            // Detect list items (bullet markers and numbered markers)
            val isBullet = trimmedLine.startsWith("- ") || trimmedLine.startsWith("* ") || trimmedLine.startsWith("• ")
            val isNumbered = """^\d+\.\s+""".toRegex().containsMatchIn(trimmedLine)
            
            val indent = line.length - trimmedLine.length
            
            // Append indentation if any
            if (indent > 0) {
                append(" ".repeat(indent))
            }
            
            val contentToParse = when {
                isBullet -> {
                    append("• ")
                    trimmedLine.substring(2)
                }
                isNumbered -> {
                    val match = """^\d+\.\s+""".toRegex().find(trimmedLine)!!
                    append(match.value)
                    trimmedLine.substring(match.value.length)
                }
                else -> line
            }
            
            // Parse inline formatting styles
            append(parseInlineMarkdown(contentToParse, primaryColor, codeBackgroundColor))
            
            if (index < lines.size - 1) {
                append("\n")
            }
        }
    }
}

private fun parseInlineMarkdown(
    text: String,
    primaryColor: Color,
    codeBackgroundColor: Color
): AnnotatedString {
    return buildAnnotatedString {
        var i = 0
        val n = text.length
        while (i < n) {
            when {
                // Inline Math: $math$
                text[i] == '$' -> {
                    val end = text.indexOf('$', i + 1)
                    if (end != -1) {
                        val mathContent = text.substring(i + 1, end)
                        val isLikelyMath = mathContent.contains('\\') ||
                                mathContent.contains('^') ||
                                mathContent.contains('_') ||
                                mathContent.length == 1 ||
                                (mathContent.length <= 15 && !mathContent.contains(' '))
                        
                        if (isLikelyMath) {
                            append(parseInlineMath(mathContent))
                        } else {
                            append("$")
                            append(parseInlineMarkdown(mathContent, primaryColor, codeBackgroundColor))
                            append("$")
                        }
                        i = end + 1
                    } else {
                        append('$')
                        i++
                    }
                }

                // Inline code: `code`
                text[i] == '`' -> {
                    val end = text.indexOf('`', i + 1)
                    if (end != -1) {
                        withStyle(style = SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            background = codeBackgroundColor,
                            fontSize = 14.sp
                        )) {
                            append(text.substring(i + 1, end))
                        }
                        i = end + 1
                    } else {
                        append('`')
                        i++
                    }
                }
                // Bold: **text**
                i + 1 < n && text[i] == '*' && text[i + 1] == '*' -> {
                    val end = text.indexOf("**", i + 2)
                    if (end != -1) {
                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                            append(text.substring(i + 2, end))
                        }
                        i = end + 2
                    } else {
                        append("**")
                        i += 2
                    }
                }
                // Bold: __text__
                i + 1 < n && text[i] == '_' && text[i + 1] == '_' -> {
                    val end = text.indexOf("__", i + 2)
                    if (end != -1) {
                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                            append(text.substring(i + 2, end))
                        }
                        i = end + 2
                    } else {
                        append("__")
                        i += 2
                    }
                }
                // Italic: *text*
                text[i] == '*' -> {
                    val end = text.indexOf('*', i + 1)
                    if (end != -1) {
                        withStyle(style = SpanStyle(fontStyle = FontStyle.Italic)) {
                            append(text.substring(i + 1, end))
                        }
                        i = end + 1
                    } else {
                        append('*')
                        i++
                    }
                }
                // Italic: _text_
                text[i] == '_' -> {
                    val end = text.indexOf('_', i + 1)
                    if (end != -1) {
                        withStyle(style = SpanStyle(fontStyle = FontStyle.Italic)) {
                            append(text.substring(i + 1, end))
                        }
                        i = end + 1
                    } else {
                        append('_')
                        i++
                    }
                }
                // Markdown Link: [anchor](url)
                text[i] == '[' -> {
                    val closingBracket = text.indexOf(']', i + 1)
                    if (closingBracket != -1 && closingBracket + 1 < n && text[closingBracket + 1] == '(') {
                        val closingParen = text.indexOf(')', closingBracket + 2)
                        if (closingParen != -1) {
                            val anchor = text.substring(i + 1, closingBracket)
                            val url = text.substring(closingBracket + 2, closingParen)
                            pushStringAnnotation(tag = "URL", annotation = url)
                            withStyle(style = SpanStyle(
                                color = primaryColor,
                                textDecoration = TextDecoration.Underline,
                                fontWeight = FontWeight.Bold
                            )) {
                                append(anchor)
                            }
                            pop()
                            i = closingParen + 1
                        } else {
                            append('[')
                            i++
                        }
                    } else {
                        append('[')
                        i++
                    }
                }
                else -> {
                    append(text[i])
                    i++
                }
            }
        }
    }
}

internal fun parseInlineMath(text: String): AnnotatedString {
    return buildAnnotatedString {
        var i = 0
        val n = text.length
        
        fun isVariable(char: Char): Boolean {
            return char in 'a'..'z' || char in 'A'..'Z'
        }
        
        while (i < n) {
            when {
                // Bold vector: \mathbf{r}
                i + 8 <= n && text.substring(i, i + 8) == "\\mathbf{" -> {
                    val end = findMatchingBracket(text, i + 7)
                    val content = text.substring(i + 8, end)
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(parseInlineMath(content))
                    }
                    i = end + 1
                }
                // Roman text: \mathrm{r} or \text{r}
                i + 8 <= n && text.substring(i, i + 8) == "\\mathrm{" -> {
                    val end = findMatchingBracket(text, i + 7)
                    val content = text.substring(i + 8, end)
                    withStyle(style = SpanStyle(fontStyle = FontStyle.Normal)) {
                        append(parseInlineMath(content))
                    }
                    i = end + 1
                }
                i + 6 <= n && text.substring(i, i + 6) == "\\text{" -> {
                    val end = findMatchingBracket(text, i + 5)
                    val content = text.substring(i + 6, end)
                    withStyle(style = SpanStyle(fontStyle = FontStyle.Normal)) {
                        append(parseInlineMath(content))
                    }
                    i = end + 1
                }
                // Hat modifier: \hat{H}
                i + 5 <= n && text.substring(i, i + 5) == "\\hat{" -> {
                    val end = findMatchingBracket(text, i + 4)
                    val content = text.substring(i + 5, end)
                    val parsedContent = parseInlineMath(content)
                    append(applyCombiningAccent(parsedContent, "\u0302"))
                    i = end + 1
                }
                // Superscript: ^2 or ^{abc}
                text[i] == '^' -> {
                    i++
                    if (i < n) {
                        if (text[i] == '{') {
                            val end = findMatchingBracket(text, i)
                            val content = text.substring(i + 1, end)
                            withStyle(style = SpanStyle(
                                baselineShift = BaselineShift.Superscript,
                                fontSize = 11.sp
                            )) {
                                append(parseInlineMath(content))
                            }
                            i = end + 1
                        } else {
                            val content = text[i].toString()
                            withStyle(style = SpanStyle(
                                baselineShift = BaselineShift.Superscript,
                                fontSize = 11.sp
                            )) {
                                append(parseInlineMath(content))
                            }
                            i++
                        }
                    }
                }
                // Subscript: _0 or _{abc}
                text[i] == '_' -> {
                    i++
                    if (i < n) {
                        if (text[i] == '{') {
                            val end = findMatchingBracket(text, i)
                            val content = text.substring(i + 1, end)
                            withStyle(style = SpanStyle(
                                baselineShift = BaselineShift.Subscript,
                                fontSize = 11.sp
                            )) {
                                append(parseInlineMath(content))
                            }
                            i = end + 1
                        } else {
                            val content = text[i].toString()
                            withStyle(style = SpanStyle(
                                baselineShift = BaselineShift.Subscript,
                                fontSize = 11.sp
                            )) {
                                append(parseInlineMath(content))
                            }
                            i++
                        }
                    }
                }
                // LaTeX Commands: \psi, \nabla, etc.
                text[i] == '\\' -> {
                    val end = findCommandEnd(text, i + 1)
                    val command = text.substring(i, end)
                    val unicode = translateCommand(command)
                    if (unicode != null) {
                        val isGreek = command.startsWith("\\") && command.substring(1) in GREEK_LETTERS
                        if (isGreek) {
                            withStyle(style = SpanStyle(fontStyle = FontStyle.Italic)) {
                                append(unicode)
                            }
                        } else {
                            append(unicode)
                        }
                    } else {
                        append(command)
                    }
                    i = end
                }
                // Single letter variables styled as italics
                isVariable(text[i]) -> {
                    val prevChar = if (i > 0) text[i - 1] else ' '
                    val nextChar = if (i + 1 < n) text[i + 1] else ' '
                    val isSingle = !isVariable(prevChar) && !isVariable(nextChar)
                    
                    if (isSingle) {
                        withStyle(style = SpanStyle(fontStyle = FontStyle.Italic)) {
                            append(text[i])
                        }
                    } else {
                        append(text[i])
                    }
                    i++
                }
                else -> {
                    append(text[i])
                    i++
                }
            }
        }
    }
}

private fun findMatchingBracket(text: String, start: Int): Int {
    var count = 0
    for (idx in start until text.length) {
        if (text[idx] == '{') count++
        if (text[idx] == '}') {
            count--
            if (count == 0) return idx
        }
    }
    return text.length - 1
}

private fun findCommandEnd(text: String, start: Int): Int {
    var idx = start
    while (idx < text.length && text[idx].isLetter()) {
        idx++
    }
    return idx
}

private fun applyCombiningAccent(annotatedString: AnnotatedString, accent: String): AnnotatedString {
    return buildAnnotatedString {
        append(annotatedString)
        append(accent)
    }
}

private val GREEK_LETTERS = setOf(
    "alpha", "beta", "gamma", "delta", "epsilon", "zeta", "eta", "theta", "iota", "kappa",
    "lambda", "mu", "nu", "xi", "pi", "rho", "sigma", "tau", "upsilon", "phi", "chi", "psi", "omega",
    "Gamma", "Delta", "Theta", "Lambda", "Xi", "Pi", "Sigma", "Upsilon", "Phi", "Psi", "Omega"
)

private fun translateCommand(command: String): String? {
    if (!command.startsWith("\\")) return null
    val name = command.substring(1)
    return when (name) {
        // Greek lowercase
        "alpha" -> "α"
        "beta" -> "β"
        "gamma" -> "γ"
        "delta" -> "δ"
        "epsilon" -> "ε"
        "zeta" -> "ζ"
        "eta" -> "η"
        "theta" -> "θ"
        "iota" -> "ι"
        "kappa" -> "κ"
        "lambda" -> "λ"
        "mu" -> "μ"
        "nu" -> "ν"
        "xi" -> "ξ"
        "pi" -> "π"
        "rho" -> "ρ"
        "sigma" -> "σ"
        "tau" -> "τ"
        "upsilon" -> "υ"
        "phi" -> "φ"
        "chi" -> "χ"
        "psi" -> "ψ"
        "omega" -> "ω"
        
        // Greek uppercase
        "Gamma" -> "Γ"
        "Delta" -> "Δ"
        "Theta" -> "Θ"
        "Lambda" -> "Λ"
        "Xi" -> "Ξ"
        "Pi" -> "Π"
        "Sigma" -> "Σ"
        "Upsilon" -> "Υ"
        "Phi" -> "Φ"
        "Psi" -> "Ψ"
        "Omega" -> "Ω"
        
        // Symbols
        "hbar" -> "ħ"
        "nabla" -> "∇"
        "partial" -> "∂"
        "infty" -> "∞"
        "times" -> "×"
        "cdot" -> "·"
        "pm" -> "±"
        "mp" -> "∓"
        "neq" -> "≠"
        "approx" -> "≈"
        "leq" -> "≤"
        "geq" -> "≥"
        "in" -> "∈"
        "notin" -> "∉"
        "forall" -> "∀"
        "exists" -> "∃"
        
        else -> null
    }
}


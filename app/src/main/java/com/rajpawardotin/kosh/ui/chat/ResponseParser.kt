package com.rajpawardotin.kosh.ui.chat

sealed interface ChatContentBlock {
    data class Text(val content: String) : ChatContentBlock
    data class Checklist(val items: List<ChecklistItem>) : ChatContentBlock
    data class CodeBlock(val language: String, val code: String) : ChatContentBlock
    data class Sources(val items: List<SourceItem>) : ChatContentBlock
}

data class ChecklistItem(
    val index: Int,
    val text: String,
    val initiallyChecked: Boolean
)

data class SourceItem(
    val title: String,
    val url: String
)

object ResponseParser {
    private val checklistRegex = """^(?:[-*+]\s*\[\s*([ xX]?)\s*\])\s+(.+)""".toRegex()
    private val markdownLinkRegex = """\[([^\]]+)\]\((https?://[^\s)]+)\)""".toRegex()
    private val plainUrlRegex = """(?<!\]\()https?://[^\s)]+""".toRegex()

    fun parse(text: String): List<ChatContentBlock> {
        val blocks = mutableListOf<ChatContentBlock>()
        val lines = text.lines()
        
        var inCodeBlock = false
        var currentLanguage = ""
        val codeBuffer = StringBuilder()
        
        val currentTextBuffer = StringBuilder()
        val currentChecklistItems = mutableListOf<ChecklistItem>()
        
        fun flushText() {
            if (currentTextBuffer.isNotEmpty()) {
                val content = currentTextBuffer.toString().trim()
                if (content.isNotEmpty()) {
                    blocks.add(ChatContentBlock.Text(content))
                }
                currentTextBuffer.clear()
            }
        }
        
        fun flushChecklist() {
            if (currentChecklistItems.isNotEmpty()) {
                blocks.add(ChatContentBlock.Checklist(currentChecklistItems.toList()))
                currentChecklistItems.clear()
            }
        }

        var globalItemIndex = 0
        
        for (line in lines) {
            val trimmedLine = line.trim()
            
            // Check code block transition
            if (trimmedLine.startsWith("```")) {
                if (inCodeBlock) {
                    // Ending code block
                    blocks.add(ChatContentBlock.CodeBlock(currentLanguage, codeBuffer.toString().trimEnd()))
                    codeBuffer.clear()
                    currentLanguage = ""
                    inCodeBlock = false
                } else {
                    // Flush existing buffers
                    flushText()
                    flushChecklist()
                    // Starting code block
                    currentLanguage = trimmedLine.substring(3).trim()
                    inCodeBlock = true
                }
                continue
            }
            
            if (inCodeBlock) {
                codeBuffer.append(line).append("\n")
                continue
            }
            
            // Try parsing list item
            val checklistMatch = checklistRegex.matchEntire(trimmedLine)
            if (checklistMatch != null) {
                // Flush text buffer if we transitioned to a checklist
                flushText()
                
                val checkedChar = checklistMatch.groupValues[1]
                var itemText = checklistMatch.groupValues[2]
                val isChecked = checkedChar.lowercase() == "x"
                
                if (itemText.startsWith("~~") && itemText.endsWith("~~")) {
                    itemText = itemText.substring(2, itemText.length - 2).trim()
                }
                
                currentChecklistItems.add(
                    ChecklistItem(
                        index = globalItemIndex++,
                        text = itemText,
                        initiallyChecked = isChecked
                    )
                )
            } else {
                // Flush checklist if we transitioned to normal text
                flushChecklist()
                currentTextBuffer.append(line).append("\n")
            }
        }
        
        // Final flushes
        if (inCodeBlock) {
            blocks.add(ChatContentBlock.CodeBlock(currentLanguage, codeBuffer.toString().trimEnd()))
        } else {
            flushText()
            flushChecklist()
        }
        
        // Parse sources from the entire text
        val sources = mutableListOf<SourceItem>()
        
        // Find markdown links
        markdownLinkRegex.findAll(text).forEach { match ->
            val title = match.groupValues[1]
            val url = match.groupValues[2]
            if (sources.none { it.url == url }) {
                sources.add(SourceItem(title, url))
            }
        }
        
        // Find plain URLs
        plainUrlRegex.findAll(text).forEach { match ->
            val url = match.value
            if (sources.none { it.url == url }) {
                // Try to infer a nice title from domain
                val title = try {
                    val domain = java.net.URI(url).host ?: ""
                    domain.replace("www.", "")
                } catch (e: Exception) {
                    "Web Link"
                }
                sources.add(SourceItem(title, url))
            }
        }
        
        if (sources.isNotEmpty()) {
            blocks.add(ChatContentBlock.Sources(sources))
        }
        
        return blocks
    }
}


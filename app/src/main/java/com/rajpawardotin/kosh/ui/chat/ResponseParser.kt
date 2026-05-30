package com.rajpawardotin.kosh.ui.chat

sealed interface ChatContentBlock {
    data class Text(val content: String) : ChatContentBlock
    data class Checklist(val items: List<ChecklistItem>) : ChatContentBlock
    data class CodeBlock(val language: String, val code: String) : ChatContentBlock
    data class Sources(val items: List<SourceItem>) : ChatContentBlock
    data class MathBlock(val formula: String) : ChatContentBlock
}

data class ChecklistItem(
    val index: Int,
    val text: String,
    val initiallyChecked: Boolean
)

data class SourceItem(
    val title: String,
    val url: String,
    val imageUrl: String? = null,
    val videoUrl: String? = null
)

object ReferenceParser {
    fun parseReferences(sourceDocuments: String?): Pair<List<String>, List<SourceItem>> {
        if (sourceDocuments.isNullOrBlank()) return Pair(emptyList(), emptyList())
        
        try {
            val docs = mutableListOf<String>()
            val web = mutableListOf<SourceItem>()
            
            val docsMatch = "\"docs\"\\s*:\\s*\\[([^\\]]*)\\]".toRegex().find(sourceDocuments)
            if (docsMatch != null) {
                val docsContent = docsMatch.groupValues[1]
                val docItems = docsContent.split(",")
                    .map { it.trim().trim('"').replace("\\\"", "\"").replace("\\\\", "\\") }
                    .filter { it.isNotEmpty() }
                docs.addAll(docItems)
            }
            
            val webMatch = "\"web\"\\s*:\\s*\\[([^\\]]*)\\]".toRegex().find(sourceDocuments)
            if (webMatch != null) {
                val webContent = webMatch.groupValues[1]
                val objRegex = "\\{([^\\}]*)\\}".toRegex()
                objRegex.findAll(webContent).forEach { objMatch ->
                    val objFields = objMatch.groupValues[1]
                    val title = extractJsonField(objFields, "title") ?: "Web Page"
                    val url = extractJsonField(objFields, "url") ?: ""
                    val imageUrl = extractJsonField(objFields, "imageUrl")?.takeIf { it.isNotEmpty() }
                    val videoUrl = extractJsonField(objFields, "videoUrl")?.takeIf { it.isNotEmpty() }
                    web.add(SourceItem(title, url, imageUrl, videoUrl))
                }
            }
            
            if (docsMatch != null || webMatch != null) {
                return Pair(docs, web)
            }
        } catch (e: Exception) {
            // Ignore and fallback
        }
        
        val docs = sourceDocuments.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        return Pair(docs, emptyList())
    }

    private fun extractJsonField(jsonFields: String, key: String): String? {
        val fieldMatch = "\"$key\"\\s*:\\s*\"([^\"]*)\"".toRegex().find(jsonFields)
        return fieldMatch?.groupValues[1]?.replace("\\\"", "\"")?.replace("\\\\", "\\")
    }
}

object ResponseParser {
    private val checklistRegex = """^(?:[-*+]\s*\[\s*([ xX]?)\s*\])\s+(.+)""".toRegex()
    private val markdownLinkRegex = """\[([^\]]+)\]\((https?://[^\s)]+)\)""".toRegex()
    private val plainUrlRegex = """(?<!\]\()https?://[^\s)]+""".toRegex()

    fun parse(text: String): List<ChatContentBlock> {
        val blocks = mutableListOf<ChatContentBlock>()
        val lines = text.lines()
        
        var inCodeBlock = false
        var inMathBlock = false
        var currentLanguage = ""
        val codeBuffer = StringBuilder()
        val mathBuffer = StringBuilder()
        
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
                    blocks.add(ChatContentBlock.CodeBlock(currentLanguage, codeBuffer.toString().trimEnd()))
                    codeBuffer.clear()
                    currentLanguage = ""
                    inCodeBlock = false
                } else {
                    flushText()
                    flushChecklist()
                    currentLanguage = trimmedLine.substring(3).trim()
                    inCodeBlock = true
                }
                continue
            }
            
            if (inCodeBlock) {
                codeBuffer.append(line).append("\n")
                continue
            }

            // Check block math transition
            if (inMathBlock) {
                if (trimmedLine.endsWith("$$")) {
                    mathBuffer.append(line.substringBeforeLast("$$"))
                    blocks.add(ChatContentBlock.MathBlock(mathBuffer.toString().trim()))
                    mathBuffer.clear()
                    inMathBlock = false
                } else if (trimmedLine.endsWith("\\]")) {
                    mathBuffer.append(line.substringBeforeLast("\\]"))
                    blocks.add(ChatContentBlock.MathBlock(mathBuffer.toString().trim()))
                    mathBuffer.clear()
                    inMathBlock = false
                } else if (trimmedLine.contains("$$")) {
                    mathBuffer.append(line.substringBefore("$$"))
                    blocks.add(ChatContentBlock.MathBlock(mathBuffer.toString().trim()))
                    mathBuffer.clear()
                    inMathBlock = false
                    val remaining = line.substringAfter("$$")
                    if (remaining.isNotBlank()) currentTextBuffer.append(remaining).append("\n")
                } else if (trimmedLine.contains("\\]")) {
                    mathBuffer.append(line.substringBefore("\\]"))
                    blocks.add(ChatContentBlock.MathBlock(mathBuffer.toString().trim()))
                    mathBuffer.clear()
                    inMathBlock = false
                    val remaining = line.substringAfter("\\]")
                    if (remaining.isNotBlank()) currentTextBuffer.append(remaining).append("\n")
                } else {
                    mathBuffer.append(line).append("\n")
                }
                continue
            } else {
                if (trimmedLine.startsWith("$$") && trimmedLine.endsWith("$$") && trimmedLine.length > 4) {
                    flushText()
                    flushChecklist()
                    val formula = trimmedLine.removePrefix("$$").removeSuffix("$$").trim()
                    blocks.add(ChatContentBlock.MathBlock(formula))
                    continue
                } else if (trimmedLine.startsWith("\\[") && trimmedLine.endsWith("\\]") && trimmedLine.length > 4) {
                    flushText()
                    flushChecklist()
                    val formula = trimmedLine.removePrefix("\\[").removeSuffix("\\]").trim()
                    blocks.add(ChatContentBlock.MathBlock(formula))
                    continue
                } else if (trimmedLine.startsWith("$$") || trimmedLine.startsWith("\\[")) {
                    flushText()
                    flushChecklist()
                    inMathBlock = true
                    val content = if (trimmedLine.startsWith("$$")) trimmedLine.removePrefix("$$") else trimmedLine.removePrefix("\\[")
                    mathBuffer.append(content).append("\n")
                    continue
                }
            }
            
            // Try parsing list item
            val checklistMatch = checklistRegex.matchEntire(trimmedLine)
            if (checklistMatch != null) {
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
                flushChecklist()
                currentTextBuffer.append(line).append("\n")
            }
        }
        
        // Final flushes
        if (inCodeBlock) {
            blocks.add(ChatContentBlock.CodeBlock(currentLanguage, codeBuffer.toString().trimEnd()))
        } else if (inMathBlock) {
            blocks.add(ChatContentBlock.MathBlock(mathBuffer.toString().trim()))
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

    fun parseToolCalls(text: String): List<ToolCall> {
        val toolCalls = mutableListOf<ToolCall>()
        
        // 1. Try finding JSON code fences
        val jsonCodeFenceRegex = """```json\s*(\{[\s\S]*?\}|\[[\s\S]*?\])\s*```""".toRegex()
        jsonCodeFenceRegex.findAll(text).forEach { match ->
            val jsonStr = match.groupValues[1].trim()
            if (jsonStr.startsWith("[")) {
                try {
                    val arr = org.json.JSONArray(jsonStr)
                    for (i in 0 until arr.length()) {
                        val obj = arr.optJSONObject(i)
                        if (obj != null) {
                            parseSingleJsonToolCall(obj.toString())?.let { toolCalls.add(it) }
                        }
                    }
                } catch (e: Exception) {}
            } else {
                parseSingleJsonToolCall(jsonStr)?.let { toolCalls.add(it) }
            }
        }

        if (toolCalls.isNotEmpty()) return toolCalls

        // 2. Try matching any curly brackets containing "tool" or "name"
        val genericJsonRegex = """(\{\s*"(?:tool|name)"\s*:\s*"[^"]*"\s*,[\s\S]*?\})""".toRegex()
        genericJsonRegex.findAll(text).forEach { match ->
            val jsonStr = match.groupValues[1].trim()
            parseSingleJsonToolCall(jsonStr)?.let { toolCalls.add(it) }
        }

        if (toolCalls.isNotEmpty()) return toolCalls

        // 3. Fallback: Parse the entire text block if it looks like pure JSON
        val trimmed = text.trim()
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            parseSingleJsonToolCall(trimmed)?.let { toolCalls.add(it) }
        }

        return toolCalls
    }

    private fun parseSingleJsonToolCall(jsonStr: String): ToolCall? {
        return try {
            val obj = org.json.JSONObject(jsonStr)
            val name = obj.optString("tool", "").takeIf { it.isNotEmpty() }
                ?: obj.optString("name", "").takeIf { it.isNotEmpty() }
                ?: return null
            
            val argsMap = mutableMapOf<String, Any>()
            val argsObj = obj.optJSONObject("arguments") ?: obj.optJSONObject("params")
            if (argsObj != null) {
                val keys = argsObj.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val value = argsObj.get(key)
                    if (value != org.json.JSONObject.NULL) {
                        argsMap[key] = value
                    }
                }
            }
            ToolCall(name, argsMap)
        } catch (e: Exception) {
            null
        }
    }
}

data class ToolCall(
    val name: String,
    val arguments: Map<String, Any>
)


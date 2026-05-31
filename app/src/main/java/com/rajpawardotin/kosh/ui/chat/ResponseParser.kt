package com.rajpawardotin.kosh.ui.chat

sealed interface ChatContentBlock {
    data class Text(val content: String) : ChatContentBlock
    data class Checklist(val items: List<ChecklistItem>) : ChatContentBlock
    data class CodeBlock(val language: String, val code: String) : ChatContentBlock
    data class Sources(val items: List<SourceItem>) : ChatContentBlock
    data class MathBlock(val formula: String) : ChatContentBlock
    data class Thinking(val content: String) : ChatContentBlock
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

    data class StreamState(
        val isThinking: Boolean,
        val thinkingContent: String,
        val cleanResponse: String
    )

    fun parseStreamState(text: String): StreamState {
        val state = parseStreamStateInternal(text)
        return StreamState(
            isThinking = state.isThinking,
            thinkingContent = state.thinkingContent.trim(),
            cleanResponse = state.cleanResponse.trim()
        )
    }

    private fun parseStreamStateInternal(text: String): StreamState {
        val xmlStart = text.indexOf("<thinking>")
        val fenceStart = text.indexOf("```thinking")
        
        if (xmlStart == -1 && fenceStart == -1) {
            return StreamState(isThinking = false, thinkingContent = "", cleanResponse = text)
        }
        
        if (xmlStart != -1 && (fenceStart == -1 || xmlStart < fenceStart)) {
            val xmlEnd = text.indexOf("</thinking>", xmlStart + 10)
            val prefix = text.substring(0, xmlStart).trim()
            if (xmlEnd != -1) {
                val thinking = text.substring(xmlStart + 10, xmlEnd).trim()
                val remaining = text.substring(xmlEnd + 11)
                val subState = parseStreamStateInternal(remaining)
                val cleanPrefix = stripTrailingListMarker(prefix)
                return StreamState(
                    isThinking = subState.isThinking,
                    thinkingContent = if (thinking.isNotEmpty()) thinking + (if (subState.thinkingContent.isNotEmpty()) "\n" + subState.thinkingContent else "") else subState.thinkingContent,
                    cleanResponse = cleanPrefix + (if (cleanPrefix.isNotEmpty() && subState.cleanResponse.isNotEmpty()) "\n" else "") + subState.cleanResponse
                )
            } else {
                val thinking = text.substring(xmlStart + 10).trim()
                val cleanPrefix = stripTrailingListMarker(prefix)
                return StreamState(
                    isThinking = true,
                    thinkingContent = thinking,
                    cleanResponse = cleanPrefix
                )
            }
        } else {
            val fenceEnd = text.indexOf("```", fenceStart + 11)
            val prefix = text.substring(0, fenceStart).trim()
            if (fenceEnd != -1) {
                val thinking = text.substring(fenceStart + 11, fenceEnd).trim()
                val remaining = text.substring(fenceEnd + 3)
                val subState = parseStreamStateInternal(remaining)
                val cleanPrefix = stripTrailingListMarker(prefix)
                return StreamState(
                    isThinking = subState.isThinking,
                    thinkingContent = if (thinking.isNotEmpty()) thinking + (if (subState.thinkingContent.isNotEmpty()) "\n" + subState.thinkingContent else "") else subState.thinkingContent,
                    cleanResponse = cleanPrefix + (if (cleanPrefix.isNotEmpty() && subState.cleanResponse.isNotEmpty()) "\n" else "") + subState.cleanResponse
                )
            } else {
                val thinking = text.substring(fenceStart + 11).trim()
                val cleanPrefix = stripTrailingListMarker(prefix)
                return StreamState(
                    isThinking = true,
                    thinkingContent = thinking,
                    cleanResponse = cleanPrefix
                )
            }
        }
    }

    private fun stripTrailingListMarker(text: String): String {
        return text.replace("""(?:\n|^)\s*[-*+•]\s*$""".toRegex(), "")
                   .replace("""(?:\n|^)\s*\d+\.\s*$""".toRegex(), "")
                   .trim()
    }

    fun extractThinkingSegments(text: String): Pair<List<String>, String> {
        val xmlMatches = """(?s)(?:^|\n)?(?:\s*[-*+•]\s*|\s*\d+\.\s*)?<thinking>(.*?)(?:</thinking>|$)""".toRegex().findAll(text)
        val fenceMatches = """(?s)(?:^|\n)?(?:\s*[-*+•]\s*|\s*\d+\.\s*)?```thinking\s*\n(.*?)(?:\n```|$)""".toRegex().findAll(text)
        
        val allMatches = (xmlMatches + fenceMatches).sortedBy { it.range.first }.toList()
        
        if (allMatches.isEmpty()) {
            return Pair(emptyList(), text)
        }
        
        val thinkingContents = mutableListOf<String>()
        val cleanTextBuilder = java.lang.StringBuilder()
        var lastIndex = 0
        
        for (match in allMatches) {
            if (match.range.first < lastIndex) continue
            if (match.range.first > lastIndex) {
                cleanTextBuilder.append(text.substring(lastIndex, match.range.first))
            }
            thinkingContents.add(match.groupValues[1].trim())
            lastIndex = match.range.last + 1
        }
        
        if (lastIndex < text.length) {
            cleanTextBuilder.append(text.substring(lastIndex))
        }
        
        return Pair(thinkingContents, cleanTextBuilder.toString().trim())
    }

    fun parse(text: String): List<ChatContentBlock> {
        val xmlMatches = """(?s)(?:^|\n)?(?:\s*[-*+•]\s*|\s*\d+\.\s*)?<thinking>(.*?)(?:</thinking>|$)""".toRegex().findAll(text)
        val fenceMatches = """(?s)(?:^|\n)?(?:\s*[-*+•]\s*|\s*\d+\.\s*)?```thinking\s*\n(.*?)(?:\n```|$)""".toRegex().findAll(text)
        
        val allMatches = (xmlMatches + fenceMatches).sortedBy { it.range.first }.toList()
        val blocks = mutableListOf<ChatContentBlock>()
        
        if (allMatches.isEmpty()) {
            val cleanBlocks = parseCleanText(text)
            blocks.addAll(cleanBlocks)
        } else {
            var lastIndex = 0
            for (match in allMatches) {
                if (match.range.first < lastIndex) continue
                
                // Parse the clean text before this thinking block
                if (match.range.first > lastIndex) {
                    val subText = text.substring(lastIndex, match.range.first)
                    blocks.addAll(parseCleanText(subText))
                }
                
                val thinking = match.groupValues[1].trim()
                if (thinking.isNotEmpty()) {
                    blocks.add(ChatContentBlock.Thinking(thinking))
                }
                lastIndex = match.range.last + 1
            }
            
            if (lastIndex < text.length) {
                val remainingText = text.substring(lastIndex)
                blocks.addAll(parseCleanText(remainingText))
            }
        }
        
        // Parse sources from the entire text
        val sources = parseSources(text)
        if (sources.isNotEmpty()) {
            blocks.add(ChatContentBlock.Sources(sources))
        }
        
        return blocks
    }

    private fun parseSources(text: String): List<SourceItem> {
        val sources = mutableListOf<SourceItem>()
        markdownLinkRegex.findAll(text).forEach { match ->
            val title = match.groupValues[1]
            val url = match.groupValues[2]
            if (sources.none { it.url == url }) {
                sources.add(SourceItem(title, url))
            }
        }
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
        return sources
    }

    private fun parseCleanText(text: String): List<ChatContentBlock> {
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


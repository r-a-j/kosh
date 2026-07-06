package com.rajpawardotin.kosh.ui.chat

import androidx.compose.runtime.Immutable

@Immutable
sealed interface ChatContentBlock {
    @Immutable
    data class Text(val content: String) : ChatContentBlock
    @Immutable
    data class Checklist(val items: List<ChecklistItem>) : ChatContentBlock
    @Immutable
    data class CodeBlock(val language: String, val code: String) : ChatContentBlock
    @Immutable
    data class Sources(val items: List<SourceItem>) : ChatContentBlock
    @Immutable
    data class MathBlock(val formula: String) : ChatContentBlock
    @Immutable
    data class Thinking(val content: String) : ChatContentBlock
}

@Immutable
data class ChecklistItem(
    val index: Int,
    val text: String,
    val initiallyChecked: Boolean
)

@Immutable
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
        if (isInitialTransitionState(text)) {
            return StreamState(
                isThinking = true,
                thinkingContent = "",
                cleanResponse = ""
            )
        }
        val state = parseStreamStateInternal(text)
        return StreamState(
            isThinking = state.isThinking,
            thinkingContent = state.thinkingContent.trim(),
            cleanResponse = sanitizeThinkingTags(state.cleanResponse).trim()
        )
    }

    private fun isPartialPrefix(text: String, target: String): Boolean {
        val trimmed = text.trimStart()
        if (trimmed.isEmpty()) return false
        return target.startsWith(trimmed) && trimmed.length < target.length
    }

    private fun isInitialTransitionState(text: String): Boolean {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return true
        
        val listMarkerRegex = """^\s*[-*+•]\s*$""".toRegex()
        val numberedMarkerRegex = """^\s*\d+\.?\s*$""".toRegex()
        if (listMarkerRegex.matches(trimmed) || numberedMarkerRegex.matches(trimmed)) {
            return true
        }
        
        val cleanAfterMarker = stripInitialListMarker(trimmed)
        if (cleanAfterMarker.isEmpty()) return true
        
        if (isPartialPrefix(cleanAfterMarker, "<thinking>") || isPartialPrefix(cleanAfterMarker, "```thinking")) {
            return true
        }
        
        return false
    }

    private fun stripInitialListMarker(text: String): String {
        return text.replaceFirst("""^\s*[-*+•]\s*""".toRegex(), "")
                   .replaceFirst("""^\s*\d+\.\s*""".toRegex(), "")
                   .trim()
    }

    fun cleanUpEmptyListMarkers(text: String): String {
        return text.lines()
            .filter { line ->
                val trimmed = line.trim()
                !trimmed.matches("""^[-*+•]$""".toRegex()) &&
                !trimmed.matches("""^[-*+•]\s+$""".toRegex()) &&
                !trimmed.matches("""^\d+\.$""".toRegex()) &&
                !trimmed.matches("""^\d+\.\s+$""".toRegex())
            }
            .joinToString("\n")
    }

    private fun parseStreamStateInternal(text: String): StreamState {
        val xmlStart = text.indexOf("<thinking>")
        val fenceStart = text.indexOf("```thinking")
        
        if (xmlStart == -1 && fenceStart == -1) {
            val startsWithXmlPrefix = isPartialPrefix(text, "<thinking>")
            val startsWithFencePrefix = isPartialPrefix(text, "```thinking")
            if (startsWithXmlPrefix || startsWithFencePrefix) {
                return StreamState(isThinking = true, thinkingContent = "", cleanResponse = "")
            }
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

    fun sanitizeThinkingTags(text: String): String {
        return text
            .replace("""\s*<thinking>\s*""".toRegex(), "\n")
            .replace("""\s*</thinking>\s*""".toRegex(), "\n")
            .replace("""\s*```thinking\s*""".toRegex(), "\n")
            .trim()
    }

    fun extractThinkingSegments(text: String): Pair<List<String>, String> {
        val xmlMatches = """(?s)(?:^|\n)?(?:\s*[-*+•]\s*|\s*\d+\.\s*)?<thinking>(.*?)(?:</thinking>|$)""".toRegex().findAll(text)
        val fenceMatches = """(?s)(?:^|\n)?(?:\s*[-*+•]\s*|\s*\d+\.\s*)?```thinking\s*\n(.*?)(?:\n```|$)""".toRegex().findAll(text)
        
        val allMatches = (xmlMatches + fenceMatches).sortedBy { it.range.first }.toList()
        
        if (allMatches.isEmpty()) {
            return Pair(emptyList(), sanitizeThinkingTags(cleanUpEmptyListMarkers(text)).trim())
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
        
        val cleanText = cleanTextBuilder.toString().trim()
        val finalCleanText = sanitizeThinkingTags(cleanUpEmptyListMarkers(cleanText)).trim()
        if (finalCleanText.isEmpty() && thinkingContents.isNotEmpty()) {
            return Pair(thinkingContents, thinkingContents.joinToString("\n\n"))
        }
        return Pair(thinkingContents, finalCleanText)
    }

    fun parse(text: String): List<ChatContentBlock> {
        val xmlMatches = """(?s)(?:^|\n)?(?:\s*[-*+•]\s*|\s*\d+\.\s*)?<thinking>(.*?)(?:</thinking>|$)""".toRegex().findAll(text)
        val fenceMatches = """(?s)(?:^|\n)?(?:\s*[-*+•]\s*|\s*\d+\.\s*)?```thinking\s*\n(.*?)(?:\n```|$)""".toRegex().findAll(text)
        
        val allMatches = (xmlMatches + fenceMatches).sortedBy { it.range.first }.toList()
        val blocks = mutableListOf<ChatContentBlock>()
        
        if (allMatches.isEmpty()) {
            val cleanBlocks = parseCleanText(sanitizeThinkingTags(cleanUpEmptyListMarkers(text)))
            blocks.addAll(cleanBlocks)
        } else {
            var lastIndex = 0
            for (match in allMatches) {
                if (match.range.first < lastIndex) continue
                
                // Parse the clean text before this thinking block
                if (match.range.first > lastIndex) {
                    val subText = text.substring(lastIndex, match.range.first)
                    blocks.addAll(parseCleanText(sanitizeThinkingTags(cleanUpEmptyListMarkers(subText))))
                }
                
                val thinking = match.groupValues[1].trim()
                if (thinking.isNotEmpty()) {
                    blocks.add(ChatContentBlock.Thinking(thinking))
                }
                lastIndex = match.range.last + 1
            }
            
            if (lastIndex < text.length) {
                val remainingText = text.substring(lastIndex)
                blocks.addAll(parseCleanText(sanitizeThinkingTags(cleanUpEmptyListMarkers(remainingText))))
            }
        }
        
        // Fallback: If no actual answer text block was produced but we have thinking blocks,
        // treat the thinking contents as the text answer as well so the user gets the analysis directly.
        val hasMainContent = blocks.any { 
            it is ChatContentBlock.Text || 
            it is ChatContentBlock.CodeBlock || 
            it is ChatContentBlock.Checklist || 
            it is ChatContentBlock.MathBlock 
        }
        if (!hasMainContent) {
            val thinkingBlocks = blocks.filterIsInstance<ChatContentBlock.Thinking>()
            if (thinkingBlocks.isNotEmpty()) {
                val combinedThinking = thinkingBlocks.joinToString("\n\n") { it.content }
                if (combinedThinking.isNotEmpty()) {
                    blocks.add(ChatContentBlock.Text(combinedThinking))
                }
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
                if (line.isBlank()) {
                    flushText()
                } else {
                    currentTextBuffer.append(line).append("\n")
                }
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


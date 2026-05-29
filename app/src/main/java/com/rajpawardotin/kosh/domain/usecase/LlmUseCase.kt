package com.rajpawardotin.kosh.domain.usecase

import com.rajpawardotin.kosh.domain.model.ChatMessage
import com.rajpawardotin.kosh.domain.model.SessionDocument
import com.rajpawardotin.kosh.domain.provider.AIProvider
import com.rajpawardotin.kosh.domain.provider.SearchProvider
import com.rajpawardotin.kosh.domain.repository.SessionRepository
import com.rajpawardotin.kosh.domain.repository.DocumentRepository

class LlmUseCase(
    private val aiProvider: AIProvider,
    private val searchProvider: SearchProvider,
    private val sessionRepository: SessionRepository,
    private val documentRepository: DocumentRepository
) {

    companion object {
        val STOP_WORDS = setOf(
            "a", "an", "and", "are", "as", "at", "be", "but", "by", "for", "if", "in", "into", "is", "it", "no", "not", "of", "on", "or", "such", "that", "the", "their", "then", "there", "these", "they", "this", "to", "was", "will", "with", "what", "how", "why", "who", "when", "where", 
            "summarize", "attached", "document", "documents", "tell", "me", "about", "please", "can", "you", "explain",
            "see", "reference", "doc", "docs", "file", "files", "pdf", "pdfs", "txt", "md", "attachment", "attachments",
            "earlier", "earliest", "previous", "previously", "above", "below", "read", "view", "check", "open", "here", "there",
            "them", "yesterday", "message", "chat", "conversation", "show", "get", "give", "display", "find", "search", "lookup", "look"
        )
    }

    fun detectSearchRequirement(prompt: String, isInternetEnabled: Boolean): Boolean {
        if (!isInternetEnabled) return false
        
        val lowercasePrompt = prompt.lowercase()
        val searchKeywords = listOf(
            "search web", "search internet", "search online", "google", "bing", "duckduckgo",
            "lookup", "look up", "find on web", "browse", "website", "realtime", "real-time",
            "latest news", "today's headlines", "todays headlines", "weather today", "current price",
            "weather", "forecast", "ticker", "stock price", "price of", "value of", "market cap",
            "vs", "versus", "score", "who won", "match result", "latest", "recent", "currently",
            "today", "yesterday", "tomorrow", "tonight", "this week", "now", "news about",
            "what happened", "current status", "flight status", "release date"
        )
        val hasUrl = prompt.contains("http://") || prompt.contains("https://") || prompt.contains("www.")
        
        return hasUrl || searchKeywords.any { lowercasePrompt.contains(it) }
    }

    fun determineSearchQuery(rawPrompt: String, chatMessages: List<ChatMessage>, lastSearchQuery: String?): String {
        val metaKeywords = listOf("search", "find", "look", "research", "tell me more")
        val isMetaQuery = rawPrompt.lowercase().trim().split(" ").size <= 4 &&
                metaKeywords.any { rawPrompt.contains(it, ignoreCase = true) }

        val baseQuery = if (isMetaQuery) {
            chatMessages.reversed().firstOrNull { it.isUser && it.text != rawPrompt }?.text ?: rawPrompt
        } else {
            rawPrompt
        }

        val lastQuery = lastSearchQuery
        if (!lastQuery.isNullOrBlank()) {
            val lowercasePrompt = baseQuery.lowercase()
            val contextIndicators = listOf(
                "he", "him", "his", "she", "her", "it", "its", "they", "them", "their",
                "this", "that", "these", "those", "who", "why", "what", "where", "how",
                "experiences", "experience", "education", "works", "work", "job", "career",
                "background", "projects", "project", "details", "info", "information",
                "website", "site", "page", "author", "creator", "owner"
            )
            val isFollowUp = contextIndicators.any { word ->
                "\\b$word\\b".toRegex().containsMatchIn(lowercasePrompt)
            }
            if (isFollowUp) {
                var cleanPrompt = baseQuery
                val prefixesToRemove = listOf(
                    "what are", "what is", "who is", "tell me about", "give me", "show me",
                    "what does", "how does", "where is", "can you", "please", "how is",
                    "what was", "who was", "where was", "how was", "do you know", "tell me"
                )
                for (prefix in prefixesToRemove) {
                    cleanPrompt = cleanPrompt.replace("(?i)^\\s*$prefix\\s+".toRegex(), "").trim()
                }
                return "$lastQuery $cleanPrompt".trim()
            }
        }
        return baseQuery
    }

    fun buildSystemPrompt(query: String, data: String, userPrompt: String): String {
        return "You have access to the web search results below to answer the user's query.\n\n" +
                "SEARCH RESULTS:\n" +
                "$data\n\n" +
                "USER QUERY: $userPrompt\n\n" +
                "INSTRUCTION: Answer the user's query using the SEARCH RESULTS above. " +
                "Provide a direct, concise, and helpful answer. " +
                "Do not state that you cannot browse the internet or access real-time information since the search results are already provided to you above."
    }

    fun resolveQueryContext(rawPrompt: String, chatMessages: List<ChatMessage>): String {
        val trimmed = rawPrompt.trim().lowercase()
        val numberRegex = Regex("^(?:option|choice|select|number|#)?\\s*(\\d+)\\.?$")
        val match = numberRegex.matchEntire(trimmed)
        if (match != null) {
            val digit = match.groupValues[1]
            val lastAssistantMsg = chatMessages.reversed().firstOrNull { !it.isUser }?.text
            if (lastAssistantMsg != null) {
                val lines = lastAssistantMsg.lines()
                val linePrefixes = listOf("$digit.", "$digit)", "[$digit]", "$digit -", "$digit:")
                for (line in lines) {
                    val trimmedLine = line.trim()
                    val matchedPrefix = linePrefixes.find { trimmedLine.startsWith(it) }
                    if (matchedPrefix != null) {
                        val optionText = trimmedLine.substring(matchedPrefix.length).trim()
                        if (optionText.isNotEmpty()) {
                            return optionText
                        }
                    }
                }
            }
        }
        return rawPrompt
    }

    fun retrieveContext(
        sessionId: String, 
        query: String, 
        isEncrypted: Boolean, 
        activeSessionDocuments: List<SessionDocument>,
        justAttached: Boolean = false
    ): Pair<String, List<String>> {
        val relevantDocs = if (isEncrypted) {
            val sanitizedQuery = query.trim().lowercase().replace(Regex("[^a-z0-9\\s]"), "")
            val terms = sanitizedQuery.split(Regex("\\s+")).filter { it.isNotBlank() && !STOP_WORDS.contains(it) }
            
            if (terms.isEmpty() || justAttached) {
                activeSessionDocuments.takeLast(3).reversed()
            } else {
                val matches = activeSessionDocuments
                    .map { doc ->
                        val text = doc.chunkText.lowercase()
                        var score = 0
                        for (term in terms) {
                            var index = 0
                            while (true) {
                                index = text.indexOf(term, index)
                                if (index == -1) break
                                score++
                                index += term.length
                            }
                        }
                        doc to score
                    }
                    .filter { it.second > 0 }
                    .sortedByDescending { it.second }
                    .map { it.first }
                    .take(3)
                
                if (matches.isEmpty()) {
                    activeSessionDocuments.takeLast(3).reversed()
                } else {
                    matches
                }
            }
        } else {
            val sanitizedQuery = query.trim().lowercase().replace(Regex("[^a-z0-9\\s]"), "")
            val terms = sanitizedQuery.split("\\s+".toRegex()).filter { it.isNotBlank() && !STOP_WORDS.contains(it) }

            if (terms.isEmpty() || justAttached) {
                activeSessionDocuments.takeLast(3).reversed()
            } else {
                val docs = documentRepository.searchSessionDocumentsFTS(sessionId, query)
                if (docs.isEmpty()) {
                    activeSessionDocuments.takeLast(3).reversed()
                } else {
                    docs.take(3)
                }
            }
        }

        if (relevantDocs.isEmpty()) return Pair("", emptyList())

        val sourceNames = mutableSetOf<String>()
        val sb = StringBuilder()
        
        for (doc in relevantDocs) {
            sourceNames.add(doc.fileName)
        }

        sb.append("### ACTIVE CONVERSATION DOCUMENTS\n")
        sb.append("The user has attached the following files to this session: ")
        sb.append(sourceNames.joinToString(", ")).append("\n\n")
        sb.append("Excerpts/Context from the attached documents:\n")
        sb.append("--- START EXCERPTS ---\n")
        for (doc in relevantDocs) {
            sb.append("File: ").append(doc.fileName).append(" (Chunk ").append(doc.chunkIndex + 1).append("):\n")
            sb.append(doc.chunkText.trim()).append("\n\n")
        }
        sb.append("--- END EXCERPTS ---")

        return Pair(sb.toString(), sourceNames.toList())
    }

    fun compileFinalPrompt(
        chatMessages: List<ChatMessage>,
        rawPrompt: String,
        documentContext: String,
        searchResults: String?,
        searchQuery: String?,
        maxContextChars: Int = 8000 // approx 2048 tokens
    ): String {
        val budgetForHistory = maxContextChars - rawPrompt.length - documentContext.length - (searchResults?.length ?: 0) - 1500
        
        val selectedHistory = mutableListOf<String>()
        var currentUsedChars = 0

        if (budgetForHistory > 0) {
            val historyMessages = chatMessages.dropLast(1)
            for (msg in historyMessages.reversed()) {
                val role = if (msg.isUser) "User" else "Assistant"
                val cleanText = msg.text.trim()
                if (cleanText.startsWith("Error:") || cleanText.isEmpty()) continue
                
                val formattedMsg = "- $role: $cleanText\n"
                if (currentUsedChars + formattedMsg.length > budgetForHistory) {
                    break
                }
                selectedHistory.add(0, formattedMsg)
                currentUsedChars += formattedMsg.length
            }
        }

        val promptSb = StringBuilder()
        
        // 1. System Prompt / Instructions
        promptSb.append("### SYSTEM INSTRUCTIONS\n")
        promptSb.append("You are Kosh, a private, secure offline personal assistant running on the user's device. ")
        promptSb.append("You help the user brainstorm, study, and analyze information.\n")
        
        if (documentContext.isNotEmpty()) {
            promptSb.append("- The user has attached documents/files to this conversation. ")
            promptSb.append("Use the provided excerpts under 'ACTIVE CONVERSATION DOCUMENTS' to answer the user's query. ")
            promptSb.append("If the user refers to 'the document', 'the PDF', 'the file', or 'reference doc attached earlier', ")
            promptSb.append("they are referring to these active conversation documents. ")
            promptSb.append("Always prioritize this context and answer based on it.\n")
            promptSb.append("- Keep your answers factual and grounded ONLY in the provided document excerpts. ")
            promptSb.append("If the information is not in the excerpts and cannot be answered from them, say 'I cannot find that in the attached document.'\n")
        }
        
        if (!searchResults.isNullOrBlank()) {
            promptSb.append("- You have access to real-time search results. Use the provided search results to answer the query.\n")
        }
        promptSb.append("\n")

        // 2. Active Documents Context
        if (documentContext.isNotEmpty()) {
            promptSb.append(documentContext).append("\n\n")
        }
        
        // 3. Search Context
        if (!searchResults.isNullOrBlank()) {
            promptSb.append("### WEB SEARCH RESULTS\n")
            promptSb.append("Search Query: ").append(searchQuery).append("\n")
            promptSb.append("--- START SEARCH RESULTS ---\n")
            promptSb.append(searchResults.trim()).append("\n")
            promptSb.append("--- END SEARCH RESULTS ---\n\n")
        }

        // 4. Conversation History Context
        if (selectedHistory.isNotEmpty()) {
            promptSb.append("### CONVERSATION HISTORY\n")
            promptSb.append("--- START HISTORY ---\n")
            for (msg in selectedHistory) {
                promptSb.append(msg)
            }
            promptSb.append("--- END HISTORY ---\n\n")
        }

        // 5. Current User Query
        promptSb.append("### USER QUERY\n")
        promptSb.append(rawPrompt.trim())
        
        return promptSb.toString()
    }
}

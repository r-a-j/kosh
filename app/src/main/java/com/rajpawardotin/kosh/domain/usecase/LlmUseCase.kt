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

    fun detectSearchRequirement(prompt: String, isInternetEnabled: Boolean): Boolean {
        if (isInternetEnabled) return true
        
        val lowercasePrompt = prompt.lowercase()
        val searchKeywords = listOf(
            "search web", "search internet", "search online", "google", "bing", "duckduckgo",
            "lookup", "look up", "find on web", "browse", "website", "realtime", "real-time",
            "latest news", "today's headlines", "todays headlines", "weather today", "current price"
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

    fun retrieveContext(
        sessionId: String, 
        query: String, 
        isEncrypted: Boolean, 
        activeSessionDocuments: List<SessionDocument>,
        justAttached: Boolean = false
    ): Pair<String, List<String>> {
        val relevantDocs = if (isEncrypted) {
            val stopWords = setOf("a", "an", "and", "are", "as", "at", "be", "but", "by", "for", "if", "in", "into", "is", "it", "no", "not", "of", "on", "or", "such", "that", "the", "their", "then", "there", "these", "they", "this", "to", "was", "will", "with", "what", "how", "why", "who", "when", "where", "summarize", "attached", "document", "documents", "tell", "me", "about", "please", "can", "you", "explain")
            val sanitizedQuery = query.trim().lowercase().replace(Regex("[^a-z0-9\\s]"), "")
            val terms = sanitizedQuery.split(Regex("\\s+")).filter { it.isNotBlank() && !stopWords.contains(it) }
            
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
            val docs = documentRepository.searchSessionDocumentsFTS(sessionId, query)
            if (docs.isEmpty() && justAttached) {
                emptyList()
            } else {
                docs.take(3)
            }
        }

        if (relevantDocs.isEmpty()) return Pair("", emptyList())

        val sb = StringBuilder()
        sb.append("Answer the user query using ONLY the following document context:\n")
        sb.append("--- START DOCUMENT CONTEXT ---\n")
        val sourceNames = mutableSetOf<String>()
        for (doc in relevantDocs) {
            sb.append("[File: ${doc.fileName}]\n")
            sb.append("${doc.chunkText}\n\n")
            sourceNames.add(doc.fileName)
        }
        sb.append("--- END DOCUMENT CONTEXT ---")
        return Pair(sb.toString(), sourceNames.toList())
    }
}

package com.rajpawardotin.kosh.domain.usecase

import com.rajpawardotin.kosh.data.CryptoUtils
import com.rajpawardotin.kosh.domain.model.ChatMessage
import com.rajpawardotin.kosh.domain.model.SessionDocument
import com.rajpawardotin.kosh.domain.provider.AIProvider
import com.rajpawardotin.kosh.domain.repository.DocumentRepository
import com.rajpawardotin.kosh.ui.chat.ResponseParser
import java.util.UUID
import javax.crypto.SecretKey

class RAGConsolidationUseCase(
    private val aiProvider: AIProvider,
    private val llmUseCase: LlmUseCase,
    private val documentRepository: DocumentRepository
) {
    suspend fun execute(
        sessionId: String,
        lastUserMsg: String,
        lastAssistantMsg: String,
        isEncrypted: Boolean,
        sessionKey: SecretKey?,
        docs: List<SessionDocument>,
        messages: List<ChatMessage>
    ): SessionDocument? {
        if (isEncrypted && sessionKey == null) {
            android.util.Log.e("KOSH_SECURITY", "RAG Memory Consolidation skipped: Session is encrypted but key is missing.")
            return null
        }

        val contentDocs = docs.filter { it.chunkIndex >= 0 }
        if (contentDocs.isEmpty()) return null

        val terms = llmUseCase.tokenizeQuery(lastUserMsg)
        if (terms.isEmpty()) return null
        
        val sectionSummaries = docs.filter { it.chunkIndex in -100..-10 }
        
        val relevantDocs = if (sectionSummaries.isNotEmpty()) {
            val rankedSections = sectionSummaries
                .map { sec -> sec to llmUseCase.scoreChunk(sec.chunkText, terms) }
                .filter { it.second > 0.0 }
                .sortedByDescending { it.second }
            
            if (rankedSections.isEmpty()) {
                emptyList()
            } else {
                val topSections = rankedSections.take(2).map { it.first }
                val candidateLeafs = topSections.flatMap { sec ->
                    llmUseCase.getLeafChunksForSection(sec, contentDocs)
                }.distinctBy { it.id }
                
                val rankedLeafs = candidateLeafs
                    .map { doc -> doc to llmUseCase.scoreChunk(doc.chunkText, terms) }
                    .filter { it.second > 0.0 }
                    .sortedByDescending { it.second }
                rankedLeafs.take(2).map { it.first }
            }
        } else {
            val rankedLeafs = contentDocs
                .map { doc -> doc to llmUseCase.scoreChunk(doc.chunkText, terms) }
                .filter { it.second > 0.0 }
                .sortedByDescending { it.second }
            rankedLeafs.take(2).map { it.first }
        }

        if (relevantDocs.isEmpty()) return null
        
        val chatHistoryText = messages.joinToString("\n") { msg ->
            val role = if (msg.isUser) "User" else "Assistant"
            val cleanText = if (msg.isUser) msg.text else ResponseParser.extractThinkingSegments(msg.text).second
            "- $role: ${cleanText.trim()}"
        }
        
        val maxContextChars = 16000
        val chatHistoryChars = chatHistoryText.length
        val retrievedChunksText = relevantDocs.joinToString("\n") { it.chunkText }
        val retrievedChunksChars = retrievedChunksText.length
        val templateChars = 2000
        
        val remainingChars = maxContextChars - chatHistoryChars - templateChars - retrievedChunksChars
        val allocatedChars = maxOf(900, minOf(3600, (remainingChars * 0.3).toInt()))
        val targetWords = allocatedChars / 6

        val existingRagMemoryDoc = docs.find { it.chunkIndex == -2 }
        val currentRagMemory = existingRagMemoryDoc?.chunkText ?: "No previous RAG memory."

        val prompt = """
            You are Kosh, a private offline assistant. Update the running consolidated memory of facts, formulas, and context extracted from the attached documents during this conversation.
            
            Current Consolidated RAG Memory:
            $currentRagMemory
            
            Newly Retrieved Document Excerpts:
            ${relevantDocs.joinToString("\n\n") { "Excerpt:\n" + it.chunkText.trim() }}
            
            Recent Interaction:
            User: $lastUserMsg
            Assistant: ${ResponseParser.extractThinkingSegments(lastAssistantMsg).second.trim()}
            
            Generate the updated, highly condensed RAG Memory (keep it under $targetWords words). Focus only on verified facts, formulas, or details extracted from the document chunks that are relevant to the user's queries/discussions.
            
            Updated RAG Memory:
        """.trimIndent()

        var newRagMemoryText = ""
        aiProvider.sendMessage(prompt).collect { token ->
            newRagMemoryText += token
        }
        newRagMemoryText = ResponseParser.extractThinkingSegments(newRagMemoryText).second.trim()
        if (newRagMemoryText.isEmpty() || newRagMemoryText.startsWith("Error:")) {
            return null
        }

        val chunkId = existingRagMemoryDoc?.id ?: UUID.randomUUID().toString()
        val storedName = if (isEncrypted && sessionKey != null) CryptoUtils.encryptMessage("RAG_Memory", sessionKey) else "RAG_Memory"
        val storedText = if (isEncrypted && sessionKey != null) CryptoUtils.encryptMessage(newRagMemoryText, sessionKey) else newRagMemoryText

        val newRagMemoryDoc = SessionDocument(
            id = chunkId,
            sessionId = sessionId,
            fileName = storedName,
            fileType = "rag_memory",
            fileSize = 0L,
            chunkIndex = -2,
            chunkText = storedText,
            isEncrypted = isEncrypted,
            createdAt = System.currentTimeMillis()
        )
        
        documentRepository.saveSessionDocument(newRagMemoryDoc)

        return SessionDocument(
            id = chunkId,
            sessionId = sessionId,
            fileName = "RAG_Memory",
            fileType = "rag_memory",
            fileSize = 0L,
            chunkIndex = -2,
            chunkText = newRagMemoryText,
            isEncrypted = isEncrypted,
            createdAt = System.currentTimeMillis()
        )
    }
}

package com.rajpawardotin.kosh.domain.usecase

import android.content.Context
import android.net.Uri
import com.rajpawardotin.kosh.data.CryptoUtils
import com.rajpawardotin.kosh.data.DocumentParser
import com.rajpawardotin.kosh.domain.model.AttachedFile
import com.rajpawardotin.kosh.domain.model.SessionDocument
import com.rajpawardotin.kosh.domain.repository.DocumentRepository
import javax.crypto.SecretKey

class DocumentProcessingUseCase(
    private val documentRepository: DocumentRepository
) {
    fun chunkText(text: String, chunkSize: Int = 1000, overlap: Int = 200): List<String> {
        if (text.isBlank()) return emptyList()
        val chunks = mutableListOf<String>()
        var start = 0
        while (start < text.length) {
            val end = minOf(start + chunkSize, text.length)
            val chunk = text.substring(start, end)
            chunks.add(chunk)
            if (end == text.length) break
            start += (chunkSize - overlap)
        }
        return chunks
    }

    fun processDocument(
        context: Context,
        file: AttachedFile,
        sessionId: String,
        isTemporarySession: Boolean,
        activeSessionKeys: Map<String, SecretKey>
    ): List<SessionDocument> {
        val uri = Uri.parse(file.uriString)
        val extractedText = DocumentParser.extractText(context, uri, file.fileName)
        val chunks = chunkText(extractedText)
        
        val isEncrypted = activeSessionKeys.containsKey(sessionId) || isTemporarySession
        val key = activeSessionKeys[sessionId]
        val processedDocs = mutableListOf<SessionDocument>()
        
        chunks.forEachIndexed { index, chunkText ->
            val chunkId = java.util.UUID.randomUUID().toString()
            
            if (!isTemporarySession) {
                val storedName = if (isEncrypted && key != null) CryptoUtils.encryptMessage(file.fileName, key) else file.fileName
                val storedText = if (isEncrypted && key != null) CryptoUtils.encryptMessage(chunkText, key) else chunkText
                
                val docChunk = SessionDocument(
                    id = chunkId,
                    sessionId = sessionId,
                    fileName = storedName,
                    fileType = file.fileType,
                    fileSize = file.fileSize,
                    chunkIndex = index,
                    chunkText = storedText,
                    isEncrypted = isEncrypted,
                    createdAt = System.currentTimeMillis()
                )
                documentRepository.saveSessionDocument(docChunk)
            }
            
            // For the in-memory representation used by RAG and UI for the active session,
            // we always want the decrypted/plaintext version
            val docChunkDecrypted = SessionDocument(
                id = chunkId,
                sessionId = sessionId,
                fileName = file.fileName,
                fileType = file.fileType,
                fileSize = file.fileSize,
                chunkIndex = index,
                chunkText = chunkText,
                isEncrypted = isEncrypted,
                createdAt = System.currentTimeMillis()
            )
            processedDocs.add(docChunkDecrypted)
        }
        return processedDocs
    }
}

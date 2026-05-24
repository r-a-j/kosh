package com.rajpawardotin.kosh.ui.chat

import com.rajpawardotin.kosh.domain.model.ChatMessage
import com.rajpawardotin.kosh.domain.model.ChatSession
import com.rajpawardotin.kosh.domain.model.SessionDocument
import com.rajpawardotin.kosh.domain.repository.DocumentRepository
import com.rajpawardotin.kosh.domain.repository.MessageRepository
import com.rajpawardotin.kosh.domain.repository.SessionRepository

class FakeSessionRepository : SessionRepository {
    val sessions = mutableListOf<ChatSession>()

    override fun saveSession(session: ChatSession) {
        sessions.removeAll { it.id == session.id }
        sessions.add(session)
    }

    override fun renameSession(sessionId: String, newTitle: String) {
        val index = sessions.indexOfFirst { it.id == sessionId }
        if (index != -1) {
            sessions[index] = sessions[index].copy(title = newTitle)
        }
    }

    override fun deleteSession(sessionId: String) {
        sessions.removeAll { it.id == sessionId }
    }

    override fun getSessionsOrderedByLastActive(): List<ChatSession> {
        return sessions.sortedByDescending { it.lastActive }
    }

    override fun closeDatabase() {
    }

    override fun mergeDatabaseBackup(tempDecryptedDbPath: String) {
    }
}

class FakeMessageRepository : MessageRepository {
    val messages = mutableMapOf<String, MutableList<ChatMessage>>()
    val checklistStates = mutableMapOf<String, Boolean>()

    override fun saveMessage(sessionId: String, message: ChatMessage) {
        val list = messages.getOrPut(sessionId) { mutableListOf() }
        list.removeAll { it.id == message.id }
        list.add(message)
    }

    override fun getMessagesForSession(sessionId: String): List<ChatMessage> {
        return messages[sessionId] ?: emptyList()
    }

    override fun saveChecklistState(messageId: String, itemIndex: Int, isChecked: Boolean) {
        checklistStates["${messageId}_$itemIndex"] = isChecked
    }

    override fun getChecklistStatesForSession(sessionId: String): Map<String, Boolean> {
        val sessionMessages = messages[sessionId] ?: return emptyMap()
        val result = mutableMapOf<String, Boolean>()
        for (msg in sessionMessages) {
            checklistStates.forEach { (key, checked) ->
                if (key.startsWith("${msg.id}_")) {
                    result[key] = checked
                }
            }
        }
        return result
    }
}

class FakeDocumentRepository : DocumentRepository {
    val documents = mutableListOf<SessionDocument>()

    override fun saveSessionDocument(document: SessionDocument) {
        documents.removeAll { it.id == document.id }
        documents.add(document)
    }

    override fun getSessionDocuments(sessionId: String): List<SessionDocument> {
        return documents.filter { it.sessionId == sessionId }.sortedBy { it.chunkIndex }
    }

    override fun searchSessionDocumentsFTS(sessionId: String, query: String): List<SessionDocument> {
        val terms = query.lowercase().split(Regex("\\s+")).filter { it.isNotBlank() }
        if (terms.isEmpty()) return emptyList()
        return documents.filter { doc ->
            doc.sessionId == sessionId && terms.any { doc.chunkText.lowercase().contains(it) }
        }
    }
}

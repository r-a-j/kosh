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

    val tagsList = mutableListOf<com.rajpawardotin.kosh.domain.model.ChatTag>()
    val sessionTags = mutableMapOf<String, MutableSet<String>>()

    override fun getTags(): List<com.rajpawardotin.kosh.domain.model.ChatTag> {
        return tagsList.sortedBy { it.name }
    }

    override fun createTag(name: String, colorHex: String): Boolean {
        val id = name.trim().lowercase()
        if (tagsList.any { it.id == id }) return false
        tagsList.add(com.rajpawardotin.kosh.domain.model.ChatTag(id, name.trim(), colorHex))
        return true
    }

    override fun updateTag(oldName: String, newName: String, colorHex: String): Boolean {
        val oldId = oldName.trim().lowercase()
        val newId = newName.trim().lowercase()
        if (oldId != newId && tagsList.any { it.id == newId }) return false
        
        val index = tagsList.indexOfFirst { it.id == oldId }
        if (index == -1) return false
        
        tagsList[index] = com.rajpawardotin.kosh.domain.model.ChatTag(newId, newName.trim(), colorHex)
        
        if (oldId != newId) {
            sessionTags.forEach { (_, set) ->
                if (set.remove(oldId)) {
                    set.add(newId)
                }
            }
            for (i in sessions.indices) {
                val s = sessions[i]
                if (s.tags.any { it.id == oldId }) {
                    val updatedTags = s.tags.map {
                        if (it.id == oldId) com.rajpawardotin.kosh.domain.model.ChatTag(newId, newName.trim(), colorHex) else it
                    }
                    sessions[i] = s.copy(tags = updatedTags)
                }
            }
        } else {
            for (i in sessions.indices) {
                val s = sessions[i]
                if (s.tags.any { it.id == oldId }) {
                    val updatedTags = s.tags.map {
                        if (it.id == oldId) com.rajpawardotin.kosh.domain.model.ChatTag(oldId, newName.trim(), colorHex) else it
                    }
                    sessions[i] = s.copy(tags = updatedTags)
                }
            }
        }
        return true
    }

    override fun deleteTag(name: String): Boolean {
        val id = name.trim().lowercase()
        tagsList.removeAll { it.id == id }
        sessionTags.values.forEach { it.remove(id) }
        for (i in sessions.indices) {
            val s = sessions[i]
            if (s.tags.any { it.id == id }) {
                sessions[i] = s.copy(tags = s.tags.filter { it.id != id })
            }
        }
        return true
    }

    override fun addTagToSession(sessionId: String, tagName: String) {
        val tagId = tagName.trim().lowercase()
        val tag = tagsList.find { it.id == tagId } ?: return
        sessionTags.getOrPut(sessionId) { mutableSetOf() }.add(tagId)
        
        val index = sessions.indexOfFirst { it.id == sessionId }
        if (index != -1) {
            val currentTags = sessions[index].tags.toMutableList()
            if (!currentTags.any { it.id == tagId }) {
                currentTags.add(tag)
                sessions[index] = sessions[index].copy(tags = currentTags)
            }
        }
    }

    override fun removeTagFromSession(sessionId: String, tagName: String) {
        val tagId = tagName.trim().lowercase()
        sessionTags[sessionId]?.remove(tagId)
        
        val index = sessions.indexOfFirst { it.id == sessionId }
        if (index != -1) {
            val currentTags = sessions[index].tags.filter { it.id != tagId }
            sessions[index] = sessions[index].copy(tags = currentTags)
        }
    }

    override fun getTagsForSession(sessionId: String): List<com.rajpawardotin.kosh.domain.model.ChatTag> {
        val ids = sessionTags[sessionId] ?: return emptyList()
        return tagsList.filter { ids.contains(it.id) }.sortedBy { it.name }
    }

    override fun getSessionTagsCount(tagName: String): Int {
        val id = tagName.trim().lowercase()
        return sessionTags.values.count { it.contains(id) }
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

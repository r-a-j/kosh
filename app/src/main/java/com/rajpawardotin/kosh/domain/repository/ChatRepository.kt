package com.rajpawardotin.kosh.domain.repository

import com.rajpawardotin.kosh.domain.model.ChatMessage
import com.rajpawardotin.kosh.domain.model.ChatSession
import com.rajpawardotin.kosh.domain.model.SessionDocument

interface ChatRepository {
    fun saveSession(session: ChatSession)
    fun renameSession(sessionId: String, newTitle: String)
    fun deleteSession(sessionId: String)
    fun getSessionsOrderedByLastActive(): List<ChatSession>
    fun saveMessage(sessionId: String, message: ChatMessage)
    fun getMessagesForSession(sessionId: String): List<ChatMessage>
    fun saveChecklistState(messageId: String, itemIndex: Int, isChecked: Boolean)
    fun getChecklistStatesForSession(sessionId: String): Map<String, Boolean>
    
    fun saveSessionDocument(document: SessionDocument)
    fun getSessionDocuments(sessionId: String): List<SessionDocument>
    fun searchSessionDocumentsFTS(sessionId: String, query: String): List<SessionDocument>
}


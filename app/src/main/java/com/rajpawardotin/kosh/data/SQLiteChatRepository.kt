package com.rajpawardotin.kosh.data

import android.content.Context
import com.rajpawardotin.kosh.domain.model.ChatMessage
import com.rajpawardotin.kosh.domain.model.ChatSession
import com.rajpawardotin.kosh.domain.model.SessionDocument
import com.rajpawardotin.kosh.domain.repository.ChatRepository

class SQLiteChatRepository(context: Context) : ChatRepository {
    private val dbHelper = KoshDatabaseHelper(context)

    override fun saveSession(session: ChatSession) {
        dbHelper.saveSession(session)
    }

    override fun renameSession(sessionId: String, newTitle: String) {
        dbHelper.renameSession(sessionId, newTitle)
    }

    override fun deleteSession(sessionId: String) {
        dbHelper.deleteSession(sessionId)
    }

    override fun getSessionsOrderedByLastActive(): List<ChatSession> {
        return dbHelper.getSessionsOrderedByLastActive()
    }

    override fun saveMessage(sessionId: String, message: ChatMessage) {
        dbHelper.saveMessage(sessionId, message)
    }

    override fun getMessagesForSession(sessionId: String): List<ChatMessage> {
        return dbHelper.getMessagesForSession(sessionId)
    }

    override fun saveChecklistState(messageId: String, itemIndex: Int, isChecked: Boolean) {
        dbHelper.saveChecklistState(messageId, itemIndex, isChecked)
    }

    override fun getChecklistStatesForSession(sessionId: String): Map<String, Boolean> {
        return dbHelper.getChecklistStatesForSession(sessionId)
    }

    override fun saveSessionDocument(document: SessionDocument) {
        dbHelper.saveSessionDocument(document)
    }

    override fun getSessionDocuments(sessionId: String): List<SessionDocument> {
        return dbHelper.getSessionDocuments(sessionId)
    }

    override fun searchSessionDocumentsFTS(sessionId: String, query: String): List<SessionDocument> {
        return dbHelper.searchSessionDocumentsFTS(sessionId, query)
    }
}


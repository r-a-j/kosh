package com.rajpawardotin.kosh.data

import com.rajpawardotin.kosh.domain.model.ChatMessage
import com.rajpawardotin.kosh.domain.repository.MessageRepository

class SQLiteMessageRepository(
    private val dbHelper: KoshDatabaseHelper
) : MessageRepository {

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
}

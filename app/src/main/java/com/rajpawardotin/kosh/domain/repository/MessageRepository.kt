package com.rajpawardotin.kosh.domain.repository

import com.rajpawardotin.kosh.domain.model.ChatMessage

interface MessageRepository {
    fun saveMessage(sessionId: String, message: ChatMessage)
    fun getMessagesForSession(sessionId: String): List<ChatMessage>
    fun saveChecklistState(messageId: String, itemIndex: Int, isChecked: Boolean)
    fun getChecklistStatesForSession(sessionId: String): Map<String, Boolean>
    fun updateMessageFeedback(messageId: String, feedback: Int)
}

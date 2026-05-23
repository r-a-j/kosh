package com.rajpawardotin.kosh.domain.model

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean,
    val isStreaming: Boolean = false,
    val isSystemMessage: Boolean = false,
    val sourceDocuments: String? = null
)


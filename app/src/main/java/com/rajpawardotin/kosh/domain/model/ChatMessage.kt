package com.rajpawardotin.kosh.domain.model

data class ChatMessage(
    val text: String,
    val isUser: Boolean,
    val isStreaming: Boolean = false,
    val isSystemMessage: Boolean = false
)


package com.rajpawardotin.kosh.domain.model

data class ChatSession(
    val id: String,
    val title: String,
    val createdAt: Long,
    val lastActive: Long,
    val modelPath: String?,
    val lastSearchQuery: String?
)

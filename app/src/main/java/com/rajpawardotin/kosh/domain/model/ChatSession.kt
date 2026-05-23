package com.rajpawardotin.kosh.domain.model

data class ChatSession(
    val id: String,
    val title: String,
    val createdAt: Long,
    val lastActive: Long,
    val modelPath: String?,
    val lastSearchQuery: String?,
    val passwordHash: String? = null,
    val salt: String? = null,
    val validationToken: String? = null,
    val encryptedKeyPassword: String? = null,
    val encryptedKeyBiometric: String? = null
)

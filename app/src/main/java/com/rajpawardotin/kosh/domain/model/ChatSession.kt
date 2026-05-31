package com.rajpawardotin.kosh.domain.model

data class ChatTag(
    val id: String,
    val name: String,
    val colorHex: String
)

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
    val encryptedKeyBiometric: String? = null,
    val encryptedKeyRecovery: String? = null,
    val tags: List<ChatTag> = emptyList(),
    val summary: String? = null,
    val facts: String? = null
)

data class AttachedFile(
    val fileName: String,
    val fileType: String,
    val fileSize: Long,
    val uriString: String
)

data class SessionDocument(
    val id: String,
    val sessionId: String,
    val fileName: String,
    val fileType: String,
    val fileSize: Long,
    val chunkIndex: Int,
    val chunkText: String,
    val isEncrypted: Boolean,
    val createdAt: Long
)


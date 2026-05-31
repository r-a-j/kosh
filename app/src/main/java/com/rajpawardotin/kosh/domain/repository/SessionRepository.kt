package com.rajpawardotin.kosh.domain.repository

import com.rajpawardotin.kosh.domain.model.ChatSession
import com.rajpawardotin.kosh.domain.model.SessionDocument

interface SessionRepository {
    fun saveSession(session: ChatSession)
    fun renameSession(sessionId: String, newTitle: String)
    fun deleteSession(sessionId: String)
    fun getSessionsOrderedByLastActive(): List<ChatSession>
    
    fun closeDatabase()
    fun mergeDatabaseBackup(tempDecryptedDbPath: String)

    fun getTags(): List<com.rajpawardotin.kosh.domain.model.ChatTag>
    fun createTag(name: String, colorHex: String): Boolean
    fun updateTag(oldName: String, newName: String, colorHex: String): Boolean
    fun deleteTag(name: String): Boolean
    fun addTagToSession(sessionId: String, tagName: String)
    fun removeTagFromSession(sessionId: String, tagName: String)
    fun getTagsForSession(sessionId: String): List<com.rajpawardotin.kosh.domain.model.ChatTag>
    fun getSessionTagsCount(tagName: String): Int

}

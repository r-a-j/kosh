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
}

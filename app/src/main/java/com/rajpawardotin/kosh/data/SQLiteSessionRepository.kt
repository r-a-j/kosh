package com.rajpawardotin.kosh.data

import com.rajpawardotin.kosh.domain.model.ChatSession
import com.rajpawardotin.kosh.domain.model.SessionDocument
import com.rajpawardotin.kosh.domain.repository.SessionRepository

class SQLiteSessionRepository(
    private val dbHelper: KoshDatabaseHelper
) : SessionRepository {

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

    override fun closeDatabase() {
        dbHelper.close()
    }

    override fun mergeDatabaseBackup(tempDecryptedDbPath: String) {
        dbHelper.mergeFromAttachedDatabase(tempDecryptedDbPath)
    }
}

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

    override fun getTags(): List<com.rajpawardotin.kosh.domain.model.ChatTag> {
        return dbHelper.getTags()
    }

    override fun createTag(name: String, colorHex: String): Boolean {
        return dbHelper.createTag(name, colorHex)
    }

    override fun updateTag(oldName: String, newName: String, colorHex: String): Boolean {
        return dbHelper.updateTag(oldName, newName, colorHex)
    }

    override fun deleteTag(name: String): Boolean {
        return dbHelper.deleteTag(name)
    }

    override fun addTagToSession(sessionId: String, tagName: String) {
        dbHelper.addTagToSession(sessionId, tagName)
    }

    override fun removeTagFromSession(sessionId: String, tagName: String) {
        dbHelper.removeTagFromSession(sessionId, tagName)
    }

    override fun getTagsForSession(sessionId: String): List<com.rajpawardotin.kosh.domain.model.ChatTag> {
        return dbHelper.getTagsForSession(sessionId)
    }

    override fun getSessionTagsCount(tagName: String): Int {
        return dbHelper.getSessionTagsCount(tagName)
    }

}

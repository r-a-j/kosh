package com.rajpawardotin.kosh.data

import com.rajpawardotin.kosh.domain.model.SessionDocument
import com.rajpawardotin.kosh.domain.repository.DocumentRepository

class SQLiteDocumentRepository(
    private val dbHelper: KoshDatabaseHelper
) : DocumentRepository {

    override fun saveSessionDocument(document: SessionDocument) {
        dbHelper.saveSessionDocument(document)
    }

    override fun getSessionDocuments(sessionId: String): List<SessionDocument> {
        return dbHelper.getSessionDocuments(sessionId)
    }

    override fun searchSessionDocumentsFTS(sessionId: String, query: String): List<SessionDocument> {
        return dbHelper.searchSessionDocumentsFTS(sessionId, query)
    }
}

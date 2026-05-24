package com.rajpawardotin.kosh.domain.repository

import com.rajpawardotin.kosh.domain.model.SessionDocument

interface DocumentRepository {
    fun saveSessionDocument(document: SessionDocument)
    fun getSessionDocuments(sessionId: String): List<SessionDocument>
    fun searchSessionDocumentsFTS(sessionId: String, query: String): List<SessionDocument>
}

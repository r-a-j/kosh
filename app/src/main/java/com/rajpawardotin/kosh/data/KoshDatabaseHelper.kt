package com.rajpawardotin.kosh.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.rajpawardotin.kosh.domain.model.ChatMessage
import com.rajpawardotin.kosh.domain.model.ChatSession

import android.database.DatabaseErrorHandler

class KoshDatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION, KoshDatabaseErrorHandler(context)) {

    private class KoshDatabaseErrorHandler(private val context: Context) : DatabaseErrorHandler {
        override fun onCorruption(dbObj: SQLiteDatabase?) {
            val path = dbObj?.path ?: context.getDatabasePath(DATABASE_NAME).absolutePath
            val originalFile = java.io.File(path)
            
            if (originalFile.exists()) {
                val backupFile = java.io.File(path + ".corrupt")
                try {
                    // Try to backup the corrupted DB for manual recovery. 
                    // Fails safely if disk is full.
                    originalFile.copyTo(backupFile, overwrite = true)
                } catch (e: Exception) {
                    // Swallow exception. If disk is full, we must prioritize deleting the DB to unbrick the app.
                }
                
                try {
                    SQLiteDatabase.deleteDatabase(originalFile)
                } catch (e: Exception) {
                    // Ignore deletion errors
                }
            }
        }
    }

    companion object {
        private const val DATABASE_NAME = "kosh_vault.db"
        private const val DATABASE_VERSION = 6



        // Sessions Table
        private const val TABLE_SESSIONS = "sessions"
        private const val KEY_SESSION_ID = "id"
        private const val KEY_SESSION_TITLE = "title"
        private const val KEY_SESSION_CREATED_AT = "created_at"
        private const val KEY_SESSION_LAST_ACTIVE = "last_active"
        private const val KEY_SESSION_MODEL_PATH = "model_path"
        private const val KEY_SESSION_LAST_SEARCH_QUERY = "last_search_query"
        
        // Encryption fields
        private const val KEY_SESSION_PASSWORD_HASH = "password_hash"
        private const val KEY_SESSION_SALT = "salt"
        private const val KEY_SESSION_VALIDATION_TOKEN = "validation_token"
        private const val KEY_SESSION_ENCRYPTED_KEY_PASSWORD = "encrypted_session_key_password"
        private const val KEY_SESSION_ENCRYPTED_KEY_BIOMETRIC = "encrypted_session_key_biometric"
        private const val KEY_SESSION_ENCRYPTED_KEY_RECOVERY = "encrypted_session_key_recovery"

        // Messages Table
        private const val TABLE_MESSAGES = "messages"
        private const val KEY_MESSAGE_ID = "id"
        private const val KEY_MESSAGE_SESSION_ID = "session_id"
        private const val KEY_MESSAGE_TEXT = "text"
        private const val KEY_MESSAGE_IS_USER = "is_user"
        private const val KEY_MESSAGE_IS_SYSTEM = "is_system"
        private const val KEY_MESSAGE_CREATED_AT = "created_at"
        private const val KEY_MESSAGE_SOURCE_DOCUMENTS = "source_documents"

        // Checklist Table
        private const val TABLE_CHECKLIST = "checklist_states"
        private const val KEY_CHECKLIST_MESSAGE_ID = "message_id"
        private const val KEY_CHECKLIST_ITEM_INDEX = "item_index"
        private const val KEY_CHECKLIST_IS_CHECKED = "is_checked"
    }

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createSessionsTable = """
            CREATE TABLE $TABLE_SESSIONS (
                $KEY_SESSION_ID TEXT PRIMARY KEY,
                $KEY_SESSION_TITLE TEXT,
                $KEY_SESSION_CREATED_AT INTEGER,
                $KEY_SESSION_LAST_ACTIVE INTEGER,
                $KEY_SESSION_MODEL_PATH TEXT,
                $KEY_SESSION_LAST_SEARCH_QUERY TEXT,
                $KEY_SESSION_PASSWORD_HASH TEXT,
                $KEY_SESSION_SALT TEXT,
                $KEY_SESSION_VALIDATION_TOKEN TEXT,
                $KEY_SESSION_ENCRYPTED_KEY_PASSWORD TEXT,
                $KEY_SESSION_ENCRYPTED_KEY_BIOMETRIC TEXT,
                $KEY_SESSION_ENCRYPTED_KEY_RECOVERY TEXT
            )
        """.trimIndent()

        val createMessagesTable = """
            CREATE TABLE $TABLE_MESSAGES (
                $KEY_MESSAGE_ID TEXT PRIMARY KEY,
                $KEY_MESSAGE_SESSION_ID TEXT,
                $KEY_MESSAGE_TEXT TEXT,
                $KEY_MESSAGE_IS_USER INTEGER,
                $KEY_MESSAGE_IS_SYSTEM INTEGER,
                $KEY_MESSAGE_CREATED_AT INTEGER,
                $KEY_MESSAGE_SOURCE_DOCUMENTS TEXT,
                FOREIGN KEY($KEY_MESSAGE_SESSION_ID) REFERENCES $TABLE_SESSIONS($KEY_SESSION_ID) ON DELETE CASCADE
            )
        """.trimIndent()

        val createChecklistTable = """
            CREATE TABLE $TABLE_CHECKLIST (
                $KEY_CHECKLIST_MESSAGE_ID TEXT,
                $KEY_CHECKLIST_ITEM_INDEX INTEGER,
                $KEY_CHECKLIST_IS_CHECKED INTEGER,
                PRIMARY KEY($KEY_CHECKLIST_MESSAGE_ID, $KEY_CHECKLIST_ITEM_INDEX),
                FOREIGN KEY($KEY_CHECKLIST_MESSAGE_ID) REFERENCES $TABLE_MESSAGES($KEY_MESSAGE_ID) ON DELETE CASCADE
            )
        """.trimIndent()

        val createSessionDocumentsTable = """
            CREATE TABLE session_documents (
                id TEXT PRIMARY KEY,
                session_id TEXT,
                file_name TEXT,
                file_type TEXT,
                file_size INTEGER,
                chunk_index INTEGER,
                chunk_text TEXT,
                is_encrypted INTEGER,
                created_at INTEGER,
                FOREIGN KEY(session_id) REFERENCES sessions(id) ON DELETE CASCADE
            )
        """.trimIndent()

        //language=none
        val createDocumentsFtsTable = """
            CREATE VIRTUAL TABLE documents_fts USING fts4(
                chunk_id,
                session_id,
                file_name,
                chunk_text,
                notindexed=chunk_id
            )
        """.trimIndent()


        val createInsertTrigger = """
            CREATE TRIGGER after_session_document_insert AFTER INSERT ON session_documents 
            WHEN new.is_encrypted = 0 
            BEGIN
                INSERT INTO documents_fts (chunk_id, session_id, file_name, chunk_text) 
                VALUES (new.id, new.session_id, new.file_name, new.chunk_text);
            END
        """.trimIndent()

        val createDeleteTrigger = """
            CREATE TRIGGER after_session_document_delete AFTER DELETE ON session_documents 
            WHEN old.is_encrypted = 0
            BEGIN
                DELETE FROM documents_fts WHERE chunk_id = old.id;
            END
        """.trimIndent()

        db.execSQL(createSessionsTable)
        db.execSQL(createMessagesTable)
        db.execSQL(createChecklistTable)
        db.execSQL(createSessionDocumentsTable)
        db.execSQL(createDocumentsFtsTable)
        db.execSQL(createInsertTrigger)
        db.execSQL(createDeleteTrigger)

        db.execSQL("""
            CREATE TABLE tags (
                id TEXT PRIMARY KEY,
                name TEXT UNIQUE,
                color TEXT
            )
        """.trimIndent())
        db.execSQL("""
            CREATE TABLE session_tags (
                session_id TEXT,
                tag_id TEXT,
                PRIMARY KEY(session_id, tag_id),
                FOREIGN KEY(session_id) REFERENCES sessions(id) ON DELETE CASCADE,
                FOREIGN KEY(tag_id) REFERENCES tags(id) ON DELETE CASCADE
            )
        """.trimIndent())
        db.execSQL("INSERT OR IGNORE INTO tags (id, name, color) VALUES ('journal', 'Journal', '#8B5CF6')")
        db.execSQL("INSERT OR IGNORE INTO tags (id, name, color) VALUES ('work', 'Work', '#3B82F6')")
        db.execSQL("INSERT OR IGNORE INTO tags (id, name, color) VALUES ('ideas', 'Ideas', '#F59E0B')")
        db.execSQL("INSERT OR IGNORE INTO tags (id, name, color) VALUES ('notes', 'Notes', '#10B981')")
    }


    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE $TABLE_SESSIONS ADD COLUMN $KEY_SESSION_PASSWORD_HASH TEXT")
            db.execSQL("ALTER TABLE $TABLE_SESSIONS ADD COLUMN $KEY_SESSION_SALT TEXT")
            db.execSQL("ALTER TABLE $TABLE_SESSIONS ADD COLUMN $KEY_SESSION_VALIDATION_TOKEN TEXT")
            db.execSQL("ALTER TABLE $TABLE_SESSIONS ADD COLUMN $KEY_SESSION_ENCRYPTED_KEY_PASSWORD TEXT")
            db.execSQL("ALTER TABLE $TABLE_SESSIONS ADD COLUMN $KEY_SESSION_ENCRYPTED_KEY_BIOMETRIC TEXT")
        }
        if (oldVersion < 3) {
            db.execSQL("ALTER TABLE $TABLE_SESSIONS ADD COLUMN $KEY_SESSION_ENCRYPTED_KEY_RECOVERY TEXT")
        }
        if (oldVersion < 4) {
            db.execSQL("""
                CREATE TABLE session_documents (
                    id TEXT PRIMARY KEY,
                    session_id TEXT,
                    file_name TEXT,
                    file_type TEXT,
                    file_size INTEGER,
                    chunk_index INTEGER,
                    chunk_text TEXT,
                    is_encrypted INTEGER,
                    created_at INTEGER,
                    FOREIGN KEY(session_id) REFERENCES sessions(id) ON DELETE CASCADE
                )
            """.trimIndent())

            //language=none
            db.execSQL("""
                CREATE VIRTUAL TABLE documents_fts USING fts4(
                    chunk_id,
                    session_id,
                    file_name,
                    chunk_text,
                    notindexed=chunk_id
                )
            """.trimIndent())


            db.execSQL("""
                CREATE TRIGGER after_session_document_insert AFTER INSERT ON session_documents 
                WHEN new.is_encrypted = 0 
                BEGIN
                    INSERT INTO documents_fts (chunk_id, session_id, file_name, chunk_text) 
                    VALUES (new.id, new.session_id, new.file_name, new.chunk_text);
                END
            """.trimIndent())

            db.execSQL("""
                CREATE TRIGGER after_session_document_delete AFTER DELETE ON session_documents 
                WHEN old.is_encrypted = 0
                BEGIN
                    DELETE FROM documents_fts WHERE chunk_id = old.id;
                END
            """.trimIndent())
        }
        if (oldVersion < 5) {
            db.execSQL("ALTER TABLE $TABLE_MESSAGES ADD COLUMN $KEY_MESSAGE_SOURCE_DOCUMENTS TEXT")
        }
        if (oldVersion < 6) {
            db.execSQL("""
                CREATE TABLE tags (
                    id TEXT PRIMARY KEY,
                    name TEXT UNIQUE,
                    color TEXT
                )
            """.trimIndent())
            db.execSQL("""
                CREATE TABLE session_tags (
                    session_id TEXT,
                    tag_id TEXT,
                    PRIMARY KEY(session_id, tag_id),
                    FOREIGN KEY(session_id) REFERENCES sessions(id) ON DELETE CASCADE,
                    FOREIGN KEY(tag_id) REFERENCES tags(id) ON DELETE CASCADE
                )
            """.trimIndent())
            db.execSQL("INSERT OR IGNORE INTO tags (id, name, color) VALUES ('journal', 'Journal', '#8B5CF6')")
            db.execSQL("INSERT OR IGNORE INTO tags (id, name, color) VALUES ('work', 'Work', '#3B82F6')")
            db.execSQL("INSERT OR IGNORE INTO tags (id, name, color) VALUES ('ideas', 'Ideas', '#F59E0B')")
            db.execSQL("INSERT OR IGNORE INTO tags (id, name, color) VALUES ('notes', 'Notes', '#10B981')")
        }
    }


    fun saveSession(session: ChatSession) = synchronized(this) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(KEY_SESSION_ID, session.id)
            put(KEY_SESSION_TITLE, session.title)
            put(KEY_SESSION_CREATED_AT, session.createdAt)
            put(KEY_SESSION_LAST_ACTIVE, session.lastActive)
            put(KEY_SESSION_MODEL_PATH, session.modelPath)
            put(KEY_SESSION_LAST_SEARCH_QUERY, session.lastSearchQuery)
            put(KEY_SESSION_PASSWORD_HASH, session.passwordHash)
            put(KEY_SESSION_SALT, session.salt)
            put(KEY_SESSION_VALIDATION_TOKEN, session.validationToken)
            put(KEY_SESSION_ENCRYPTED_KEY_PASSWORD, session.encryptedKeyPassword)
            put(KEY_SESSION_ENCRYPTED_KEY_BIOMETRIC, session.encryptedKeyBiometric)
            put(KEY_SESSION_ENCRYPTED_KEY_RECOVERY, session.encryptedKeyRecovery)
        }
        val rowsUpdated = db.update(TABLE_SESSIONS, values, "$KEY_SESSION_ID = ?", arrayOf(session.id))
        if (rowsUpdated == 0) {
            db.insert(TABLE_SESSIONS, null, values)
        }
    }

    fun renameSession(sessionId: String, newTitle: String) = synchronized(this) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(KEY_SESSION_TITLE, newTitle)
        }
        db.update(TABLE_SESSIONS, values, "$KEY_SESSION_ID = ?", arrayOf(sessionId))
    }

    fun deleteSession(sessionId: String) = synchronized(this) {
        val db = writableDatabase
        db.delete(TABLE_SESSIONS, "$KEY_SESSION_ID = ?", arrayOf(sessionId))
    }

    fun getSessionsOrderedByLastActive(): List<ChatSession> {
        val sessionsList = mutableListOf<ChatSession>()
        val db = readableDatabase

        // Load all session tag mappings in a single query
        val sessionTagsMap = mutableMapOf<String, MutableList<com.rajpawardotin.kosh.domain.model.ChatTag>>()
        db.rawQuery("""
            SELECT st.session_id, t.id, t.name, t.color 
            FROM session_tags st
            JOIN tags t ON st.tag_id = t.id
        """.trimIndent(), null).use { tagCursor ->
            if (tagCursor.moveToFirst()) {
                val sessIdIdx = tagCursor.getColumnIndexOrThrow("session_id")
                val idIdx = tagCursor.getColumnIndexOrThrow("id")
                val nameIdx = tagCursor.getColumnIndexOrThrow("name")
                val colIdx = tagCursor.getColumnIndexOrThrow("color")
                do {
                    val sessId = tagCursor.getString(sessIdIdx)
                    val tag = com.rajpawardotin.kosh.domain.model.ChatTag(
                        id = tagCursor.getString(idIdx),
                        name = tagCursor.getString(nameIdx),
                        colorHex = tagCursor.getString(colIdx)
                    )
                    sessionTagsMap.getOrPut(sessId) { mutableListOf() }.add(tag)
                } while (tagCursor.moveToNext())
            }
        }

        val query = "SELECT * FROM $TABLE_SESSIONS ORDER BY $KEY_SESSION_LAST_ACTIVE DESC"
        db.rawQuery(query, null).use { cursor ->
            if (cursor.moveToFirst()) {
                val idIdx = cursor.getColumnIndexOrThrow(KEY_SESSION_ID)
                val titleIdx = cursor.getColumnIndexOrThrow(KEY_SESSION_TITLE)
                val createdIdx = cursor.getColumnIndexOrThrow(KEY_SESSION_CREATED_AT)
                val activeIdx = cursor.getColumnIndexOrThrow(KEY_SESSION_LAST_ACTIVE)
                val modelIdx = cursor.getColumnIndexOrThrow(KEY_SESSION_MODEL_PATH)
                val searchIdx = cursor.getColumnIndexOrThrow(KEY_SESSION_LAST_SEARCH_QUERY)
                val hashIdx = cursor.getColumnIndexOrThrow(KEY_SESSION_PASSWORD_HASH)
                val saltIdx = cursor.getColumnIndexOrThrow(KEY_SESSION_SALT)
                val tokenIdx = cursor.getColumnIndexOrThrow(KEY_SESSION_VALIDATION_TOKEN)
                val encPassIdx = cursor.getColumnIndexOrThrow(KEY_SESSION_ENCRYPTED_KEY_PASSWORD)
                val encBioIdx = cursor.getColumnIndexOrThrow(KEY_SESSION_ENCRYPTED_KEY_BIOMETRIC)
                val encRecIdx = cursor.getColumnIndexOrThrow(KEY_SESSION_ENCRYPTED_KEY_RECOVERY)

                do {
                    val sessionId = cursor.getString(idIdx)
                    sessionsList.add(
                        ChatSession(
                            id = sessionId,
                            title = cursor.getString(titleIdx),
                            createdAt = cursor.getLong(createdIdx),
                            lastActive = cursor.getLong(activeIdx),
                            modelPath = cursor.getString(modelIdx),
                            lastSearchQuery = cursor.getString(searchIdx),
                            passwordHash = cursor.getString(hashIdx),
                            salt = cursor.getString(saltIdx),
                            validationToken = cursor.getString(tokenIdx),
                            encryptedKeyPassword = cursor.getString(encPassIdx),
                            encryptedKeyBiometric = cursor.getString(encBioIdx),
                            encryptedKeyRecovery = cursor.getString(encRecIdx),
                            tags = sessionTagsMap[sessionId] ?: emptyList()
                        )
                    )
                } while (cursor.moveToNext())
            }
        }
        return sessionsList
    }

    fun saveMessage(sessionId: String, message: ChatMessage) = synchronized(this) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(KEY_MESSAGE_ID, message.id)
            put(KEY_MESSAGE_SESSION_ID, sessionId)
            put(KEY_MESSAGE_TEXT, message.text)
            put(KEY_MESSAGE_IS_USER, if (message.isUser) 1 else 0)
            put(KEY_MESSAGE_IS_SYSTEM, if (message.isSystemMessage) 1 else 0)
            put(KEY_MESSAGE_CREATED_AT, System.currentTimeMillis())
            put(KEY_MESSAGE_SOURCE_DOCUMENTS, message.sourceDocuments)
        }
        val rowsUpdated = db.update(TABLE_MESSAGES, values, "$KEY_MESSAGE_ID = ?", arrayOf(message.id))
        if (rowsUpdated == 0) {
            db.insert(TABLE_MESSAGES, null, values)
        }
    }

    fun getMessagesForSession(sessionId: String): List<ChatMessage> {
        val messagesList = mutableListOf<ChatMessage>()
        val db = readableDatabase
        val query = "SELECT * FROM $TABLE_MESSAGES WHERE $KEY_MESSAGE_SESSION_ID = ? ORDER BY $KEY_MESSAGE_CREATED_AT ASC"
        db.rawQuery(query, arrayOf(sessionId)).use { cursor ->
            if (cursor.moveToFirst()) {
                val idIdx = cursor.getColumnIndexOrThrow(KEY_MESSAGE_ID)
                val textIdx = cursor.getColumnIndexOrThrow(KEY_MESSAGE_TEXT)
                val userIdx = cursor.getColumnIndexOrThrow(KEY_MESSAGE_IS_USER)
                val systemIdx = cursor.getColumnIndexOrThrow(KEY_MESSAGE_IS_SYSTEM)
                val sourceDocsIdx = cursor.getColumnIndex(KEY_MESSAGE_SOURCE_DOCUMENTS)

                do {
                    val sourceDocs = if (sourceDocsIdx != -1 && !cursor.isNull(sourceDocsIdx)) cursor.getString(sourceDocsIdx) else null
                    messagesList.add(
                        ChatMessage(
                            id = cursor.getString(idIdx),
                            text = cursor.getString(textIdx),
                            isUser = cursor.getInt(userIdx) == 1,
                            isSystemMessage = cursor.getInt(systemIdx) == 1,
                            isStreaming = false,
                            sourceDocuments = sourceDocs
                        )
                    )
                } while (cursor.moveToNext())
            }
        }
        return messagesList
    }

    fun saveChecklistState(messageId: String, itemIndex: Int, isChecked: Boolean) = synchronized(this) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(KEY_CHECKLIST_MESSAGE_ID, messageId)
            put(KEY_CHECKLIST_ITEM_INDEX, itemIndex)
            put(KEY_CHECKLIST_IS_CHECKED, if (isChecked) 1 else 0)
        }
        db.insertWithOnConflict(TABLE_CHECKLIST, null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun getChecklistStatesForSession(sessionId: String): Map<String, Boolean> {
        val states = mutableMapOf<String, Boolean>()
        val db = readableDatabase
        val query = """
            SELECT c.$KEY_CHECKLIST_MESSAGE_ID, c.$KEY_CHECKLIST_ITEM_INDEX, c.$KEY_CHECKLIST_IS_CHECKED 
            FROM $TABLE_CHECKLIST c
            JOIN $TABLE_MESSAGES m ON c.$KEY_CHECKLIST_MESSAGE_ID = m.$KEY_MESSAGE_ID
            WHERE m.$KEY_MESSAGE_SESSION_ID = ?
        """.trimIndent()
        
        db.rawQuery(query, arrayOf(sessionId)).use { cursor ->
            if (cursor.moveToFirst()) {
                val msgIdIdx = cursor.getColumnIndexOrThrow(KEY_CHECKLIST_MESSAGE_ID)
                val itemIdxIdx = cursor.getColumnIndexOrThrow(KEY_CHECKLIST_ITEM_INDEX)
                val isCheckedIdx = cursor.getColumnIndexOrThrow(KEY_CHECKLIST_IS_CHECKED)
                
                do {
                    val messageId = cursor.getString(msgIdIdx)
                    val itemIndex = cursor.getInt(itemIdxIdx)
                    val isChecked = cursor.getInt(isCheckedIdx) == 1
                    states["${messageId}_$itemIndex"] = isChecked
                } while (cursor.moveToNext())
            }
        }
        return states
    }

    fun saveSessionDocument(document: com.rajpawardotin.kosh.domain.model.SessionDocument) = synchronized(this) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("id", document.id)
            put("session_id", document.sessionId)
            put("file_name", document.fileName)
            put("file_type", document.fileType)
            put("file_size", document.fileSize)
            put("chunk_index", document.chunkIndex)
            put("chunk_text", document.chunkText)
            put("is_encrypted", if (document.isEncrypted) 1 else 0)
            put("created_at", document.createdAt)
        }
        db.insertWithOnConflict("session_documents", null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun getSessionDocuments(sessionId: String): List<com.rajpawardotin.kosh.domain.model.SessionDocument> {
        val list = mutableListOf<com.rajpawardotin.kosh.domain.model.SessionDocument>()
        val db = readableDatabase
        val query = "SELECT * FROM session_documents WHERE session_id = ? ORDER BY chunk_index ASC"
        db.rawQuery(query, arrayOf(sessionId)).use { cursor ->
            if (cursor.moveToFirst()) {
                val idIdx = cursor.getColumnIndexOrThrow("id")
                val sessIdIdx = cursor.getColumnIndexOrThrow("session_id")
                val nameIdx = cursor.getColumnIndexOrThrow("file_name")
                val typeIdx = cursor.getColumnIndexOrThrow("file_type")
                val sizeIdx = cursor.getColumnIndexOrThrow("file_size")
                val indexIdx = cursor.getColumnIndexOrThrow("chunk_index")
                val textIdx = cursor.getColumnIndexOrThrow("chunk_text")
                val encIdx = cursor.getColumnIndexOrThrow("is_encrypted")
                val createdIdx = cursor.getColumnIndexOrThrow("created_at")

                do {
                    list.add(
                        com.rajpawardotin.kosh.domain.model.SessionDocument(
                            id = cursor.getString(idIdx),
                            sessionId = cursor.getString(sessIdIdx),
                            fileName = cursor.getString(nameIdx),
                            fileType = cursor.getString(typeIdx),
                            fileSize = cursor.getLong(sizeIdx),
                            chunkIndex = cursor.getInt(indexIdx),
                            chunkText = cursor.getString(textIdx),
                            isEncrypted = cursor.getInt(encIdx) == 1,
                            createdAt = cursor.getLong(createdIdx)
                        )
                    )
                } while (cursor.moveToNext())
            }
        }
        return list
    }

    fun searchSessionDocumentsFTS(sessionId: String, query: String): List<com.rajpawardotin.kosh.domain.model.SessionDocument> {
        val db = readableDatabase
        
        val stopWords = setOf(
            "a", "an", "and", "are", "as", "at", "be", "but", "by", "for", "if", "in", "into", "is", "it", "no", "not", "of", "on", "or", "such", "that", "the", "their", "then", "there", "these", "they", "this", "to", "was", "will", "with", "what", "how", "why", "who", "when", "where", 
            "summarize", "attached", "document", "documents", "tell", "me", "about", "please", "can", "you", "explain",
            "see", "reference", "doc", "docs", "file", "files", "pdf", "pdfs", "txt", "md", "attachment", "attachments",
            "earlier", "earliest", "previous", "previously", "above", "below", "read", "view", "check", "open", "here", "there",
            "them", "yesterday", "message", "chat", "conversation", "show", "get", "give", "display", "find", "search", "lookup", "look"
        )
        
        val terms = query.trim().lowercase()
            .split(Regex("[\\s,;!?()\"'<>#\$^*%&@~\\[\\]]+"))
            .map { token ->
                token.removeSurrounding("[", "]")
                    .removeSurrounding("(", ")")
                    .removeSurrounding("{", "}")
                    .removeSuffix(".").removeSuffix(",").removeSuffix(":")
            }
            .filter { it.isNotBlank() && !stopWords.contains(it) }

        val allDocs = getSessionDocuments(sessionId).filter { !it.isEncrypted }
        
        if (terms.isEmpty() || allDocs.isEmpty()) {
            val list = mutableListOf<com.rajpawardotin.kosh.domain.model.SessionDocument>()
            val fallbackSql = "SELECT * FROM session_documents WHERE session_id = ? AND is_encrypted = 0 ORDER BY created_at DESC, chunk_index ASC LIMIT 3"
            db.rawQuery(fallbackSql, arrayOf(sessionId)).use { cursor ->
                parseSessionDocumentsCursor(cursor, list)
            }
            return list
        }

        // Rank candidate chunks in memory using identical scoring logic
        val rankedMatches = allDocs
            .map { doc ->
                val textLower = doc.chunkText.lowercase()
                val chunkTokens = textLower.split(Regex("[\\s,;!?()\"'<>#\$^*%&@~\\[\\]]+"))
                    .map { token ->
                        token.removeSurrounding("[", "]")
                            .removeSurrounding("(", ")")
                            .removeSurrounding("{", "}")
                            .removeSuffix(".").removeSuffix(",").removeSuffix(":")
                    }
                    .filter { it.isNotBlank() }
                    .toSet()
                
                var uniqueMatched = 0
                var totalMatchScore = 0.0
                val uniqueTerms = terms.toSet()
                
                for (term in uniqueTerms) {
                    val isWordMatch = chunkTokens.contains(term)
                    val isSubstringMatch = textLower.contains(term)
                    if (isWordMatch || isSubstringMatch) {
                        uniqueMatched++
                        var count = 0
                        var index = 0
                        while (true) {
                            index = textLower.indexOf(term, index)
                            if (index == -1) break
                            count++
                            index += term.length
                        }
                        val weight = if (isWordMatch) 10.0 else 1.0
                        totalMatchScore += count * weight
                    }
                }
                
                val score = if (uniqueMatched == 0) 0.0 else {
                    val coverage = uniqueMatched.toDouble() / uniqueTerms.size.toDouble()
                    (coverage * 1000.0) + totalMatchScore
                }
                
                doc to score
            }
            .filter { it.second > 0.0 }
            .sortedByDescending { it.second }

        if (rankedMatches.isEmpty()) {
            val list = mutableListOf<com.rajpawardotin.kosh.domain.model.SessionDocument>()
            val fallbackSql = "SELECT * FROM session_documents WHERE session_id = ? AND is_encrypted = 0 ORDER BY created_at DESC, chunk_index ASC LIMIT 3"
            db.rawQuery(fallbackSql, arrayOf(sessionId)).use { cursor ->
                parseSessionDocumentsCursor(cursor, list)
            }
            return list
        }

        val topMatches = rankedMatches.take(2).map { it.first }
        val resultSet = mutableSetOf<com.rajpawardotin.kosh.domain.model.SessionDocument>()
        
        for (match in topMatches) {
            resultSet.add(match)
            val preceding = allDocs.find { it.fileName == match.fileName && it.chunkIndex == match.chunkIndex - 1 }
            if (preceding != null) resultSet.add(preceding)
            val succeeding = allDocs.find { it.fileName == match.fileName && it.chunkIndex == match.chunkIndex + 1 }
            if (succeeding != null) resultSet.add(succeeding)
        }

        return resultSet.sortedWith(compareBy<com.rajpawardotin.kosh.domain.model.SessionDocument> { it.fileName }.thenBy { it.chunkIndex })
    }

    private fun parseSessionDocumentsCursor(cursor: android.database.Cursor, list: MutableList<com.rajpawardotin.kosh.domain.model.SessionDocument>) {
        if (cursor.moveToFirst()) {
            val idIdx = cursor.getColumnIndexOrThrow("id")
            val sessIdIdx = cursor.getColumnIndexOrThrow("session_id")
            val nameIdx = cursor.getColumnIndexOrThrow("file_name")
            val typeIdx = cursor.getColumnIndexOrThrow("file_type")
            val sizeIdx = cursor.getColumnIndexOrThrow("file_size")
            val indexIdx = cursor.getColumnIndexOrThrow("chunk_index")
            val textIdx = cursor.getColumnIndexOrThrow("chunk_text")
            val encIdx = cursor.getColumnIndexOrThrow("is_encrypted")
            val createdIdx = cursor.getColumnIndexOrThrow("created_at")

            do {
                list.add(
                    com.rajpawardotin.kosh.domain.model.SessionDocument(
                        id = cursor.getString(idIdx),
                        sessionId = cursor.getString(sessIdIdx),
                        fileName = cursor.getString(nameIdx),
                        fileType = cursor.getString(typeIdx),
                        fileSize = cursor.getLong(sizeIdx),
                        chunkIndex = cursor.getInt(indexIdx),
                        chunkText = cursor.getString(textIdx),
                        isEncrypted = cursor.getInt(encIdx) == 1,
                        createdAt = cursor.getLong(createdIdx)
                    )
                )
            } while (cursor.moveToNext())
        }
    }

    fun mergeFromAttachedDatabase(backupDbPath: String) = synchronized(this) {
        val db = writableDatabase
        try {
            db.execSQL("ATTACH DATABASE ? AS backup_db", arrayOf(backupDbPath))
            
            db.beginTransaction()
            try {
                db.execSQL("INSERT OR IGNORE INTO sessions SELECT * FROM backup_db.sessions")
                db.execSQL("INSERT OR IGNORE INTO messages SELECT * FROM backup_db.messages")
                db.execSQL("INSERT OR IGNORE INTO checklist_states SELECT * FROM backup_db.checklist_states")
                db.execSQL("INSERT OR IGNORE INTO session_documents SELECT * FROM backup_db.session_documents")
                
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
        } finally {
            try {
                db.execSQL("DETACH DATABASE backup_db")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun getTags(): List<com.rajpawardotin.kosh.domain.model.ChatTag> = synchronized(this) {
        val list = mutableListOf<com.rajpawardotin.kosh.domain.model.ChatTag>()
        val db = readableDatabase
        db.rawQuery("SELECT * FROM tags ORDER BY name ASC", null).use { cursor ->
            if (cursor.moveToFirst()) {
                val idIdx = cursor.getColumnIndexOrThrow("id")
                val nameIdx = cursor.getColumnIndexOrThrow("name")
                val colIdx = cursor.getColumnIndexOrThrow("color")
                do {
                    list.add(
                        com.rajpawardotin.kosh.domain.model.ChatTag(
                            id = cursor.getString(idIdx),
                            name = cursor.getString(nameIdx),
                            colorHex = cursor.getString(colIdx)
                        )
                    )
                } while (cursor.moveToNext())
            }
        }
        return list
    }

    fun createTag(name: String, colorHex: String): Boolean = synchronized(this) {
        if (name.isBlank()) return false
        val db = writableDatabase
        val id = name.trim().lowercase()
        val values = ContentValues().apply {
            put("id", id)
            put("name", name.trim())
            put("color", colorHex)
        }
        val result = db.insertWithOnConflict("tags", null, values, SQLiteDatabase.CONFLICT_IGNORE)
        return result != -1L
    }

    fun updateTag(oldName: String, newName: String, colorHex: String): Boolean = synchronized(this) {
        if (newName.isBlank()) return false
        val db = writableDatabase
        val oldId = oldName.trim().lowercase()
        val newId = newName.trim().lowercase()
        
        db.beginTransaction()
        try {
            if (oldId != newId) {
                val cursor = db.rawQuery("SELECT 1 FROM tags WHERE id = ?", arrayOf(newId))
                val exists = cursor.use { it.moveToFirst() }
                if (exists) {
                    db.endTransaction()
                    return false
                }
                
                val values = ContentValues().apply {
                    put("id", newId)
                    put("name", newName.trim())
                    put("color", colorHex)
                }
                db.insert("tags", null, values)
                
                db.execSQL("UPDATE session_tags SET tag_id = ? WHERE tag_id = ?", arrayOf(newId, oldId))
                db.delete("tags", "id = ?", arrayOf(oldId))
            } else {
                val values = ContentValues().apply {
                    put("name", newName.trim())
                    put("color", colorHex)
                }
                db.update("tags", values, "id = ?", arrayOf(oldId))
            }
            db.setTransactionSuccessful()
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        } finally {
            try {
                db.endTransaction()
            } catch (e: Exception) {}
        }
    }

    fun deleteTag(name: String): Boolean = synchronized(this) {
        val db = writableDatabase
        val id = name.trim().lowercase()
        db.beginTransaction()
        try {
            db.delete("session_tags", "tag_id = ?", arrayOf(id))
            db.delete("tags", "id = ?", arrayOf(id))
            db.setTransactionSuccessful()
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        } finally {
            try {
                db.endTransaction()
            } catch (e: Exception) {}
        }
    }

    fun addTagToSession(sessionId: String, tagName: String) = synchronized(this) {
        val db = writableDatabase
        val tagId = tagName.trim().lowercase()
        val values = ContentValues().apply {
            put("session_id", sessionId)
            put("tag_id", tagId)
        }
        db.insertWithOnConflict("session_tags", null, values, SQLiteDatabase.CONFLICT_IGNORE)
    }

    fun removeTagFromSession(sessionId: String, tagName: String) = synchronized(this) {
        val db = writableDatabase
        val tagId = tagName.trim().lowercase()
        db.delete("session_tags", "session_id = ? AND tag_id = ?", arrayOf(sessionId, tagId))
    }

    fun getTagsForSession(sessionId: String): List<com.rajpawardotin.kosh.domain.model.ChatTag> = synchronized(this) {
        val list = mutableListOf<com.rajpawardotin.kosh.domain.model.ChatTag>()
        val db = readableDatabase
        val query = """
            SELECT t.id, t.name, t.color 
            FROM tags t
            JOIN session_tags st ON t.id = st.tag_id
            WHERE st.session_id = ?
            ORDER BY t.name ASC
        """.trimIndent()
        db.rawQuery(query, arrayOf(sessionId)).use { cursor ->
            if (cursor.moveToFirst()) {
                val idIdx = cursor.getColumnIndexOrThrow("id")
                val nameIdx = cursor.getColumnIndexOrThrow("name")
                val colIdx = cursor.getColumnIndexOrThrow("color")
                do {
                    list.add(
                        com.rajpawardotin.kosh.domain.model.ChatTag(
                            id = cursor.getString(idIdx),
                            name = cursor.getString(nameIdx),
                            colorHex = cursor.getString(colIdx)
                        )
                    )
                } while (cursor.moveToNext())
            }
        }
        return list
    }

    fun getSessionTagsCount(tagName: String): Int = synchronized(this) {
        val db = readableDatabase
        val tagId = tagName.trim().lowercase()
        db.rawQuery("SELECT COUNT(*) FROM session_tags WHERE tag_id = ?", arrayOf(tagId)).use { cursor ->
            if (cursor.moveToFirst()) cursor.getInt(0) else 0
        }
    }
}


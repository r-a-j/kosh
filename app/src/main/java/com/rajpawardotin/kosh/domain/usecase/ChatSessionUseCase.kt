package com.rajpawardotin.kosh.domain.usecase

import android.content.Context
import android.net.Uri
import com.rajpawardotin.kosh.data.CryptoUtils
import com.rajpawardotin.kosh.domain.model.ChatSession
import com.rajpawardotin.kosh.domain.repository.SessionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.crypto.SecretKey

class ChatSessionUseCase(
    private val sessionRepository: SessionRepository
) {
    fun decryptSession(session: ChatSession, activeSessionKeys: Map<String, SecretKey>): ChatSession {
        val isEncrypted = session.encryptedKeyPassword != null
        val key = activeSessionKeys[session.id]
        return if (isEncrypted && key != null) {
            val decryptedLastSearchQuery = session.lastSearchQuery?.let {
                try {
                    CryptoUtils.decryptMessage(it, key)
                } catch (e: Exception) {
                    null
                }
            }
            val decryptedSummary = session.summary?.let {
                try {
                    CryptoUtils.decryptMessage(it, key)
                } catch (e: Exception) {
                    null
                }
            }
            val decryptedFacts = session.facts?.let {
                try {
                    CryptoUtils.decryptMessage(it, key)
                } catch (e: Exception) {
                    null
                }
            }
            session.copy(
                lastSearchQuery = decryptedLastSearchQuery,
                summary = decryptedSummary,
                facts = decryptedFacts
            )
        } else {
            session
        }
    }

    fun saveSessionEncrypted(session: ChatSession, activeSessionKeys: Map<String, SecretKey>) {
        val key = activeSessionKeys[session.id]
        if (session.encryptedKeyPassword != null && key != null) {
            val encryptedLastSearchQuery = session.lastSearchQuery?.let {
                try {
                    CryptoUtils.encryptMessage(it, key)
                } catch (e: Exception) {
                    it
                }
            }
            val encryptedSummary = session.summary?.let {
                try {
                    CryptoUtils.encryptMessage(it, key)
                } catch (e: Exception) {
                    it
                }
            }
            val encryptedFacts = session.facts?.let {
                try {
                    CryptoUtils.encryptMessage(it, key)
                } catch (e: Exception) {
                    it
                }
            }
            sessionRepository.saveSession(session.copy(
                lastSearchQuery = encryptedLastSearchQuery,
                summary = encryptedSummary,
                facts = encryptedFacts
            ))
        } else {
            sessionRepository.saveSession(session)
        }
    }

    suspend fun exportBackup(context: Context, destUri: Uri, password: String): Boolean = withContext(Dispatchers.IO) {
        val dbFile = context.getDatabasePath("kosh_vault.db")
        if (!dbFile.exists()) {
            throw Exception("Database does not exist.")
        }
        
        val tempFile = File(context.cacheDir, "kosh_backup_temp.db")
        val success = CryptoUtils.encryptDatabaseBackup(dbFile, tempFile, password)
        if (!success) {
            throw Exception("Encryption failed.")
        }
        
        try {
            context.contentResolver.openOutputStream(destUri)?.use { output ->
                tempFile.inputStream().use { input ->
                    input.copyTo(output)
                }
            }
            CryptoUtils.secureDelete(tempFile)
            true
        } catch (e: Exception) {
            CryptoUtils.secureDelete(tempFile)
            throw e
        }
    }

    suspend fun importBackup(context: Context, srcUri: Uri, password: String): Boolean = withContext(Dispatchers.IO) {
        val tempBackupFile = File(context.cacheDir, "kosh_backup_import_temp.db")
        try {
            context.contentResolver.openInputStream(srcUri)?.use { input ->
                tempBackupFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        } catch (e: Exception) {
            throw Exception("Failed to read backup file.")
        }
        
        val dbFile = context.getDatabasePath("kosh_vault.db")
        val tempDecryptedFile = File(context.cacheDir, "kosh_decrypted_temp.db")
        
        val success = CryptoUtils.decryptDatabaseBackup(tempBackupFile, tempDecryptedFile, password)
        CryptoUtils.secureDelete(tempBackupFile)
        
        if (!success) {
            CryptoUtils.secureDelete(tempDecryptedFile)
            throw Exception("Invalid password or corrupted backup.")
        }
        
        try {
            sessionRepository.mergeDatabaseBackup(tempDecryptedFile.absolutePath)
            true
        } catch (e: Exception) {
            throw Exception(e.localizedMessage ?: "Failed to restore database.")
        } finally {
            CryptoUtils.secureDelete(tempDecryptedFile)
        }
    }
}

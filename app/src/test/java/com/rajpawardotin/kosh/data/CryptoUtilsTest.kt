package com.rajpawardotin.kosh.data

import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class CryptoUtilsTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun testDestroyableSecretKeyLifecycle() {
        val originalBytes = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8)
        val keyBytes = originalBytes.clone()
        val key = DestroyableSecretKey(keyBytes, "AES")

        assertEquals("AES", key.algorithm)
        assertEquals("RAW", key.format)
        
        val encoded1 = key.encoded
        assertNotNull(encoded1)
        assertArrayEquals(originalBytes, encoded1)
        
        // Mutating the returned array should not affect the key (cloning test)
        encoded1!![0] = 99
        assertArrayEquals(originalBytes, key.encoded)

        // Clear the key
        key.clear()
        assertNull(key.encoded)
        
        // The original array passed should be zeroed out
        assertArrayEquals(ByteArray(originalBytes.size), keyBytes)
    }

    @Test
    fun testDatabaseBackupEncryptionDecryptionSuccess() {
        val dbFile = tempFolder.newFile("kosh_vault.db")
        val backupFile = tempFolder.newFile("kosh_vault.db.backup")
        val restoredDbFile = File(tempFolder.root, "kosh_vault_restored.db")

        val databaseContent = "Kosh Database Main Content for testing encryption and decryption."
        dbFile.writeText(databaseContent)

        val password = "SuperSecretSecurePassword123"

        // Encrypt backup
        val encryptResult = CryptoUtils.encryptDatabaseBackup(dbFile, backupFile, password)
        assertTrue(encryptResult)
        assertTrue(backupFile.length() > databaseContent.length)

        // Decrypt backup
        val decryptResult = CryptoUtils.decryptDatabaseBackup(backupFile, restoredDbFile, password)
        assertTrue(decryptResult)
        assertEquals(databaseContent, restoredDbFile.readText())
    }

    @Test
    fun testDatabaseBackupDecryptionFailsWithWrongPassword() {
        val dbFile = tempFolder.newFile("kosh_vault.db")
        val backupFile = tempFolder.newFile("kosh_vault.db.backup")
        val restoredDbFile = File(tempFolder.root, "kosh_vault_restored.db")

        val databaseContent = "Sensitive data"
        dbFile.writeText(databaseContent)

        // Encrypt backup
        val encryptResult = CryptoUtils.encryptDatabaseBackup(dbFile, backupFile, "correct_password")
        assertTrue(encryptResult)

        // Decrypt backup with WRONG password
        val decryptResult = CryptoUtils.decryptDatabaseBackup(backupFile, restoredDbFile, "wrong_password")
        assertFalse(decryptResult)
        
        // Ensure no garbage decrypted file was created/written
        assertFalse(restoredDbFile.exists())
    }

    @Test
    fun testDatabaseBackupDecryptionFailsWithTamperedBackup() {
        val dbFile = tempFolder.newFile("kosh_vault.db")
        val backupFile = tempFolder.newFile("kosh_vault.db.backup")
        val restoredDbFile = File(tempFolder.root, "kosh_vault_restored.db")

        val databaseContent = "Sensitive data"
        dbFile.writeText(databaseContent)

        // Encrypt backup
        val encryptResult = CryptoUtils.encryptDatabaseBackup(dbFile, backupFile, "correct_password")
        assertTrue(encryptResult)

        // Tamper with the backup file bytes (flip some bits in ciphertext)
        val backupBytes = backupFile.readBytes()
        if (backupBytes.size > 20) {
            backupBytes[backupBytes.size - 5] = (backupBytes[backupBytes.size - 5] + 1).toByte()
        }
        backupFile.writeBytes(backupBytes)

        // Decrypt backup with correct password but tampered data
        val decryptResult = CryptoUtils.decryptDatabaseBackup(backupFile, restoredDbFile, "correct_password")
        assertFalse(decryptResult)
        
        // Ensure no garbage decrypted file was created/written
        assertFalse(restoredDbFile.exists())
    }
}

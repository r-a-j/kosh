package com.rajpawardotin.kosh.data

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.KeyStore
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

class DestroyableSecretKey(private var keyBytes: ByteArray?, private val algorithm: String = "AES") : SecretKey {
    override fun getAlgorithm(): String = algorithm
    override fun getFormat(): String = "RAW"
    override fun getEncoded(): ByteArray? = keyBytes?.clone()
    
    fun clear() {
        keyBytes?.let { java.util.Arrays.fill(it, 0.toByte()) }
        keyBytes = null
    }
}

object CryptoUtils {

    private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
    private const val BIOMETRIC_KEY_ALIAS = "kosh_biometric_key"
    private const val ALGORITHM_AES = "AES"
    private const val AES_KEY_SIZE = 256
    private const val GCM_IV_LENGTH = 12
    private const val GCM_TAG_LENGTH = 128
    
    // PBKDF2 Settings
    private const val PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val PBKDF2_ITERATIONS = 310000
    private const val PBKDF2_SALT_LENGTH = 16

    // Secure Random Generator
    private val secureRandom = SecureRandom()

    fun generateSalt(): ByteArray {
        val salt = ByteArray(PBKDF2_SALT_LENGTH)
        secureRandom.nextBytes(salt)
        return salt
    }

    fun generateRandomSessionKey(): SecretKey {
        val keyBytes = ByteArray(32) // 256-bit
        secureRandom.nextBytes(keyBytes)
        return DestroyableSecretKey(keyBytes, ALGORITHM_AES)
    }

    fun deriveKeyFromPassword(password: String, salt: ByteArray): SecretKey {
        val spec = PBEKeySpec(password.toCharArray(), salt, PBKDF2_ITERATIONS, AES_KEY_SIZE)
        val factory = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM)
        val derived = factory.generateSecret(spec).encoded
        return DestroyableSecretKey(derived, ALGORITHM_AES)
    }

    // Message Encryption (AES-GCM)
    fun encryptMessage(text: String, secretKey: SecretKey): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val iv = ByteArray(GCM_IV_LENGTH)
        secureRandom.nextBytes(iv)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec)
        
        val ciphertext = cipher.doFinal(text.toByteArray(Charsets.UTF_8))
        
        // Output format: IV + Ciphertext
        val combined = ByteArray(iv.size + ciphertext.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(ciphertext, 0, combined, iv.size, ciphertext.size)
        
        return Base64.getEncoder().encodeToString(combined)
    }

    fun decryptMessage(encryptedBase64: String, secretKey: SecretKey): String {
        val combined = Base64.getDecoder().decode(encryptedBase64)
        if (combined.size < GCM_IV_LENGTH) {
            throw IllegalArgumentException("Encrypted text too short")
        }
        
        val iv = ByteArray(GCM_IV_LENGTH)
        System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH)
        
        val ciphertext = ByteArray(combined.size - GCM_IV_LENGTH)
        System.arraycopy(combined, GCM_IV_LENGTH, ciphertext, 0, ciphertext.size)
        
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
        
        val decryptedBytes = cipher.doFinal(ciphertext)
        return String(decryptedBytes, Charsets.UTF_8)
    }

    // Keystore Biometric Key Setup
    fun getOrCreateBiometricKey() {
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
        if (!keyStore.containsAlias(BIOMETRIC_KEY_ALIAS)) {
            val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER)
            val spec = KeyGenParameterSpec.Builder(
                BIOMETRIC_KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(AES_KEY_SIZE)
                .setUserAuthenticationRequired(true)
                .setInvalidatedByBiometricEnrollment(true)
                .setUserAuthenticationValidityDurationSeconds(-1)
                .setUnlockedDeviceRequired(true)
                .build()
            keyGenerator.init(spec)
            keyGenerator.generateKey()
        }
    }

    fun getBiometricCipherForEncryption(): Cipher {
        getOrCreateBiometricKey()
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
        val key = keyStore.getKey(BIOMETRIC_KEY_ALIAS, null) as SecretKey
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key)
        return cipher
    }

    fun getBiometricCipherForDecryption(iv: ByteArray): Cipher {
        getOrCreateBiometricKey()
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
        val key = keyStore.getKey(BIOMETRIC_KEY_ALIAS, null) as SecretKey
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.DECRYPT_MODE, key, spec)
        return cipher
    }

    fun wrapSessionKey(sessionKey: SecretKey, cipher: Cipher): String {
        val iv = cipher.iv
        val wrappedBytes = cipher.doFinal(sessionKey.encoded)
        
        val combined = ByteArray(iv.size + wrappedBytes.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(wrappedBytes, 0, combined, iv.size, wrappedBytes.size)
        
        return Base64.getEncoder().encodeToString(combined)
    }

    fun unwrapSessionKey(wrappedBase64: String, cipher: Cipher): SecretKey {
        val combined = Base64.getDecoder().decode(wrappedBase64)
        val ciphertext = ByteArray(combined.size - GCM_IV_LENGTH)
        System.arraycopy(combined, GCM_IV_LENGTH, ciphertext, 0, ciphertext.size)
        
        val unwrappedBytes = cipher.doFinal(ciphertext)
        return DestroyableSecretKey(unwrappedBytes, ALGORITHM_AES)
    }

    // Database File Encryption for Backups (Direct memory-based AES-GCM)
    fun encryptDatabaseBackup(dbFile: File, backupFile: File, password: String): Boolean {
        return try {
            val salt = generateSalt()
            val secretKey = deriveKeyFromPassword(password, salt)
            
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val iv = ByteArray(GCM_IV_LENGTH)
            secureRandom.nextBytes(iv)
            val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec)

            val plaintext = dbFile.readBytes()
            val ciphertext = cipher.doFinal(plaintext)
            
            FileOutputStream(backupFile).use { fos ->
                fos.write(salt)
                fos.write(iv)
                fos.write(ciphertext)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun decryptDatabaseBackup(backupFile: File, dbFile: File, password: String): Boolean {
        return try {
            val backupBytes = backupFile.readBytes()
            if (backupBytes.size < PBKDF2_SALT_LENGTH + GCM_IV_LENGTH) {
                return false
            }
            
            val salt = ByteArray(PBKDF2_SALT_LENGTH)
            System.arraycopy(backupBytes, 0, salt, 0, PBKDF2_SALT_LENGTH)
            
            val iv = ByteArray(GCM_IV_LENGTH)
            System.arraycopy(backupBytes, PBKDF2_SALT_LENGTH, iv, 0, GCM_IV_LENGTH)
            
            val ciphertext = ByteArray(backupBytes.size - PBKDF2_SALT_LENGTH - GCM_IV_LENGTH)
            System.arraycopy(backupBytes, PBKDF2_SALT_LENGTH + GCM_IV_LENGTH, ciphertext, 0, ciphertext.size)
            
            val secretKey = deriveKeyFromPassword(password, salt)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
            
            val decrypted = cipher.doFinal(ciphertext)
            dbFile.writeBytes(decrypted)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun secureDelete(file: File) {
        if (!file.exists()) return
        val length = file.length()
        if (length <= 0) {
            file.delete()
            return
        }
        val random = SecureRandom()
        val buf = ByteArray(4096)
        try {
            FileOutputStream(file).use { fos ->
                var remaining = length
                while (remaining > 0) {
                    random.nextBytes(buf)
                    val chunk = minOf(buf.size.toLong(), remaining).toInt()
                    fos.write(buf, 0, chunk)
                    remaining -= chunk
                }
                fos.flush()
                fos.fd.sync()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            file.delete()
        }
    }
}

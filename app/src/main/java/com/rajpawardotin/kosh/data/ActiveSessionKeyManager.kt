package com.rajpawardotin.kosh.data

import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshots.SnapshotStateMap
import javax.crypto.SecretKey

class ActiveSessionKeyManager {
    val activeSessionKeys: SnapshotStateMap<String, SecretKey> = mutableStateMapOf()

    fun destroyKey(key: SecretKey) {
        if (key is DestroyableSecretKey) {
            key.clear()
        } else {
            try {
                val keyField = key.javaClass.getDeclaredField("key")
                keyField.isAccessible = true
                val keyBytes = keyField.get(key) as? ByteArray
                if (keyBytes != null) {
                    java.util.Arrays.fill(keyBytes, 0.toByte())
                }
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    fun clearActiveSessionKeys() {
        for (key in activeSessionKeys.values) {
            destroyKey(key)
        }
        activeSessionKeys.clear()
    }

    fun get(sessionId: String): SecretKey? = activeSessionKeys[sessionId]

    fun put(sessionId: String, key: SecretKey) {
        activeSessionKeys[sessionId] = key
    }

    fun remove(sessionId: String): SecretKey? {
        val key = activeSessionKeys.remove(sessionId)
        if (key != null) {
            destroyKey(key)
        }
        return key
    }

    fun containsKey(sessionId: String): Boolean = activeSessionKeys.containsKey(sessionId)
}

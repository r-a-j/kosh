package com.rajpawardotin.kosh.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.rajpawardotin.kosh.domain.provider.SettingsProvider

import android.content.SharedPreferences

class SharedPrefsSettingsProvider(private val context: Context) : SettingsProvider {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs: SharedPreferences by lazy {
        try {
            EncryptedSharedPreferences.create(
                context,
                "neural_core_prefs",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            // Delete corrupted prefs and recreate
            context.getSharedPreferences("neural_core_prefs", Context.MODE_PRIVATE)
                .edit().clear().commit()
            val file = java.io.File(context.filesDir.parentFile, "shared_prefs/neural_core_prefs.xml")
            if (file.exists()) {
                file.delete()
            }
            EncryptedSharedPreferences.create(
                context,
                "neural_core_prefs",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        }
    }

    override fun getString(key: String, defaultValue: String): String {
        return prefs.getString(key, defaultValue) ?: defaultValue
    }

    override fun putString(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }

    override fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        return prefs.getBoolean(key, defaultValue)
    }

    override fun putBoolean(key: String, value: Boolean) {
        prefs.edit().putBoolean(key, value).apply()
    }

    override fun commitBoolean(key: String, value: Boolean) {
        prefs.edit().putBoolean(key, value).commit()
    }
}

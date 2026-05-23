package com.rajpawardotin.kosh.data

import android.content.Context
import com.rajpawardotin.kosh.domain.provider.SettingsProvider

class SharedPrefsSettingsProvider(context: Context) : SettingsProvider {
    private val prefs = context.getSharedPreferences("neural_core_prefs", Context.MODE_PRIVATE)

    override fun getString(key: String, defaultValue: String): String {
        return prefs.getString(key, defaultValue) ?: defaultValue
    }

    override fun putString(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }
}

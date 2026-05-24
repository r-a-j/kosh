package com.rajpawardotin.kosh.ui.chat

import com.rajpawardotin.kosh.domain.provider.SettingsProvider

class FakeSettingsProvider : SettingsProvider {
    private val settings = mutableMapOf<String, Any>()

    override fun getString(key: String, defaultValue: String): String {
        return settings[key] as? String ?: defaultValue
    }

    override fun putString(key: String, value: String) {
        settings[key] = value
    }

    override fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        return settings[key] as? Boolean ?: defaultValue
    }

    override fun putBoolean(key: String, value: Boolean) {
        settings[key] = value
    }

    override fun commitBoolean(key: String, value: Boolean) {
        settings[key] = value
    }
}

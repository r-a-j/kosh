package com.rajpawardotin.kosh.ui.chat

import com.rajpawardotin.kosh.domain.provider.SettingsProvider

class FakeSettingsProvider : SettingsProvider {
    private val map = mutableMapOf<String, String>()
    override fun getString(key: String, defaultValue: String): String {
        return map[key] ?: defaultValue
    }
    override fun putString(key: String, value: String) {
        map[key] = value
    }
}

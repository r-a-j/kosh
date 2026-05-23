package com.rajpawardotin.kosh.domain.provider

interface SettingsProvider {
    fun getString(key: String, defaultValue: String): String
    fun putString(key: String, value: String)
}

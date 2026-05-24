package com.rajpawardotin.kosh.domain.provider

interface SettingsProvider {
    fun getString(key: String, defaultValue: String): String
    fun putString(key: String, value: String)
    fun getBoolean(key: String, defaultValue: Boolean): Boolean
    fun putBoolean(key: String, value: Boolean)
    fun commitBoolean(key: String, value: Boolean)
}

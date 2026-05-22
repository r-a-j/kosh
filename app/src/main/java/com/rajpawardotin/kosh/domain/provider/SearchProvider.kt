package com.rajpawardotin.kosh.domain.provider

interface SearchProvider {
    suspend fun performSearch(query: String): String
}


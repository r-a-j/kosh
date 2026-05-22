package com.rajpawardotin.kosh.domain.provider

interface SearchProvider {
    suspend fun performSearch(
        query: String,
        searchEngine: String,
        onStatusUpdate: (String) -> Unit = {}
    ): String
}


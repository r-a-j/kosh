package com.rajpawardotin.aicoredemo.domain.provider

interface SearchProvider {
    suspend fun performSearch(query: String): String
}

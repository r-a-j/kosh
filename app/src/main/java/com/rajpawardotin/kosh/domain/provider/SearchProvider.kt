package com.rajpawardotin.kosh.domain.provider

data class SearchSource(
    val title: String,
    val url: String,
    val snippet: String = "",
    val imageUrl: String? = null,
    val videoUrl: String? = null
)

data class SearchResponse(
    val contextText: String,
    val sources: List<SearchSource>
)

interface SearchProvider {
    suspend fun performSearch(
        query: String,
        searchEngine: String,
        onStatusUpdate: (String) -> Unit = {}
    ): SearchResponse
}


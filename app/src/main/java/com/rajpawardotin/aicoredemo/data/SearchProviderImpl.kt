package com.rajpawardotin.aicoredemo.data

import com.rajpawardotin.aicoredemo.domain.provider.SearchProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import java.net.URLEncoder

class SearchProviderImpl : SearchProvider {
    private val client = OkHttpClient()

    override suspend fun performSearch(query: String): String = withContext(Dispatchers.IO) {
        try {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val url = "https://html.duckduckgo.com/html/?q=$encodedQuery"
            
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                .build()

            val response = client.newCall(request).execute()
            val html = response.body?.string() ?: return@withContext "No results found."
            
            val doc = Jsoup.parse(html)
            val results = doc.select(".result")
            
            if (results.isEmpty()) return@withContext "No results found."

            val contextBuilder = StringBuilder("INTERNET SEARCH RESULTS FOR \"$query\":\n\n")
            
            results.take(5).forEachIndexed { index, element ->
                val title = element.select(".result__title").text()
                val snippet = element.select(".result__snippet").text()
                val link = element.select(".result__url").text()
                
                contextBuilder.append("${index + 1}. $title\n")
                contextBuilder.append("Snippet: $snippet\n")
                contextBuilder.append("Source: $link\n\n")
            }
            
            contextBuilder.toString()
        } catch (e: Exception) {
            "Error performing search: ${e.localizedMessage}"
        }
    }
}

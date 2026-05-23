package com.rajpawardotin.kosh.data

import android.app.ActivityManager
import android.content.Context
import com.rajpawardotin.kosh.domain.provider.SearchProvider
import kotlinx.coroutines.*
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.jsoup.Jsoup
import java.net.URLDecoder
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

class SearchProviderImpl(private val context: Context) : SearchProvider {
    private val client = OkHttpClient()

    override suspend fun performSearch(
        query: String,
        searchEngine: String,
        onStatusUpdate: (String) -> Unit
    ): String = withContext(Dispatchers.IO) {
        try {
            val urlsInQuery = extractUrls(query)
            val (maxPages, maxChars) = getRAMBasedLimits()
            
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val memoryInfo = ActivityManager.MemoryInfo()
            activityManager.getMemoryInfo(memoryInfo)
            val freeRamGb = memoryInfo.availMem.toDouble() / (1024.0 * 1024.0 * 1024.0)
            onStatusUpdate(String.format("Core RAM: %.1fGB free • Dynamic context optimized", freeRamGb))
            delay(600)
            
            if (urlsInQuery.isNotEmpty()) {
                onStatusUpdate("Direct URL detected. Crawling content...")
                val resultsToCrawl = urlsInQuery.take(maxPages)
                val deferredCrawls = resultsToCrawl.map { url ->
                    async(Dispatchers.IO) {
                        val content = crawlUrl(url, maxChars, onStatusUpdate)
                        url to content
                    }
                }
                val crawled = deferredCrawls.awaitAll()
                val contextBuilder = StringBuilder("DIRECT WEBPAGE CONTENTS LOADED:\n\n")
                crawled.forEachIndexed { index, (url, content) ->
                    contextBuilder.append("URL ${index + 1}: $url\n")
                    contextBuilder.append("Content:\n$content\n\n")
                }
                return@withContext contextBuilder.toString()
            }
            
            onStatusUpdate("Searching $searchEngine...")
            val results = fetchSearchResults(query, searchEngine, onStatusUpdate)
            if (results.isEmpty()) {
                return@withContext "No search results found."
            }
            
            onStatusUpdate("Found ${results.size} matches. Concurrently scraping pages...")
            val resultsToCrawl = results.take(maxPages)
            
            val deferredCrawls = resultsToCrawl.map { res ->
                async(Dispatchers.IO) {
                    val pageContent = crawlUrl(res.link, maxChars, onStatusUpdate)
                    res to pageContent
                }
            }
            
            val crawled = deferredCrawls.awaitAll()
            
            val contextBuilder = StringBuilder("INTERNET SEARCH RESULTS FOR \"$query\":\n\n")
            results.take(5).forEachIndexed { index, res ->
                contextBuilder.append("${index + 1}. ${res.title}\n")
                contextBuilder.append("Snippet: ${res.snippet}\n")
                contextBuilder.append("Source: ${res.link}\n\n")
            }
            
            contextBuilder.append("\nDEEP CRAWL CONTENT INTEGRATED:\n\n")
            crawled.forEachIndexed { index, (res, content) ->
                contextBuilder.append("--- PAGE ${index + 1}: ${res.title} (${res.link}) ---\n")
                contextBuilder.append("$content\n\n")
            }
            
            contextBuilder.toString()
        } catch (e: Exception) {
            "Error performing search: ${e.localizedMessage}"
        }
    }

    private fun getRAMBasedLimits(): Pair<Int, Int> {
        return try {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val memoryInfo = ActivityManager.MemoryInfo()
            activityManager.getMemoryInfo(memoryInfo)
            val freeRamGb = memoryInfo.availMem.toDouble() / (1024.0 * 1024.0 * 1024.0)
            
            when {
                freeRamGb > 6.0 -> Pair(5, 6000)
                freeRamGb > 3.0 -> Pair(3, 4000)
                else -> Pair(2, 2000)
            }
        } catch (e: Exception) {
            Pair(2, 2000)
        }
    }

    private fun extractUrls(text: String): List<String> {
        val regex = "(https?://[\\w\\d:#@%/;$()~_?\\+-=\\\\.&]+)".toRegex()
        return regex.findAll(text).map { it.value }.toList()
    }

    private fun extractDdgLink(rawLink: String): String {
        return try {
            if (rawLink.contains("uddg=")) {
                val encodedUrl = rawLink.substringAfter("uddg=").substringBefore("&")
                URLDecoder.decode(encodedUrl, "UTF-8")
            } else if (rawLink.startsWith("//")) {
                "https:$rawLink"
            } else {
                rawLink
            }
        } catch (e: Exception) {
            rawLink
        }
    }

    private fun fetchSearchResults(
        query: String,
        searchEngine: String,
        onStatusUpdate: (String) -> Unit
    ): List<SearchResult> {
        val prefs = context.getSharedPreferences("neural_core_prefs", Context.MODE_PRIVATE)
        val tavilyKey = prefs.getString("tavily_api_key", "") ?: ""
        val braveKey = prefs.getString("brave_api_key", "") ?: ""

        when (searchEngine) {
            "Tavily API" -> {
                if (tavilyKey.isBlank()) {
                    onStatusUpdate("Tavily API Key missing. Trying free fallback...")
                    return fetchSearchResultsWithFallback(query, onStatusUpdate)
                }
                try {
                    onStatusUpdate("Querying Tavily API...")
                    val searchResult = queryTavilyApi(query, tavilyKey)
                    if (searchResult.isNotEmpty()) return searchResult
                    onStatusUpdate("Tavily returned no results. Trying fallbacks...")
                } catch (e: Exception) {
                    onStatusUpdate("Tavily API error: ${e.localizedMessage}. Trying fallbacks...")
                }
                return fetchSearchResultsWithFallback(query, onStatusUpdate)
            }
            "Brave Search API" -> {
                if (braveKey.isBlank()) {
                    onStatusUpdate("Brave Search API Key missing. Trying free fallback...")
                    return fetchSearchResultsWithFallback(query, onStatusUpdate)
                }
                try {
                    onStatusUpdate("Querying Brave Search API...")
                    val searchResult = queryBraveSearchApi(query, braveKey)
                    if (searchResult.isNotEmpty()) return searchResult
                    onStatusUpdate("Brave returned no results. Trying fallbacks...")
                } catch (e: Exception) {
                    onStatusUpdate("Brave API error: ${e.localizedMessage}. Trying fallbacks...")
                }
                return fetchSearchResultsWithFallback(query, onStatusUpdate)
            }
            "DuckDuckGo (Free)" -> {
                try {
                    onStatusUpdate("Searching DuckDuckGo...")
                    val searchResult = queryDuckDuckGoVqd(query)
                    if (searchResult.isNotEmpty()) return searchResult
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                onStatusUpdate("DDG (Free) failed. Trying legacy HTML scraper...")
                return fetchSearchResultsWithFallback(query, onStatusUpdate)
            }
            "Google Scraper" -> {
                try {
                    onStatusUpdate("Querying Google Scraper...")
                    val googleResult = queryGoogleScraper(query)
                    if (googleResult.isNotEmpty()) return googleResult
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                onStatusUpdate("Google Scraper failed. Trying fallbacks...")
                return fetchSearchResultsWithFallback(query, onStatusUpdate)
            }
            "Bing Scraper" -> {
                try {
                    onStatusUpdate("Querying Bing Scraper...")
                    val bingResult = queryBingScraper(query)
                    if (bingResult.isNotEmpty()) return bingResult
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                onStatusUpdate("Bing Scraper failed. Trying fallbacks...")
                return fetchSearchResultsWithFallback(query, onStatusUpdate)
            }
            "DuckDuckGo Lite" -> {
                try {
                    onStatusUpdate("Querying DDG Lite Scraper...")
                    val ddgLiteResult = queryDuckDuckGoLite(query)
                    if (ddgLiteResult.isNotEmpty()) return ddgLiteResult
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                onStatusUpdate("DDG Lite failed. Trying fallbacks...")
                return fetchSearchResultsWithFallback(query, onStatusUpdate)
            }
            else -> {
                return fetchSearchResultsWithFallback(query, onStatusUpdate)
            }
        }
    }

    private fun fetchSearchResultsWithFallback(
        query: String,
        onStatusUpdate: (String) -> Unit
    ): List<SearchResult> {
        // Try VQD first
        try {
            onStatusUpdate("Trying DuckDuckGo (Free)...")
            val res = queryDuckDuckGoVqd(query)
            if (res.isNotEmpty()) return res
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Try DuckDuckGo HTML
        try {
            onStatusUpdate("Trying DuckDuckGo HTML Scraper...")
            val res = queryDuckDuckGoHtml(query)
            if (res.isNotEmpty()) return res
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Try DuckDuckGo Lite
        try {
            onStatusUpdate("Trying DuckDuckGo Lite...")
            val res = queryDuckDuckGoLite(query)
            if (res.isNotEmpty()) return res
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Try Bing Scraper
        try {
            onStatusUpdate("Trying Bing Scraper...")
            val res = queryBingScraper(query)
            if (res.isNotEmpty()) return res
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Try Google Scraper
        try {
            onStatusUpdate("Trying Google Scraper...")
            val res = queryGoogleScraper(query)
            if (res.isNotEmpty()) return res
        } catch (e: Exception) {
            e.printStackTrace()
        }

        onStatusUpdate("All free engines failed/blocked.")
        return emptyList()
    }

    private fun queryDuckDuckGoVqd(query: String): List<SearchResult> {
        val results = mutableListOf<SearchResult>()
        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        
        // Step 1: Bootstrap VQD Token
        val bootstrapUrl = "https://duckduckgo.com/?q=$encodedQuery"
        val bootstrapRequest = Request.Builder()
            .url(bootstrapUrl)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            .build()
        
        val bootstrapResponse = client.newCall(bootstrapRequest).execute()
        val bootstrapHtml = bootstrapResponse.body?.string() ?: return results
        
        val vqdRegex = """vqd\s*=\s*['"]([^'"]+)['"]""".toRegex()
        val matchResult = vqdRegex.find(bootstrapHtml) ?: return results
        val vqdToken = matchResult.groupValues[1]
        
        // Step 2: Query links.duckduckgo.com/d.js
        val djsUrl = "https://links.duckduckgo.com/d.js?q=$encodedQuery&vqd=$vqdToken&s=0&o=json&api=d.js"
        val djsRequest = Request.Builder()
            .url(djsUrl)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            .header("Referer", "https://duckduckgo.com/")
            .build()
            
        val djsResponse = client.newCall(djsRequest).execute()
        val djsBody = djsResponse.body?.string() ?: return results
        
        val startIdx = djsBody.indexOf('[')
        val endIdx = djsBody.lastIndexOf(']')
        if (startIdx != -1 && endIdx != -1 && endIdx > startIdx) {
            val jsonArrayString = djsBody.substring(startIdx, endIdx + 1)
            val jsonArray = org.json.JSONArray(jsonArrayString)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val title = obj.optString("t", "")
                val rawLink = obj.optString("u", "")
                val snippet = obj.optString("a", "")
                val link = extractDdgLink(rawLink)
                if (title.isNotEmpty() && link.startsWith("http")) {
                    results.add(SearchResult(title, link, snippet))
                }
            }
        }
        return results
    }

    private fun queryTavilyApi(query: String, apiKey: String): List<SearchResult> {
        val results = mutableListOf<SearchResult>()
        
        val jsonRequest = org.json.JSONObject().apply {
            put("api_key", apiKey)
            put("query", query)
            put("search_depth", "basic")
            put("include_answer", false)
            put("max_results", 5)
        }
        
        val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
        val requestBody = jsonRequest.toString().toRequestBody(mediaType)
        
        val request = Request.Builder()
            .url("https://api.tavily.com/search")
            .post(requestBody)
            .build()
            
        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            throw Exception("HTTP error code: ${response.code}")
        }
        val responseBody = response.body?.string() ?: return results
        val jsonResponse = org.json.JSONObject(responseBody)
        if (jsonResponse.has("results")) {
            val resultsArray = jsonResponse.getJSONArray("results")
            for (i in 0 until resultsArray.length()) {
                val obj = resultsArray.getJSONObject(i)
                val title = obj.optString("title", "")
                val link = obj.optString("url", "")
                val snippet = obj.optString("content", "")
                if (title.isNotEmpty() && link.startsWith("http")) {
                    results.add(SearchResult(title, link, snippet))
                }
            }
        }
        return results
    }

    private fun queryBraveSearchApi(query: String, apiKey: String): List<SearchResult> {
        val results = mutableListOf<SearchResult>()
        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        val url = "https://api.search.brave.com/res/v1/web/search?q=$encodedQuery"
        
        val request = Request.Builder()
            .url(url)
            .header("X-Subscription-Token", apiKey)
            .header("Accept", "application/json")
            .build()
            
        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            throw Exception("HTTP error code: ${response.code}")
        }
        val responseBody = response.body?.string() ?: return results
        val jsonResponse = org.json.JSONObject(responseBody)
        if (jsonResponse.has("web")) {
            val web = jsonResponse.getJSONObject("web")
            if (web.has("results")) {
                val resultsArray = web.getJSONArray("results")
                for (i in 0 until resultsArray.length()) {
                    val obj = resultsArray.getJSONObject(i)
                    val title = obj.optString("title", "")
                    val link = obj.optString("url", "")
                    val snippet = obj.optString("description", "")
                    if (title.isNotEmpty() && link.startsWith("http")) {
                        results.add(SearchResult(title, link, snippet))
                    }
                }
            }
        }
        return results
    }

    private fun queryDuckDuckGoHtml(query: String): List<SearchResult> {
        val results = mutableListOf<SearchResult>()
        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        val url = "https://html.duckduckgo.com/html/?q=$encodedQuery"
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            .build()
        val response = client.newCall(request).execute()
        val html = response.body?.string() ?: return results
        val doc = Jsoup.parse(html)
        doc.select(".result").forEach { element ->
            val titleLink = element.select("a.result__a").firstOrNull() ?: return@forEach
            val title = titleLink.text()
            val rawLink = titleLink.attr("href")
            val link = extractDdgLink(rawLink)
            val snippet = element.select(".result__snippet").text()
            if (title.isNotEmpty() && link.startsWith("http")) {
                results.add(SearchResult(title, link, snippet))
            }
        }
        return results
    }

    private fun queryDuckDuckGoLite(query: String): List<SearchResult> {
        val results = mutableListOf<SearchResult>()
        val formBody = FormBody.Builder()
            .add("q", query)
            .build()
        val request = Request.Builder()
            .url("https://lite.duckduckgo.com/lite/")
            .post(formBody)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            .build()
        val response = client.newCall(request).execute()
        val html = response.body?.string() ?: return results
        val doc = Jsoup.parse(html)
        val tdLinks = doc.select("td.result-link")
        tdLinks.forEach { td ->
            val a = td.select("a").firstOrNull() ?: return@forEach
            val title = a.text()
            val rawLink = a.attr("href")
            val link = extractDdgLink(rawLink)
            val tr = td.parent()
            val nextTr = tr?.nextElementSibling()
            val snippet = nextTr?.select("td.result-snippet")?.text() ?: ""
            if (title.isNotEmpty() && link.startsWith("http")) {
                results.add(SearchResult(title, link, snippet))
            }
        }
        return results
    }

    private fun queryGoogleScraper(query: String): List<SearchResult> {
        val results = mutableListOf<SearchResult>()
        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        val url = "https://www.google.com/search?q=$encodedQuery"
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            .build()
        val response = client.newCall(request).execute()
        val html = response.body?.string() ?: return results
        val doc = Jsoup.parse(html)
        val googleG = doc.select(".g")
        if (googleG.isNotEmpty()) {
            googleG.forEach { element ->
                val a = element.select("a[href]").firstOrNull() ?: return@forEach
                val link = a.attr("href")
                val title = element.select("h3").text()
                val snippet = element.select("div[style*=-webkit-line-clamp], .VwiC3b").text()
                if (title.isNotEmpty() && link.startsWith("http")) {
                    results.add(SearchResult(title, link, snippet))
                }
            }
        } else {
            doc.select("a[href]").forEach { a ->
                val href = a.attr("href")
                if (href.startsWith("/url?q=")) {
                    val actualUrl = href.substringAfter("/url?q=").substringBefore("&")
                    val decodedUrl = URLDecoder.decode(actualUrl, "UTF-8")
                    val title = a.select("h3").text()
                    val parent = a.parent()?.parent()?.parent()
                    val snippet = parent?.select(".VwiC3b, .yD755d, .BNeawe")?.text() ?: ""
                    if (title.isNotEmpty() && decodedUrl.startsWith("http")) {
                        results.add(SearchResult(title, decodedUrl, snippet))
                    }
                }
            }
        }
        return results
    }

    private fun queryBingScraper(query: String): List<SearchResult> {
        val results = mutableListOf<SearchResult>()
        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        val url = "https://www.bing.com/search?q=$encodedQuery"
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            .build()
        val response = client.newCall(request).execute()
        val html = response.body?.string() ?: return results
        val doc = Jsoup.parse(html)
        doc.select(".b_algo").forEach { element ->
            val a = element.select("h2 a").firstOrNull() ?: return@forEach
            val link = a.attr("href")
            val title = a.text()
            val snippet = element.select(".b_caption p, .b_snippet").text()
            if (title.isNotEmpty() && link.startsWith("http")) {
                results.add(SearchResult(title, link, snippet))
            }
        }
        return results
    }

    private fun isBannedIp(host: String?): Boolean {
        if (host == null || host.isBlank()) return true
        return try {
            val addresses = java.net.InetAddress.getAllByName(host)
            for (address in addresses) {
                if (address.isLoopbackAddress || 
                    address.isSiteLocalAddress || 
                    address.isLinkLocalAddress || 
                    address.isAnyLocalAddress) {
                    return true
                }
            }
            false
        } catch (e: Exception) {
            true
        }
    }

    private suspend fun crawlUrl(url: String, maxChars: Int, onStatusUpdate: (String) -> Unit): String = withContext(Dispatchers.IO) {
        try {
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                return@withContext "Invalid URL"
            }
            val uri = java.net.URI(url)
            val host = uri.host
            if (host == null || isBannedIp(host)) {
                return@withContext "Access Denied: Local IP blocked"
            }
            val domain = host
            onStatusUpdate("Scraping: $domain...")
            
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .build()
                
            val clientWithTimeout = client.newBuilder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(5, TimeUnit.SECONDS)
                .build()
                
            val response = clientWithTimeout.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext "HTTP error: ${response.code}"
            }
            
            val contentType = response.header("Content-Type") ?: ""
            if (!contentType.contains("text/html") && !contentType.contains("text/plain")) {
                return@withContext "Skipped non-text content"
            }
            
            val html = response.body?.string() ?: return@withContext "No content"
            val doc = Jsoup.parse(html, url)
            
            doc.select("script, style, iframe, footer, header, nav, aside, noscript, form").remove()
            
            val textBuilder = StringBuilder()
            val elements = doc.select("h1, h2, h3, p, li")
            var totalLength = 0
            for (element in elements) {
                val text = element.text().trim()
                if (text.isNotEmpty() && text.length > 20) {
                    textBuilder.append(text).append("\n\n")
                    totalLength += text.length
                    if (totalLength >= maxChars) {
                        break
                    }
                }
            }
            
            val extracted = textBuilder.toString().trim()
            if (extracted.isEmpty()) {
                val bodyText = doc.body().text().trim()
                if (bodyText.length > maxChars) bodyText.substring(0, maxChars) else bodyText
            } else {
                if (extracted.length > maxChars) extracted.substring(0, maxChars) else extracted
            }
        } catch (e: Exception) {
            "Scrape failed: ${e.localizedMessage}"
        }
    }
}

private data class SearchResult(val title: String, val link: String, val snippet: String)

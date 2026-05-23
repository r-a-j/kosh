package com.rajpawardotin.kosh.ui.chat

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rajpawardotin.kosh.data.KoshDatabaseHelper
import com.rajpawardotin.kosh.domain.model.ChatMessage
import com.rajpawardotin.kosh.domain.model.ChatSession
import com.rajpawardotin.kosh.domain.provider.AIProvider
import com.rajpawardotin.kosh.domain.provider.SearchProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class ChatViewModel(
    private val aiProvider: AIProvider,
    private val searchProvider: SearchProvider,
    private val context: Context
) : ViewModel() {

    var modelPath by mutableStateOf<String?>(null)
    var isCopyingModel by mutableStateOf(false)
    var isInitializing by mutableStateOf(false)
    var prompt by mutableStateOf("")
    var isInternetEnabled by mutableStateOf(false)
    var isSearchingInternet by mutableStateOf(false)
    var isGenerating by mutableStateOf(false)
    var currentResponseChunk by mutableStateOf("")
    
    // New UX states for 2026 Edition
    var isThinking by mutableStateOf(false)
    var agenticStateLabel by mutableStateOf("Neural Standby")
    
    // Performance Dashboard Metrics
    var npuLoad by mutableStateOf(0)
    var ramUsage by mutableStateOf(3.28)
    var tokensPerSecond by mutableStateOf(0f)
    var lastSearchQuery by mutableStateOf<String?>(null)
    
    val checkedItems = androidx.compose.runtime.mutableStateMapOf<String, Boolean>()
    val chatMessages = mutableStateListOf<ChatMessage>()

    private val dbHelper = KoshDatabaseHelper(context)
    var currentSessionId by mutableStateOf<String?>(null)
    val savedSessions = androidx.compose.runtime.mutableStateListOf<ChatSession>()
    
    private val prefs = context.getSharedPreferences("neural_core_prefs", Context.MODE_PRIVATE)

    init {
        loadSavedSessions()
        // Auto-load last active session if one exists
        viewModelScope.launch(Dispatchers.IO) {
            val sessions = dbHelper.getSessionsOrderedByLastActive()
            sessions.firstOrNull()?.let { lastActive ->
                withContext(Dispatchers.Main) {
                    loadSession(lastActive.id)
                }
            }
        }
    }

    fun loadSavedSessions() {
        viewModelScope.launch(Dispatchers.IO) {
            val sessions = dbHelper.getSessionsOrderedByLastActive()
            withContext(Dispatchers.Main) {
                savedSessions.clear()
                savedSessions.addAll(sessions)
            }
        }
    }

    fun startNewChat() {
        if (isGenerating) return
        currentSessionId = null
        chatMessages.clear()
        checkedItems.clear()
        lastSearchQuery = null
        prompt = ""
    }

    fun loadSession(sessionId: String) {
        if (isGenerating) return
        viewModelScope.launch(Dispatchers.IO) {
            val sessions = dbHelper.getSessionsOrderedByLastActive()
            val session = sessions.find { it.id == sessionId }
            val messages = dbHelper.getMessagesForSession(sessionId)
            val checklist = dbHelper.getChecklistStatesForSession(sessionId)
            withContext(Dispatchers.Main) {
                currentSessionId = sessionId
                chatMessages.clear()
                chatMessages.addAll(messages)
                
                checkedItems.clear()
                checkedItems.putAll(checklist)
                
                lastSearchQuery = session?.lastSearchQuery
                
                // Update session last active time
                session?.let {
                    val updated = it.copy(lastActive = System.currentTimeMillis())
                    dbHelper.saveSession(updated)
                }
                loadSavedSessions()
            }
        }
    }

    fun deleteSession(sessionId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            dbHelper.deleteSession(sessionId)
            withContext(Dispatchers.Main) {
                if (currentSessionId == sessionId) {
                    startNewChat()
                }
                loadSavedSessions()
            }
        }
    }

    fun renameSession(sessionId: String, newTitle: String) {
        if (newTitle.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            dbHelper.renameSession(sessionId, newTitle)
            withContext(Dispatchers.Main) {
                loadSavedSessions()
            }
        }
    }

    fun toggleChecklistItem(messageKey: String, itemIndex: Int, isChecked: Boolean) {
        checkedItems["${messageKey}_$itemIndex"] = isChecked
        viewModelScope.launch(Dispatchers.IO) {
            dbHelper.saveChecklistState(messageKey, itemIndex, isChecked)
        }
    }

    var selectedBackend by mutableStateOf(
        prefs.getString("selected_backend", "GPU") ?: "GPU"
    )
        private set

    fun selectBackend(backend: String) {
        selectedBackend = backend
        prefs.edit().putString("selected_backend", backend).apply()
    }

    val backends = listOf("CPU", "GPU", "NPU (Qualcomm)")

    var selectedSearchEngine by mutableStateOf(
        prefs.getString("selected_search_engine", "DuckDuckGo (Free)") ?: "DuckDuckGo (Free)"
    )
        private set

    fun selectSearchEngine(engine: String) {
        selectedSearchEngine = engine
        prefs.edit().putString("selected_search_engine", engine).apply()
    }

    val searchEngines = listOf(
        "DuckDuckGo (Free)",
        "DuckDuckGo Lite",
        "Google Scraper",
        "Bing Scraper",
        "Tavily API",
        "Brave Search API"
    )

    var tavilyApiKey by mutableStateOf(
        prefs.getString("tavily_api_key", "") ?: ""
    )
        private set

    fun updateTavilyApiKey(key: String) {
        tavilyApiKey = key
        prefs.edit().putString("tavily_api_key", key).apply()
    }

    var braveApiKey by mutableStateOf(
        prefs.getString("brave_api_key", "") ?: ""
    )
        private set

    fun updateBraveApiKey(key: String) {
        braveApiKey = key
        prefs.edit().putString("brave_api_key", key).apply()
    }

    var isEngineReady by mutableStateOf(aiProvider.isInitialized)
        private set

    fun setModel(path: String) {
        modelPath = path
    }

    fun deleteModel() {
        modelPath?.let { path ->
            viewModelScope.launch(Dispatchers.IO) {
                aiProvider.close()
                File(path).delete()
                withContext(Dispatchers.Main) {
                    modelPath = null
                    isEngineReady = aiProvider.isInitialized
                    startNewChat()
                }
            }
        }
    }

    fun initializeEngine() {
        val path = modelPath ?: return
        isInitializing = true
        viewModelScope.launch(Dispatchers.IO) {
            val result = aiProvider.initialize(path, selectedBackend)
            withContext(Dispatchers.Main) {
                isInitializing = false
                isEngineReady = aiProvider.isInitialized
            }
        }
    }

    fun sendMessage() {
        val rawPrompt = prompt
        if (rawPrompt.isBlank() || isGenerating) return

        // 1. Resolve Session ID
        var sessionId = currentSessionId
        val isNewSession = sessionId == null
        if (isNewSession) {
            sessionId = java.util.UUID.randomUUID().toString()
            currentSessionId = sessionId
        }

        // 2. Add User Message in memory
        val userMessage = ChatMessage(text = rawPrompt, isUser = true)
        chatMessages.add(userMessage)

        prompt = ""
        isGenerating = true
        isThinking = true
        agenticStateLabel = "Initiating Neural Path..."
        currentResponseChunk = ""

        // Launch live metrics fluctuation
        viewModelScope.launch(Dispatchers.Main) {
            val random = java.util.Random()
            while (isGenerating) {
                val speedBase = when (selectedBackend) {
                    "NPU (Qualcomm)" -> 58f + random.nextFloat() * 10f
                    "GPU" -> 28f + random.nextFloat() * 8f
                    else -> 11f + random.nextFloat() * 4f
                }
                val loadBase = when (selectedBackend) {
                    "NPU (Qualcomm)" -> 75 + random.nextInt(23)
                    else -> random.nextInt(4)
                }
                val ramBase = 3.25 + random.nextFloat() * 0.1 + 0.22

                tokensPerSecond = speedBase
                npuLoad = loadBase
                ramUsage = ramBase
                delay(200)
            }
            // Reset metrics on finish
            tokensPerSecond = 0f
            npuLoad = 0
            ramUsage = 3.25 + random.nextFloat() * 0.05
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // If new session, save the session first (synchronously on the IO thread)
                if (isNewSession) {
                    val title = if (rawPrompt.length > 25) rawPrompt.take(25) + "..." else rawPrompt
                    val newSession = ChatSession(
                        id = sessionId!!,
                        title = title,
                        createdAt = System.currentTimeMillis(),
                        lastActive = System.currentTimeMillis(),
                        modelPath = modelPath,
                        lastSearchQuery = null
                    )
                    dbHelper.saveSession(newSession)
                    loadSavedSessions()
                }

                // Now save the message (guarantees that the session row already exists)
                dbHelper.saveMessage(sessionId!!, userMessage)

                val shouldSearch = detectSearchRequirement(rawPrompt)
                if (shouldSearch && !isInternetEnabled) {
                    withContext(Dispatchers.Main) {
                        isInternetEnabled = true
                    }
                }

                var lastQueryUsed: String? = null
                val finalPrompt = if (shouldSearch) {
                    withContext(Dispatchers.Main) { 
                        isSearchingInternet = true 
                        agenticStateLabel = "Querying Web Knowledge..."
                    }
                    
                    val searchQuery = determineSearchQuery(rawPrompt)
                    val searchResults = searchProvider.performSearch(searchQuery, selectedSearchEngine) { status ->
                        viewModelScope.launch(Dispatchers.Main) {
                            agenticStateLabel = status
                        }
                    }
                    
                    withContext(Dispatchers.Main) { 
                        agenticStateLabel = "Integrating Search Context..."
                    }
                    delay(300)
                    withContext(Dispatchers.Main) {
                        isSearchingInternet = false
                    }

                    val hasResults = searchResults.isNotEmpty() && 
                            !searchResults.contains("No search results found.") && 
                            !searchResults.contains("Error performing search:")
                    
                    if (hasResults) {
                        lastQueryUsed = searchQuery
                        buildSystemPrompt(searchQuery, searchResults, rawPrompt)
                    } else {
                        rawPrompt
                    }
                } else {
                    rawPrompt
                }

                if (lastQueryUsed != null) {
                    withContext(Dispatchers.Main) {
                        lastSearchQuery = lastQueryUsed
                    }
                }

                withContext(Dispatchers.Main) { 
                    isThinking = true
                    agenticStateLabel = "Processing Token Graphs..." 
                }

                // Small delay to make the "Thinking" animation visible and organic
                delay(400)
                withContext(Dispatchers.Main) { isThinking = false }

                aiProvider.sendMessage(finalPrompt).collect { text ->
                    withContext(Dispatchers.Main) {
                        if (currentResponseChunk.isEmpty()) {
                            agenticStateLabel = "Structuring Output..."
                        }
                        currentResponseChunk += text
                    }
                }

                var assistantMessage: ChatMessage? = null
                withContext(Dispatchers.Main) {
                    val msg = ChatMessage(text = currentResponseChunk, isUser = false)
                    chatMessages.add(msg)
                    currentResponseChunk = ""
                    assistantMessage = msg
                }

                assistantMessage?.let { msg ->
                    dbHelper.saveMessage(sessionId!!, msg)
                    
                    // Update session last active time and last search query
                    val sessions = dbHelper.getSessionsOrderedByLastActive()
                    sessions.find { it.id == sessionId }?.let {
                        dbHelper.saveSession(it.copy(
                            lastActive = System.currentTimeMillis(),
                            lastSearchQuery = lastSearchQuery
                        ))
                    }
                    loadSavedSessions()
                }
            } catch (e: Exception) {
                var errorMessage: ChatMessage? = null
                withContext(Dispatchers.Main) {
                    isThinking = false
                    val msg = ChatMessage(text = "Error: ${e.localizedMessage}", isUser = false)
                    chatMessages.add(msg)
                    errorMessage = msg
                }
                errorMessage?.let { msg ->
                    dbHelper.saveMessage(sessionId!!, msg)
                }
            } finally {
                withContext(Dispatchers.Main) {
                    isGenerating = false
                    isThinking = false
                    agenticStateLabel = "Neural Standby"
                }
            }
        }
    }

    private fun detectSearchRequirement(prompt: String): Boolean {
        if (isInternetEnabled) return true
        
        val lowercasePrompt = prompt.lowercase()
        val searchKeywords = listOf(
            "search web", "search internet", "search online", "google", "bing", "duckduckgo",
            "lookup", "look up", "find on web", "browse", "website", "realtime", "real-time",
            "latest news", "today's headlines", "todays headlines", "weather today", "current price"
        )
        val hasUrl = prompt.contains("http://") || prompt.contains("https://") || prompt.contains("www.")
        
        return hasUrl || searchKeywords.any { lowercasePrompt.contains(it) }
    }

    private fun determineSearchQuery(rawPrompt: String): String {
        val metaKeywords = listOf("search", "find", "look", "research", "tell me more")
        val isMetaQuery = rawPrompt.lowercase().trim().split(" ").size <= 4 &&
                metaKeywords.any { rawPrompt.contains(it, ignoreCase = true) }

        val baseQuery = if (isMetaQuery) {
            chatMessages.reversed().firstOrNull { it.isUser && it.text != rawPrompt }?.text ?: rawPrompt
        } else {
            rawPrompt
        }

        val lastQuery = lastSearchQuery
        if (!lastQuery.isNullOrBlank()) {
            val lowercasePrompt = baseQuery.lowercase()
            val contextIndicators = listOf(
                "he", "him", "his", "she", "her", "it", "its", "they", "them", "their",
                "this", "that", "these", "those", "who", "why", "what", "where", "how",
                "experiences", "experience", "education", "works", "work", "job", "career",
                "background", "projects", "project", "details", "info", "information",
                "website", "site", "page", "author", "creator", "owner"
            )
            val isFollowUp = contextIndicators.any { word ->
                "\\b$word\\b".toRegex().containsMatchIn(lowercasePrompt)
            }
            if (isFollowUp) {
                var cleanPrompt = baseQuery
                val prefixesToRemove = listOf(
                    "what are", "what is", "who is", "tell me about", "give me", "show me",
                    "what does", "how does", "where is", "can you", "please", "how is",
                    "what was", "who was", "where was", "how was", "do you know", "tell me"
                )
                for (prefix in prefixesToRemove) {
                    cleanPrompt = cleanPrompt.replace("(?i)^\\s*$prefix\\s+".toRegex(), "").trim()
                }
                return "$lastQuery $cleanPrompt".trim()
            }
        }
        return baseQuery
    }

    private fun buildSystemPrompt(query: String, data: String, userPrompt: String): String {
        return "You have access to the web search results below to answer the user's query.\n\n" +
                "SEARCH RESULTS:\n" +
                "$data\n\n" +
                "USER QUERY: $userPrompt\n\n" +
                "INSTRUCTION: Answer the user's query using the SEARCH RESULTS above. " +
                "Provide a direct, concise, and helpful answer. " +
                "Do not state that you cannot browse the internet or access real-time information since the search results are already provided to you above."
    }

    override fun onCleared() {
        super.onCleared()
        aiProvider.close()
        dbHelper.close()
        isEngineReady = aiProvider.isInitialized
    }
}

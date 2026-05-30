package com.rajpawardotin.kosh.domain.usecase

import com.rajpawardotin.kosh.domain.provider.AIProvider
import com.rajpawardotin.kosh.domain.provider.SearchProvider
import com.rajpawardotin.kosh.domain.repository.SessionRepository
import com.rajpawardotin.kosh.domain.repository.DocumentRepository
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock

class LlmUseCaseTest {

    private lateinit var llmUseCase: LlmUseCase

    @Before
    fun setUp() {
        val mockAiProvider = mock<AIProvider>()
        val mockSearchProvider = mock<SearchProvider>()
        val mockSessionRepository = mock<SessionRepository>()
        val mockDocumentRepository = mock<DocumentRepository>()
        
        llmUseCase = LlmUseCase(
            mockAiProvider,
            mockSearchProvider,
            mockSessionRepository,
            mockDocumentRepository
        )
    }

    @Test
    fun testDetectSearchRequirementWhenInternetDisabledReturnsFalse() {
        // Even with search keywords or URLs, if internet is disabled, search requirement should be false.
        assertFalse(llmUseCase.detectSearchRequirement("google today's weather", isInternetEnabled = false))
        assertFalse(llmUseCase.detectSearchRequirement("https://example.com info", isInternetEnabled = false))
        assertFalse(llmUseCase.detectSearchRequirement("hello how are you", isInternetEnabled = false))
    }

    @Test
    fun testDetectSearchRequirementWhenInternetEnabledAndKeywordsMatchReturnsTrue() {
        // If internet is enabled and keywords match, should be true
        assertTrue(llmUseCase.detectSearchRequirement("google today's weather", isInternetEnabled = true))
        assertTrue(llmUseCase.detectSearchRequirement("what is the latest news", isInternetEnabled = true))
        assertTrue(llmUseCase.detectSearchRequirement("search online for Kosh Android", isInternetEnabled = true))
    }

    @Test
    fun testDetectSearchRequirementWhenInternetEnabledAndUrlPresentReturnsTrue() {
        // If internet is enabled and URL is present, should be true
        assertTrue(llmUseCase.detectSearchRequirement("check out http://example.com", isInternetEnabled = true))
        assertTrue(llmUseCase.detectSearchRequirement("www.github.com features", isInternetEnabled = true))
    }

    @Test
    fun testDetectSearchRequirementWhenInternetEnabledButNoKeywordsMatchReturnsFalse() {
        // If internet is enabled but no keywords or URLs are present, should be false (greetings, general queries)
        assertFalse(llmUseCase.detectSearchRequirement("hello, who are you?", isInternetEnabled = true))
        assertFalse(llmUseCase.detectSearchRequirement("Explain the concept of quantum computing.", isInternetEnabled = true))
    }

    @Test
    fun testCompileFinalPromptFormatsCorrectlyWithEmptyHistory() {
        val prompt = llmUseCase.compileFinalPrompt(
            chatMessages = emptyList(),
            rawPrompt = "How does photosynthesis work?",
            documentContext = "Photosynthesis is the process used by plants to convert light energy into chemical energy.",
            searchResults = null,
            searchQuery = null
        )
        
        assertTrue(prompt.contains("Photosynthesis is the process"))
        assertTrue(prompt.contains("### USER QUERY\nHow does photosynthesis work?"))
        assertFalse(prompt.contains("--- START HISTORY ---"))
    }

    @Test
    fun testCompileFinalPromptFormatsCorrectlyWithHistory() {
        val messages = listOf(
            com.rajpawardotin.kosh.domain.model.ChatMessage(text = "Hello Kosh", isUser = true),
            com.rajpawardotin.kosh.domain.model.ChatMessage(text = "Hello! I am Kosh.", isUser = false),
            com.rajpawardotin.kosh.domain.model.ChatMessage(text = "Current question", isUser = true)
        )

        val prompt = llmUseCase.compileFinalPrompt(
            chatMessages = messages,
            rawPrompt = "Current question",
            documentContext = "",
            searchResults = "Web search result snippet",
            searchQuery = "SearchQuery"
        )

        assertTrue(prompt.contains("--- START HISTORY ---"))
        assertTrue(prompt.contains("- User: Hello Kosh"))
        assertTrue(prompt.contains("- Assistant: Hello! I am Kosh."))
        assertTrue(prompt.contains("Search Query: SearchQuery"))
        assertTrue(prompt.contains("Web search result snippet"))
        assertTrue(prompt.contains("### USER QUERY\nCurrent question"))
    }

    @Test
    fun testCompileFinalPromptTruncatesOlderMessagesWhenBudgetExceeded() {
        val messages = mutableListOf<com.rajpawardotin.kosh.domain.model.ChatMessage>()
        for (i in 1..20) {
            messages.add(com.rajpawardotin.kosh.domain.model.ChatMessage(text = "User prompt $i", isUser = true))
            messages.add(com.rajpawardotin.kosh.domain.model.ChatMessage(text = "Assistant response $i", isUser = false))
        }
        messages.add(com.rajpawardotin.kosh.domain.model.ChatMessage(text = "Current question", isUser = true))

        val prompt = llmUseCase.compileFinalPrompt(
            chatMessages = messages,
            rawPrompt = "Current question",
            documentContext = "",
            searchResults = null,
            searchQuery = null,
            maxContextChars = 2000
        )

        assertTrue(prompt.contains("User prompt 20"))
        assertTrue(prompt.contains("Assistant response 20"))
        assertFalse(prompt.contains("User prompt 1\n"))
    }

    @Test
    fun testCompileFinalPromptWithNegativeOrZeroBudget() {
        val messages = listOf(
            com.rajpawardotin.kosh.domain.model.ChatMessage(text = "Hello Kosh", isUser = true),
            com.rajpawardotin.kosh.domain.model.ChatMessage(text = "Hello! I am Kosh.", isUser = false),
            com.rajpawardotin.kosh.domain.model.ChatMessage(text = "Current question", isUser = true)
        )

        // Set maxContextChars extremely low so that budgetForHistory becomes negative:
        // budget = 500 - 16 (rawPrompt) - 1000 (docContext) - 0 (search) - 1500 = -2016
        val prompt = llmUseCase.compileFinalPrompt(
            chatMessages = messages,
            rawPrompt = "Current question",
            documentContext = "a".repeat(1000),
            searchResults = null,
            searchQuery = null,
            maxContextChars = 500
        )

        // The prompt should NOT contain any conversation history
        assertFalse(prompt.contains("--- START HISTORY ---"))
        assertFalse(prompt.contains("Hello Kosh"))
        assertFalse(prompt.contains("Hello! I am Kosh."))
        assertTrue(prompt.contains("### USER QUERY\nCurrent question"))
    }
}

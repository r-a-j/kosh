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
}

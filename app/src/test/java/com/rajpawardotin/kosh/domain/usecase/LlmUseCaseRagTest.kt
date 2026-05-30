package com.rajpawardotin.kosh.domain.usecase

import com.rajpawardotin.kosh.domain.model.SessionDocument
import com.rajpawardotin.kosh.domain.provider.AIProvider
import com.rajpawardotin.kosh.domain.provider.SearchProvider
import com.rajpawardotin.kosh.domain.repository.SessionRepository
import com.rajpawardotin.kosh.domain.repository.DocumentRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock

class LlmUseCaseRagTest {

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
    fun testTokenizeQueryPreservesDecimalsAndHyphens() {
        val query = "Lets solve problem 1.1 and test AES-GCM"
        val tokens = llmUseCase.tokenizeQuery(query)
        
        // "lets" and "and" are standard stop words.
        // "1.1" and "aes-gcm" should be preserved exactly as tokens!
        assertTrue(tokens.contains("1.1"))
        assertTrue(tokens.contains("aes-gcm"))
        assertTrue(tokens.contains("problem"))
        assertTrue(tokens.contains("solve"))
        
        // Ensure standard split items don't have trailing punctuation
        val queryWithPunctuation = "Check [1.1], v2: and volume_up!"
        val cleanTokens = llmUseCase.tokenizeQuery(queryWithPunctuation)
        assertTrue(cleanTokens.contains("1.1"))
        assertTrue(cleanTokens.contains("v2"))
        assertTrue(cleanTokens.contains("volume_up"))
    }

    @Test
    fun testScoreChunkPrioritizesUniqueTermCoverage() {
        val terms = listOf("problem", "1.1", "part")
        
        // Chunk A matches "problem" 5 times, but nothing else
        val chunkA = "This is a problem. Another problem. Yes, problem. Still, problem. A big problem."
        
        // Chunk B matches "problem", "1.1", and "part" once each
        val chunkB = "Here is the part where we solve problem 1.1."
        
        val scoreA = llmUseCase.scoreChunk(chunkA, terms)
        val scoreB = llmUseCase.scoreChunk(chunkB, terms)
        
        // Chunk B has 3/3 unique terms matched (coverage = 1.0 -> score >= 1000)
        // Chunk A has 1/3 unique terms matched (coverage = 0.33 -> score ~333)
        // Hence scoreB must be strictly greater than scoreA despite A's high frequency!
        assertTrue("Chunk B score ($scoreB) should be greater than Chunk A score ($scoreA)", scoreB > scoreA)
    }

    @Test
    fun testRetrieveContextInjectsNeighborsAndSortsChronologically() {
        // Document has 5 sequential chunks
        val file = "stats.pdf"
        val chunks = listOf(
            SessionDocument("id0", "sess1", file, "pdf", 1000L, 0, "Introduction and basic definitions.", false, 1L),
            SessionDocument("id1", "sess1", file, "pdf", 1000L, 1, "Section 1.0 outlines the first issue.", false, 2L),
            SessionDocument("id2", "sess1", file, "pdf", 1000L, 2, "Here is the key derivation for Problem 1.1.", false, 3L),
            SessionDocument("id3", "sess1", file, "pdf", 1000L, 3, "And here we complete the proof.", false, 4L),
            SessionDocument("id4", "sess1", file, "pdf", 1000L, 4, "Section 2.0 starts next here.", false, 5L)
        )

        // Query targets chunk index 2 ("Problem 1.1")
        val query = "derive problem 1.1"
        
        val (contextString, sourceNames) = llmUseCase.retrieveContext(
            sessionId = "sess1",
            query = query,
            isEncrypted = false,
            activeSessionDocuments = chunks
        )
        
        // We expect the matched chunk (index 2) to be retrieved.
        // Because of neighbor injection, preceding chunk (index 1) and succeeding chunk (index 3) should also be retrieved.
        // So final result should contain chunks 1, 2, and 3.
        // Importantly, they should be sorted chronologically by index (1, then 2, then 3).
        assertTrue(contextString.contains("Chunk 2")) // index 1 (printed as index + 1)
        assertTrue(contextString.contains("Chunk 3")) // index 2 (printed as index + 1)
        assertTrue(contextString.contains("Chunk 4")) // index 3 (printed as index + 1)
        
        // Index 0 ("Chunk 1") and index 4 ("Chunk 5") should not be matched or injected
        org.junit.Assert.assertFalse(contextString.contains("Chunk 1"))
        org.junit.Assert.assertFalse(contextString.contains("Chunk 5"))
        
        // Check exact chronological sequence order in prompt block
        val indexChunk2 = contextString.indexOf("Chunk 2")
        val indexChunk3 = contextString.indexOf("Chunk 3")
        val indexChunk4 = contextString.indexOf("Chunk 4")
        
        assertTrue(indexChunk2 < indexChunk3)
        assertTrue(indexChunk3 < indexChunk4)
        
        assertEquals(1, sourceNames.size)
        assertEquals("stats.pdf", sourceNames[0])
    }
}

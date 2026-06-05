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
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import javax.crypto.SecretKey

class LlmUseCaseRagTest {

    private lateinit var llmUseCase: LlmUseCase
    private lateinit var mockAiProvider: AIProvider
    private lateinit var mockDocumentRepository: DocumentRepository

    @Before
    fun setUp() {
        mockAiProvider = mock<AIProvider>()
        val mockSearchProvider = mock<SearchProvider>()
        val mockSessionRepository = mock<SessionRepository>()
        mockDocumentRepository = mock<DocumentRepository>()
        
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

    @Test
    fun testRetrieveContextFallbackTakesFirstThreeChronologicallyAndSeparatesSummary() {
        val file = "stats.pdf"
        val chunks = listOf(
            SessionDocument("id_sum", "sess1", file, "summary", 1000L, -1, "High level summary of stats exam.", false, 1L),
            SessionDocument("id0", "sess1", file, "pdf", 1000L, 0, "Intro stats definitions.", false, 2L),
            SessionDocument("id1", "sess1", file, "pdf", 1000L, 1, "Section 1.0 details.", false, 3L),
            SessionDocument("id2", "sess1", file, "pdf", 1000L, 2, "Section 2.0 details.", false, 4L),
            SessionDocument("id3", "sess1", file, "pdf", 1000L, 3, "Section 3.0 details.", false, 5L)
        )

        val (contextString, sourceNames) = llmUseCase.retrieveContext(
            sessionId = "sess1",
            query = "",
            isEncrypted = false,
            activeSessionDocuments = chunks,
            justAttached = true
        )

        // Verify that the overview section is built using the summary chunk (chunkIndex = -1)
        assertTrue(contextString.contains("### CONSOLIDATED DOCUMENT OVERVIEW"))
        assertTrue(contextString.contains("High level summary of stats exam."))

        // Verify that the fallback excerpts contain chunks 1, 2, and 3 (which are index 0, 1, and 2 printed as index + 1)
        assertTrue(contextString.contains("Chunk 1"))
        assertTrue(contextString.contains("Chunk 2"))
        assertTrue(contextString.contains("Chunk 3"))
        org.junit.Assert.assertFalse(contextString.contains("Chunk 4")) // index 3

        // Chronological order verification in excerpts text
        val indexChunk1 = contextString.indexOf("Chunk 1")
        val indexChunk2 = contextString.indexOf("Chunk 2")
        val indexChunk3 = contextString.indexOf("Chunk 3")
        assertTrue(indexChunk1 < indexChunk2)
        assertTrue(indexChunk2 < indexChunk3)
        
        assertEquals(1, sourceNames.size)
        assertEquals("stats.pdf", sourceNames[0])
    }

    @Test
    fun testRetrieveContextPartitionsRagMemoryCorrectly() {
        val file = "stats.pdf"
        val chunks = listOf(
            SessionDocument("id_rag", "sess1", "RAG_Memory", "rag_memory", 0L, -2, "Consolidated memory of formulas.", false, 1L),
            SessionDocument("id0", "sess1", file, "pdf", 1000L, 0, "Intro stats definitions.", false, 2L),
            SessionDocument("id1", "sess1", file, "pdf", 1000L, 1, "Section 1.0 details.", false, 3L)
        )

        val (contextString, sourceNames) = llmUseCase.retrieveContext(
            sessionId = "sess1",
            query = "intro definitions",
            isEncrypted = false,
            activeSessionDocuments = chunks
        )

        // Verify that the consolidated memory block is rendered
        assertTrue(contextString.contains("### CONSOLIDATED RAG KNOWLEDGE MEMORY"))
        assertTrue(contextString.contains("Consolidated memory of formulas."))

        // Verify that the sourceNames list does NOT contain "RAG_Memory"
        assertTrue(sourceNames.contains("stats.pdf"))
        org.junit.Assert.assertFalse(sourceNames.contains("RAG_Memory"))
        
        // Exclude RAG_Memory from the printed list of files
        org.junit.Assert.assertFalse(contextString.contains("The user has attached the following files to this session: stats.pdf, RAG_Memory"))
    }

    @Test
    fun testHierarchicalRetrievalScoresSectionsAndFiltersLeafChunks() {
        val file = "stats.pdf"
        val sectionSummaries = listOf(
            SessionDocument("id_sec0", "sess1", file, "section_summary", 0L, -10, "This section is about probability theory.", false, 1L),
            SessionDocument("id_sec1", "sess1", file, "section_summary", 0L, -11, "This section is about regression analysis.", false, 1L)
        )
        
        val leafChunks = (0..9).map { idx ->
            val text = if (idx in 5..9) {
                "Regression line calculation leaf chunk $idx."
            } else {
                "General probability content leaf chunk $idx."
            }
            SessionDocument("id$idx", "sess1", file, "pdf", 1000L, idx, text, false, 2L)
        }
        
        val chunks = sectionSummaries + leafChunks

        // Query regression
        val query = "regression calculation"
        val (contextString, _) = llmUseCase.retrieveContext(
            sessionId = "sess1",
            query = query,
            isEncrypted = false,
            activeSessionDocuments = chunks
        )

        // It should match Section 1 (index -11) because it contains "regression analysis"
        // Section 1 corresponds to index -11 => sectionIndex = 1
        // For total leaf chunks = 10, sectionSize = maxOf(5, 10/5) = 5
        // So Section 1 corresponds to leaf chunks in range 5..9.
        // Chunks in range 5..9 contain "Regression line calculation leaf chunk 5."
        assertTrue(contextString.contains("Regression line calculation leaf chunk 5"))
    }

    @Test
    fun testGenerateDocumentSummaryIfMissingWithSelfBalancingGrouping() = runBlocking {
        whenever(mockAiProvider.sendMessage(any())).thenReturn(flowOf("Test summary content"))

        val file = "stats.pdf"
        val chunks = (0..9).map { idx ->
            SessionDocument("id$idx", "sess1", file, "pdf", 1000L, idx, "Chunk text $idx", false, 1L)
        }

        val result = llmUseCase.generateDocumentSummaryIfMissing(
            sessionId = "sess1",
            fileName = file,
            chunks = chunks,
            isTemporarySession = true,
            activeSessionKeys = emptyMap()
        )

        // totalChunks = 10 (>5), so we expect section grouping
        // sectionSize = maxOf(5, 10 / 5) = 5
        // sectionsCount = (10 + 5 - 1) / 5 = 2 section summaries
        // plus 1 master summary = 3 summaries total!
        assertEquals(3, result.size)

        val sec0 = result.find { it.chunkIndex == -10 }
        val sec1 = result.find { it.chunkIndex == -11 }
        val master = result.find { it.chunkIndex == -1 }

        assertTrue(sec0 != null && sec0.fileType == "section_summary")
        assertTrue(sec1 != null && sec1.fileType == "section_summary")
        assertTrue(master != null && master.fileType == "summary")
        
        assertEquals("Test summary content", sec0?.chunkText)
        assertEquals("Test summary content", master?.chunkText)
    }
}

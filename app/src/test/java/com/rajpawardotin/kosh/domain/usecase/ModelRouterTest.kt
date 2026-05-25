package com.rajpawardotin.kosh.domain.usecase

import com.rajpawardotin.kosh.data.ModelTag
import org.junit.Assert.assertEquals
import org.junit.Test

class ModelRouterTest {

    private val router = ModelRouter()

    @Test
    fun testDetectIntentWithDocumentsReturnsRagReader() {
        val prompt = "What is the weather?"
        val result = router.detectIntent(prompt, hasDocuments = true)
        assertEquals(ModelTag.RAG_READER, result)
    }

    @Test
    fun testDetectIntentWithCodingKeywordsReturnsCoder() {
        val prompt1 = "Write a Kotlin class to handle user login flow."
        val result1 = router.detectIntent(prompt1, hasDocuments = false)
        assertEquals(ModelTag.CODER, result1)

        val prompt2 = "Kotlin recursive binary search algorithm"
        val result2 = router.detectIntent(prompt2, hasDocuments = false)
        assertEquals(ModelTag.CODER, result2)
    }

    @Test
    fun testDetectIntentGeneralReturnsGeneral() {
        val prompt = "Tell me a joke about programmers, but keep it simple."
        val result = router.detectIntent(prompt, hasDocuments = false)
        assertEquals(ModelTag.GENERAL, result)
    }
}

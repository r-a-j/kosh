package com.rajpawardotin.kosh.ui.chat

import org.junit.Assert.*
import org.junit.Test

class ResponseParserTest {

    @Test
    fun testStandardChecklist() {
        val response = """
            Here is your list:
            - [ ] **Genre:** Sci-Fi
            - [x] ~~**Tone:** Suspenseful~~
            - [X] Bold **Text**
        """.trimIndent()

        val blocks = ResponseParser.parse(response)
        
        // We expect: 
        // 1. Text block: "Here is your list:"
        // 2. Checklist block with 3 items
        assertEquals(2, blocks.size)
        
        val textBlock = blocks[0] as ChatContentBlock.Text
        assertEquals("Here is your list:", textBlock.content)

        val checklistBlock = blocks[1] as ChatContentBlock.Checklist
        assertEquals(3, checklistBlock.items.size)

        // Verify item 1
        assertEquals(0, checklistBlock.items[0].index)
        assertEquals("**Genre:** Sci-Fi", checklistBlock.items[0].text)
        assertFalse(checklistBlock.items[0].initiallyChecked)

        // Verify item 2 (should strip the ~~ wrapper)
        assertEquals(1, checklistBlock.items[1].index)
        assertEquals("**Tone:** Suspenseful", checklistBlock.items[1].text)
        assertTrue(checklistBlock.items[1].initiallyChecked)

        // Verify item 3
        assertEquals(2, checklistBlock.items[2].index)
        assertEquals("Bold **Text**", checklistBlock.items[2].text)
        assertTrue(checklistBlock.items[2].initiallyChecked)
    }

    @Test
    fun testNormalBulletList() {
        val response = """
            Options:
            * **Genre:** (Fantasy, Sci-Fi)
            - **Main Character:** (Hero)
        """.trimIndent()

        val blocks = ResponseParser.parse(response)

        // We expect only text blocks, since standard bullet lists are NOT checklists.
        // It should accumulate into one or more Text blocks.
        assertTrue(blocks.isNotEmpty())
        assertTrue(blocks.all { it is ChatContentBlock.Text })
        
        val combinedText = blocks.filterIsInstance<ChatContentBlock.Text>()
            .joinToString("\n") { it.content }
            
        assertTrue(combinedText.contains("* **Genre:**"))
        assertTrue(combinedText.contains("- **Main Character:**"))
    }

    @Test
    fun testNumberedList() {
        val response = """
            Steps:
            1. First step
            2. Second step
        """.trimIndent()

        val blocks = ResponseParser.parse(response)

        // We expect only text blocks, since numbered lists are NOT checklists.
        assertTrue(blocks.isNotEmpty())
        assertTrue(blocks.all { it is ChatContentBlock.Text })
        
        val combinedText = blocks.filterIsInstance<ChatContentBlock.Text>()
            .joinToString("\n") { it.content }
            
        assertTrue(combinedText.contains("1. First step"))
        assertTrue(combinedText.contains("2. Second step"))
    }

    @Test
    fun testReferenceParserJson() {
        val jsonRefs = """
            {
                "docs": ["Document 1", "Document 2"],
                "web": [
                    {
                        "title": "Kotlin Lang",
                        "url": "https://kotlinlang.org",
                        "imageUrl": "https://kotlinlang.org/preview.png",
                        "videoUrl": ""
                    },
                    {
                        "title": "Google Search",
                        "url": "https://google.com",
                        "imageUrl": "",
                        "videoUrl": "https://youtube.com/watch?v=123"
                    }
                ]
            }
        """.trimIndent()

        val (docs, web) = ReferenceParser.parseReferences(jsonRefs)

        // Verify documents
        assertEquals(2, docs.size)
        assertEquals("Document 1", docs[0])
        assertEquals("Document 2", docs[1])

        // Verify web sources
        assertEquals(2, web.size)
        assertEquals("Kotlin Lang", web[0].title)
        assertEquals("https://kotlinlang.org", web[0].url)
        assertEquals("https://kotlinlang.org/preview.png", web[0].imageUrl)
        assertNull(web[0].videoUrl)

        assertEquals("Google Search", web[1].title)
        assertEquals("https://google.com", web[1].url)
        assertNull(web[1].imageUrl)
        assertEquals("https://youtube.com/watch?v=123", web[1].videoUrl)
    }

    @Test
    fun testReferenceParserLegacyFallback() {
        val legacyRefs = "Legacy Doc A, Legacy Doc B, Legacy Doc C"
        val (docs, web) = ReferenceParser.parseReferences(legacyRefs)

        // Verify fallback parses comma-separated documents
        assertEquals(3, docs.size)
        assertEquals("Legacy Doc A", docs[0])
        assertEquals("Legacy Doc B", docs[1])
        assertEquals("Legacy Doc C", docs[2])

        // Verify web source list is empty
        assertTrue(web.isEmpty())
    }

    @Test
    fun testReferenceParserEmptyOrNull() {
        val (docs1, web1) = ReferenceParser.parseReferences(null)
        assertTrue(docs1.isEmpty())
        assertTrue(web1.isEmpty())

        val (docs2, web2) = ReferenceParser.parseReferences("")
        assertTrue(docs2.isEmpty())
        assertTrue(web2.isEmpty())
    }

    @Test
    fun testResponseParserMathBlockSingleLine() {
        val text = """
            Here is a basic formula:
            $$ x = 42 $$
            And here is another:
            \[ E = mc^2 \]
        """.trimIndent()

        val blocks = ResponseParser.parse(text)
        
        // We expect:
        // 1. Text: "Here is a basic formula:"
        // 2. MathBlock: "x = 42"
        // 3. Text: "And here is another:"
        // 4. MathBlock: "E = mc^2"
        assertEquals(4, blocks.size)
        
        assertEquals("Here is a basic formula:", (blocks[0] as ChatContentBlock.Text).content)
        assertEquals("x = 42", (blocks[1] as ChatContentBlock.MathBlock).formula)
        assertEquals("And here is another:", (blocks[2] as ChatContentBlock.Text).content)
        assertEquals("E = mc^2", (blocks[3] as ChatContentBlock.MathBlock).formula)
    }

    @Test
    fun testResponseParserMathBlockMultiLine() {
        val text = """
            Consider this system:
            $$
            y = mx + c
            c = 10
            $$
            End of formula.
        """.trimIndent()

        val blocks = ResponseParser.parse(text)
        
        // We expect:
        // 1. Text: "Consider this system:"
        // 2. MathBlock: "y = mx + c\nc = 10" (trimmed)
        // 3. Text: "End of formula."
        assertEquals(3, blocks.size)
        assertEquals("Consider this system:", (blocks[0] as ChatContentBlock.Text).content)
        assertEquals("y = mx + c\nc = 10", (blocks[1] as ChatContentBlock.MathBlock).formula.replace("\r\n", "\n"))
        assertEquals("End of formula.", (blocks[2] as ChatContentBlock.Text).content)
    }
}


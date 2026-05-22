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
}

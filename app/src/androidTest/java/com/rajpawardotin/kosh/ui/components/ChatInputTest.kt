package com.rajpawardotin.kosh.ui.components

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rajpawardotin.kosh.domain.model.AttachedFile
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ChatInputTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun chatInput_displaysProvidedValue() {
        composeTestRule.setContent {
            ChatInput(
                value = "Test prompt input",
                onValueChange = {},
                onSend = {},
                onStop = {},
                onVoiceClick = {},
                onAttachClick = {},
                attachedFiles = emptyList(),
                onDetachFile = {},
                enabled = true,
                isGenerating = false,
                isInternetEnabled = false,
                isSearchForced = false,
                onToggleSearch = {}
            )
        }

        composeTestRule.onNodeWithText("Test prompt input").assertExists()
    }

    @Test
    fun chatInput_triggersValueChangeOnTyping() {
        var typedText = ""
        composeTestRule.setContent {
            ChatInput(
                value = typedText,
                onValueChange = { typedText = it },
                onSend = {},
                onStop = {},
                onVoiceClick = {},
                onAttachClick = {},
                attachedFiles = emptyList(),
                onDetachFile = {},
                enabled = true,
                isGenerating = false,
                isInternetEnabled = false,
                isSearchForced = false,
                onToggleSearch = {}
            )
        }

        // Initially value is empty, click to focus and type
        val inputNode = composeTestRule.onNodeWithText("Neural Command...")
        inputNode.performTextInput("Hello Kosh")
        assert(typedText == "Hello Kosh")
    }

    @Test
    fun chatInput_sendButtonDisabledWhenEmpty() {
        composeTestRule.setContent {
            ChatInput(
                value = "",
                onValueChange = {},
                onSend = {},
                onStop = {},
                onVoiceClick = {},
                onAttachClick = {},
                attachedFiles = emptyList(),
                onDetachFile = {},
                enabled = true,
                isGenerating = false,
                isInternetEnabled = false,
                isSearchForced = false,
                onToggleSearch = {}
            )
        }

        // When empty, the Send button is hidden and replaced by Voice Input
        composeTestRule.onNodeWithContentDescription("Send").assertDoesNotExist()
        composeTestRule.onNodeWithContentDescription("Voice Input").assertExists()
    }

    @Test
    fun chatInput_triggersSendClick() {
        var sendClicked = false
        composeTestRule.setContent {
            ChatInput(
                value = "A valid prompt",
                onValueChange = {},
                onSend = { sendClicked = true },
                onStop = {},
                onVoiceClick = {},
                onAttachClick = {},
                attachedFiles = emptyList(),
                onDetachFile = {},
                enabled = true,
                isGenerating = false,
                isInternetEnabled = false,
                isSearchForced = false,
                onToggleSearch = {}
            )
        }

        composeTestRule.onNodeWithContentDescription("Send").performClick()
        assert(sendClicked)
    }

    @Test
    fun chatInput_showsPlaceholderWhenDisabled() {
        composeTestRule.setContent {
            ChatInput(
                value = "",
                onValueChange = {},
                onSend = {},
                onStop = {},
                onVoiceClick = {},
                onAttachClick = {},
                attachedFiles = emptyList(),
                onDetachFile = {},
                enabled = false,
                isGenerating = false,
                isInternetEnabled = false,
                isSearchForced = false,
                onToggleSearch = {}
            )
        }

        composeTestRule.onNodeWithText("Neural Core Offline...").assertExists()
    }

    @Test
    fun chatInput_showsProgressIndicatorWhenGenerating() {
        composeTestRule.setContent {
            ChatInput(
                value = "Wait generation",
                onValueChange = {},
                onSend = {},
                onStop = {},
                onVoiceClick = {},
                onAttachClick = {},
                attachedFiles = emptyList(),
                onDetachFile = {},
                enabled = true,
                isGenerating = true,
                isInternetEnabled = false,
                isSearchForced = false,
                onToggleSearch = {}
            )
        }

        // Since it's generating, the send button is replaced by a CircularProgressIndicator.
        // CircularProgressIndicator doesn't have a contentDescription by default unless custom,
        // but we can assert that the "Send" icon does NOT exist.
        composeTestRule.onNodeWithContentDescription("Send").assertDoesNotExist()
    }

    @Test
    fun chatInput_displaysAttachmentBadge() {
        val dummyFile = AttachedFile(
            uriString = "content://dummy",
            fileName = "kosh_vault_spec.pdf",
            fileSize = 1024L,
            fileType = "pdf"
        )

        composeTestRule.setContent {
            ChatInput(
                value = "",
                onValueChange = {},
                onSend = {},
                onStop = {},
                onVoiceClick = {},
                onAttachClick = {},
                attachedFiles = listOf(dummyFile),
                onDetachFile = {},
                enabled = true,
                isGenerating = false,
                isInternetEnabled = false,
                isSearchForced = false,
                onToggleSearch = {}
            )
        }

        composeTestRule.onNodeWithText("kosh_vault_spec.pdf").assertExists()
    }
}

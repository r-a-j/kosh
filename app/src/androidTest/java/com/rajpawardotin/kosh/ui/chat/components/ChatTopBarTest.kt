package com.rajpawardotin.kosh.ui.chat.components

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rajpawardotin.kosh.domain.model.ChatSession
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ChatTopBarTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun chatTopBar_displaysCorrectModelName() {
        composeTestRule.setContent {
            ChatTopBar(
                isEngineReady = true,
                modelPath = "/path/to/gemini-1.5-pro.bin",
                currentSession = null,
                isCurrentSessionUnlocked = false,
                isTemporarySession = false,
                isGenerating = false,
                onMenuClick = {},
                onCoreSelectorClick = {},
                onLockSettingsClick = {},
                onManageLockClick = {},
                onNewChatClick = {},
                onSettingsClick = {}
            )
        }

        // We expect the model path file name (without extension and mapped "model" -> "Model") to be uppercase
        composeTestRule.onNodeWithText("GEMINI-1.5-PRO").assertExists()
    }

    @Test
    fun chatTopBar_displaysSelectCoreWhenNotReady() {
        composeTestRule.setContent {
            ChatTopBar(
                isEngineReady = false,
                modelPath = null,
                currentSession = null,
                isCurrentSessionUnlocked = false,
                isTemporarySession = false,
                isGenerating = false,
                onMenuClick = {},
                onCoreSelectorClick = {},
                onLockSettingsClick = {},
                onManageLockClick = {},
                onNewChatClick = {},
                onSettingsClick = {}
            )
        }

        composeTestRule.onNodeWithText("SELECT MODEL").assertExists()
    }

    @Test
    fun chatTopBar_showsLockSettingsIconWhenEncryptedAndLocked() {
        val session = ChatSession(
            id = "test-session",
            title = "Secure Chat",
            createdAt = 123456789L,
            lastActive = 123456789L,
            modelPath = null,
            lastSearchQuery = null,
            encryptedKeyPassword = "encrypted-password-hash"
        )

        composeTestRule.setContent {
            ChatTopBar(
                isEngineReady = true,
                modelPath = null,
                currentSession = session,
                isCurrentSessionUnlocked = false, // Locked
                isTemporarySession = false,
                isGenerating = false,
                onMenuClick = {},
                onCoreSelectorClick = {},
                onLockSettingsClick = {},
                onManageLockClick = {},
                onNewChatClick = {},
                onSettingsClick = {}
            )
        }

        // Icon should have contentDescription "Chat Lock Settings"
        composeTestRule.onNodeWithContentDescription("Chat Lock Settings").assertExists()
    }

    @Test
    fun chatTopBar_triggersNewChatClick() {
        var newChatClicked = false
        var isTempClicked = false

        composeTestRule.setContent {
            ChatTopBar(
                isEngineReady = true,
                modelPath = null,
                currentSession = null,
                isCurrentSessionUnlocked = false,
                isTemporarySession = false,
                isGenerating = false,
                onMenuClick = {},
                onCoreSelectorClick = {},
                onLockSettingsClick = {},
                onManageLockClick = {},
                onNewChatClick = { isTemp -> 
                    newChatClicked = true
                    isTempClicked = isTemp
                },
                onSettingsClick = {}
            )
        }

        // Click on the new chat button (New Saved Chat)
        composeTestRule.onNodeWithContentDescription("New Saved Chat").performClick()
        assert(newChatClicked)
        assert(!isTempClicked)
    }

    @Test
    fun chatTopBar_displaysCleanModelName_npu() {
        composeTestRule.setContent {
            ChatTopBar(
                isEngineReady = true,
                modelPath = "/path/to/gemma-4-E2B-it_qualcomm_sm8750.litertlm",
                currentSession = null,
                isCurrentSessionUnlocked = false,
                isTemporarySession = false,
                isGenerating = false,
                onMenuClick = {},
                onCoreSelectorClick = {},
                onLockSettingsClick = {},
                onManageLockClick = {},
                onNewChatClick = {},
                onSettingsClick = {}
            )
        }

        // Check if the suffix _qualcomm_sm8750 is replaced with (NPU) and -it is removed
        composeTestRule.onNodeWithText("GEMMA-4-E2B (NPU)").assertExists()
    }

    @Test
    fun chatTopBar_temporarySession_hidesLockIcon() {
        val session = ChatSession(
            id = "test-session",
            title = "Secure Chat",
            createdAt = 123456789L,
            lastActive = 123456789L,
            modelPath = null,
            lastSearchQuery = null,
            encryptedKeyPassword = "encrypted-password-hash"
        )

        composeTestRule.setContent {
            ChatTopBar(
                isEngineReady = true,
                modelPath = null,
                currentSession = session,
                isCurrentSessionUnlocked = false,
                isTemporarySession = true, // Temporary Session
                isGenerating = false,
                onMenuClick = {},
                onCoreSelectorClick = {},
                onLockSettingsClick = {},
                onManageLockClick = {},
                onNewChatClick = {},
                onSettingsClick = {}
            )
        }

        // Chat Lock Settings button should not be displayed in temporary sessions
        composeTestRule.onNodeWithContentDescription("Chat Lock Settings").assertDoesNotExist()
    }
}

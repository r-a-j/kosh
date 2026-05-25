package com.rajpawardotin.kosh.ui.chat.components

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ChatTopBarTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun chatTopBar_displaysCorrectTitle() {
        composeTestRule.setContent {
            ChatTopBar(
                title = "My Custom Vault",
                isLocked = false,
                appLockEnabled = false,
                isGenerating = false,
                selectedModel = "gemini-1.5-pro",
                onMenuClick = {},
                onLockClick = {},
                onSettingsClick = {},
                onModelSelect = {}
            )
        }

        composeTestRule.onNodeWithText("My Custom Vault").assertExists()
    }

    @Test
    fun chatTopBar_showsLockIconWhenLocked() {
        composeTestRule.setContent {
            ChatTopBar(
                title = "Locked Vault",
                isLocked = true,
                appLockEnabled = false,
                isGenerating = false,
                selectedModel = "gemini-1.5-pro",
                onMenuClick = {},
                onLockClick = {},
                onSettingsClick = {},
                onModelSelect = {}
            )
        }

        // Vault is locked, so we expect the "Unlock Vault" icon
        composeTestRule.onNodeWithContentDescription("Unlock Vault").assertExists()
    }

    @Test
    fun chatTopBar_triggersLockClick() {
        var clicked = false
        composeTestRule.setContent {
            ChatTopBar(
                title = "Vault",
                isLocked = false,
                appLockEnabled = false,
                isGenerating = false,
                selectedModel = "gemini-1.5-pro",
                onMenuClick = {},
                onLockClick = { clicked = true },
                onSettingsClick = {},
                onModelSelect = {}
            )
        }

        // Vault is unlocked, so we expect the "Lock Vault" icon
        composeTestRule.onNodeWithContentDescription("Lock Vault").performClick()
        assert(clicked)
    }
}

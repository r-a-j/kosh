package com.rajpawardotin.kosh.ui.chat.components

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppLockOverlayTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun appLockOverlay_displaysUnlockButton() {
        composeTestRule.setContent {
            AppLockOverlay(
                onUnlockClick = {}
            )
        }

        // The overlay should have a button with text "UNLOCK VAULT"
        composeTestRule.onNodeWithText("UNLOCK VAULT").assertExists()
    }

    @Test
    fun appLockOverlay_triggersUnlockClick() {
        var clicked = false
        composeTestRule.setContent {
            AppLockOverlay(
                onUnlockClick = { clicked = true }
            )
        }

        composeTestRule.onNodeWithText("UNLOCK VAULT").performClick()
        assert(clicked)
    }
}

package com.rajpawardotin.kosh.ui.chat.dialogs

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rajpawardotin.kosh.domain.model.ChatSession
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LockVaultDialogTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun lockVaultDialog_submitEnabledOnlyWhenPasswordsMatch() {
        var submittedPassword = ""
        val dummySession = ChatSession(id = "1", title = "Vault", createdAt = 0L, lastActive = 0L, modelPath = null, lastSearchQuery = null)
        
        composeTestRule.setContent {
            LockVaultDialog(
                session = dummySession,
                onDismiss = {},
                onLockSubmit = { password, _ -> submittedPassword = password }
            )
        }

        // Initially submit button should be disabled because passwords are empty
        composeTestRule.onNodeWithText("Encrypt Chat").assertExists()
        
        // Enter password
        composeTestRule.onNodeWithText("Set Passcode").performTextInput("secure123")
        composeTestRule.onNodeWithText("Encrypt Chat").performClick()
        
        // Should not have submitted yet because confirm password is empty
        assert(submittedPassword.isEmpty())

        // Enter wrong confirm password
        composeTestRule.onNodeWithText("Confirm Passcode").performTextInput("secure12")
        composeTestRule.onNodeWithText("Encrypt Chat").performClick()
        assert(submittedPassword.isEmpty())

        // Enter correct confirm password
        composeTestRule.onNodeWithText("Confirm Passcode").performTextInput("3") // "secure123"
        composeTestRule.onNodeWithText("Encrypt Chat").performClick()
        
        // Should submit successfully
        assert(submittedPassword == "secure123")
    }
}

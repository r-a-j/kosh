package com.rajpawardotin.kosh.ui.chat

import android.content.Context
import com.rajpawardotin.kosh.domain.model.ChatMessage
import com.rajpawardotin.kosh.domain.model.ChatSession
import com.rajpawardotin.kosh.domain.provider.AIProvider
import com.rajpawardotin.kosh.domain.provider.SearchProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {
 
    private val testDispatcher = StandardTestDispatcher()
    private val context = mock<Context>()
    private lateinit var fakeSessionRepo: FakeSessionRepository
    private lateinit var fakeMessageRepo: FakeMessageRepository
    private lateinit var fakeDocumentRepo: FakeDocumentRepository
    private lateinit var fakeTts: FakeTtsProvider
    private lateinit var fakeSettings: FakeSettingsProvider
    private lateinit var fakeAI: FakeAIProvider
    private lateinit var fakeSearch: FakeSearchProvider
    private lateinit var viewModel: ChatViewModel
 
    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeSessionRepo = FakeSessionRepository()
        fakeMessageRepo = FakeMessageRepository()
        fakeDocumentRepo = FakeDocumentRepository()
        fakeTts = FakeTtsProvider()
        fakeSettings = FakeSettingsProvider()
        fakeAI = FakeAIProvider()
        fakeSearch = FakeSearchProvider()
        
        val mockLibraryManager = mock<com.rajpawardotin.kosh.data.ModelLibraryManager>()
        org.mockito.kotlin.whenever(mockLibraryManager.getModels()).thenReturn(emptyList())
        org.mockito.kotlin.whenever(mockLibraryManager.getModelByTag(org.mockito.kotlin.any())).thenReturn(null)
        
        val mockModelRouter = mock<com.rajpawardotin.kosh.domain.usecase.ModelRouter>()
        org.mockito.kotlin.whenever(mockModelRouter.detectIntent(org.mockito.kotlin.any(), org.mockito.kotlin.any())).thenReturn(com.rajpawardotin.kosh.data.ModelTag.GENERAL)
        
        viewModel = ChatViewModel(
            context,
            fakeAI, 
            fakeSearch, 
            fakeSessionRepo, 
            fakeMessageRepo, 
            fakeDocumentRepo, 
            fakeSettings, 
            fakeTts, 
            mockLibraryManager, 
            mockModelRouter, 
            testDispatcher
        )
        viewModel.modelPath = "fake/model/path.bin"
    }
 
    @After
    fun tearDown() {
        if (::viewModel.isInitialized) {
            viewModel.stopTrackingMetrics()
        }
        Dispatchers.resetMain()
    }
 
    @Test
    fun testInitialState() {
        assertNull(viewModel.currentSessionId)
        assertTrue(viewModel.chatMessages.isEmpty())
        assertTrue(viewModel.savedSessions.isEmpty())
    }
 
    @Test
    fun testSendMessageCreatesSessionAndSavesMessages() = runTest(testDispatcher) {
        viewModel.prompt = "Hello AI"
        viewModel.sendMessage(context)

        
        testScheduler.advanceUntilIdle()

        // 1. Session must be created
        assertNotNull(viewModel.currentSessionId)
        val sessionId = viewModel.currentSessionId!!

        // 2. Session title must be generated based on prompt
        val sessions = fakeSessionRepo.sessions
        assertEquals(1, sessions.size)
        assertEquals("Hello AI", sessions[0].title)

        // 3. User and Assistant messages must be saved in repository
        val messages = fakeMessageRepo.messages[sessionId]
        assertNotNull(messages)
        assertEquals(2, messages!!.size)
        
        // First message: User
        assertEquals("Hello AI", messages[0].text)
        assertTrue(messages[0].isUser)

        // Second message: Assistant (Mocked output)
        assertEquals("Mock response from Kosh", messages[1].text)
        assertFalse(messages[1].isUser)

        // 4. Memory representation must be in sync
        assertEquals(2, viewModel.chatMessages.size)
        assertEquals("Hello AI", viewModel.chatMessages[0].text)
        assertEquals("Mock response from Kosh", viewModel.chatMessages[1].text)
    }

    @Test
    fun testLongPromptTruncatesSessionTitle() = runTest(testDispatcher) {
        viewModel.prompt = "This is an extremely long user prompt that should be truncated when generating the session title"
        viewModel.sendMessage(context)

        
        testScheduler.advanceUntilIdle()

        val sessions = fakeSessionRepo.sessions
        assertEquals(1, sessions.size)
        assertEquals("This is an extremely long...", sessions[0].title)
    }

    @Test
    fun testNewChatButtonResetsCurrentSession() = runTest(testDispatcher) {
        viewModel.prompt = "First Session Message"
        viewModel.sendMessage(context)

        testScheduler.advanceUntilIdle()

        assertNotNull(viewModel.currentSessionId)
        val firstSessionId = viewModel.currentSessionId!!

        // Click new chat
        viewModel.startNewChat()
        assertNull(viewModel.currentSessionId)
        assertTrue(viewModel.chatMessages.isEmpty())

        // Send a message in the new session
        viewModel.prompt = "Second Session Message"
        viewModel.sendMessage(context)

        testScheduler.advanceUntilIdle()

        assertNotNull(viewModel.currentSessionId)
        val secondSessionId = viewModel.currentSessionId!!
        assertNotEquals(firstSessionId, secondSessionId)

        // Verify both sessions exist in repository
        assertEquals(2, fakeSessionRepo.sessions.size)
    }

    @Test
    fun testLoadSessionRestoresMessagesAndChecklist() = runTest(testDispatcher) {
        val sessionId = "custom-session-id"
        val session = ChatSession(
            id = sessionId,
            title = "Custom Thread",
            createdAt = 1000L,
            lastActive = 2000L,
            modelPath = null,
            lastSearchQuery = null
        )
        fakeSessionRepo.saveSession(session)

        val msg1 = ChatMessage(id = "msg1", text = "Hello", isUser = true)
        val msg2 = ChatMessage(id = "msg2", text = "Hi there", isUser = false)
        fakeMessageRepo.saveMessage(sessionId, msg1)
        fakeMessageRepo.saveMessage(sessionId, msg2)
        fakeMessageRepo.saveChecklistState("msg2", 0, true)

        // Load the session
        viewModel.loadSession(sessionId)
        testScheduler.advanceUntilIdle()

        assertEquals(sessionId, viewModel.currentSessionId)
        assertEquals(2, viewModel.chatMessages.size)
        assertEquals("Hello", viewModel.chatMessages[0].text)
        assertEquals("Hi there", viewModel.chatMessages[1].text)
        assertTrue(viewModel.checkedItems["msg2_0"] == true)
    }

    @Test
    fun testSendMessageInTemporaryChatDoesNotSaveToRepository() = runTest(testDispatcher) {
        viewModel.startNewChat(isTemporary = true)
        assertTrue(viewModel.isTemporarySession)

        viewModel.prompt = "Temporary Prompt"
        viewModel.sendMessage(context)

        testScheduler.advanceUntilIdle()

        // 1. Session id should still be set in memory to manage UI checklist items, etc.
        assertNotNull(viewModel.currentSessionId)

        // 2. Chat messages must be added in memory
        assertEquals(2, viewModel.chatMessages.size)
        assertEquals("Temporary Prompt", viewModel.chatMessages[0].text)
        assertEquals("Mock response from Kosh", viewModel.chatMessages[1].text)

        // 3. BUT nothing should be saved in the database helper (FakeChatRepository)
        assertTrue(fakeSessionRepo.sessions.isEmpty())
        assertTrue(fakeMessageRepo.messages.isEmpty())
    }

    @Test
    fun testStartNewChatResetsTemporaryFlag() = runTest(testDispatcher) {
        viewModel.startNewChat(isTemporary = true)
        assertTrue(viewModel.isTemporarySession)

        viewModel.startNewChat(isTemporary = false)
        assertFalse(viewModel.isTemporarySession)
        assertNull(viewModel.currentSessionId)
        assertTrue(viewModel.chatMessages.isEmpty())
    }

    @Test
    fun testLoadSavedSessionResetsTemporaryFlag() = runTest(testDispatcher) {
        val sessionId = "saved-session"
        val session = ChatSession(
            id = sessionId,
            title = "Saved Thread",
            createdAt = 1000L,
            lastActive = 2000L,
            modelPath = null,
            lastSearchQuery = null
        )
        fakeSessionRepo.saveSession(session)

        viewModel.startNewChat(isTemporary = true)
        assertTrue(viewModel.isTemporarySession)

        // Loading a saved session should reset isTemporarySession to false
        viewModel.loadSession(sessionId)
        testScheduler.advanceUntilIdle()

        assertFalse(viewModel.isTemporarySession)
        assertEquals(sessionId, viewModel.currentSessionId)
    }

    @Test
    fun testAppLockSettingToggles() {
        assertFalse(viewModel.isAppLockEnabled)
        viewModel.toggleAppLock(true)
        assertTrue(viewModel.isAppLockEnabled)
        assertEquals("true", fakeSettings.getString("app_lock_enabled", "false"))
        
        viewModel.toggleAppLock(false)
        assertFalse(viewModel.isAppLockEnabled)
        assertEquals("false", fakeSettings.getString("app_lock_enabled", "false"))
    }

    @Test
    fun testAppThemeSettingUpdates() {
        assertEquals("SYSTEM", viewModel.appTheme)
        
        viewModel.updateAppTheme("OLED_OBSIDIAN")
        assertEquals("OLED_OBSIDIAN", viewModel.appTheme)
        assertEquals("OLED_OBSIDIAN", fakeSettings.getString("app_theme", "SYSTEM"))
        
        viewModel.updateAppTheme("MINIMALIST_SAND")
        assertEquals("MINIMALIST_SAND", viewModel.appTheme)
        assertEquals("MINIMALIST_SAND", fakeSettings.getString("app_theme", "SYSTEM"))
    }

    @Test
    fun testLockAndUnlockSessionWithPassword() = runTest(testDispatcher) {
        val sessionId = "vault-1"
        val session = ChatSession(
            id = sessionId,
            title = "Secret Vault",
            createdAt = 1000L,
            lastActive = 2000L,
            modelPath = null,
            lastSearchQuery = null
        )
        fakeSessionRepo.saveSession(session)
        val msg1 = ChatMessage(id = "m1", text = "This is a secret message", isUser = true)
        val msg2 = ChatMessage(id = "m2", text = "AI response about secrets", isUser = false)
        fakeMessageRepo.saveMessage(sessionId, msg1)
        fakeMessageRepo.saveMessage(sessionId, msg2)

        var lockSuccess = false
        var recoveryMnemonic: String? = null
        val mockContext = mock<Context>()
        val mockAssets = mock<android.content.res.AssetManager>()
        org.mockito.kotlin.whenever(mockContext.assets).thenReturn(mockAssets)
        val realFile = java.io.File("app/src/main/assets/bip39_english.txt").let {
            if (it.exists()) it else java.io.File("src/main/assets/bip39_english.txt")
        }
        org.mockito.kotlin.whenever(mockAssets.open("bip39_english.txt")).thenAnswer {
            java.io.FileInputStream(realFile)
        }

        viewModel.lockSession(sessionId, "strong_password_123", enableBiometric = false, context = mockContext) { success, mnemonic ->
            lockSuccess = success
            recoveryMnemonic = mnemonic
        }
        testScheduler.advanceUntilIdle()

        assertTrue(lockSuccess)
        assertNotNull(recoveryMnemonic)
        assertEquals(12, recoveryMnemonic!!.split(" ").size)
        
        val savedSession = fakeSessionRepo.sessions.find { it.id == sessionId }
        assertNotNull(savedSession)
        assertNull(savedSession!!.passwordHash)
        assertNotNull(savedSession.salt)
        assertNull(savedSession.validationToken)
        assertNotNull(savedSession.encryptedKeyPassword)

        val savedMessages = fakeMessageRepo.getMessagesForSession(sessionId)
        assertNotEquals("This is a secret message", savedMessages[0].text)
        assertNotEquals("AI response about secrets", savedMessages[1].text)
        
        viewModel.loadSession(sessionId)
        testScheduler.advanceUntilIdle()
        assertEquals(2, viewModel.chatMessages.size)
        assertEquals("This is a secret message", viewModel.chatMessages[0].text)
        assertEquals("AI response about secrets", viewModel.chatMessages[1].text)

        viewModel.activeSessionKeys.clear()
        
        viewModel.loadSession(sessionId)
        testScheduler.advanceUntilIdle()
        assertTrue(viewModel.chatMessages.isEmpty())

        var incorrectUnlockSuccess = true
        viewModel.unlockSessionWithPassword(sessionId, "wrong_pass") { success ->
            incorrectUnlockSuccess = success
        }
        testScheduler.advanceUntilIdle()
        assertFalse(incorrectUnlockSuccess)
        assertTrue(viewModel.chatMessages.isEmpty())

        var correctUnlockSuccess = false
        viewModel.unlockSessionWithPassword(sessionId, "strong_password_123") { success ->
            correctUnlockSuccess = success
        }
        testScheduler.advanceUntilIdle()
        assertTrue(correctUnlockSuccess)
        
        assertEquals(2, viewModel.chatMessages.size)
        assertEquals("This is a secret message", viewModel.chatMessages[0].text)
        assertEquals("AI response about secrets", viewModel.chatMessages[1].text)
    }

    @Test
    fun testVaultRecoveryWithMnemonic() = runTest(testDispatcher) {
        val sessionId = "vault-recovery"
        val session = ChatSession(
            id = sessionId,
            title = "Locked Vault",
            createdAt = 1000L,
            lastActive = 2000L,
            modelPath = null,
            lastSearchQuery = null
        )
        fakeSessionRepo.saveSession(session)
        
        val mockContext = mock<Context>()
        val mockAssets = mock<android.content.res.AssetManager>()
        org.mockito.kotlin.whenever(mockContext.assets).thenReturn(mockAssets)
        val realFile = java.io.File("app/src/main/assets/bip39_english.txt").let {
            if (it.exists()) it else java.io.File("src/main/assets/bip39_english.txt")
        }
        org.mockito.kotlin.whenever(mockAssets.open("bip39_english.txt")).thenAnswer {
            java.io.FileInputStream(realFile)
        }

        // 1. Lock the session first to get its recovery key and payload
        var lockSuccess = false
        var recoveryMnemonic: String? = null
        viewModel.lockSession(sessionId, "old_pass", enableBiometric = false, context = mockContext) { success, mnemonic ->
            lockSuccess = success
            recoveryMnemonic = mnemonic
        }
        testScheduler.advanceUntilIdle()
        assertTrue(lockSuccess)
        assertNotNull(recoveryMnemonic)

        // Clear active keys to simulate sealed vault
        viewModel.activeSessionKeys.clear()

        // 2. Perform recovery with correct mnemonic and new password
        var recoverySuccess = false
        viewModel.recoverSessionWithMnemonic(sessionId, recoveryMnemonic!!, "new_pass", mockContext) { success ->
            recoverySuccess = success
        }
        testScheduler.advanceUntilIdle()
        assertTrue(recoverySuccess)

        // Verify key is in active session keys
        assertTrue(viewModel.activeSessionKeys.containsKey(sessionId))

        // 3. Clear active keys again, and verify we can unlock with the new password
        viewModel.activeSessionKeys.clear()
        var unlockSuccess = false
        viewModel.unlockSessionWithPassword(sessionId, "new_pass") { success ->
            unlockSuccess = success
        }
        testScheduler.advanceUntilIdle()
        assertTrue(unlockSuccess)

        // 4. Verify we cannot unlock with the old password
        viewModel.activeSessionKeys.clear()
        var oldUnlockSuccess = true
        viewModel.unlockSessionWithPassword(sessionId, "old_pass") { success ->
            oldUnlockSuccess = success
        }
        testScheduler.advanceUntilIdle()
        assertFalse(oldUnlockSuccess)
    }

    @Test
    fun testStopGenerationCancelsActiveJobAndResetsState() = runTest(testDispatcher) {
        viewModel.prompt = "Test Cancel Prompt"
        viewModel.sendMessage(context)
        
        // Active states should be set
        assertTrue(viewModel.isGenerating)
        assertTrue(viewModel.isThinking)
        
        // Cancel the generation
        viewModel.stopGeneration()
        testScheduler.advanceUntilIdle()
        
        // Active states should be reset
        assertFalse(viewModel.isGenerating)
        assertFalse(viewModel.isThinking)
        assertEquals("Ready", viewModel.agenticStateLabel)
    }

    @Test
    fun testRepetitionLoopDetectionHaltsGeneration() = runTest(testDispatcher) {
        // Define a custom flow that emits repeating tokens
        fakeAI.customFlow = kotlinx.coroutines.flow.flow {
            emit("Standard prefix text ")
            repeat(30) {
                emit("🌌")
                kotlinx.coroutines.delay(10)
            }
        }
        
        viewModel.prompt = "Write emojis"
        viewModel.sendMessage(context)
        
        testScheduler.advanceUntilIdle()
        
        // Check that isGenerating was reset
        assertFalse(viewModel.isGenerating)
        
        // The last message in chatMessages must have the Neural Loop Protection warning
        assertTrue(viewModel.chatMessages.isNotEmpty())
        val lastMsg = viewModel.chatMessages.last()
        assertTrue(lastMsg.text.contains("[Repetition halted]"))
        
        // Clean up
        fakeAI.customFlow = null
    }

    @Test
    fun testCancellationPreservesPartialResponse() = runTest(testDispatcher) {
        // Define a custom flow that emits and suspends
        fakeAI.customFlow = kotlinx.coroutines.flow.flow {
            emit("Partial AI Response")
            kotlinx.coroutines.delay(2000)
            emit("This should not be reached")
        }
        
        viewModel.prompt = "Tell me something"
        viewModel.sendMessage(context)
        
        // Let it run until it suspends (after emitting first chunk)
        testScheduler.advanceTimeBy(500)
        
        // Verify currentResponseChunk has the first chunk
        assertEquals("Partial AI Response", viewModel.currentResponseChunk)
        
        // Stop generation
        viewModel.stopGeneration()
        testScheduler.advanceUntilIdle()
        
        // Verify that the partial message was added to chatMessages
        assertEquals(2, viewModel.chatMessages.size) // User message + Partial Assistant message
        assertEquals("Tell me something", viewModel.chatMessages[0].text)
        assertEquals("Partial AI Response", viewModel.chatMessages[1].text)
        
        // Clean up
        fakeAI.customFlow = null
    }

    @Test
    fun testLockAppOnBackgroundOrClearSessionKeysStopsGeneration() = runTest(testDispatcher) {
        viewModel.prompt = "Test Cancel Background Prompt"
        viewModel.sendMessage(context)
        
        assertTrue(viewModel.isGenerating)
        
        // Locking app / clearing keys should call stopGeneration
        viewModel.clearActiveSessionKeys()
        testScheduler.advanceUntilIdle()
        
        assertFalse(viewModel.isGenerating)
    }

    @Test
    fun testLockSessionStopsGenerationForActiveSession() = runTest(testDispatcher) {
        val sessionId = "active-generation-session"
        val session = ChatSession(
            id = sessionId,
            title = "Generating Session",
            createdAt = 1000L,
            lastActive = 2000L,
            modelPath = null,
            lastSearchQuery = null
        )
        fakeSessionRepo.saveSession(session)
        
        viewModel.loadSession(sessionId)
        testScheduler.advanceUntilIdle()
        
        viewModel.prompt = "Generate something long"
        viewModel.sendMessage(context)
        
        assertTrue(viewModel.isGenerating)
        
        // Lock this active session
        var lockSuccess = false
        val mockContext = mock<Context>()
        val mockAssets = mock<android.content.res.AssetManager>()
        org.mockito.kotlin.whenever(mockContext.assets).thenReturn(mockAssets)
        val realFile = java.io.File("app/src/main/assets/bip39_english.txt").let {
            if (it.exists()) it else java.io.File("src/main/assets/bip39_english.txt")
        }
        org.mockito.kotlin.whenever(mockAssets.open("bip39_english.txt")).thenAnswer {
            java.io.FileInputStream(realFile)
        }

        viewModel.lockSession(sessionId, "password_123", enableBiometric = false, context = mockContext) { success, _ ->
            lockSuccess = success
        }
        testScheduler.advanceUntilIdle()
        
        // Should immediately stop generation
        assertFalse(viewModel.isGenerating)
    }

    @Test
    fun testSizeLimitedInputStreamThrowsOnLimitExceeded() {
        val rawData = "a".repeat(15)
        val stream = com.rajpawardotin.kosh.data.SizeLimitedInputStream(rawData.byteInputStream(), 10)
        
        try {
            val buf = ByteArray(20)
            stream.read(buf)
            fail("Expected IOException was not thrown")
        } catch (e: java.io.IOException) {
            assertTrue(e.message!!.contains("secure size limit"))
        }
    }

    @Test
    fun testTextChunkingSlidingWindow() {
        val usecase = com.rajpawardotin.kosh.domain.usecase.DocumentProcessingUseCase(fakeDocumentRepo)
        val method = com.rajpawardotin.kosh.domain.usecase.DocumentProcessingUseCase::class.java.getDeclaredMethod("chunkText", String::class.java, Int::class.java, Int::class.java)
        method.isAccessible = true
        
        val longText = "a".repeat(1800)
        @Suppress("UNCHECKED_CAST")
        val chunks = method.invoke(usecase, longText, 1000, 200) as List<String>
        
        assertEquals(2, chunks.size)
        assertEquals(1000, chunks[0].length)
        assertEquals(1000, chunks[1].length)
    }

    @Test
    fun testRAMPurgingOnSwitchAndLock() = runTest(testDispatcher) {
        val doc = com.rajpawardotin.kosh.domain.model.SessionDocument(
            id = "doc1",
            sessionId = "sess1",
            fileName = "file.txt",
            fileType = "txt",
            fileSize = 100,
            chunkIndex = 0,
            chunkText = "hello text",
            isEncrypted = true,
            createdAt = 1000L
        )
        viewModel.activeSessionDocuments.add(doc)
        assertFalse(viewModel.activeSessionDocuments.isEmpty())
        
        // Lock app/keys -> activeSessionDocuments must be cleared
        viewModel.clearActiveSessionKeys()
        assertTrue(viewModel.activeSessionDocuments.isEmpty())
        
        // Populate again
        viewModel.activeSessionDocuments.add(doc)
        assertFalse(viewModel.activeSessionDocuments.isEmpty())
        
        // Load another session -> activeSessionDocuments must be cleared
        val mockSession = ChatSession(id = "sess2", title = "Sess 2", createdAt = 0L, lastActive = 0L, modelPath = null, lastSearchQuery = null)
        fakeSessionRepo.saveSession(mockSession)
        
        viewModel.loadSession("sess2")
        testScheduler.advanceUntilIdle()
        
        assertTrue(viewModel.activeSessionDocuments.isEmpty())
    }

    @Test
    fun testAutoIgnitionWhenEngineNotInitialized() = runTest(testDispatcher) {
        // Set engine to not initialized
        fakeAI.isInitialized = false
        fakeAI.initializeCallCount = 0
        viewModel.modelPath = "fake/model/path.bin"
        
        viewModel.prompt = "Hello AI"
        viewModel.sendMessage(context)
        testScheduler.advanceUntilIdle()
        
        // Engine must be auto-initialized
        assertEquals(1, fakeAI.initializeCallCount)
        assertTrue(fakeAI.isInitialized)
        assertTrue(viewModel.isEngineReady)
        
        // Message should be processed successfully
        assertEquals(2, viewModel.chatMessages.size)
        assertEquals("Mock response from Kosh", viewModel.chatMessages[1].text)
    }

    @Test
    fun testModelDoesNotSwapOnCodingKeywords() = runTest(testDispatcher) {
        viewModel.modelPath = "fake/model/path.bin"
        fakeAI.isInitialized = true
        fakeAI.initializeCallCount = 0
        
        // Even with coding keywords in the prompt, there should be no model swapping/re-initialisation
        viewModel.prompt = "Write a Kotlin class to sort an array"
        viewModel.sendMessage(context)
        testScheduler.advanceUntilIdle()
        
        // Model path should remain the same and no re-initialization calls should have occurred
        assertEquals("fake/model/path.bin", viewModel.modelPath)
        assertEquals(0, fakeAI.initializeCallCount)
    }

    @Test
    fun testSetupScreenshotPasscodeUpdatesStateAndSettings() = runTest(testDispatcher) {
        var setupCallbackCalled = false
        var setupSuccess = false
        
        viewModel.setupScreenshotPasscode("screenshot123", false, context) { success ->
            setupCallbackCalled = true
            setupSuccess = success
        }
        testScheduler.advanceUntilIdle()
        
        assertTrue(setupCallbackCalled)
        assertTrue(setupSuccess)
        assertTrue(viewModel.isScreenshotPasscodeSet)
        assertFalse(viewModel.isScreenshotBiometricEnabled)
        assertTrue(viewModel.isScreenshotEnabled)
        
        val encryptedKey = fakeSettings.getString("screenshot_encrypted_key", "")
        assertTrue(encryptedKey.isNotEmpty())
    }

    @Test
    fun testUnlockScreenshotWithCorrectPasswordSucceeds() = runTest(testDispatcher) {
        viewModel.setupScreenshotPasscode("screenshot123", false, context) { }
        testScheduler.advanceUntilIdle()
        
        // Turn screenshots off
        viewModel.toggleScreenshot(false)
        assertFalse(viewModel.isScreenshotEnabled)
        
        var unlockCallbackCalled = false
        var unlockSuccess = false
        viewModel.unlockScreenshotWithPassword("screenshot123") { success ->
            unlockCallbackCalled = true
            unlockSuccess = success
        }
        testScheduler.advanceUntilIdle()
        
        assertTrue(unlockCallbackCalled)
        assertTrue(unlockSuccess)
        assertTrue(viewModel.isScreenshotEnabled)
    }

    @Test
    fun testUnlockScreenshotWithIncorrectPasswordFails() = runTest(testDispatcher) {
        viewModel.setupScreenshotPasscode("screenshot123", false, context) { }
        testScheduler.advanceUntilIdle()
        
        // Turn screenshots off
        viewModel.toggleScreenshot(false)
        assertFalse(viewModel.isScreenshotEnabled)
        
        var unlockCallbackCalled = false
        var unlockSuccess = false
        viewModel.unlockScreenshotWithPassword("wrongpass") { success ->
            unlockCallbackCalled = true
            unlockSuccess = success
        }
        testScheduler.advanceUntilIdle()
        
        assertTrue(unlockCallbackCalled)
        assertFalse(unlockSuccess)
        assertFalse(viewModel.isScreenshotEnabled)
    }

    @Test
    fun testSendMessageIncludesHistoryInPrompt() = runTest(testDispatcher) {
        val sessionId = "history-session-id"
        val session = ChatSession(
            id = sessionId,
            title = "History Thread",
            createdAt = 1000L,
            lastActive = 2000L,
            modelPath = null,
            lastSearchQuery = null
        )
        fakeSessionRepo.saveSession(session)

        val msg1 = ChatMessage(id = "msg1", text = "Hello", isUser = true)
        val msg2 = ChatMessage(id = "msg2", text = "Hi there", isUser = false)
        fakeMessageRepo.saveMessage(sessionId, msg1)
        fakeMessageRepo.saveMessage(sessionId, msg2)

        viewModel.loadSession(sessionId)
        testScheduler.advanceUntilIdle()

        viewModel.prompt = "What did I say first?"
        viewModel.sendMessage(context)
        testScheduler.advanceUntilIdle()

         val sentPrompt = fakeAI.lastSentPrompt
        assertNotNull(sentPrompt)
        assertTrue(sentPrompt!!.contains("--- START HISTORY ---"))
        assertTrue(sentPrompt.contains("- User: Hello"))
        assertTrue(sentPrompt.contains("- Assistant: Hi there"))
        assertTrue(sentPrompt.contains("### USER QUERY\nWhat did I say first?"))
    }

    @Test
    fun testStartNewChatClearsActiveSessionDocuments() = runTest(testDispatcher) {
        val mockDoc = com.rajpawardotin.kosh.domain.model.SessionDocument(
            id = "doc-test",
            sessionId = "session-test",
            fileName = "Day 9 DT.pdf",
            fileType = "pdf",
            fileSize = 1024L,
            chunkIndex = 0,
            chunkText = "This is a document sample.",
            isEncrypted = false,
            createdAt = 12345L
        )
        viewModel.activeSessionDocuments.add(mockDoc)
        assertEquals(1, viewModel.activeSessionDocuments.size)

        // Act
        viewModel.startNewChat()
        testScheduler.advanceUntilIdle()

        // Assert that activeSessionDocuments is cleared
        assertTrue(viewModel.activeSessionDocuments.isEmpty())
    }

    class FakeAIProvider : AIProvider {
        override var isInitialized: Boolean = true
        var initializeCallCount = 0
        var lastSentPrompt: String? = null
        var customFlow: Flow<String>? = null
        override suspend fun initialize(modelPath: String, backend: String): Result<Unit> {
            initializeCallCount++
            isInitialized = true
            return Result.success(Unit)
        }
        override fun sendMessage(prompt: String): Flow<String> {
            lastSentPrompt = prompt
            return customFlow ?: flowOf("Mock", " response", " from", " Kosh")
        }
        override fun close() {}
    }

    class FakeSearchProvider : SearchProvider {
        override suspend fun performSearch(
            query: String,
            searchEngine: String,
            onStatusUpdate: (String) -> Unit
        ): com.rajpawardotin.kosh.domain.provider.SearchResponse {
            return com.rajpawardotin.kosh.domain.provider.SearchResponse(
                contextText = "Mock search results",
                sources = listOf(
                    com.rajpawardotin.kosh.domain.provider.SearchSource(
                        title = "Mock Title",
                        url = "https://mocksite.com",
                        snippet = "Mock search results"
                    )
                )
            )
        }
    }

    class FakeTtsProvider : com.rajpawardotin.kosh.data.TtsProvider {
        override val currentlySpeakingMessageId = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)
        override fun speak(messageId: String, text: String) {}
        override fun stop() {}
        override fun shutdown() {}
    }

    @Test
    fun testCreateTagAndRetrieve() = runTest(testDispatcher) {
        viewModel.createTag("Journal", "#8B5CF6")
        testScheduler.advanceUntilIdle()
        
        assertEquals(1, viewModel.allTags.size)
        assertEquals("Journal", viewModel.allTags[0].name)
        assertEquals("#8B5CF6", viewModel.allTags[0].colorHex)
    }

    @Test
    fun testCreateDuplicateTagFails() = runTest(testDispatcher) {
        viewModel.createTag("Journal", "#8B5CF6")
        testScheduler.advanceUntilIdle()
        
        viewModel.createTag("Journal", "#EF4444")
        testScheduler.advanceUntilIdle()
        
        // Count should still be 1
        assertEquals(1, viewModel.allTags.size)
    }

    @Test
    fun testUpdateTagNoWarningWhenNotAssociated() = runTest(testDispatcher) {
        viewModel.createTag("Journal", "#8B5CF6")
        testScheduler.advanceUntilIdle()
        
        var warningCalled = false
        viewModel.updateTag("Journal", "Journal Log", "#EF4444") { _, _ ->
            warningCalled = true
        }
        testScheduler.advanceUntilIdle()
        
        assertFalse(warningCalled)
        assertEquals(1, viewModel.allTags.size)
        assertEquals("Journal Log", viewModel.allTags[0].name)
        assertEquals("#EF4444", viewModel.allTags[0].colorHex)
    }

    @Test
    fun testUpdateTagWithWarningWhenAssociated() = runTest(testDispatcher) {
        viewModel.createTag("Journal", "#8B5CF6")
        testScheduler.advanceUntilIdle()
        
        // Start a chat session and associate the tag
        viewModel.startNewChat()
        viewModel.prompt = "Test chat message"
        viewModel.sendMessage(context)
        testScheduler.advanceUntilIdle()
        
        viewModel.addTagToActiveSession("Journal")
        testScheduler.advanceUntilIdle()
        
        var warningCalled = false
        var warningCount = 0
        var proceedCallback: (() -> Unit)? = null
        
        viewModel.updateTag("Journal", "Journal Log", "#EF4444") { count, proceed ->
            warningCalled = true
            warningCount = count
            proceedCallback = proceed
        }
        testScheduler.advanceUntilIdle()
        
        assertTrue(warningCalled)
        assertEquals(1, warningCount)
        assertNotNull(proceedCallback)
        
        // Before proceeding, tag name should still be the old one
        assertEquals("Journal", viewModel.allTags[0].name)
        
        // Now trigger proceed
        proceedCallback!!.invoke()
        testScheduler.advanceUntilIdle()
        
        // Tag name should be updated
        assertEquals("Journal Log", viewModel.allTags[0].name)
    }

    @Test
    fun testDeleteTagWithWarningAndDisassociate() = runTest(testDispatcher) {
        viewModel.createTag("Journal", "#8B5CF6")
        testScheduler.advanceUntilIdle()
        
        viewModel.startNewChat()
        viewModel.prompt = "Test reflection journal"
        viewModel.sendMessage(context)
        testScheduler.advanceUntilIdle()
        
        viewModel.addTagToActiveSession("Journal")
        testScheduler.advanceUntilIdle()
        
        var warningCalled = false
        var warningCount = 0
        var proceedCallback: (() -> Unit)? = null
        
        viewModel.deleteTag("Journal") { count, proceed ->
            warningCalled = true
            warningCount = count
            proceedCallback = proceed
        }
        testScheduler.advanceUntilIdle()
        
        assertTrue(warningCalled)
        assertEquals(1, warningCount)
        assertNotNull(proceedCallback)
        
        // Proceed with deletion
        proceedCallback!!.invoke()
        testScheduler.advanceUntilIdle()
        
        // Tag should be deleted from allTags and activeSessionTags
        assertTrue(viewModel.allTags.isEmpty())
        assertTrue(viewModel.activeSessionTags.isEmpty())
    }

    @Test
    fun testStartNewChatWithTagsAssociatesCorrectly() = runTest(testDispatcher) {
        // Create the tag first so it is valid
        viewModel.createTag("Journal", "#8B5CF6")
        testScheduler.advanceUntilIdle()
        
        viewModel.startNewChatWithTags(isTemporary = false, listOf("Journal"))
        viewModel.prompt = "Write my day"
        viewModel.sendMessage(context)
        testScheduler.advanceUntilIdle()
        
        // The active session tags should contain "Journal"
        assertEquals(1, viewModel.activeSessionTags.size)
        assertEquals("Journal", viewModel.activeSessionTags[0].name)
    }
}


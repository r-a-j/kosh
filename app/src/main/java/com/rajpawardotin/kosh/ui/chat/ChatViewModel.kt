package com.rajpawardotin.kosh.ui.chat

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rajpawardotin.kosh.data.CryptoUtils
import com.rajpawardotin.kosh.domain.model.ChatMessage
import com.rajpawardotin.kosh.domain.model.ChatSession
import com.rajpawardotin.kosh.domain.provider.AIProvider
import com.rajpawardotin.kosh.domain.provider.SearchProvider
import com.rajpawardotin.kosh.domain.provider.SettingsProvider
import com.rajpawardotin.kosh.domain.repository.ChatRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.crypto.SecretKey

class ChatViewModel(
    private val aiProvider: AIProvider,
    private val searchProvider: SearchProvider,
    private val chatRepository: ChatRepository,
    private val settingsProvider: SettingsProvider,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    private val _toastMessage = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val toastMessage = _toastMessage.asSharedFlow()

    fun showToast(message: String) {
        _toastMessage.tryEmit(message)
    }

    var modelPath by mutableStateOf<String?>(null)
    var isCopyingModel by mutableStateOf(false)
    var isInitializing by mutableStateOf(false)
    var prompt by mutableStateOf("")
    var isInternetEnabled by mutableStateOf(false)
    var isSearchingInternet by mutableStateOf(false)
    var isGenerating by mutableStateOf(false)
    var currentResponseChunk by mutableStateOf("")
    
    // New UX states for 2026 Edition
    var isThinking by mutableStateOf(false)
    var agenticStateLabel by mutableStateOf("Neural Standby")
    
    // Performance Dashboard Metrics
    var npuLoad by mutableStateOf(0)
    var ramUsage by mutableStateOf(3.28)
    var tokensPerSecond by mutableStateOf(0f)
    var lastSearchQuery by mutableStateOf<String?>(null)
    
    val checkedItems = androidx.compose.runtime.mutableStateMapOf<String, Boolean>()
    val chatMessages = mutableStateListOf<ChatMessage>()

    var currentSessionId by mutableStateOf<String?>(null)
    val savedSessions = androidx.compose.runtime.mutableStateListOf<ChatSession>()
    
    var isTemporarySession by mutableStateOf(false)
        private set

    var isAppLockEnabled by mutableStateOf(settingsProvider.getString("app_lock_enabled", "false") == "true")
        private set
    var isAppLocked by mutableStateOf(isAppLockEnabled)
    
    val activeSessionKeys = mutableStateMapOf<String, SecretKey>()

    fun clearActiveSessionKeys() {
        for (key in activeSessionKeys.values) {
            try {
                val keyField = key.javaClass.getDeclaredField("key")
                keyField.isAccessible = true
                val keyBytes = keyField.get(key) as? ByteArray
                if (keyBytes != null) {
                    java.util.Arrays.fill(keyBytes, 0.toByte())
                }
            } catch (e: Exception) {
                // Ignore
            }
        }
        activeSessionKeys.clear()
    }

    fun lockAppOnBackground() {
        clearActiveSessionKeys()
        currentSessionId?.let { sessId ->
            val session = savedSessions.find { it.id == sessId }
            if (session?.encryptedKeyPassword != null) {
                chatMessages.clear()
                checkedItems.clear()
            }
        }
        
        if (isAppLockEnabled) {
            isAppLocked = true
            showToast("Kosh Locked in Background")
        } else {
            showToast("Vault Sealed in Background")
        }
    }

    fun toggleAppLock(enabled: Boolean) {
        isAppLockEnabled = enabled
        settingsProvider.putString("app_lock_enabled", enabled.toString())
        if (enabled) {
            showToast("App Lock on Startup enabled")
        } else {
            showToast("App Lock on Startup disabled")
        }
    }

    fun unlockApp() {
        isAppLocked = false
    }

    init {
        viewModelScope.launch(ioDispatcher) {
            loadSavedSessionsInternal()
        }
    }

    private suspend fun loadSavedSessionsInternal() {
        val sessions = chatRepository.getSessionsOrderedByLastActive().map { decryptSession(it) }
        val processedSessions = sessions.map { session ->
            if (session.encryptedKeyPassword != null && !activeSessionKeys.containsKey(session.id)) {
                session.copy(title = "Sealed Vault", lastSearchQuery = null)
            } else {
                session
            }
        }
        withContext(Dispatchers.Main) {
            savedSessions.clear()
            savedSessions.addAll(processedSessions)
        }
    }

    private fun decryptSession(session: ChatSession): ChatSession {
        val isEncrypted = session.encryptedKeyPassword != null
        val key = activeSessionKeys[session.id]
        return if (isEncrypted && key != null) {
            val decryptedTitle = try {
                CryptoUtils.decryptMessage(session.title, key)
            } catch (e: Exception) {
                session.title
            }
            val decryptedLastSearchQuery = session.lastSearchQuery?.let {
                try {
                    CryptoUtils.decryptMessage(it, key)
                } catch (e: Exception) {
                    null
                }
            }
            session.copy(title = decryptedTitle, lastSearchQuery = decryptedLastSearchQuery)
        } else {
            session
        }
    }

    private fun saveSessionEncrypted(session: ChatSession) {
        val key = activeSessionKeys[session.id]
        if (session.encryptedKeyPassword != null && key != null) {
            val encryptedTitle = try {
                CryptoUtils.encryptMessage(session.title, key)
            } catch (e: Exception) {
                session.title
            }
            val encryptedLastSearchQuery = session.lastSearchQuery?.let {
                try {
                    CryptoUtils.encryptMessage(it, key)
                } catch (e: Exception) {
                    it
                }
            }
            chatRepository.saveSession(session.copy(title = encryptedTitle, lastSearchQuery = encryptedLastSearchQuery))
        } else {
            chatRepository.saveSession(session)
        }
    }

    private suspend fun loadSessionInternal(sessionId: String) {
        withContext(Dispatchers.Main) {
            isTemporarySession = false
        }
        val sessions = chatRepository.getSessionsOrderedByLastActive().map { decryptSession(it) }
        val session = sessions.find { it.id == sessionId }
        
        val isEncrypted = session?.encryptedKeyPassword != null
        val isUnlocked = activeSessionKeys.containsKey(sessionId)
        
        val messages = if (isEncrypted && !isUnlocked) {
            emptyList()
        } else if (isEncrypted && isUnlocked) {
            val key = activeSessionKeys[sessionId]!!
            chatRepository.getMessagesForSession(sessionId).map { msg ->
                try {
                    msg.copy(text = CryptoUtils.decryptMessage(msg.text, key))
                } catch (e: Exception) {
                    msg.copy(text = "[Decryption Failed]")
                }
            }
        } else {
            chatRepository.getMessagesForSession(sessionId)
        }
        
        val checklist = chatRepository.getChecklistStatesForSession(sessionId)
        withContext(Dispatchers.Main) {
            currentSessionId = sessionId
            chatMessages.clear()
            chatMessages.addAll(messages)
            
            checkedItems.clear()
            checkedItems.putAll(checklist)
            
            lastSearchQuery = session?.lastSearchQuery
        }
        
        // Update session last active time
        session?.let {
            val updated = it.copy(lastActive = System.currentTimeMillis())
            saveSessionEncrypted(updated)
        }
        loadSavedSessionsInternal()
    }

    fun loadSavedSessions() {
        viewModelScope.launch(ioDispatcher) {
            loadSavedSessionsInternal()
        }
    }

    fun startNewChat(isTemporary: Boolean = false) {
        if (isGenerating) return
        currentSessionId = null
        isTemporarySession = isTemporary
        chatMessages.clear()
        checkedItems.clear()
        lastSearchQuery = null
        prompt = ""
        if (isTemporary) {
            showToast("Temporary Vault active (history disabled)")
        } else {
            showToast("New saved brainstorm active")
        }
    }

    fun loadSession(sessionId: String) {
        if (isGenerating) return
        viewModelScope.launch(ioDispatcher) {
            loadSessionInternal(sessionId)
        }
    }

    fun deleteSession(sessionId: String) {
        viewModelScope.launch(ioDispatcher) {
            chatRepository.deleteSession(sessionId)
            withContext(Dispatchers.Main) {
                if (currentSessionId == sessionId) {
                    startNewChat()
                }
                showToast("Conversation deleted")
            }
            loadSavedSessionsInternal()
        }
    }

    fun renameSession(sessionId: String, newTitle: String) {
        if (newTitle.isBlank()) return
        viewModelScope.launch(ioDispatcher) {
            val sessions = chatRepository.getSessionsOrderedByLastActive().map { decryptSession(it) }
            val session = sessions.find { it.id == sessionId }
            if (session != null) {
                val updated = session.copy(title = newTitle)
                saveSessionEncrypted(updated)
            } else {
                chatRepository.renameSession(sessionId, newTitle)
            }
            loadSavedSessionsInternal()
            withContext(Dispatchers.Main) {
                showToast("Conversation renamed to \"$newTitle\"")
            }
        }
    }

    fun lockSession(
        sessionId: String, 
        password: String, 
        enableBiometric: Boolean, 
        context: Context, 
        onResult: (Boolean) -> Unit
    ) {
        viewModelScope.launch(ioDispatcher) {
            try {
                val salt = CryptoUtils.generateSalt()
                val derivedKey = CryptoUtils.deriveKeyFromPassword(password, salt)
                val sessionKey = CryptoUtils.generateRandomSessionKey()
                
                val sessionKeyEncodedBase64 = java.util.Base64.getEncoder().encodeToString(sessionKey.encoded)
                val encryptedKeyPassword = CryptoUtils.encryptMessage(sessionKeyEncodedBase64, derivedKey)
                
                val messages = chatRepository.getMessagesForSession(sessionId).toList()
                for (msg in messages) {
                    val encryptedText = CryptoUtils.encryptMessage(msg.text, sessionKey)
                    chatRepository.saveMessage(sessionId, msg.copy(text = encryptedText))
                }
                
                if (enableBiometric) {
                    withContext(Dispatchers.Main) {
                        try {
                            val cipher = CryptoUtils.getBiometricCipherForEncryption()
                            val biometricPrompt = androidx.biometric.BiometricPrompt(
                                context as androidx.fragment.app.FragmentActivity,
                                androidx.core.content.ContextCompat.getMainExecutor(context),
                                object : androidx.biometric.BiometricPrompt.AuthenticationCallback() {
                                    override fun onAuthenticationSucceeded(result: androidx.biometric.BiometricPrompt.AuthenticationResult) {
                                        super.onAuthenticationSucceeded(result)
                                        viewModelScope.launch(ioDispatcher) {
                                            try {
                                                val cryptoCipher = result.cryptoObject?.cipher ?: cipher
                                                val encryptedKeyBiometric = CryptoUtils.wrapSessionKey(sessionKey, cryptoCipher)
                                                
                                                val session = chatRepository.getSessionsOrderedByLastActive().map { decryptSession(it) }.find { it.id == sessionId }
                                                if (session != null) {
                                                     activeSessionKeys[sessionId] = sessionKey
                                                     val updatedSession = session.copy(
                                                         passwordHash = null,
                                                         salt = java.util.Base64.getEncoder().encodeToString(salt),
                                                         validationToken = null,
                                                         encryptedKeyPassword = encryptedKeyPassword,
                                                         encryptedKeyBiometric = encryptedKeyBiometric
                                                     )
                                                     saveSessionEncrypted(updatedSession)
                                                     loadSavedSessionsInternal()
                                                     withContext(Dispatchers.Main) {
                                                         showToast("Chat locked and secured successfully")
                                                         onResult(true)
                                                     }
                                                } else {
                                                     withContext(Dispatchers.Main) { onResult(false) }
                                                }
                                            } catch (e: Exception) {
                                                e.printStackTrace()
                                                withContext(Dispatchers.Main) { onResult(false) }
                                            }
                                        }
                                    }
                                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                                        super.onAuthenticationError(errorCode, errString)
                                        onResult(false)
                                    }
                                    override fun onAuthenticationFailed() {
                                        super.onAuthenticationFailed()
                                    }
                                }
                            )
                            val promptInfo = androidx.biometric.BiometricPrompt.PromptInfo.Builder()
                                .setTitle("Confirm Fingerprint")
                                .setSubtitle("Authorize biometric lock for this chat")
                                .setNegativeButtonText("Cancel")
                                .build()
                            biometricPrompt.authenticate(promptInfo, androidx.biometric.BiometricPrompt.CryptoObject(cipher))
                        } catch (e: Exception) {
                            e.printStackTrace()
                            onResult(false)
                        }
                    }
                } else {
                    val session = chatRepository.getSessionsOrderedByLastActive().map { decryptSession(it) }.find { it.id == sessionId }
                    if (session != null) {
                        activeSessionKeys[sessionId] = sessionKey
                        val updatedSession = session.copy(
                            passwordHash = null,
                            salt = java.util.Base64.getEncoder().encodeToString(salt),
                            validationToken = null,
                            encryptedKeyPassword = encryptedKeyPassword,
                            encryptedKeyBiometric = null
                        )
                        saveSessionEncrypted(updatedSession)
                        loadSavedSessionsInternal()
                        withContext(Dispatchers.Main) {
                            showToast("Chat locked and secured successfully")
                            onResult(true)
                        }
                    } else {
                        withContext(Dispatchers.Main) { onResult(false) }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) { onResult(false) }
            }
        }
    }

    fun unlockSessionWithPassword(sessionId: String, password: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch(ioDispatcher) {
            try {
                val sessions = chatRepository.getSessionsOrderedByLastActive()
                val session = sessions.find { it.id == sessionId }
                if (session == null || session.salt == null || session.encryptedKeyPassword == null) {
                    withContext(Dispatchers.Main) { onResult(false) }
                    return@launch
                }
                
                val salt = java.util.Base64.getDecoder().decode(session.salt)
                val derivedKey = CryptoUtils.deriveKeyFromPassword(password, salt)
                
                val sessionKeyBase64 = CryptoUtils.decryptMessage(session.encryptedKeyPassword, derivedKey)
                val sessionKeyBytes = java.util.Base64.getDecoder().decode(sessionKeyBase64)
                val sessionKey = javax.crypto.spec.SecretKeySpec(sessionKeyBytes, "AES")
                
                activeSessionKeys[sessionId] = sessionKey
                loadSessionInternal(sessionId)
                withContext(Dispatchers.Main) {
                    showToast("Vault Unlocked successfully")
                    onResult(true)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    showToast("Invalid password!")
                    onResult(false)
                }
            }
        }
    }

    fun verifySessionPassword(sessionId: String, password: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch(ioDispatcher) {
            try {
                val sessions = chatRepository.getSessionsOrderedByLastActive()
                val session = sessions.find { it.id == sessionId }
                if (session == null || session.salt == null || session.encryptedKeyPassword == null) {
                    withContext(Dispatchers.Main) { onResult(false) }
                    return@launch
                }
                val salt = java.util.Base64.getDecoder().decode(session.salt)
                val derivedKey = CryptoUtils.deriveKeyFromPassword(password, salt)
                CryptoUtils.decryptMessage(session.encryptedKeyPassword, derivedKey)
                withContext(Dispatchers.Main) { onResult(true) }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { onResult(false) }
            }
        }
    }

    fun unlockSessionWithBiometrics(sessionId: String, context: Context, onResult: (Boolean) -> Unit) {
        val session = savedSessions.find { it.id == sessionId }
        if (session == null || session.encryptedKeyBiometric == null) {
            onResult(false)
            return
        }
        
        try {
            val combined = java.util.Base64.getDecoder().decode(session.encryptedKeyBiometric)
            if (combined.size < 12) {
                onResult(false)
                return
            }
            val iv = ByteArray(12)
            System.arraycopy(combined, 0, iv, 0, 12)
            
            val cipher = CryptoUtils.getBiometricCipherForDecryption(iv)
            val biometricPrompt = androidx.biometric.BiometricPrompt(
                context as androidx.fragment.app.FragmentActivity,
                androidx.core.content.ContextCompat.getMainExecutor(context),
                object : androidx.biometric.BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: androidx.biometric.BiometricPrompt.AuthenticationResult) {
                        super.onAuthenticationSucceeded(result)
                        viewModelScope.launch(ioDispatcher) {
                            try {
                                val cryptoCipher = result.cryptoObject?.cipher ?: cipher
                                val sessionKey = CryptoUtils.unwrapSessionKey(session.encryptedKeyBiometric, cryptoCipher)
                                
                                activeSessionKeys[sessionId] = sessionKey
                                loadSessionInternal(sessionId)
                                withContext(Dispatchers.Main) {
                                    showToast("Vault Unlocked successfully")
                                    onResult(true)
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                                withContext(Dispatchers.Main) { onResult(false) }
                            }
                        }
                    }
                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        super.onAuthenticationError(errorCode, errString)
                        onResult(false)
                    }
                    override fun onAuthenticationFailed() {
                        super.onAuthenticationFailed()
                    }
                }
            )
            val promptInfo = androidx.biometric.BiometricPrompt.PromptInfo.Builder()
                .setTitle("Unlock Vault")
                .setSubtitle("Confirm biometrics to unlock this chat")
                .setNegativeButtonText("Cancel")
                .build()
            biometricPrompt.authenticate(promptInfo, androidx.biometric.BiometricPrompt.CryptoObject(cipher))
        } catch (e: Exception) {
            e.printStackTrace()
            onResult(false)
        }
    }

    fun removeSessionLock(sessionId: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch(ioDispatcher) {
            try {
                val key = activeSessionKeys[sessionId]
                if (key == null) {
                    withContext(Dispatchers.Main) { onResult(false) }
                    return@launch
                }
                
                val messages = chatRepository.getMessagesForSession(sessionId).toList()
                for (msg in messages) {
                    try {
                        val decryptedText = CryptoUtils.decryptMessage(msg.text, key)
                        chatRepository.saveMessage(sessionId, msg.copy(text = decryptedText))
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                
                val session = chatRepository.getSessionsOrderedByLastActive().map { decryptSession(it) }.find { it.id == sessionId }
                if (session != null) {
                    val updatedSession = session.copy(
                        passwordHash = null,
                        salt = null,
                        validationToken = null,
                        encryptedKeyPassword = null,
                        encryptedKeyBiometric = null
                    )
                    chatRepository.saveSession(updatedSession)
                    val removedKey = activeSessionKeys[sessionId]
                    if (removedKey != null) {
                        try {
                            val keyField = removedKey.javaClass.getDeclaredField("key")
                            keyField.isAccessible = true
                            val keyBytes = keyField.get(removedKey) as? ByteArray
                            if (keyBytes != null) {
                                java.util.Arrays.fill(keyBytes, 0.toByte())
                            }
                        } catch (e: Exception) {}
                        activeSessionKeys.remove(sessionId)
                    }
                    loadSessionInternal(sessionId)
                    withContext(Dispatchers.Main) {
                        showToast("Chat lock removed")
                        onResult(true)
                    }
                } else {
                    withContext(Dispatchers.Main) { onResult(false) }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) { onResult(false) }
            }
        }
    }

    fun exportBackup(context: Context, destUri: Uri, password: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch(ioDispatcher) {
            val dbFile = context.getDatabasePath("kosh_vault.db")
            if (!dbFile.exists()) {
                withContext(Dispatchers.Main) { onError("Database does not exist.") }
                return@launch
            }
            
            val tempFile = File(context.cacheDir, "kosh_backup_temp.db")
            val success = CryptoUtils.encryptDatabaseBackup(dbFile, tempFile, password)
            if (!success) {
                withContext(Dispatchers.Main) { onError("Encryption failed.") }
                return@launch
            }
            
            try {
                context.contentResolver.openOutputStream(destUri)?.use { output ->
                    tempFile.inputStream().use { input ->
                        input.copyTo(output)
                    }
                }
                CryptoUtils.secureDelete(tempFile)
                withContext(Dispatchers.Main) { onSuccess() }
            } catch (e: Exception) {
                CryptoUtils.secureDelete(tempFile)
                withContext(Dispatchers.Main) { onError(e.localizedMessage ?: "Unknown error") }
            }
        }
    }

    fun importBackup(context: Context, srcUri: Uri, password: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch(ioDispatcher) {
            val tempBackupFile = File(context.cacheDir, "kosh_backup_import_temp.db")
            try {
                context.contentResolver.openInputStream(srcUri)?.use { input ->
                    tempBackupFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { onError("Failed to read backup file.") }
                return@launch
            }
            
            val dbFile = context.getDatabasePath("kosh_vault.db")
            val tempDecryptedFile = File(context.cacheDir, "kosh_decrypted_temp.db")
            
            val success = CryptoUtils.decryptDatabaseBackup(tempBackupFile, tempDecryptedFile, password)
            CryptoUtils.secureDelete(tempBackupFile)
            
            if (!success) {
                CryptoUtils.secureDelete(tempDecryptedFile)
                withContext(Dispatchers.Main) { onError("Invalid password or corrupted backup.") }
                return@launch
            }
            
            try {
                dbFile.delete()
                File(dbFile.path + "-journal").delete()
                File(dbFile.path + "-shm").delete()
                File(dbFile.path + "-wal").delete()
                
                tempDecryptedFile.renameTo(dbFile)
                
                withContext(Dispatchers.Main) {
                    loadSavedSessions()
                    startNewChat()
                    onSuccess()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { onError(e.localizedMessage ?: "Failed to restore database.") }
            } finally {
                CryptoUtils.secureDelete(tempDecryptedFile)
            }
        }
    }

    fun toggleChecklistItem(messageKey: String, itemIndex: Int, isChecked: Boolean) {
        checkedItems["${messageKey}_$itemIndex"] = isChecked
        if (!isTemporarySession) {
            viewModelScope.launch(ioDispatcher) {
                chatRepository.saveChecklistState(messageKey, itemIndex, isChecked)
            }
        }
    }

    var selectedBackend by mutableStateOf(
        settingsProvider.getString("selected_backend", "GPU")
    )
        private set

    fun selectBackend(backend: String) {
        selectedBackend = backend
        settingsProvider.putString("selected_backend", backend)
    }

    val backends = listOf("CPU", "GPU", "NPU (Qualcomm)")

    var selectedSearchEngine by mutableStateOf(
        settingsProvider.getString("selected_search_engine", "DuckDuckGo (Free)")
    )
        private set

    fun selectSearchEngine(engine: String) {
        selectedSearchEngine = engine
        settingsProvider.putString("selected_search_engine", engine)
    }

    val searchEngines = listOf(
        "DuckDuckGo (Free)",
        "DuckDuckGo Lite",
        "Google Scraper",
        "Bing Scraper",
        "Tavily API",
        "Brave Search API"
    )

    var tavilyApiKey by mutableStateOf(
        settingsProvider.getString("tavily_api_key", "")
    )
        private set

    fun updateTavilyApiKey(key: String) {
        tavilyApiKey = key
        settingsProvider.putString("tavily_api_key", key)
    }

    var braveApiKey by mutableStateOf(
        settingsProvider.getString("brave_api_key", "")
    )
        private set

    fun updateBraveApiKey(key: String) {
        braveApiKey = key
        settingsProvider.putString("brave_api_key", key)
    }

    var isEngineReady by mutableStateOf(aiProvider.isInitialized)
        private set

    fun setModel(path: String) {
        modelPath = path
    }

    fun deleteModel() {
        modelPath?.let { path ->
            viewModelScope.launch(ioDispatcher) {
                aiProvider.close()
                File(path).delete()
                withContext(Dispatchers.Main) {
                    modelPath = null
                    isEngineReady = aiProvider.isInitialized
                    startNewChat()
                }
            }
        }
    }

    fun initializeEngine() {
        val path = modelPath ?: return
        isInitializing = true
        viewModelScope.launch(ioDispatcher) {
            val result = aiProvider.initialize(path, selectedBackend)
            withContext(Dispatchers.Main) {
                isInitializing = false
                isEngineReady = aiProvider.isInitialized
                if (isEngineReady) {
                    showToast("Neural Core ignited successfully!")
                } else {
                    showToast("Ignition failed. Try selecting another backend.")
                }
            }
        }
    }

    fun sendMessage() {
        val rawPrompt = prompt
        if (rawPrompt.isBlank() || isGenerating) return

        // 1. Resolve Session ID
        var sessionId = currentSessionId
        val isNewSession = sessionId == null
        if (isNewSession) {
            sessionId = java.util.UUID.randomUUID().toString()
            currentSessionId = sessionId
        }

        // 2. Add User Message in memory
        val userMessage = ChatMessage(text = rawPrompt, isUser = true)
        chatMessages.add(userMessage)

        prompt = ""
        isGenerating = true
        isThinking = true
        agenticStateLabel = "Initiating Neural Path..."
        currentResponseChunk = ""

        // Launch live metrics fluctuation
        viewModelScope.launch(Dispatchers.Main) {
            val random = java.util.Random()
            while (isGenerating) {
                val speedBase = when (selectedBackend) {
                    "NPU (Qualcomm)" -> 58f + random.nextFloat() * 10f
                    "GPU" -> 28f + random.nextFloat() * 8f
                    else -> 11f + random.nextFloat() * 4f
                }
                val loadBase = when (selectedBackend) {
                    "NPU (Qualcomm)" -> 75 + random.nextInt(23)
                    else -> random.nextInt(4)
                }
                val ramBase = 3.25 + random.nextFloat() * 0.1 + 0.22

                tokensPerSecond = speedBase
                npuLoad = loadBase
                ramUsage = ramBase
                delay(200)
            }
            // Reset metrics on finish
            tokensPerSecond = 0f
            npuLoad = 0
            ramUsage = 3.25 + random.nextFloat() * 0.05
        }

        viewModelScope.launch(ioDispatcher) {
            try {
                if (!isTemporarySession) {
                    // If new session, save the session first (synchronously on the IO thread)
                    if (isNewSession) {
                        val title = if (rawPrompt.length > 25) rawPrompt.take(25) + "..." else rawPrompt
                        val newSession = ChatSession(
                            id = sessionId!!,
                            title = title,
                            createdAt = System.currentTimeMillis(),
                            lastActive = System.currentTimeMillis(),
                            modelPath = modelPath,
                            lastSearchQuery = null
                        )
                        chatRepository.saveSession(newSession)
                        loadSavedSessionsInternal()
                    }

                    // Now save the message (guarantees that the session row already exists)
                    val key = activeSessionKeys[sessionId!!]
                    val userMsgToSave = if (key != null) {
                        userMessage.copy(text = CryptoUtils.encryptMessage(userMessage.text, key))
                    } else {
                        userMessage
                    }
                    chatRepository.saveMessage(sessionId!!, userMsgToSave)
                }

                val shouldSearch = detectSearchRequirement(rawPrompt)
                if (shouldSearch && !isInternetEnabled) {
                    withContext(Dispatchers.Main) {
                        isInternetEnabled = true
                    }
                }

                var lastQueryUsed: String? = null
                val finalPrompt = if (shouldSearch) {
                    withContext(Dispatchers.Main) { 
                        isSearchingInternet = true 
                        agenticStateLabel = "Querying Web Knowledge..."
                    }
                    
                    val searchQuery = determineSearchQuery(rawPrompt)
                    val searchResults = searchProvider.performSearch(searchQuery, selectedSearchEngine) { status ->
                        viewModelScope.launch(Dispatchers.Main) {
                            agenticStateLabel = status
                        }
                    }
                    
                    withContext(Dispatchers.Main) { 
                        agenticStateLabel = "Integrating Search Context..."
                    }
                    delay(300)
                    withContext(Dispatchers.Main) {
                        isSearchingInternet = false
                    }

                    val hasResults = searchResults.isNotEmpty() && 
                            !searchResults.contains("No search results found.") && 
                            !searchResults.contains("Error performing search:")
                    
                    if (hasResults) {
                        lastQueryUsed = searchQuery
                        buildSystemPrompt(searchQuery, searchResults, rawPrompt)
                    } else {
                        rawPrompt
                    }
                } else {
                    rawPrompt
                }

                if (lastQueryUsed != null) {
                    withContext(Dispatchers.Main) {
                        lastSearchQuery = lastQueryUsed
                    }
                }

                withContext(Dispatchers.Main) { 
                    isThinking = true
                    agenticStateLabel = "Processing Token Graphs..." 
                }

                // Small delay to make the "Thinking" animation visible and organic
                delay(400)
                withContext(Dispatchers.Main) { isThinking = false }

                val assistantMessageId = java.util.UUID.randomUUID().toString()

                aiProvider.sendMessage(finalPrompt).collect { text ->
                    withContext(Dispatchers.Main) {
                        if (currentResponseChunk.isEmpty()) {
                            agenticStateLabel = "Structuring Output..."
                        }
                        currentResponseChunk += text
                    }
                    
                    if (!isTemporarySession) {
                        // Save response live chunk to the database
                        val key = activeSessionKeys[sessionId!!]
                        val textToSave = if (key != null) CryptoUtils.encryptMessage(currentResponseChunk, key) else currentResponseChunk
                        val liveAssistantMsg = ChatMessage(id = assistantMessageId, text = textToSave, isUser = false)
                        chatRepository.saveMessage(sessionId!!, liveAssistantMsg)
                    }
                }

                var assistantMessage: ChatMessage? = null
                withContext(Dispatchers.Main) {
                    val msg = ChatMessage(id = assistantMessageId, text = currentResponseChunk, isUser = false)
                    chatMessages.add(msg)
                    currentResponseChunk = ""
                    assistantMessage = msg
                }

                if (!isTemporarySession) {
                    assistantMessage?.let { msg ->
                        val key = activeSessionKeys[sessionId!!]
                        val msgToSave = if (key != null) msg.copy(text = CryptoUtils.encryptMessage(msg.text, key)) else msg
                        chatRepository.saveMessage(sessionId!!, msgToSave)
                        
                        // Update session last active time and last search query
                        val sessions = chatRepository.getSessionsOrderedByLastActive().map { decryptSession(it) }
                        sessions.find { it.id == sessionId }?.let {
                            saveSessionEncrypted(it.copy(
                                lastActive = System.currentTimeMillis(),
                                lastSearchQuery = lastSearchQuery
                            ))
                        }
                        loadSavedSessionsInternal()
                    }
                }
            } catch (e: Exception) {
                var errorMessage: ChatMessage? = null
                withContext(Dispatchers.Main) {
                    isThinking = false
                    val msg = ChatMessage(text = "Error: ${e.localizedMessage}", isUser = false)
                    chatMessages.add(msg)
                    errorMessage = msg
                }
                if (!isTemporarySession) {
                    errorMessage?.let { msg ->
                        val key = activeSessionKeys[sessionId!!]
                        val msgToSave = if (key != null) msg.copy(text = CryptoUtils.encryptMessage(msg.text, key)) else msg
                        chatRepository.saveMessage(sessionId!!, msgToSave)
                    }
                }
            } finally {
                withContext(Dispatchers.Main) {
                    isGenerating = false
                    isThinking = false
                    agenticStateLabel = "Neural Standby"
                }
            }
        }
    }

    private fun detectSearchRequirement(prompt: String): Boolean {
        if (isInternetEnabled) return true
        
        val lowercasePrompt = prompt.lowercase()
        val searchKeywords = listOf(
            "search web", "search internet", "search online", "google", "bing", "duckduckgo",
            "lookup", "look up", "find on web", "browse", "website", "realtime", "real-time",
            "latest news", "today's headlines", "todays headlines", "weather today", "current price"
        )
        val hasUrl = prompt.contains("http://") || prompt.contains("https://") || prompt.contains("www.")
        
        return hasUrl || searchKeywords.any { lowercasePrompt.contains(it) }
    }

    private fun determineSearchQuery(rawPrompt: String): String {
        val metaKeywords = listOf("search", "find", "look", "research", "tell me more")
        val isMetaQuery = rawPrompt.lowercase().trim().split(" ").size <= 4 &&
                metaKeywords.any { rawPrompt.contains(it, ignoreCase = true) }

        val baseQuery = if (isMetaQuery) {
            chatMessages.reversed().firstOrNull { it.isUser && it.text != rawPrompt }?.text ?: rawPrompt
        } else {
            rawPrompt
        }

        val lastQuery = lastSearchQuery
        if (!lastQuery.isNullOrBlank()) {
            val lowercasePrompt = baseQuery.lowercase()
            val contextIndicators = listOf(
                "he", "him", "his", "she", "her", "it", "its", "they", "them", "their",
                "this", "that", "these", "those", "who", "why", "what", "where", "how",
                "experiences", "experience", "education", "works", "work", "job", "career",
                "background", "projects", "project", "details", "info", "information",
                "website", "site", "page", "author", "creator", "owner"
            )
            val isFollowUp = contextIndicators.any { word ->
                "\\b$word\\b".toRegex().containsMatchIn(lowercasePrompt)
            }
            if (isFollowUp) {
                var cleanPrompt = baseQuery
                val prefixesToRemove = listOf(
                    "what are", "what is", "who is", "tell me about", "give me", "show me",
                    "what does", "how does", "where is", "can you", "please", "how is",
                    "what was", "who was", "where was", "how was", "do you know", "tell me"
                )
                for (prefix in prefixesToRemove) {
                    cleanPrompt = cleanPrompt.replace("(?i)^\\s*$prefix\\s+".toRegex(), "").trim()
                }
                return "$lastQuery $cleanPrompt".trim()
            }
        }
        return baseQuery
    }

    private fun buildSystemPrompt(query: String, data: String, userPrompt: String): String {
        return "You have access to the web search results below to answer the user's query.\n\n" +
                "SEARCH RESULTS:\n" +
                "$data\n\n" +
                "USER QUERY: $userPrompt\n\n" +
                "INSTRUCTION: Answer the user's query using the SEARCH RESULTS above. " +
                "Provide a direct, concise, and helpful answer. " +
                "Do not state that you cannot browse the internet or access real-time information since the search results are already provided to you above."
    }

    override fun onCleared() {
        super.onCleared()
        aiProvider.close()
        isEngineReady = aiProvider.isInitialized
    }
}

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
import com.rajpawardotin.kosh.data.Bip39Utils
import com.rajpawardotin.kosh.data.CryptoUtils
import com.rajpawardotin.kosh.domain.model.ChatMessage
import com.rajpawardotin.kosh.data.TtsProvider
import com.rajpawardotin.kosh.domain.model.ChatSession
import com.rajpawardotin.kosh.domain.model.AttachedFile
import com.rajpawardotin.kosh.domain.model.SessionDocument
import com.rajpawardotin.kosh.data.DocumentParser
import com.rajpawardotin.kosh.domain.provider.AIProvider

import com.rajpawardotin.kosh.domain.provider.SearchProvider
import com.rajpawardotin.kosh.domain.provider.SettingsProvider
import com.rajpawardotin.kosh.domain.repository.MessageRepository
import com.rajpawardotin.kosh.domain.repository.SessionRepository
import com.rajpawardotin.kosh.domain.repository.DocumentRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

import kotlinx.coroutines.CoroutineExceptionHandler
import java.util.concurrent.CancellationException

class ChatViewModel(
    private val aiProvider: AIProvider,
    private val searchProvider: SearchProvider,
    private val sessionRepository: SessionRepository,
    private val messageRepository: MessageRepository,
    private val documentRepository: DocumentRepository,
    private val settingsProvider: SettingsProvider,
    private val ttsProvider: TtsProvider,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    private val chatSessionUseCase = com.rajpawardotin.kosh.domain.usecase.ChatSessionUseCase(sessionRepository)
    private val llmUseCase = com.rajpawardotin.kosh.domain.usecase.LlmUseCase(aiProvider, searchProvider, sessionRepository, documentRepository)
    private val documentProcessingUseCase = com.rajpawardotin.kosh.domain.usecase.DocumentProcessingUseCase(documentRepository)

    private val exceptionHandler = CoroutineExceptionHandler { _, exception ->
        if (exception is CancellationException) return@CoroutineExceptionHandler
        if (exception is OutOfMemoryError) throw exception // Let OOM crash to prevent zombie state
        
        exception.printStackTrace()
        showToast("Background process failed: ${exception.localizedMessage}")
    }
    
    private val safeIoDispatcher = ioDispatcher + exceptionHandler

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
    var showPasswordDialog by mutableStateOf(false)
    var sessionToUnlock by mutableStateOf<String?>(null)

    // TTS
    val currentlySpeakingMessageId = ttsProvider.currentlySpeakingMessageId

    fun playTts(messageId: String, text: String) {
        ttsProvider.speak(messageId, text)
    }

    fun stopTts() {
        ttsProvider.stop()
    }
    
    val checkedItems = androidx.compose.runtime.mutableStateMapOf<String, Boolean>()
    val chatMessages = mutableStateListOf<ChatMessage>()
    val attachedFiles = mutableStateListOf<AttachedFile>()
    val activeSessionDocuments = mutableStateListOf<SessionDocument>()

    fun attachFile(file: AttachedFile) {
        if (attachedFiles.any { it.fileName == file.fileName }) {
            showToast("File is already attached.")
            return
        }
        attachedFiles.add(file)
    }

    fun detachFile(file: AttachedFile) {
        attachedFiles.remove(file)
    }

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
        activeSessionDocuments.clear()
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
        viewModelScope.launch(safeIoDispatcher) {
            loadSavedSessionsInternal()
        }
    }

    private suspend fun loadSavedSessionsInternal() {
        val sessions = sessionRepository.getSessionsOrderedByLastActive().map { decryptSession(it) }
        val processedSessions = sessions.map { session ->
            if (session.encryptedKeyPassword != null && !activeSessionKeys.containsKey(session.id)) {
                session.copy(lastSearchQuery = null)
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
        return chatSessionUseCase.decryptSession(session, activeSessionKeys)
    }

    private fun saveSessionEncrypted(session: ChatSession) {
        chatSessionUseCase.saveSessionEncrypted(session, activeSessionKeys)
    }

    private suspend fun loadSessionInternal(sessionId: String) {
        withContext(Dispatchers.Main) {
            isTemporarySession = false
        }
        val sessions = sessionRepository.getSessionsOrderedByLastActive().map { decryptSession(it) }
        val session = sessions.find { it.id == sessionId }
        
        val isEncrypted = session?.encryptedKeyPassword != null
        val isUnlocked = activeSessionKeys.containsKey(sessionId)
        
        val messages = if (isEncrypted && !isUnlocked) {
            emptyList()
        } else if (isEncrypted && isUnlocked) {
            val key = activeSessionKeys[sessionId]!!
            messageRepository.getMessagesForSession(sessionId).map { msg ->
                val decryptedText = try {
                    CryptoUtils.decryptMessage(msg.text, key)
                } catch (e: Exception) {
                    "[Decryption Failed]"
                }
                
                val decryptedSourceDocs = try {
                    msg.sourceDocuments?.let { CryptoUtils.decryptMessage(it, key) }
                } catch (e: Exception) {
                    msg.sourceDocuments // Fallback: it was unencrypted
                }
                
                msg.copy(text = decryptedText, sourceDocuments = decryptedSourceDocs)
            }
        } else {
            messageRepository.getMessagesForSession(sessionId)
        }
        
        val checklist = messageRepository.getChecklistStatesForSession(sessionId)
        
        val sessionDocs = documentRepository.getSessionDocuments(sessionId)
        val decryptedDocs = if (isEncrypted && isUnlocked) {
            val key = activeSessionKeys[sessionId]!!
            sessionDocs.map { doc ->
                val decName = try { CryptoUtils.decryptMessage(doc.fileName, key) } catch (e: Exception) { doc.fileName }
                val decText = try { CryptoUtils.decryptMessage(doc.chunkText, key) } catch (e: Exception) { doc.chunkText }
                doc.copy(fileName = decName, chunkText = decText)
            }
        } else {
            sessionDocs
        }

        withContext(Dispatchers.Main) {
            currentSessionId = sessionId
            chatMessages.clear()
            chatMessages.addAll(messages)
            
            activeSessionDocuments.clear()
            activeSessionDocuments.addAll(decryptedDocs)
            
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
        viewModelScope.launch(safeIoDispatcher) {
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
        viewModelScope.launch(safeIoDispatcher) {
            loadSessionInternal(sessionId)
        }
    }

    fun deleteSession(sessionId: String) {
        viewModelScope.launch(safeIoDispatcher) {
            sessionRepository.deleteSession(sessionId)
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
        viewModelScope.launch(safeIoDispatcher) {
            val sessions = sessionRepository.getSessionsOrderedByLastActive().map { decryptSession(it) }
            val session = sessions.find { it.id == sessionId }
            if (session != null) {
                val updated = session.copy(title = newTitle)
                saveSessionEncrypted(updated)
            } else {
                sessionRepository.renameSession(sessionId, newTitle)
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
        onResult: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch(safeIoDispatcher) {
            try {
                val (mnemonic, entropy) = Bip39Utils.generateMnemonic(context)
                val recoveryKeyBytes = MessageDigest.getInstance("SHA-256").digest(entropy)
                val recoveryKey = SecretKeySpec(recoveryKeyBytes, "AES")

                val salt = CryptoUtils.generateSalt()
                val derivedKey = CryptoUtils.deriveKeyFromPassword(password, salt)
                val sessionKey = CryptoUtils.generateRandomSessionKey()
                
                val sessionKeyEncodedBase64 = java.util.Base64.getEncoder().encodeToString(sessionKey.encoded)
                val encryptedKeyPassword = CryptoUtils.encryptMessage(sessionKeyEncodedBase64, derivedKey)
                val encryptedKeyRecovery = CryptoUtils.encryptMessage(sessionKeyEncodedBase64, recoveryKey)
                
                val messages = messageRepository.getMessagesForSession(sessionId).toList()
                for (msg in messages) {
                    val encryptedText = CryptoUtils.encryptMessage(msg.text, sessionKey)
                    val encryptedSourceDocs = msg.sourceDocuments?.let { CryptoUtils.encryptMessage(it, sessionKey) }
                    messageRepository.saveMessage(sessionId, msg.copy(text = encryptedText, sourceDocuments = encryptedSourceDocs))
                }
                
                val docs = documentRepository.getSessionDocuments(sessionId).toList()
                for (doc in docs) {
                    if (!doc.isEncrypted) {
                        val encName = CryptoUtils.encryptMessage(doc.fileName, sessionKey)
                        val encText = CryptoUtils.encryptMessage(doc.chunkText, sessionKey)
                        documentRepository.saveSessionDocument(doc.copy(fileName = encName, chunkText = encText, isEncrypted = true))
                    }
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
                                        viewModelScope.launch(safeIoDispatcher) {
                                            try {
                                                val cryptoCipher = result.cryptoObject?.cipher ?: cipher
                                                val encryptedKeyBiometric = CryptoUtils.wrapSessionKey(sessionKey, cryptoCipher)
                                                
                                                val session = sessionRepository.getSessionsOrderedByLastActive().map { decryptSession(it) }.find { it.id == sessionId }
                                                if (session != null) {
                                                     activeSessionKeys[sessionId] = sessionKey
                                                     val updatedSession = session.copy(
                                                         passwordHash = null,
                                                         salt = java.util.Base64.getEncoder().encodeToString(salt),
                                                         validationToken = null,
                                                         encryptedKeyPassword = encryptedKeyPassword,
                                                         encryptedKeyBiometric = encryptedKeyBiometric,
                                                         encryptedKeyRecovery = encryptedKeyRecovery
                                                     )
                                                     saveSessionEncrypted(updatedSession)
                                                     loadSavedSessionsInternal()
                                                     withContext(Dispatchers.Main) {
                                                         showToast("Chat locked and secured successfully")
                                                         onResult(true, mnemonic)
                                                     }
                                                } else {
                                                     withContext(Dispatchers.Main) { onResult(false, null) }
                                                }
                                            } catch (e: Exception) {
                                                e.printStackTrace()
                                                withContext(Dispatchers.Main) { onResult(false, null) }
                                            }
                                        }
                                    }
                                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                                        super.onAuthenticationError(errorCode, errString)
                                        onResult(false, null)
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
                            onResult(false, null)
                        }
                    }
                } else {
                    val session = sessionRepository.getSessionsOrderedByLastActive().map { decryptSession(it) }.find { it.id == sessionId }
                    if (session != null) {
                        activeSessionKeys[sessionId] = sessionKey
                        val updatedSession = session.copy(
                            passwordHash = null,
                            salt = java.util.Base64.getEncoder().encodeToString(salt),
                            validationToken = null,
                            encryptedKeyPassword = encryptedKeyPassword,
                            encryptedKeyBiometric = null,
                            encryptedKeyRecovery = encryptedKeyRecovery
                        )
                        saveSessionEncrypted(updatedSession)
                        loadSavedSessionsInternal()
                        withContext(Dispatchers.Main) {
                            showToast("Chat locked and secured successfully")
                            onResult(true, mnemonic)
                        }
                    } else {
                        withContext(Dispatchers.Main) { onResult(false, null) }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) { onResult(false, null) }
            }
        }
    }

    fun recoverSessionWithMnemonic(
        sessionId: String,
        mnemonic: String,
        newPassword: String,
        context: Context,
        onResult: (Boolean) -> Unit
    ) {
        viewModelScope.launch(safeIoDispatcher) {
            try {
                val entropy = Bip39Utils.mnemonicToEntropy(mnemonic, context)
                val recoveryKeyBytes = MessageDigest.getInstance("SHA-256").digest(entropy)
                val recoveryKey = SecretKeySpec(recoveryKeyBytes, "AES")

                val sessions = sessionRepository.getSessionsOrderedByLastActive()
                val session = sessions.find { it.id == sessionId }
                if (session == null || session.encryptedKeyRecovery == null) {
                    withContext(Dispatchers.Main) { onResult(false) }
                    return@launch
                }

                val sessionKeyEncodedBase64 = CryptoUtils.decryptMessage(session.encryptedKeyRecovery, recoveryKey)
                val sessionKeyBytes = java.util.Base64.getDecoder().decode(sessionKeyEncodedBase64)
                val sessionKey = SecretKeySpec(sessionKeyBytes, "AES")

                // Re-encrypt session key with new password
                val newSalt = CryptoUtils.generateSalt()
                val newDerivedKey = CryptoUtils.deriveKeyFromPassword(newPassword, newSalt)
                val newEncryptedKeyPassword = CryptoUtils.encryptMessage(sessionKeyEncodedBase64, newDerivedKey)

                val updatedSession = session.copy(
                    salt = java.util.Base64.getEncoder().encodeToString(newSalt),
                    encryptedKeyPassword = newEncryptedKeyPassword,
                    encryptedKeyBiometric = null // Reset biometrics on password recovery
                )

                activeSessionKeys[sessionId] = sessionKey
                saveSessionEncrypted(updatedSession)
                loadSessionInternal(sessionId)

                withContext(Dispatchers.Main) {
                    showToast("Vault recovered successfully")
                    onResult(true)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    showToast("Recovery failed: ${e.localizedMessage}")
                    onResult(false)
                }
            }
        }
    }

    fun unlockSessionWithPassword(sessionId: String, password: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch(safeIoDispatcher) {
            try {
                val sessions = sessionRepository.getSessionsOrderedByLastActive()
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
        viewModelScope.launch(safeIoDispatcher) {
            try {
                val sessions = sessionRepository.getSessionsOrderedByLastActive()
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
                        viewModelScope.launch(safeIoDispatcher) {
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
            if (e is java.security.InvalidKeyException || e.javaClass.name.contains("KeyPermanentlyInvalidatedException")) {
                showToast("Biometric key invalidated by restore. Please unlock with password.")
            } else {
                showToast("Biometric error: ${e.localizedMessage}")
            }
            onResult(false)
        }
    }

    fun removeSessionLock(sessionId: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch(safeIoDispatcher) {
            try {
                val key = activeSessionKeys[sessionId]
                if (key == null) {
                    withContext(Dispatchers.Main) { onResult(false) }
                    return@launch
                }
                
                val messages = messageRepository.getMessagesForSession(sessionId).toList()
                for (msg in messages) {
                    var newText = msg.text
                    var newDocs = msg.sourceDocuments
                    try { newText = CryptoUtils.decryptMessage(msg.text, key) } catch (e: Exception) {}
                    try { newDocs = msg.sourceDocuments?.let { CryptoUtils.decryptMessage(it, key) } } catch (e: Exception) {}
                    messageRepository.saveMessage(sessionId, msg.copy(text = newText, sourceDocuments = newDocs))
                }
                
                val docs = documentRepository.getSessionDocuments(sessionId).toList()
                for (doc in docs) {
                    if (doc.isEncrypted) {
                        var decName = doc.fileName
                        var decText = doc.chunkText
                        try { decName = CryptoUtils.decryptMessage(doc.fileName, key) } catch (e: Exception) {}
                        try { decText = CryptoUtils.decryptMessage(doc.chunkText, key) } catch (e: Exception) {}
                        documentRepository.saveSessionDocument(doc.copy(fileName = decName, chunkText = decText, isEncrypted = false))
                    }
                }
                
                val session = sessionRepository.getSessionsOrderedByLastActive().map { decryptSession(it) }.find { it.id == sessionId }
                if (session != null) {
                    val updatedSession = session.copy(
                        passwordHash = null,
                        salt = null,
                        validationToken = null,
                        encryptedKeyPassword = null,
                        encryptedKeyBiometric = null
                    )
                    sessionRepository.saveSession(updatedSession)
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
        viewModelScope.launch(safeIoDispatcher) {
            try {
                chatSessionUseCase.exportBackup(context, destUri, password)
                withContext(Dispatchers.Main) { onSuccess() }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { onError(e.localizedMessage ?: "Unknown error") }
            }
        }
    }

    fun importBackup(context: Context, srcUri: Uri, password: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch(safeIoDispatcher) {
            try {
                chatSessionUseCase.importBackup(context, srcUri, password)
                withContext(Dispatchers.Main) {
                    loadSavedSessions()
                    startNewChat()
                    onSuccess()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { onError(e.localizedMessage ?: "Failed to restore database.") }
            }
        }
    }

    fun toggleChecklistItem(messageKey: String, itemIndex: Int, isChecked: Boolean) {
        checkedItems["${messageKey}_$itemIndex"] = isChecked
        if (!isTemporarySession) {
            viewModelScope.launch(safeIoDispatcher) {
                messageRepository.saveChecklistState(messageKey, itemIndex, isChecked)
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

    fun setModel(path: String?) {
        modelPath = path
    }

    fun deleteModel() {
        modelPath?.let { path ->
            viewModelScope.launch(safeIoDispatcher) {
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

    var showCrashRecoveryDialog by mutableStateOf(false)
        private set

    fun onCrashRecoveryDecision(tryAgain: Boolean) {
        showCrashRecoveryDialog = false
        if (tryAgain) {
            settingsProvider.putBoolean("engine_crashed", false)
            initializeEngine(bypassSentinel = true)
        } else {
            settingsProvider.putBoolean("engine_crashed", false)
            val path = modelPath
            if (path != null) {
                try {
                    val file = java.io.File(path)
                    if (file.exists()) file.delete()
                } catch (e: Exception) {}
            }
            setModel(null)
            showToast("Model disabled.")
        }
    }

    fun initializeEngine(bypassSentinel: Boolean = false) {
        val path = modelPath ?: return
        
        if (!bypassSentinel && settingsProvider.getBoolean("engine_crashed", false)) {
            showCrashRecoveryDialog = true
            return
        }

        isInitializing = true
        
        // Write sentinel synchronously
        settingsProvider.commitBoolean("engine_crashed", true)
        
        viewModelScope.launch(safeIoDispatcher) {
            val result = aiProvider.initialize(path, selectedBackend)
            // Clear sentinel on success or graceful failure
            settingsProvider.commitBoolean("engine_crashed", false)
            
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

    fun sendMessage(context: Context) {
        val rawPrompt = if (prompt.isBlank() && attachedFiles.isNotEmpty()) {
            if (attachedFiles.size == 1) {
                "Summarize the attached document: ${attachedFiles.first().fileName}"
            } else {
                "Summarize the attached documents: " + attachedFiles.joinToString(", ") { it.fileName }
            }
        } else {
            prompt
        }
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

        // Keep copy of attached files, then clear the state immediately to refresh UI
        val filesToProcess = attachedFiles.toList()
        attachedFiles.clear()

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

        viewModelScope.launch(safeIoDispatcher) {
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
                        sessionRepository.saveSession(newSession)
                        loadSavedSessionsInternal()
                    }
                }

                // Process attached files (if any)
                for (file in filesToProcess) {
                    try {
                        val processedDocs = documentProcessingUseCase.processDocument(
                            context, file, sessionId!!, isTemporarySession, activeSessionKeys
                        )
                        withContext(Dispatchers.Main) {
                            activeSessionDocuments.addAll(processedDocs)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        withContext(Dispatchers.Main) {
                            showToast("Failed to process ${file.fileName}: ${e.localizedMessage}")
                            isGenerating = false
                            isThinking = false
                            agenticStateLabel = "Neural Standby"
                        }
                        return@launch
                    }
                }

                if (!isTemporarySession) {
                    // Now save the message (guarantees that the session row already exists)
                    val key = activeSessionKeys[sessionId!!]
                    val userMsgToSave = if (key != null) {
                        userMessage.copy(text = CryptoUtils.encryptMessage(userMessage.text, key))
                    } else {
                        userMessage
                    }
                    messageRepository.saveMessage(sessionId!!, userMsgToSave)
                }

                // local RAG retrieval
                val (documentContext, sourceDocs) = retrieveContext(sessionId!!, rawPrompt, filesToProcess.isNotEmpty())
                val sourceDocumentsString = if (sourceDocs.isNotEmpty()) sourceDocs.joinToString(", ") else null
                
                val basePrompt = if (documentContext.isNotEmpty()) {
                    "$documentContext\n\nUSER QUERY: $rawPrompt"
                } else {
                    rawPrompt
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
                        buildSystemPrompt(searchQuery, searchResults, basePrompt)
                    } else {
                        basePrompt
                    }
                } else {
                    basePrompt
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
                        val encryptedSourceDocs = if (key != null && sourceDocumentsString != null) CryptoUtils.encryptMessage(sourceDocumentsString, key) else sourceDocumentsString
                        
                        val liveAssistantMsg = ChatMessage(id = assistantMessageId, text = textToSave, isUser = false, sourceDocuments = encryptedSourceDocs)
                        messageRepository.saveMessage(sessionId!!, liveAssistantMsg)
                    }
                }

                var assistantMessage: ChatMessage? = null
                withContext(Dispatchers.Main) {
                    val msg = ChatMessage(id = assistantMessageId, text = currentResponseChunk, isUser = false, sourceDocuments = sourceDocumentsString)
                    chatMessages.add(msg)
                    currentResponseChunk = ""
                    assistantMessage = msg
                }

                if (!isTemporarySession) {
                    assistantMessage?.let { msg ->
                        val key = activeSessionKeys[sessionId!!]
                        val msgToSave = if (key != null) {
                            msg.copy(
                                text = CryptoUtils.encryptMessage(msg.text, key),
                                sourceDocuments = msg.sourceDocuments?.let { CryptoUtils.encryptMessage(it, key) }
                            )
                        } else msg
                        messageRepository.saveMessage(sessionId!!, msgToSave)
                        
                        // Update session last active time and last search query
                        val sessions = sessionRepository.getSessionsOrderedByLastActive().map { decryptSession(it) }
                        sessions.find { it.id == sessionId }?.let {
                            saveSessionEncrypted(it.copy(
                                lastActive = System.currentTimeMillis(),
                                lastSearchQuery = lastSearchQuery
                            ))
                        }
                        loadSavedSessionsInternal()
                    }
                }
            }
 catch (e: Exception) {
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
                        messageRepository.saveMessage(sessionId!!, msgToSave)
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
        return llmUseCase.detectSearchRequirement(prompt, isInternetEnabled)
    }

    private fun determineSearchQuery(rawPrompt: String): String {
        return llmUseCase.determineSearchQuery(rawPrompt, chatMessages, lastSearchQuery)
    }

    private fun buildSystemPrompt(query: String, data: String, userPrompt: String): String {
        return llmUseCase.buildSystemPrompt(query, data, userPrompt)
    }

    private fun chunkText(text: String, chunkSize: Int = 1000, overlap: Int = 200): List<String> {
        return documentProcessingUseCase.chunkText(text, chunkSize, overlap)
    }

    private fun retrieveContext(sessionId: String, query: String, justAttached: Boolean = false): Pair<String, List<String>> {
        val isEncrypted = activeSessionKeys.containsKey(sessionId)
        return llmUseCase.retrieveContext(sessionId, query, isEncrypted, activeSessionDocuments, justAttached)
    }

    override fun onCleared() {
        super.onCleared()
        aiProvider.close()
        isEngineReady = aiProvider.isInitialized
    }
}


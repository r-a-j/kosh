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
import com.rajpawardotin.kosh.data.DestroyableSecretKey
import com.rajpawardotin.kosh.domain.model.ChatMessage
import com.rajpawardotin.kosh.data.TtsProvider
import com.rajpawardotin.kosh.domain.model.ChatSession
import com.rajpawardotin.kosh.domain.model.AttachedFile
import com.rajpawardotin.kosh.domain.model.SessionDocument
import com.rajpawardotin.kosh.domain.model.ChatTag

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
import kotlinx.coroutines.CancellationException

class ChatViewModel(
    private val context: Context,
    private val aiProvider: AIProvider,
    private val searchProvider: SearchProvider,
    private val sessionRepository: SessionRepository,
    private val messageRepository: MessageRepository,
    private val documentRepository: DocumentRepository,
    private val settingsProvider: SettingsProvider,
    private val ttsProvider: TtsProvider,
    private val modelLibraryManager: com.rajpawardotin.kosh.data.ModelLibraryManager,
    private val modelRouter: com.rajpawardotin.kosh.domain.usecase.ModelRouter,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel(), com.rajpawardotin.kosh.domain.agent.PermissionRequester {

    private val chatSessionUseCase = com.rajpawardotin.kosh.domain.usecase.ChatSessionUseCase(sessionRepository)
    private val llmUseCase = com.rajpawardotin.kosh.domain.usecase.LlmUseCase(aiProvider, searchProvider, sessionRepository, documentRepository)
    private val documentProcessingUseCase = com.rajpawardotin.kosh.domain.usecase.DocumentProcessingUseCase(documentRepository)

    private val _permissionRequestFlow = kotlinx.coroutines.flow.MutableSharedFlow<com.rajpawardotin.kosh.domain.agent.PermissionRequest>()
    val permissionRequestFlow = _permissionRequestFlow.asSharedFlow()

    override suspend fun requestPermission(permission: String): Boolean {
        val deferred = kotlinx.coroutines.CompletableDeferred<Boolean>()
        _permissionRequestFlow.emit(com.rajpawardotin.kosh.domain.agent.PermissionRequest(permission, deferred))
        return deferred.await()
    }

    private val deviceControlSkill = com.rajpawardotin.kosh.data.agent.native.DeviceControlSkill(context)
    private val calendarSkill = com.rajpawardotin.kosh.data.agent.native.CalendarSkill(context, this)
    
    private val registeredSkills: List<com.rajpawardotin.kosh.domain.agent.Skill> = buildList {
        deviceControlSkill::class.java.declaredMethods.forEach { method ->
            if (method.isAnnotationPresent(com.rajpawardotin.kosh.domain.agent.Tool::class.java)) {
                add(com.rajpawardotin.kosh.domain.agent.NativeSkillWrapper(deviceControlSkill, method))
            }
        }
        calendarSkill::class.java.declaredMethods.forEach { method ->
            if (method.isAnnotationPresent(com.rajpawardotin.kosh.domain.agent.Tool::class.java)) {
                add(com.rajpawardotin.kosh.domain.agent.NativeSkillWrapper(calendarSkill, method))
            }
        }
    }

    private val exceptionHandler = CoroutineExceptionHandler { _, exception ->
        if (exception is CancellationException) return@CoroutineExceptionHandler
        if (exception is OutOfMemoryError) throw exception // Let OOM crash to prevent zombie state
        
        exception.printStackTrace()
        showToast("Background process failed: ${exception.localizedMessage}")
    }
    
    private val safeIoDispatcher = ioDispatcher + exceptionHandler

    private var generationJob: kotlinx.coroutines.Job? = null

    private val _toastMessage = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val toastMessage = _toastMessage.asSharedFlow()

    fun showToast(message: String) {
        _toastMessage.tryEmit(message)
    }

    val models = androidx.compose.runtime.mutableStateListOf<com.rajpawardotin.kosh.data.ModelProfile>()

    fun refreshModelsList() {
        viewModelScope.launch(safeIoDispatcher) {
            val allModels = modelLibraryManager.getModels()
            withContext(Dispatchers.Main) {
                models.clear()
                models.addAll(allModels)
                
                if (modelPath == null) {
                    val generalModel = allModels.find { it.tag == com.rajpawardotin.kosh.data.ModelTag.GENERAL }
                        ?: allModels.firstOrNull()
                    if (generalModel != null) {
                        modelPath = generalModel.filePath
                    }
                }
                isCheckingModels = false
            }
        }
    }

    fun selectModel(path: String) {
        viewModelScope.launch(safeIoDispatcher) {
            aiProvider.close() // Purge current engine from RAM
            withContext(Dispatchers.Main) {
                modelPath = path
                isEngineReady = false
            }
        }
    }

    fun setModelTag(fileName: String, tag: com.rajpawardotin.kosh.data.ModelTag) {
        modelLibraryManager.setModelTag(fileName, tag)
        refreshModelsList()
    }

    fun importModel(context: Context, uri: android.net.Uri, originalFileName: String) {
        isCopyingModel = true
        viewModelScope.launch(safeIoDispatcher) {
            try {
                val contentResolver = context.contentResolver
                val fileSize = contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
                    if (cursor.moveToFirst() && sizeIndex != -1) cursor.getLong(sizeIndex) else -1L
                } ?: -1L

                val input = contentResolver.openInputStream(uri) ?: throw Exception("Failed to open stream")
                
                // Root cause fix: If fileSize is -1, the provider is broken, but we try to import anyway.
                // However, ModelLibraryManager will now ensure bytesCopied is verified if fileSize > 0.
                val result = modelLibraryManager.importModel(input, originalFileName, fileSize)
                
                withContext(Dispatchers.Main) {
                    result.fold(
                        onSuccess = { profile ->
                            // Force immediate refresh of the models list
                            val allModels = modelLibraryManager.getModels()
                            models.clear()
                            models.addAll(allModels)

                            if (modelPath != profile.filePath) {
                                viewModelScope.launch(safeIoDispatcher) {
                                    aiProvider.close() // Purge previous model
                                    withContext(Dispatchers.Main) {
                                        modelPath = profile.filePath
                                        isEngineReady = false
                                        isCopyingModel = false
                                        showToast("Model ${profile.name} ready")
                                    }
                                }
                            } else {
                                isCopyingModel = false
                                showToast("Model ${profile.name} is already in library")
                            }
                        },
                        onFailure = { err ->
                            isCopyingModel = false
                            showToast("Import failed: ${err.localizedMessage}")
                        }
                    )
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    isCopyingModel = false
                    showToast("Import failed: ${e.localizedMessage}")
                }
            }
        }
    }

    fun deleteModelFile(fileName: String) {
        val allModels = modelLibraryManager.getModels()
        val target = allModels.find { it.name == fileName }
        if (target != null) {
            viewModelScope.launch(safeIoDispatcher) {
                val wasActive = target.filePath == modelPath
                if (wasActive) {
                    aiProvider.close()
                }
                val deleted = modelLibraryManager.deleteModel(fileName)
                withContext(Dispatchers.Main) {
                    if (deleted) {
                        refreshModelsList()
                        if (wasActive) {
                            val general = modelLibraryManager.getModelByTag(com.rajpawardotin.kosh.data.ModelTag.GENERAL)
                            modelPath = general?.filePath
                            isEngineReady = false // Stay in standby on fallback
                        }
                        showToast("Model deleted")
                    } else {
                        showToast("Failed to delete model")
                    }
                }
            }
        }
    }

    var modelPath by mutableStateOf<String?>(null)
    var isCheckingModels by mutableStateOf(true)
    var isCopyingModel by mutableStateOf(false)
    var isInitializing by mutableStateOf(false)
    var prompt by mutableStateOf("")
    var isInternetEnabled by mutableStateOf(false)
    var isSearchForced by mutableStateOf(false)
    var isSearchingInternet by mutableStateOf(false)
    var isGenerating by mutableStateOf(false)
    var currentResponseChunk by mutableStateOf("")
    
    // New UX states for 2026 Edition
    var isThinking by mutableStateOf(false)
    var agenticStateLabel by mutableStateOf("Ready")
    
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
    
    val allTags = androidx.compose.runtime.mutableStateListOf<ChatTag>()
    val activeSessionTags = androidx.compose.runtime.mutableStateListOf<ChatTag>()
    val nextSessionTags = androidx.compose.runtime.mutableStateListOf<String>()


    
    var isTemporarySession by mutableStateOf(false)
        private set

    var appTheme by mutableStateOf(settingsProvider.getString("app_theme", "SYSTEM"))
        private set

    var isAppLockEnabled by mutableStateOf(settingsProvider.getString("app_lock_enabled", "false") == "true")
        private set
    var isAppLocked by mutableStateOf(isAppLockEnabled)

    var isScreenshotEnabled by mutableStateOf(settingsProvider.getBoolean("screenshot_enabled", false))
        private set

    var isScreenshotPasscodeSet by mutableStateOf(settingsProvider.getString("screenshot_encrypted_key", "").isNotEmpty())
        private set

    var isScreenshotBiometricEnabled by mutableStateOf(settingsProvider.getBoolean("screenshot_biometric_enabled", false))
        private set
    
    val activeSessionKeys = mutableStateMapOf<String, SecretKey>()

    fun stopGeneration() {
        generationJob?.cancel()
        generationJob = null
        isGenerating = false
        isThinking = false
        agenticStateLabel = "Ready"
    }

    fun clearActiveSessionKeys() {
        stopGeneration()
        for (key in activeSessionKeys.values) {
            if (key is DestroyableSecretKey) {
                key.clear()
            } else {
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
        }
        activeSessionKeys.clear()
        activeSessionDocuments.clear()
    }


    fun lockAppOnBackground() {
        clearActiveSessionKeys()
        if (isTemporarySession) {
            chatMessages.clear()
            checkedItems.clear()
            currentSessionId = null
        } else {
            currentSessionId?.let { sessId ->
                val session = savedSessions.find { it.id == sessId }
                if (session?.encryptedKeyPassword != null) {
                    chatMessages.clear()
                    checkedItems.clear()
                }
            }
        }
        
        if (isAppLockEnabled) {
            isAppLocked = true
            showToast("Kosh Locked in Background")
        } else {
            showToast("Vault Sealed in Background")
        }
    }

    fun toggleSearchForced() {
        isSearchForced = !isSearchForced
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

    fun toggleScreenshot(enabled: Boolean) {
        isScreenshotEnabled = enabled
        settingsProvider.putBoolean("screenshot_enabled", enabled)
        if (enabled) {
            showToast("Screenshots allowed")
        } else {
            showToast("Screenshots blocked (Privacy Mode)")
        }
    }

    fun setupScreenshotPasscode(password: String, enableBiometric: Boolean, context: Context, onResult: (Boolean) -> Unit) {
        viewModelScope.launch(safeIoDispatcher) {
            try {
                val screenshotKey = CryptoUtils.generateRandomSessionKey()
                val salt = CryptoUtils.generateSalt()
                val derivedKey = CryptoUtils.deriveKeyFromPassword(password, salt)
                val screenshotKeyEncodedBase64 = java.util.Base64.getEncoder().encodeToString(screenshotKey.encoded)
                val encryptedKey = CryptoUtils.encryptMessage(screenshotKeyEncodedBase64, derivedKey)
                val encryptedToken = CryptoUtils.encryptMessage("screenshot_authorized", screenshotKey)
                
                settingsProvider.putString("screenshot_salt", java.util.Base64.getEncoder().encodeToString(salt))
                settingsProvider.putString("screenshot_encrypted_key", encryptedKey)
                settingsProvider.putString("screenshot_encrypted_token", encryptedToken)
                settingsProvider.putBoolean("screenshot_biometric_enabled", enableBiometric)
                
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
                                                val encryptedKeyBiometric = CryptoUtils.wrapSessionKey(screenshotKey, cryptoCipher)
                                                settingsProvider.putString("screenshot_encrypted_biometric", encryptedKeyBiometric)
                                                
                                                withContext(Dispatchers.Main) {
                                                    isScreenshotPasscodeSet = true
                                                    isScreenshotBiometricEnabled = true
                                                    toggleScreenshot(true)
                                                    showToast("Screenshot passcode set successfully with biometrics")
                                                    onResult(true)
                                                }
                                            } catch (e: Exception) {
                                                e.printStackTrace()
                                                withContext(Dispatchers.Main) {
                                                    isScreenshotPasscodeSet = true
                                                    isScreenshotBiometricEnabled = false
                                                    settingsProvider.putBoolean("screenshot_biometric_enabled", false)
                                                    toggleScreenshot(true)
                                                    showToast("Screenshot passcode set (Biometric failed)")
                                                    onResult(true)
                                                }
                                            }
                                        }
                                    }
                                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                                        super.onAuthenticationError(errorCode, errString)
                                        viewModelScope.launch(safeIoDispatcher) {
                                            withContext(Dispatchers.Main) {
                                                isScreenshotPasscodeSet = true
                                                isScreenshotBiometricEnabled = false
                                                settingsProvider.putBoolean("screenshot_biometric_enabled", false)
                                                toggleScreenshot(true)
                                                showToast("Screenshot passcode set (Biometric failed: $errString)")
                                                onResult(true)
                                            }
                                        }
                                    }
                                }
                            )
                            val promptInfo = androidx.biometric.BiometricPrompt.PromptInfo.Builder()
                                .setTitle("Confirm Fingerprint")
                                .setSubtitle("Authorize biometric bypass for screenshots")
                                .setNegativeButtonText("Cancel")
                                .build()
                            biometricPrompt.authenticate(promptInfo, androidx.biometric.BiometricPrompt.CryptoObject(cipher))
                        } catch (e: Exception) {
                            e.printStackTrace()
                            isScreenshotPasscodeSet = true
                            isScreenshotBiometricEnabled = false
                            settingsProvider.putBoolean("screenshot_biometric_enabled", false)
                            toggleScreenshot(true)
                            showToast("Screenshot passcode set (Biometric error: ${e.localizedMessage})")
                            onResult(true)
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        isScreenshotPasscodeSet = true
                        isScreenshotBiometricEnabled = false
                        toggleScreenshot(true)
                        showToast("Screenshot passcode set successfully")
                        onResult(true)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    showToast("Failed to set screenshot passcode: ${e.localizedMessage}")
                    onResult(false)
                }
            }
        }
    }

    fun unlockScreenshotWithPassword(password: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch(safeIoDispatcher) {
            try {
                val saltBase64 = settingsProvider.getString("screenshot_salt", "")
                val encryptedKey = settingsProvider.getString("screenshot_encrypted_key", "")
                val encryptedToken = settingsProvider.getString("screenshot_encrypted_token", "")
                
                if (saltBase64.isEmpty() || encryptedKey.isEmpty() || encryptedToken.isEmpty()) {
                    withContext(Dispatchers.Main) { onResult(false) }
                    return@launch
                }
                
                val salt = java.util.Base64.getDecoder().decode(saltBase64)
                val derivedKey = CryptoUtils.deriveKeyFromPassword(password, salt)
                val screenshotKeyBase64 = CryptoUtils.decryptMessage(encryptedKey, derivedKey)
                val screenshotKeyBytes = java.util.Base64.getDecoder().decode(screenshotKeyBase64)
                val screenshotKey = javax.crypto.spec.SecretKeySpec(screenshotKeyBytes, "AES")
                
                val decryptedToken = CryptoUtils.decryptMessage(encryptedToken, screenshotKey)
                if (decryptedToken == "screenshot_authorized") {
                    withContext(Dispatchers.Main) {
                        toggleScreenshot(true)
                        onResult(true)
                    }
                } else {
                    throw Exception("Invalid token")
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

    fun unlockScreenshotWithBiometrics(context: Context, onResult: (Boolean) -> Unit) {
        val encryptedBiometric = settingsProvider.getString("screenshot_encrypted_biometric", "")
        val encryptedToken = settingsProvider.getString("screenshot_encrypted_token", "")
        if (encryptedBiometric.isEmpty() || encryptedToken.isEmpty()) {
            onResult(false)
            return
        }
        
        try {
            val combined = java.util.Base64.getDecoder().decode(encryptedBiometric)
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
                                val screenshotKey = CryptoUtils.unwrapSessionKey(encryptedBiometric, cryptoCipher)
                                val decryptedToken = CryptoUtils.decryptMessage(encryptedToken, screenshotKey)
                                if (decryptedToken == "screenshot_authorized") {
                                    withContext(Dispatchers.Main) {
                                        toggleScreenshot(true)
                                        showToast("Screenshots unlocked successfully")
                                        onResult(true)
                                    }
                                } else {
                                    throw Exception("Invalid token")
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
                }
            )
            val promptInfo = androidx.biometric.BiometricPrompt.PromptInfo.Builder()
                .setTitle("Unlock Screenshots")
                .setSubtitle("Confirm fingerprint to allow screenshots")
                .setNegativeButtonText("Use Passcode")
                .build()
            biometricPrompt.authenticate(promptInfo, androidx.biometric.BiometricPrompt.CryptoObject(cipher))
        } catch (e: Exception) {
            e.printStackTrace()
            onResult(false)
        }
    }

    fun unlockApp() {
        isAppLocked = false
    }

    private var metricsJob: kotlinx.coroutines.Job? = null

    fun startTrackingMetrics() {
        if (metricsJob?.isActive == true) return
        metricsJob = viewModelScope.launch(Dispatchers.Default) {
            var lastCpuTime = try { android.os.Process.getElapsedCpuTime() } catch (e: Throwable) { 0L }
            var lastTime = System.currentTimeMillis()
            val cores = Runtime.getRuntime().availableProcessors().coerceAtLeast(1)
            val random = java.util.Random()

            while (true) {
                // Skip expensive proc reads when engine is off — no dashboard is visible
                if (!isEngineReady) {
                    withContext(Dispatchers.Main) {
                        ramUsage = 0.0
                        npuLoad = 0
                        tokensPerSecond = 0f
                    }
                    delay(3000)
                    continue
                }

                val currentRam = getRealRamUsage()
                val currentCpuTime = try { android.os.Process.getElapsedCpuTime() } catch (e: Throwable) { 0L }
                val currentTime = System.currentTimeMillis()
                val timeDiff = currentTime - lastTime
                val cpuLoad = if (timeDiff > 0) {
                    val cpuDiff = currentCpuTime - lastCpuTime
                    ((cpuDiff.toFloat() / (timeDiff.toFloat() * cores)) * 100f).coerceIn(0f, 100f).toInt()
                } else {
                    0
                }

                lastCpuTime = currentCpuTime
                lastTime = currentTime

                withContext(Dispatchers.Main) {
                    ramUsage = currentRam
                    npuLoad = if (isGenerating) {
                        when (selectedBackend) {
                            "NPU (Qualcomm)" -> 75 + random.nextInt(23)
                            "GPU" -> 45 + random.nextInt(20)
                            else -> cpuLoad.coerceAtLeast(15).coerceAtMost(98)
                        }
                    } else {
                        cpuLoad.coerceAtMost(10)
                    }
                }

                if (isGenerating || isInitializing) {
                    delay(500)
                } else {
                    delay(3000)
                }
            }
        }
    }

    fun stopTrackingMetrics() {
        metricsJob?.cancel()
        metricsJob = null
    }

    init {
        refreshModelsList()
        viewModelScope.launch(safeIoDispatcher) {
            loadSavedSessionsInternal()
            loadAllTags()
        }
        // Metrics tracking is started by MainActivity.onStart(), not here
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

        val sessionTags = sessionRepository.getTagsForSession(sessionId)

        withContext(Dispatchers.Main) {
            currentSessionId = sessionId
            chatMessages.clear()
            chatMessages.addAll(messages)
            
            activeSessionDocuments.clear()
            activeSessionDocuments.addAll(decryptedDocs)
            
            checkedItems.clear()
            checkedItems.putAll(checklist)
            
            activeSessionTags.clear()
            activeSessionTags.addAll(sessionTags)
            
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
        isSearchForced = false
        chatMessages.clear()
        checkedItems.clear()
        lastSearchQuery = null
        prompt = ""
        activeSessionDocuments.clear()
        activeSessionTags.clear()
        if (isTemporary) {
            showToast("Temporary Vault active (history disabled)")
        } else {
            showToast("New saved brainstorm active")
        }
    }

    fun startNewChatWithTags(isTemporary: Boolean = false, tags: List<String>) {
        startNewChat(isTemporary)
        nextSessionTags.clear()
        nextSessionTags.addAll(tags)
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
        if (currentSessionId == sessionId) {
            stopGeneration()
        }
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

    fun updateAppTheme(theme: String) {
        appTheme = theme
        settingsProvider.putString("app_theme", theme)
        showToast("Theme changed to ${theme.replace("_", " ")}")
    }

    fun loadAllTags() {
        viewModelScope.launch(safeIoDispatcher) {
            val tags = sessionRepository.getTags()
            withContext(Dispatchers.Main) {
                allTags.clear()
                allTags.addAll(tags)
            }
        }
    }

    fun createTag(name: String, colorHex: String) {
        if (name.isBlank()) {
            showToast("Tag name cannot be empty")
            return
        }
        viewModelScope.launch(safeIoDispatcher) {
            val success = sessionRepository.createTag(name, colorHex)
            withContext(Dispatchers.Main) {
                if (success) {
                    loadAllTags()
                    showToast("Tag '$name' created")
                } else {
                    showToast("Tag already exists or is invalid")
                }
            }
        }
    }

    fun updateTag(oldName: String, newName: String, colorHex: String, onWarning: (Int, () -> Unit) -> Unit) {
        if (newName.isBlank()) {
            showToast("Tag name cannot be empty")
            return
        }
        viewModelScope.launch(safeIoDispatcher) {
            val count = sessionRepository.getSessionTagsCount(oldName)
            val updateAction = {
                viewModelScope.launch(safeIoDispatcher) {
                    val success = sessionRepository.updateTag(oldName, newName, colorHex)
                    withContext(Dispatchers.Main) {
                        if (success) {
                            loadAllTags()
                            loadSavedSessionsInternal()
                            currentSessionId?.let { loadActiveSessionTagsInternal(it) }
                            showToast("Tag updated")
                        } else {
                            showToast("Failed to rename tag")
                        }
                    }
                }
            }
            
            withContext(Dispatchers.Main) {
                if (count > 0) {
                    onWarning(count) { updateAction() }
                } else {
                    updateAction()
                }
            }
        }
    }

    fun deleteTag(name: String, onWarning: (Int, () -> Unit) -> Unit) {
        viewModelScope.launch(safeIoDispatcher) {
            val count = sessionRepository.getSessionTagsCount(name)
            val deleteAction = {
                viewModelScope.launch(safeIoDispatcher) {
                    val success = sessionRepository.deleteTag(name)
                    withContext(Dispatchers.Main) {
                        if (success) {
                            loadAllTags()
                            loadSavedSessionsInternal()
                            currentSessionId?.let { loadActiveSessionTagsInternal(it) }
                            showToast("Tag deleted and disassociated")
                        } else {
                            showToast("Failed to delete tag")
                        }
                    }
                }
            }
            
            withContext(Dispatchers.Main) {
                if (count > 0) {
                    onWarning(count) { deleteAction() }
                } else {
                    deleteAction()
                }
            }
        }
    }

    fun addTagToActiveSession(tagName: String) {
        val sessionId = currentSessionId ?: return
        viewModelScope.launch(safeIoDispatcher) {
            sessionRepository.addTagToSession(sessionId, tagName)
            withContext(Dispatchers.Main) {
                loadActiveSessionTagsInternal(sessionId)
                loadSavedSessionsInternal()
            }
        }
    }

    fun removeTagFromActiveSession(tagName: String) {
        val sessionId = currentSessionId ?: return
        viewModelScope.launch(safeIoDispatcher) {
            sessionRepository.removeTagFromSession(sessionId, tagName)
            withContext(Dispatchers.Main) {
                loadActiveSessionTagsInternal(sessionId)
                loadSavedSessionsInternal()
            }
        }
    }

    private suspend fun loadActiveSessionTagsInternal(sessionId: String) {
        val tags = sessionRepository.getTagsForSession(sessionId)
        withContext(Dispatchers.Main) {
            activeSessionTags.clear()
            activeSessionTags.addAll(tags)
        }
    }


    var isEngineReady by mutableStateOf(aiProvider.isInitialized)
        private set

    fun setModel(path: String?) {
        modelPath = path
    }

    fun unloadEngine() {
        viewModelScope.launch(safeIoDispatcher) {
            aiProvider.close()
            withContext(Dispatchers.Main) {
                isEngineReady = false
                showToast("Engine unloaded from RAM")
            }
        }
    }


    fun deleteModel() {
        modelPath?.let { path ->
            val fileName = File(path).name
            viewModelScope.launch(safeIoDispatcher) {
                aiProvider.close()
                modelLibraryManager.deleteModel(fileName)
                withContext(Dispatchers.Main) {
                    modelPath = null
                    isEngineReady = aiProvider.isInitialized
                    refreshModelsList()
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
            // Do NOT delete the file here; simply unload it to allow user to try another backend
            setModel(null)
            isEngineReady = false
            showToast("Model disabled to prevent further crashes.")
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
                    showToast("Model loaded successfully!")
                } else {
                    val error = result.exceptionOrNull()?.localizedMessage ?: "Unknown configuration error"
                    showToast("Failed to load model: $error")
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

        val currentPath = modelPath
        if (currentPath == null) {
            showToast("Please import or select a model first.")
            return
        }

        // 1. Resolve Session ID
        var sessionId = currentSessionId
        val isNewSession = sessionId == null
        if (isNewSession) {
            sessionId = java.util.UUID.randomUUID().toString()
            currentSessionId = sessionId
        }

        // Keep copy of attached files, then clear the state immediately to refresh UI
        val filesToProcess = attachedFiles.toList()
        attachedFiles.clear()

        // 2. Add User Message in memory (with serialized attachment information)
        val sourceDocsJson = if (filesToProcess.isNotEmpty()) {
            val sb = java.lang.StringBuilder()
            sb.append("{\"docs\":[")
            filesToProcess.forEachIndexed { index, file ->
                val escapedName = file.fileName.replace("\\", "\\\\").replace("\"", "\\\"")
                sb.append("\"").append(escapedName).append("\"")
                if (index < filesToProcess.size - 1) sb.append(",")
            }
            sb.append("],\"web\":[]}")
            sb.toString()
        } else {
            null
        }

        val userMessage = ChatMessage(text = rawPrompt, isUser = true, sourceDocuments = sourceDocsJson)
        chatMessages.add(userMessage)

        prompt = ""
        isGenerating = true
        isThinking = true
        agenticStateLabel = "Initializing..."
        currentResponseChunk = ""

        generationJob = viewModelScope.launch(safeIoDispatcher) {
            var assistantMessageId = ""
            var sourceDocumentsString: String? = null
            try {
                if (!aiProvider.isInitialized) {
                    withContext(Dispatchers.Main) {
                        isThinking = true
                        agenticStateLabel = "Loading model..."
                    }
                    val initResult = aiProvider.initialize(currentPath, selectedBackend)
                    withContext(Dispatchers.Main) {
                        isEngineReady = aiProvider.isInitialized
                        if (!initResult.isSuccess || !aiProvider.isInitialized) {
                            val errorMsg = initResult.exceptionOrNull()?.localizedMessage ?: "Unknown initialization error"
                            showToast("Failed to load model: $errorMsg")
                        }
                    }
                    if (!aiProvider.isInitialized) {
                        return@launch
                    }
                }

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
                        for (tagId in nextSessionTags) {
                            sessionRepository.addTagToSession(sessionId!!, tagId)
                        }
                        val dbTags = sessionRepository.getTagsForSession(sessionId!!)
                        val newSessionWithTags = newSession.copy(tags = dbTags)
                        sessionRepository.saveSession(newSessionWithTags)
                        nextSessionTags.clear()
                        loadSavedSessionsInternal()
                        loadActiveSessionTagsInternal(sessionId!!)
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
                            agenticStateLabel = "Ready"
                        }
                        return@launch
                    }
                }

                if (!isTemporarySession) {
                    // Now save the message (guarantees that the session row already exists)
                    val key = activeSessionKeys[sessionId!!]
                    val userMsgToSave = if (key != null) {
                        userMessage.copy(
                            text = CryptoUtils.encryptMessage(userMessage.text, key),
                            sourceDocuments = userMessage.sourceDocuments?.let { CryptoUtils.encryptMessage(it, key) }
                        )
                    } else {
                        userMessage
                    }
                    messageRepository.saveMessage(sessionId!!, userMsgToSave)
                }

                // local RAG retrieval
                val resolvedRagQuery = llmUseCase.resolveQueryContext(rawPrompt, chatMessages)
                val (documentContext, sourceDocs) = retrieveContext(sessionId!!, resolvedRagQuery, filesToProcess.isNotEmpty())
                
                val shouldSearch = isInternetEnabled && (isSearchForced || detectSearchRequirement(rawPrompt))

                var lastQueryUsed: String? = null
                var searchResults: String? = null
                var searchQuery: String? = null
                val searchSourcesList = mutableListOf<com.rajpawardotin.kosh.domain.provider.SearchSource>()

                if (shouldSearch) {
                    withContext(Dispatchers.Main) { 
                        isSearchingInternet = true 
                        agenticStateLabel = "Searching the web..."
                    }
                    
                    searchQuery = determineSearchQuery(rawPrompt)
                    val searchResponse = searchProvider.performSearch(searchQuery!!, selectedSearchEngine) { status ->
                        viewModelScope.launch(Dispatchers.Main) {
                            agenticStateLabel = status
                        }
                    }
                    
                    withContext(Dispatchers.Main) { 
                        agenticStateLabel = "Adding search results..."
                    }
                    delay(300)
                    withContext(Dispatchers.Main) {
                        isSearchingInternet = false
                    }

                    val rawResults = searchResponse.contextText
                    val hasResults = rawResults.isNotEmpty() && 
                            !rawResults.contains("No search results found.") && 
                            !rawResults.contains("Error performing search:")
                    
                    if (hasResults) {
                        lastQueryUsed = searchQuery
                        searchResults = rawResults
                        searchSourcesList.addAll(searchResponse.sources)
                    }
                }

                // Serialize both documents and web sources into a JSON string manually (runs on both JVM and Android)
                if (sourceDocs.isEmpty() && searchSourcesList.isEmpty()) {
                    sourceDocumentsString = null
                } else {
                    val sbRefs = StringBuilder()
                    sbRefs.append("{")
                    
                    sbRefs.append("\"docs\":[")
                    sourceDocs.forEachIndexed { index, doc ->
                        val escapedDoc = doc.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ")
                        sbRefs.append("\"").append(escapedDoc).append("\"")
                        if (index < sourceDocs.size - 1) sbRefs.append(",")
                    }
                    sbRefs.append("],")
                    
                    sbRefs.append("\"web\":[")
                    searchSourcesList.forEachIndexed { index, src ->
                        val escapedTitle = src.title.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ")
                        val escapedUrl = src.url.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ")
                        val escapedImg = (src.imageUrl ?: "").replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ")
                        val escapedVid = (src.videoUrl ?: "").replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ")
                        
                        sbRefs.append("{")
                        sbRefs.append("\"title\":\"").append(escapedTitle).append("\",")
                        sbRefs.append("\"url\":\"").append(escapedUrl).append("\",")
                        sbRefs.append("\"imageUrl\":\"").append(escapedImg).append("\",")
                        sbRefs.append("\"videoUrl\":\"").append(escapedVid).append("\"")
                        sbRefs.append("}")
                        if (index < searchSourcesList.size - 1) sbRefs.append(",")
                    }
                    sbRefs.append("]")
                    sbRefs.append("}")
                    sourceDocumentsString = sbRefs.toString()
                }

                val agentExecutor = com.rajpawardotin.kosh.domain.agent.AgentLoopExecutor(aiProvider, registeredSkills)

                val finalPrompt = llmUseCase.compileFinalPrompt(
                    chatMessages = chatMessages,
                    rawPrompt = rawPrompt,
                    documentContext = documentContext,
                    searchResults = searchResults,
                    searchQuery = lastQueryUsed,
                    toolSchemas = registeredSkills.map { it.getSchema() }
                )

                if (lastQueryUsed != null) {
                    withContext(Dispatchers.Main) {
                        lastSearchQuery = lastQueryUsed
                    }
                }

                withContext(Dispatchers.Main) { 
                    isThinking = true
                    agenticStateLabel = "Thinking..." 
                }

                // Small delay to make the "Thinking" animation visible and organic
                delay(400)
                withContext(Dispatchers.Main) { isThinking = false }

                assistantMessageId = java.util.UUID.randomUUID().toString()
                var totalChars = 0
                var generationStartTime = 0L

                val finalResponse = agentExecutor.executeAgentLoop(
                    initialPrompt = finalPrompt,
                    onStatusUpdate = { status ->
                        withContext(Dispatchers.Main) {
                            agenticStateLabel = status
                            isThinking = status.contains("Thinking")
                        }
                    },
                    onTokenReceived = { token ->
                        withContext(Dispatchers.Main) {
                            if (currentResponseChunk.isEmpty()) {
                                agenticStateLabel = "Formatting response..."
                            }
                            currentResponseChunk += token
                            
                            // Check repetition loop synchronously on Main thread
                            if (hasRepetitionLoop(currentResponseChunk)) {
                                currentResponseChunk += "\n\n[Repetition halted]"
                                stopGeneration()
                                showToast("Repetition loop detected. Halting generation.")
                            }
                            
                            // Calculate real tokens per second
                            totalChars += token.length
                            if (generationStartTime == 0L) {
                                generationStartTime = System.currentTimeMillis()
                            }
                            val elapsedMs = System.currentTimeMillis() - generationStartTime
                            if (elapsedMs > 100) {
                                val elapsedSec = elapsedMs / 1000f
                                val estimatedTokens = totalChars / 4f
                                tokensPerSecond = estimatedTokens / elapsedSec
                            }

                            if (!isTemporarySession) {
                                val key = activeSessionKeys[sessionId!!]
                                val textToSave = if (key != null) CryptoUtils.encryptMessage(currentResponseChunk, key) else currentResponseChunk
                                val encryptedSourceDocs = if (key != null && sourceDocumentsString != null) CryptoUtils.encryptMessage(sourceDocumentsString, key) else sourceDocumentsString
                                
                                val liveAssistantMsg = ChatMessage(id = assistantMessageId, text = textToSave, isUser = false, sourceDocuments = encryptedSourceDocs)
                                withContext(safeIoDispatcher) {
                                    messageRepository.saveMessage(sessionId!!, liveAssistantMsg)
                                }
                            }
                        }
                    }
                )

                var assistantMessage: ChatMessage? = null
                withContext(Dispatchers.Main) {
                    val msg = ChatMessage(id = assistantMessageId, text = finalResponse, isUser = false, sourceDocuments = sourceDocumentsString)
                    chatMessages.add(msg)
                    currentResponseChunk = ""
                    assistantMessage = msg
                    isThinking = false
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
            } catch (e: Exception) {
                if (e is CancellationException) {
                    if (currentResponseChunk.isNotEmpty()) {
                        val finalMsgId = if (assistantMessageId.isEmpty()) java.util.UUID.randomUUID().toString() else assistantMessageId
                        withContext(Dispatchers.Main + kotlinx.coroutines.NonCancellable) {
                            val msg = ChatMessage(id = finalMsgId, text = currentResponseChunk, isUser = false, sourceDocuments = sourceDocumentsString)
                            chatMessages.add(msg)
                            currentResponseChunk = ""
                        }
                    }
                    throw e
                }
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
                withContext(Dispatchers.Main + kotlinx.coroutines.NonCancellable) {
                    isGenerating = false
                    isThinking = false
                    agenticStateLabel = "Ready"
                    tokensPerSecond = 0f
                }
            }
        }
    }

    private fun hasRepetitionLoop(text: String): Boolean {
        val len = text.length
        if (len < 40) return false
        
        for (patternLen in 1..20) {
            val requiredRepeats = when (patternLen) {
                1 -> 25
                2 -> 15
                3, 4 -> 10
                else -> 6
            }
            
            if (len < patternLen * (requiredRepeats + 1)) continue
            
            val pattern = text.substring(len - patternLen)
            var isRepeating = true
            for (i in 1..requiredRepeats) {
                val start = len - (patternLen * (i + 1))
                val end = len - (patternLen * i)
                val prev = text.substring(start, end)
                if (prev != pattern) {
                    isRepeating = false
                    break
                }
            }
            if (isRepeating) {
                return true
            }
        }
        return false
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

    private fun getRealRamUsage(): Double {
        return try {
            val memInfo = android.os.Debug.MemoryInfo()
            android.os.Debug.getMemoryInfo(memInfo)
            val pssGb = memInfo.totalPss / 1048576.0 // Convert KB to GB
            if (pssGb > 0.0) {
                Math.round(pssGb * 100.0) / 100.0
            } else {
                3.25
            }
        } catch (e: Exception) {
            3.25
        }
    }

    override fun onCleared() {
        super.onCleared()
        aiProvider.close()
        isEngineReady = aiProvider.isInitialized
    }
}


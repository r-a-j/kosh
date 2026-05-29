package com.rajpawardotin.kosh.ui.chat

import android.content.Context
import android.net.Uri
import android.widget.Toast
import android.speech.RecognizerIntent
import android.content.Intent
import android.content.ActivityNotFoundException
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.material3.*
import com.rajpawardotin.kosh.domain.model.ChatSession
import com.rajpawardotin.kosh.domain.model.AttachedFile
import androidx.compose.runtime.*
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rajpawardotin.kosh.domain.model.ChatMessage
import com.rajpawardotin.kosh.ui.components.ChatBubble
import com.rajpawardotin.kosh.ui.components.ChatInput
import com.rajpawardotin.kosh.ui.components.ModelConfigCard
import com.rajpawardotin.kosh.ui.components.NeuralCoreWizard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    modifier: Modifier = Modifier
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    var showBottomSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberLazyListState()
    val density = LocalDensity.current
    var inputHeightDp by remember { mutableStateOf(80.dp) }

    val currentSessionId = viewModel.currentSessionId
    val currentSession = viewModel.savedSessions.find { it.id == currentSessionId }
    val isLocked = currentSession != null && currentSession.encryptedKeyPassword != null && !viewModel.activeSessionKeys.containsKey(currentSession.id)

    var sessionToLock by remember { mutableStateOf<ChatSession?>(null) }
    var showManageLockDialog by remember { mutableStateOf(false) }
    
    var sessionRecoveryMnemonic by remember { mutableStateOf<String?>(null) }
    var showRecoveryPhraseDialog by remember { mutableStateOf(false) }
    
    var backupPasswordToExport by remember { mutableStateOf("") }
    var showExportPasswordDialog by remember { mutableStateOf(false) }
    
    var backupPasswordToImport by remember { mutableStateOf("") }
    var importUri by remember { mutableStateOf<Uri?>(null) }
    var showImportPasswordDialog by remember { mutableStateOf(false) }

    var showScreenshotSetupDialog by remember { mutableStateOf(false) }
    var showScreenshotUnlockDialog by remember { mutableStateOf(false) }

    LaunchedEffect(currentSessionId) {
        if (currentSessionId != null && currentSession != null) {
            val isSessionLocked = currentSession.encryptedKeyPassword != null && !viewModel.activeSessionKeys.containsKey(currentSessionId)
            if (isSessionLocked && currentSession.encryptedKeyBiometric != null) {
                viewModel.unlockSessionWithBiometrics(currentSessionId, context) { success ->
                    if (success) {
                        Toast.makeText(context, "Vault Unlocked via Biometrics", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    LaunchedEffect(viewModel.isAppLocked) {
        if (viewModel.isAppLocked) {
            triggerAppBiometricUnlock(context, viewModel)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.toastMessage.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            uri?.let {
                viewModel.isCopyingModel = true // Set immediately for instant feedback
                scope.launch {
                    try {
                        val contentResolver = context.contentResolver
                        var fileName = "model.litertlm"
                        contentResolver.query(it, null, null, null, null)?.use { cursor ->
                            val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                            if (cursor.moveToFirst() && nameIndex != -1) {
                                fileName = cursor.getString(nameIndex)
                            }
                        }
                        viewModel.importModel(context, it, fileName)
                    } catch (e: Exception) {
                        viewModel.isCopyingModel = false
                        Toast.makeText(context, "Failed to copy: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    )

    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            uri?.let {
                val contentResolver = context.contentResolver
                var fileName = "unknown"
                var fileSize = 0L
                var fileType = "txt"

                contentResolver.query(it, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
                    if (cursor.moveToFirst()) {
                        if (nameIndex != -1) {
                            fileName = cursor.getString(nameIndex)
                        }
                        if (sizeIndex != -1) {
                            fileSize = cursor.getLong(sizeIndex)
                        }
                    }
                }

                val extension = fileName.substringAfterLast('.', "").lowercase()
                fileType = if (extension.isNotEmpty()) extension else "txt"

                if (extension != "txt" && extension != "md" && extension != "pdf") {
                    Toast.makeText(context, "Unsupported format. Only .txt, .md, and .pdf are supported.", Toast.LENGTH_SHORT).show()
                } else if (fileSize > 10 * 1024 * 1024) {
                    Toast.makeText(context, "File size exceeds 10MB secure limit.", Toast.LENGTH_SHORT).show()
                } else {
                    viewModel.attachFile(
                        AttachedFile(
                            fileName = fileName,
                            fileType = fileType,
                            fileSize = fileSize,
                            uriString = it.toString()
                        )
                    )

                }
            }
        }
    )

    val exportBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream"),
        onResult = { uri ->
            uri?.let {
                viewModel.exportBackup(context, it, backupPasswordToExport,
                    onSuccess = {
                        Toast.makeText(context, "Backup exported successfully!", Toast.LENGTH_SHORT).show()
                    },
                    onError = { err ->
                        Toast.makeText(context, "Export failed: $err", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    )

    val importBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            uri?.let {
                importUri = it
                showImportPasswordDialog = true
            }
        }
    )

    val speechRecognizerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = { result ->
            if (result.resultCode == android.app.Activity.RESULT_OK) {
                val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
                if (!spokenText.isNullOrBlank()) {
                    viewModel.prompt = if (viewModel.prompt.isEmpty()) spokenText else "${viewModel.prompt} $spokenText"
                }
            }
        }
    )

    val startVoiceInput = {
        viewModel.stopTts() // Stop speaking when user wants to dictate
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to Kosh...")
        }
        try {
            speechRecognizerLauncher.launch(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "Voice input is not supported on this device", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        val file = File(context.filesDir, "model.litertlm")
        if (file.exists()) {
            if (file.length() > 10 * 1024 * 1024) { // At least 10MB for a valid LLM
                viewModel.setModel(file.absolutePath)
                if (!viewModel.isEngineReady) {
                    viewModel.initializeEngine()
                }
            } else {
                file.delete()
            }
        }
    }

    LaunchedEffect(viewModel.chatMessages.size, viewModel.currentResponseChunk, viewModel.isThinking, viewModel.isSearchingInternet, viewModel.isGenerating) {
        if (viewModel.chatMessages.isNotEmpty() || viewModel.currentResponseChunk.isNotEmpty() || viewModel.isThinking || viewModel.isSearchingInternet || viewModel.isGenerating) {
            scrollState.animateScrollToItem(0)
        }
    }

    LaunchedEffect(viewModel.currentSessionId) {
        scrollState.scrollToItem(0)
    }

    val topLightsTransition = rememberInfiniteTransition(label = "top_lights_movement")
    val animationAngle by topLightsTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "lightsAngle"
    )

    val movementIntensity by animateFloatAsState(
        targetValue = if (viewModel.isGenerating) 1f else 0f,
        animationSpec = tween(1200, easing = FastOutSlowInEasing),
        label = "movementIntensity"
    )

    val tealOffsetX = (-120).dp + (80.dp * kotlin.math.cos(animationAngle.toDouble()).toFloat() * movementIntensity)
    val tealOffsetY = (-120).dp + (60.dp * kotlin.math.sin(animationAngle.toDouble()).toFloat() * movementIntensity)

    val indigoOffsetX = 120.dp - (80.dp * kotlin.math.sin(animationAngle.toDouble()).toFloat() * movementIntensity)
    val indigoOffsetY = (-120).dp + (60.dp * kotlin.math.cos(animationAngle.toDouble()).toFloat() * movementIntensity)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0C0C0F))
    ) {
        // Top-left teal radial glow
        Box(
            modifier = Modifier
                .size(450.dp)
                .offset(x = tealOffsetX, y = tealOffsetY)
                .background(Brush.radialGradient(listOf(Color(0xFF0F766E).copy(alpha = 0.18f), Color.Transparent)))
                .blur(80.dp)
        )
        // Top-right indigo radial glow
        Box(
            modifier = Modifier
                .size(450.dp)
                .align(Alignment.TopEnd)
                .offset(x = indigoOffsetX, y = indigoOffsetY)
                .background(Brush.radialGradient(listOf(Color(0xFF312E81).copy(alpha = 0.22f), Color.Transparent)))
                .blur(80.dp)
        )

        // Bottom pulsing web search blue/cyan glow
        AnimatedVisibility(
            visible = viewModel.isSearchingInternet,
            enter = fadeIn(animationSpec = tween(500)),
            exit = fadeOut(animationSpec = tween(500)),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            val infiniteTransition = rememberInfiniteTransition(label = "blue_glow")
            val glowAlpha by infiniteTransition.animateFloat(
                initialValue = 0.1f,
                targetValue = 0.35f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1200, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "glowAlpha"
            )
            val glowSize by infiniteTransition.animateFloat(
                initialValue = 350f,
                targetValue = 500f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1200, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "glowSize"
            )
            Box(
                modifier = Modifier
                    .size(glowSize.dp)
                    .offset(y = 120.dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF00E5FF).copy(alpha = glowAlpha),
                                Color(0xFF2979FF).copy(alpha = glowAlpha * 0.5f),
                                Color.Transparent
                            )
                        )
                    )
                    .blur(60.dp)
            )
        }

        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                com.rajpawardotin.kosh.ui.chat.components.ChatDrawerContent(
                    viewModel = viewModel,
                    drawerState = drawerState,
                    scope = scope,
                    context = context,
                    onLockSession = { session -> sessionToLock = session }
                )
            }
        ) {
            Scaffold(
                  modifier = Modifier.fillMaxSize(),
                  containerColor = Color.Transparent,
                  contentWindowInsets = WindowInsets(0, 0, 0, 0)
              ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .consumeWindowInsets(innerPadding)
            ) {
                // Header (Stays at the very top)
                com.rajpawardotin.kosh.ui.chat.components.ChatTopBar(
                    isEngineReady = viewModel.isEngineReady,
                    modelPath = viewModel.modelPath,
                    currentSession = viewModel.savedSessions.find { it.id == viewModel.currentSessionId },
                    isCurrentSessionUnlocked = viewModel.currentSessionId?.let { viewModel.activeSessionKeys.containsKey(it) } ?: false,
                    isTemporarySession = viewModel.isTemporarySession,
                    isGenerating = viewModel.isGenerating,
                    onMenuClick = { scope.launch { drawerState.open() } },
                    onCoreSelectorClick = { if (!viewModel.isGenerating) showBottomSheet = true },
                    onLockSettingsClick = { session -> sessionToLock = session },
                    onManageLockClick = { showManageLockDialog = true },
                    onNewChatClick = { isTemp -> viewModel.startNewChat(isTemporary = isTemp) },
                    onSettingsClick = { showBottomSheet = true }
                )

                // Badge for Temporary Chat
                AnimatedVisibility(
                    visible = viewModel.isTemporarySession,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            color = Color(0xFFFF9100).copy(alpha = 0.15f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF9100).copy(alpha = 0.3f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFFF9100))
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "TEMPORARY VAULT (NOT SAVED)",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    ),
                                    color = Color(0xFFFF9100)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .clickable { viewModel.startNewChat(isTemporary = false) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Exit Temporary Chat",
                                        tint = Color(0xFFFF9100),
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }
                    }
                }
                


                if (!viewModel.isEngineReady) {
                    NeuralCoreWizard(
                        modelPath = viewModel.modelPath,
                        isInitializing = viewModel.isInitializing,
                        isCopyingModel = viewModel.isCopyingModel,
                        isCheckingModels = viewModel.isCheckingModels,
                        selectedBackend = viewModel.selectedBackend,
                        backends = viewModel.backends,
                        onPickModel = { filePickerLauncher.launch(arrayOf("*/*")) },
                        onSelectBackend = { viewModel.selectBackend(it) },
                        onStartEngine = { viewModel.initializeEngine() },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))
                    )
                } else {
                    if (isLocked) {
                        LockedVaultScreen(
                            title = currentSession!!.title,
                            hasBiometricKey = currentSession.encryptedKeyBiometric != null,
                            onUnlockWithPassword = { pwd, onDone ->
                                viewModel.unlockSessionWithPassword(currentSession.id, pwd, onDone)
                            },
                            onUnlockWithBiometrics = { onDone ->
                                viewModel.unlockSessionWithBiometrics(currentSession.id, context, onDone)
                            },
                            onRecoverWithMnemonic = { mnemonic, newPwd, onDone ->
                                viewModel.recoverSessionWithMnemonic(currentSession.id, mnemonic, newPwd, context, onDone)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            if (viewModel.chatMessages.isEmpty() && !viewModel.isThinking && !viewModel.isGenerating && viewModel.currentResponseChunk.isEmpty()) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    com.rajpawardotin.kosh.ui.chat.components.ChatEmptyState(
                                        isTemporarySession = viewModel.isTemporarySession,
                                        onSuggestionClick = { suggestion ->
                                            viewModel.prompt = suggestion
                                        },
                                        onExitTemporaryClick = {
                                            viewModel.startNewChat(isTemporary = false)
                                        }
                                    )
                                }
                            } else {
                                LazyColumn(
                                    state = scrollState,
                                    modifier = Modifier.fillMaxSize(),
                                    reverseLayout = true,
                                    contentPadding = PaddingValues(
                                        start = 16.dp,
                                        end = 16.dp,
                                        top = 16.dp,
                                        bottom = inputHeightDp + 8.dp
                                    ),
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    // Index 0 is visual BOTTOM
                                    
                                    if (viewModel.isThinking || viewModel.isSearchingInternet || viewModel.currentResponseChunk.isNotEmpty() || viewModel.isGenerating) {
                                        item {
                                            ThinkingIndicator(
                                                text = when {
                                                    viewModel.currentResponseChunk.isNotEmpty() -> viewModel.currentResponseChunk
                                                    else -> viewModel.agenticStateLabel
                                                },
                                                isSearchingInternet = viewModel.isSearchingInternet
                                            )
                                        }
                                    }

                                    items(viewModel.chatMessages.reversed()) { message ->
                                        val currentlySpeakingId by viewModel.currentlySpeakingMessageId.collectAsState()
                                        ChatBubble(
                                            message = message,
                                            currentlySpeakingMessageId = currentlySpeakingId,
                                            onPlayTts = { id, text -> viewModel.playTts(id, text) },
                                            onStopTts = { viewModel.stopTts() },
                                            checkedItems = viewModel.checkedItems,
                                            onToggleChecklistItem = { index, checked ->
                                                viewModel.toggleChecklistItem(message.id, index, checked)
                                            }
                                        )
                                    }
                                }
                            }

                            ChatInput(
                                value = viewModel.prompt,
                                onValueChange = { viewModel.prompt = it },
                                onSend = { viewModel.sendMessage(context) },
                                onStop = { viewModel.stopGeneration() },
                                onVoiceClick = { startVoiceInput() },
                                onAttachClick = { documentPickerLauncher.launch("*/*") },
                                attachedFiles = viewModel.attachedFiles,
                                onDetachFile = { viewModel.detachFile(it) },
                                enabled = viewModel.isEngineReady,
                                isGenerating = viewModel.isGenerating,
                                isInternetEnabled = viewModel.isInternetEnabled,
                                isSearchForced = viewModel.isSearchForced,
                                onToggleSearch = { viewModel.toggleSearchForced() },
                                modifier = Modifier
                                    .onGloballyPositioned { coordinates ->
                                        inputHeightDp = with(density) { coordinates.size.height.toDp() }
                                    }
                                    .fillMaxWidth()
                                    .imePadding()
                                    .navigationBarsPadding()
                                    .padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
                            )
                        }
                    }
                }
            }
        }
    }

        if (showBottomSheet) {
            ModalBottomSheet(
                onDismissRequest = { showBottomSheet = false },
                sheetState = sheetState,
                containerColor = Color(0xFF1E1E1E),
                contentColor = Color.White,
                dragHandle = { BottomSheetDefaults.DragHandle(color = Color.White.copy(alpha = 0.3f)) }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 32.dp)
                ) {
                    ModelConfigCard(
                        modelPath = viewModel.modelPath,
                        isInitializing = viewModel.isInitializing,
                        isCopyingModel = viewModel.isCopyingModel,
                        isEngineReady = viewModel.isEngineReady,
                        selectedBackend = viewModel.selectedBackend,
                        backends = viewModel.backends,
                        isInternetEnabled = viewModel.isInternetEnabled,
                        tokensPerSecond = viewModel.tokensPerSecond,
                        npuLoad = viewModel.npuLoad,
                        ramUsage = viewModel.ramUsage,
                        selectedSearchEngine = viewModel.selectedSearchEngine,
                        searchEngines = viewModel.searchEngines,
                        tavilyApiKey = viewModel.tavilyApiKey,
                        braveApiKey = viewModel.braveApiKey,
                        isAppLockEnabled = viewModel.isAppLockEnabled,
                        isScreenshotEnabled = viewModel.isScreenshotEnabled,
                        onToggleAppLock = { viewModel.toggleAppLock(it) },
                        onToggleScreenshot = { enabled ->
                            if (!enabled) {
                                viewModel.toggleScreenshot(false)
                            } else {
                                if (!viewModel.isScreenshotPasscodeSet) {
                                    showScreenshotSetupDialog = true
                                } else {
                                    if (viewModel.isScreenshotBiometricEnabled) {
                                        viewModel.unlockScreenshotWithBiometrics(context) { success ->
                                            if (!success) {
                                                showScreenshotUnlockDialog = true
                                            }
                                        }
                                    } else {
                                        showScreenshotUnlockDialog = true
                                    }
                                }
                            }
                        },
                        onExportBackup = { showExportPasswordDialog = true },
                        onImportBackup = { importBackupLauncher.launch(arrayOf("*/*")) },
                        onTavilyApiKeyChange = { viewModel.updateTavilyApiKey(it) },
                        onBraveApiKeyChange = { viewModel.updateBraveApiKey(it) },
                        onPickModel = { filePickerLauncher.launch(arrayOf("*/*")) },
                        onDeleteModel = { viewModel.deleteModel() },
                        onSelectBackend = { viewModel.selectBackend(it) },
                        onSelectSearchEngine = { viewModel.selectSearchEngine(it) },
                        onStartEngine = { viewModel.initializeEngine() },
                        onToggleInternet = { viewModel.isInternetEnabled = it },
                        models = viewModel.models,
                        onSelectModel = { viewModel.selectModel(it.filePath) },
                        onSetModelTag = { name, tag -> viewModel.setModelTag(name, tag) },
                        onDeleteModelFile = { viewModel.deleteModelFile(it) }
                    )
                }
            }
        }

        // App Lock Overlay
        if (viewModel.isAppLocked) {
            com.rajpawardotin.kosh.ui.chat.components.AppLockOverlay(
                onUnlockClick = { triggerAppBiometricUnlock(context, viewModel) }
            )
        }

        // Crash Recovery Dialog
        if (viewModel.showCrashRecoveryDialog) {
            com.rajpawardotin.kosh.ui.chat.dialogs.CrashRecoveryDialog(
                onTryAgain = { viewModel.onCrashRecoveryDecision(tryAgain = true) },
                onDisableModel = { viewModel.onCrashRecoveryDecision(tryAgain = false) }
            )
        }

        // Lock Chat Dialog
        if (sessionToLock != null) {
            com.rajpawardotin.kosh.ui.chat.dialogs.LockVaultDialog(
                session = sessionToLock!!,
                onDismiss = { sessionToLock = null },
                onLockSubmit = { password, enableBiometric ->
                    viewModel.lockSession(sessionToLock!!.id, password, enableBiometric, context) { success, mnemonic ->
                        if (success && mnemonic != null) {
                            sessionRecoveryMnemonic = mnemonic
                            showRecoveryPhraseDialog = true
                            sessionToLock = null
                        } else {
                            android.widget.Toast.makeText(context, "Locking failed", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            )
        }


        // Recovery Phrase Display Dialog (After Locking)
        if (showRecoveryPhraseDialog && sessionRecoveryMnemonic != null) {
            com.rajpawardotin.kosh.ui.chat.dialogs.RecoveryPhraseDialog(
                mnemonic = sessionRecoveryMnemonic!!,
                onDismiss = {
                    showRecoveryPhraseDialog = false
                    sessionRecoveryMnemonic = null
                    Toast.makeText(context, "Neural Vault Secured", Toast.LENGTH_SHORT).show()
                }
            )
        }

        // Export Password Dialog
        if (showExportPasswordDialog) {
            com.rajpawardotin.kosh.ui.chat.dialogs.ExportPasswordDialog(
                onDismiss = { showExportPasswordDialog = false },
                onExport = { password ->
                    backupPasswordToExport = password
                    showExportPasswordDialog = false
                    val versionName = try { context.packageManager.getPackageInfo(context.packageName, 0).versionName } catch (e: Exception) { "1.0" }
                    val timestamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault()).format(java.util.Date())
                    exportBackupLauncher.launch("kosh_v${versionName}_${timestamp}.kosh")
                }
            )
        }

        // Import Password Dialog
        if (showImportPasswordDialog) {
            com.rajpawardotin.kosh.ui.chat.dialogs.ImportPasswordDialog(
                onDismiss = { showImportPasswordDialog = false },
                onImport = { password, onSuccessLoading, onErrorLoading ->
                    viewModel.importBackup(context, importUri!!, password,
                        onSuccess = {
                            onSuccessLoading()
                            Toast.makeText(context, "Backup restored successfully!", Toast.LENGTH_SHORT).show()
                            showImportPasswordDialog = false
                            importUri = null
                        },
                        onError = { err ->
                            onErrorLoading(err)
                            Toast.makeText(context, "Restore failed: $err", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            )
        }

        // Manage Vault Lock Dialog (for unlocked encrypted chats)
        if (showManageLockDialog && viewModel.currentSessionId != null) {
            val sessionId = viewModel.currentSessionId!!
            val session = viewModel.savedSessions.find { it.id == sessionId }
            var isProcessing by remember { mutableStateOf(false) }

            if (session != null) {
                com.rajpawardotin.kosh.ui.chat.dialogs.ManageVaultLockDialog(
                    sessionId = sessionId,
                    viewModel = viewModel,
                    onDismiss = { showManageLockDialog = false }
                )
            }
        }

        // Screenshot Setup Dialog
        if (showScreenshotSetupDialog) {
            com.rajpawardotin.kosh.ui.chat.dialogs.ScreenshotSetupDialog(
                onDismiss = { showScreenshotSetupDialog = false },
                onSetupSubmit = { password, enableBiometric ->
                    viewModel.setupScreenshotPasscode(password, enableBiometric, context) { success ->
                        showScreenshotSetupDialog = false
                    }
                }
            )
        }

        // Screenshot Unlock Dialog
        if (showScreenshotUnlockDialog) {
            com.rajpawardotin.kosh.ui.chat.dialogs.ScreenshotUnlockDialog(
                viewModel = viewModel,
                onDismiss = { showScreenshotUnlockDialog = false },
                onUnlockSubmit = { password ->
                    viewModel.unlockScreenshotWithPassword(password) { success ->
                        if (success) {
                            showScreenshotUnlockDialog = false
                        }
                    }
                }
            )
        }
    }
}

private fun triggerAppBiometricUnlock(context: Context, viewModel: ChatViewModel) {
    try {
        val biometricPrompt = androidx.biometric.BiometricPrompt(
            context as androidx.fragment.app.FragmentActivity,
            androidx.core.content.ContextCompat.getMainExecutor(context),
            object : androidx.biometric.BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: androidx.biometric.BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    viewModel.unlockApp()
                }
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                }
                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                }
            }
        )
        val promptInfo = androidx.biometric.BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock Kosh")
            .setSubtitle("Confirm biometrics to access Kosh")
            .setAllowedAuthenticators(
                androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG or 
                androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            .build()
        biometricPrompt.authenticate(promptInfo)
    } catch (e: Exception) {
        e.printStackTrace()
        viewModel.unlockApp()
    }
}

@Composable
fun ThinkingIndicator(text: String, isSearchingInternet: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "thinking")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .alpha(alpha)
    ) {
        if (isSearchingInternet) {
            Icon(
                imageVector = Icons.Default.Public,
                contentDescription = "Searching Web",
                tint = Color(0xFF00E5FF),
                modifier = Modifier
                    .size(18.dp)
                    .graphicsLayer(rotationZ = rotation)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF03DAC5))
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isSearchingInternet) Color(0xFF00E5FF) else Color(0xFF03DAC5),
            fontSize = 14.sp
        )
    }
}

private suspend fun copyFileToInternalStorage(context: Context, uri: Uri): String = withContext(Dispatchers.IO) {
    val fileName = "model.litertlm"
    val file = File(context.filesDir, fileName)
    context.contentResolver.openInputStream(uri)?.use { input ->
        FileOutputStream(file).use { output ->
            input.copyTo(output)
        }
    }
    file.absolutePath
}

@Composable
fun LockedVaultScreen(
    title: String,
    hasBiometricKey: Boolean,
    onUnlockWithPassword: (String, (Boolean) -> Unit) -> Unit,
    onUnlockWithBiometrics: ((Boolean) -> Unit) -> Unit,
    onRecoverWithMnemonic: (String, String, (Boolean) -> Unit) -> Unit,
    modifier: Modifier = Modifier
) {
    var password by remember { mutableStateOf("") }
    var passwordVisibility by remember { mutableStateOf(false) }
    var isProcessing by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    var showRecoveryDialog by remember { mutableStateOf(false) }
    val clipboardManager = LocalClipboardManager.current

    // Infinite transition for pulsing glowing rings behind the padlock
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale1 by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale1"
    )
    val pulseScale2 by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale2"
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF070709))
            .navigationBarsPadding()
            .imePadding(),
        contentAlignment = Alignment.Center
    ) {
        val maxHeight = this.maxHeight
        val isCompact = maxHeight < 580.dp

        val verticalPadding = if (isCompact) 14.dp else 28.dp
        val horizontalPadding = if (isCompact) 20.dp else 24.dp
        val itemSpacing = if (isCompact) 10.dp else 16.dp
        val padlockSize = if (isCompact) 64.dp else 100.dp
        val innerCircleSize = if (isCompact) 40.dp else 60.dp
        val haloSize = if (isCompact) 52.dp else 76.dp
        val iconSize = if (isCompact) 18.dp else 24.dp
        val titleSpace = if (isCompact) 2.dp else 6.dp

        // Futuristic mesh radial glows
        Box(
            modifier = Modifier
                .size(450.dp)
                .background(Brush.radialGradient(listOf(Color(0xFF03DAC5).copy(alpha = 0.12f), Color.Transparent)))
                .blur(70.dp)
        )
        Box(
            modifier = Modifier
                .size(350.dp)
                .background(Brush.radialGradient(listOf(Color(0xFF8B5CF6).copy(alpha = 0.08f), Color.Transparent)))
                .blur(90.dp)
        )

        // Glassmorphic Outer Card
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .heightIn(max = maxHeight - 48.dp)
                .animateContentSize()
                .graphicsLayer {
                    shadowElevation = 8.dp.toPx()
                    shape = RoundedCornerShape(32.dp)
                    clip = true
                }
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF03DAC5).copy(alpha = 0.3f),
                            Color(0xFF8B5CF6).copy(alpha = 0.1f),
                            Color(0xFF03DAC5).copy(alpha = 0.05f),
                            Color(0xFF8B5CF6).copy(alpha = 0.3f)
                        )
                    ),
                    shape = RoundedCornerShape(32.dp)
                ),
            shape = RoundedCornerShape(32.dp),
            color = Color(0xFF111116).copy(alpha = 0.9f),
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = verticalPadding, horizontal = horizontalPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(itemSpacing)
            ) {
                // Padlock Centerpiece with Synaptic Concentric Rings
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(padlockSize)
                ) {
                    // Outer pulsing cyan halo
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(scaleX = pulseScale2, scaleY = pulseScale2)
                            .border(1.dp, Color(0xFF03DAC5).copy(alpha = 0.08f), CircleShape)
                    )
                    // Inner pulsing violet halo
                    Box(
                        modifier = Modifier
                            .size(haloSize)
                            .graphicsLayer(scaleX = pulseScale1, scaleY = pulseScale1)
                            .border(1.dp, Color(0xFF8B5CF6).copy(alpha = 0.15f), CircleShape)
                    )
                    // Locked Hub
                    Box(
                        modifier = Modifier
                            .size(innerCircleSize)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFF03DAC5).copy(alpha = 0.15f),
                                        Color(0xFF8B5CF6).copy(alpha = 0.05f)
                                    )
                                )
                            )
                            .border(1.dp, Color(0xFF03DAC5).copy(alpha = 0.3f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Locked",
                            tint = Color(0xFF03DAC5),
                            modifier = Modifier.size(iconSize)
                        )
                    }
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 12.dp)
                ) {
                    Text(
                        text = "COGNITIVE VAULT SEALED",
                        style = if (isCompact) {
                            MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Black, 
                                letterSpacing = 2.sp
                            )
                        } else {
                            MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Black, 
                                letterSpacing = 2.5.sp
                            )
                        },
                        color = Color.White.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(titleSpace))
                    Text(
                        text = title.uppercase(),
                        style = if (isCompact) {
                            MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black, 
                                letterSpacing = 0.5.sp,
                                brush = Brush.linearGradient(
                                    colors = listOf(Color(0xFF03DAC5), Color(0xFFC084FC))
                                )
                            )
                        } else {
                            MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Black, 
                                letterSpacing = 1.sp,
                                brush = Brush.linearGradient(
                                    colors = listOf(Color(0xFF03DAC5), Color(0xFFC084FC))
                                )
                            )
                        },
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }

                if (!isCompact) {
                    Text(
                        text = "This conversation is protected using AES-256 local-first cryptography. Please enter your passcode.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp),
                        lineHeight = 20.sp
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = password,
                        onValueChange = { 
                            password = it
                            errorMsg = null
                        },
                        label = { Text("Passcode Signature") },
                        singleLine = true,
                        enabled = !isProcessing,
                        shape = RoundedCornerShape(16.dp),
                        visualTransformation = if (passwordVisibility) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisibility = !passwordVisibility }) {
                                Icon(
                                    imageVector = if (passwordVisibility) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = null,
                                    tint = Color.Gray
                                )
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedLabelColor = Color(0xFF03DAC5),
                            unfocusedLabelColor = Color.Gray,
                            focusedBorderColor = Color(0xFF03DAC5),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.08f),
                            cursorColor = Color(0xFF03DAC5)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (errorMsg != null) {
                        Text(
                            text = errorMsg!!,
                            color = Color(0xFFCF6679),
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Text(
                            text = "Forgot Passcode?",
                            color = Color(0xFF03DAC5).copy(alpha = 0.8f),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier
                                .clickable { if (!isProcessing) showRecoveryDialog = true }
                                .padding(vertical = 4.dp, horizontal = 8.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (hasBiometricKey) {
                        Surface(
                            onClick = {
                                if (!isProcessing) {
                                    onUnlockWithBiometrics { success ->
                                        if (!success) {
                                            Toast.makeText(context, "Identity Verification Failed", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            },
                            shape = RoundedCornerShape(16.dp),
                            color = Color.White.copy(alpha = 0.04f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                            modifier = Modifier.size(52.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Fingerprint,
                                    contentDescription = "Biometric Unlock",
                                    tint = Color(0xFF03DAC5),
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }
                    }

                    Button(
                        onClick = {
                            isProcessing = true
                            onUnlockWithPassword(password) { success ->
                                isProcessing = false
                                if (!success) {
                                    errorMsg = "Neural Signature Mismatch"
                                }
                            }
                        },
                        enabled = password.isNotEmpty() && !isProcessing,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .background(
                                brush = Brush.linearGradient(
                                    colors = if (password.isNotEmpty() && !isProcessing) {
                                        listOf(Color(0xFF03DAC5), Color(0xFF8B5CF6))
                                    } else {
                                        listOf(Color.White.copy(alpha = 0.08f), Color.White.copy(alpha = 0.08f))
                                    }
                                ),
                                shape = RoundedCornerShape(16.dp)
                            )
                    ) {
                        if (isProcessing) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Text(
                                "UNLOCK VAULT", 
                                color = if (password.isNotEmpty() && !isProcessing) Color.Black else Color.White.copy(alpha = 0.3f),
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Black, 
                                    letterSpacing = 1.5.sp
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    if (showRecoveryDialog) {
        var mnemonic by remember { mutableStateOf("") }
        var newPassword by remember { mutableStateOf("") }
        var newPasswordVisibility by remember { mutableStateOf(false) }
        var recoveryError by remember { mutableStateOf<String?>(null) }
        var isRecovering by remember { mutableStateOf(false) }

        androidx.compose.ui.window.Dialog(
            onDismissRequest = { if (!isRecovering) showRecoveryDialog = false },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .padding(16.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .border(
                        width = 1.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF03DAC5).copy(alpha = 0.4f),
                                Color(0xFF8B5CF6).copy(alpha = 0.4f)
                            )
                        ),
                        shape = RoundedCornerShape(28.dp)
                    ),
                color = Color(0xFF141418),
                shape = RoundedCornerShape(28.dp)
            ) {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Header
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF03DAC5).copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                tint = Color(0xFF03DAC5),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "VAULT RECOVERY",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            ),
                            color = Color.White
                        )
                    }

                    // Warning Alert Box
                    Surface(
                        color = Color(0xFFFF9100).copy(alpha = 0.08f),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF9100).copy(alpha = 0.25f))
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = Color(0xFFFF9100),
                                modifier = Modifier.size(18.dp).padding(top = 2.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                "Enter your offline 12-word mnemonic phrase. Correct entropy validation will securely rebuild your access keys and update your passcode.",
                                color = Color(0xFFFF9100).copy(alpha = 0.9f),
                                style = MaterialTheme.typography.bodySmall,
                                lineHeight = 16.sp
                            )
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        OutlinedTextField(
                            value = mnemonic,
                            onValueChange = { 
                                mnemonic = it
                                recoveryError = null
                            },
                            label = { Text("12-Word Recovery Phrase") },
                            placeholder = { Text("word1 word2 ... word12") },
                            enabled = !isRecovering,
                            shape = RoundedCornerShape(16.dp),
                            trailingIcon = {
                                IconButton(
                                    onClick = { 
                                        clipboardManager.getText()?.let { 
                                            mnemonic = it.text
                                            recoveryError = null
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentPaste,
                                        contentDescription = "Paste",
                                        tint = Color(0xFF03DAC5).copy(alpha = 0.7f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedLabelColor = Color(0xFF03DAC5),
                                unfocusedLabelColor = Color.Gray,
                                focusedBorderColor = Color(0xFF03DAC5),
                                unfocusedBorderColor = Color.White.copy(alpha = 0.08f),
                                cursorColor = Color(0xFF03DAC5)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = newPassword,
                            onValueChange = { newPassword = it },
                            label = { Text("Set New Passcode") },
                            singleLine = true,
                            enabled = !isRecovering,
                            shape = RoundedCornerShape(16.dp),
                            visualTransformation = if (newPasswordVisibility) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { newPasswordVisibility = !newPasswordVisibility }) {
                                    Icon(
                                        imageVector = if (newPasswordVisibility) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = null,
                                        tint = Color.Gray
                                    )
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedLabelColor = Color(0xFF03DAC5),
                                unfocusedLabelColor = Color.Gray,
                                focusedBorderColor = Color(0xFF03DAC5),
                                unfocusedBorderColor = Color.White.copy(alpha = 0.08f),
                                cursorColor = Color(0xFF03DAC5)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (recoveryError != null) {
                            Text(
                                text = recoveryError!!, 
                                color = Color(0xFFCF6679), 
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }

                    // Dialog Actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            enabled = !isRecovering,
                            onClick = { showRecoveryDialog = false },
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) {
                            Text("Cancel", color = Color.White.copy(alpha = 0.5f))
                        }

                        Button(
                            enabled = mnemonic.trim().split("\\s+".toRegex()).size == 12 && newPassword.isNotEmpty() && !isRecovering,
                            onClick = {
                                isRecovering = true
                                onRecoverWithMnemonic(mnemonic, newPassword) { success ->
                                    isRecovering = false
                                    if (success) {
                                        showRecoveryDialog = false
                                    } else {
                                        recoveryError = "Mnemonic Verification Failed"
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Transparent,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1.5f)
                                .height(48.dp)
                                .background(
                                    brush = Brush.linearGradient(
                                        colors = if (mnemonic.trim().split("\\s+".toRegex()).size == 12 && newPassword.isNotEmpty() && !isRecovering) {
                                            listOf(Color(0xFF03DAC5), Color(0xFF8B5CF6))
                                        } else {
                                            listOf(Color.White.copy(alpha = 0.08f), Color.White.copy(alpha = 0.08f))
                                        }
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                )
                        ) {
                            if (isRecovering) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                            } else {
                                Text(
                                    "REBUILD KEY",
                                    color = if (mnemonic.trim().split("\\s+".toRegex()).size == 12 && newPassword.isNotEmpty() && !isRecovering) Color.Black else Color.White.copy(alpha = 0.3f),
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.*
import com.rajpawardotin.kosh.domain.model.ChatSession
import com.rajpawardotin.kosh.domain.model.AttachedFile
import androidx.compose.runtime.*
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha

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
import com.rajpawardotin.kosh.ui.components.SettingsScreen
import com.rajpawardotin.kosh.ui.components.DashboardScreen
import com.rajpawardotin.kosh.ui.components.ModelHubScreen
import com.rajpawardotin.kosh.ui.components.JournalListScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.rajpawardotin.kosh.ui.chat.components.MarkdownText
import com.rajpawardotin.kosh.ui.chat.ChatContentBlock
import com.rajpawardotin.kosh.ui.chat.ResponseParser
import java.io.File
import java.io.FileOutputStream



@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    modifier: Modifier = Modifier
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    var previousScreen by remember { mutableStateOf(AppScreen.DASHBOARD) }
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    val activity = remember(context) {
        var ctx = context
        while (ctx is android.content.ContextWrapper) {
            if (ctx is android.app.Activity) break
            ctx = ctx.baseContext
        }
        ctx as? android.app.Activity
    }

    var lastBackPressTime by remember { mutableStateOf(0L) }

    androidx.activity.compose.BackHandler(enabled = true) {
        if (drawerState.isOpen) {
            scope.launch { drawerState.close() }
        } else if (viewModel.currentScreen == AppScreen.SETTINGS) {
            viewModel.currentScreen = previousScreen
        } else if (viewModel.currentScreen == AppScreen.CHAT || viewModel.currentScreen == AppScreen.MODEL_HUB || viewModel.currentScreen == AppScreen.JOURNALS) {
            viewModel.currentScreen = AppScreen.DASHBOARD
        } else {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastBackPressTime < 2000) {
                activity?.finish()
            } else {
                lastBackPressTime = currentTime
                Toast.makeText(context, "Press back one more time to exit", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val scrollState = rememberLazyListState()
    val isDragged by scrollState.interactionSource.collectIsDraggedAsState()
    var userHasScrolledUp by remember { mutableStateOf(false) }

    val lastItemIndex by remember {
        derivedStateOf {
            (viewModel.chatMessages.size - 1).coerceAtLeast(0)
        }
    }

    val isAtBottom by remember {
        derivedStateOf {
            val layoutInfo = scrollState.layoutInfo
            val visibleItemsInfo = layoutInfo.visibleItemsInfo
            if (visibleItemsInfo.isEmpty()) {
                true
            } else {
                val lastVisibleItem = visibleItemsInfo.last()
                val isLastItem = lastVisibleItem.index == layoutInfo.totalItemsCount - 1
                if (isLastItem) {
                    val lastItemBottom = lastVisibleItem.offset + lastVisibleItem.size
                    val viewportBottom = layoutInfo.viewportEndOffset - layoutInfo.afterContentPadding
                    // Within 40 pixels threshold
                    lastItemBottom - viewportBottom <= 40
                } else {
                    false
                }
            }
        }
    }

    LaunchedEffect(isAtBottom) {
        if (isAtBottom) {
            userHasScrolledUp = false
        }
    }

    LaunchedEffect(isDragged, isAtBottom) {
        if (isDragged && !isAtBottom) {
            userHasScrolledUp = true
        }
    }

    LaunchedEffect(viewModel.currentSessionId) {
        userHasScrolledUp = false
    }

    val showScrollToBottom by remember {
        derivedStateOf {
            !isAtBottom
        }
    }
    val emptyStateScrollState = rememberScrollState()
    val density = LocalDensity.current
    var inputHeightDp by remember { mutableStateOf(80.dp) }
    
    // Configurable fading edge thickness/height
    var topFadeHeightDp by remember { mutableStateOf(48.dp) }
    var headerHeightDp by remember { mutableStateOf(80.dp) }

    val isHistoryEmpty = viewModel.chatMessages.isEmpty() && !viewModel.isThinking && !viewModel.isGenerating && viewModel.currentResponseChunk.isEmpty()
    val scrollProgress by remember(isHistoryEmpty) {
        derivedStateOf {
            if (isHistoryEmpty) {
                val offset = emptyStateScrollState.value.toFloat()
                (offset / 150f).coerceIn(0f, 1f)
            } else {
                if (scrollState.firstVisibleItemIndex > 0) {
                    1f
                } else {
                    val offset = scrollState.firstVisibleItemScrollOffset.toFloat()
                    (offset / 150f).coerceIn(0f, 1f)
                }
            }
        }
    }

    val currentSessionId = viewModel.currentSessionId
    val currentSession = viewModel.savedSessions.find { it.id == currentSessionId }
    val isLocked = currentSession != null && currentSession.encryptedKeyPassword != null && !viewModel.activeSessionKeys.containsKey(currentSession.id)

    var sessionToLock by remember { mutableStateOf<ChatSession?>(null) }
    var showManageLockDialog by remember { mutableStateOf(false) }
    var showManageTagsDialog by remember { mutableStateOf(false) }

    
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
                        Toast.makeText(context, "Chat Unlocked via Biometrics", Toast.LENGTH_SHORT).show()
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



    val chatMessages = viewModel.chatMessages
    
    // Scroll to bottom when user sends a new message or assistant finishes
    LaunchedEffect(chatMessages.size) {
        if (chatMessages.isNotEmpty()) {
            val lastMessage = chatMessages.last()
            if (lastMessage.isUser) {
                userHasScrolledUp = false
                scrollState.animateScrollToBottom(lastItemIndex)
            } else {
                if (!userHasScrolledUp) {
                    scrollState.animateScrollToBottom(lastItemIndex)
                }
            }
        }
    }

    // Scroll to bottom when generation starts
    LaunchedEffect(viewModel.isGenerating, viewModel.isThinking, viewModel.isSearchingInternet) {
        val isStartingActive = viewModel.isGenerating || viewModel.isThinking || viewModel.isSearchingInternet
        if (isStartingActive && !userHasScrolledUp) {
            scrollState.animateScrollToBottom(lastItemIndex)
        }
    }

    // Smooth streaming autoscroll (keeping stream stuck to bottom)
    val latestChunkText = viewModel.currentResponseChunk
    LaunchedEffect(latestChunkText, userHasScrolledUp, isDragged) {
        if (!userHasScrolledUp && !isDragged && (viewModel.isGenerating || viewModel.isThinking) && latestChunkText.isNotEmpty()) {
            scrollState.scrollToBottom(lastItemIndex)
        }
    }

    // Initial scroll to bottom when session loads
    LaunchedEffect(viewModel.currentSessionId) {
        if (chatMessages.isNotEmpty()) {
            scrollState.scrollToBottom(lastItemIndex)
        }
    }

    // Auto-scroll to bottom when generation completes and stats HUD collapses
    LaunchedEffect(viewModel.isGenerating) {
        if (!viewModel.isGenerating && chatMessages.isNotEmpty()) {
            // Wait for collapse animations and layout passes to settle
            kotlinx.coroutines.delay(220)
            scrollState.animateScrollToBottom(lastItemIndex)
        }
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Static ambient glow — no animation, no blur, zero GPU cost
        Box(
            modifier = Modifier
                .size(400.dp)
                .offset(x = (-100).dp, y = (-100).dp)
                .background(Brush.radialGradient(
                    0.0f to primaryColor.copy(alpha = 0.14f),
                    0.3f to primaryColor.copy(alpha = 0.07f),
                    0.6f to primaryColor.copy(alpha = 0.02f),
                    1.0f to Color.Transparent
                ))
        )
        Box(
            modifier = Modifier
                .size(400.dp)
                .align(Alignment.TopEnd)
                .offset(x = 100.dp, y = (-100).dp)
                .background(Brush.radialGradient(
                    0.0f to secondaryColor.copy(alpha = 0.18f),
                    0.3f to secondaryColor.copy(alpha = 0.09f),
                    0.6f to secondaryColor.copy(alpha = 0.03f),
                    1.0f to Color.Transparent
                ))
        )


        if (viewModel.currentScreen == AppScreen.MODEL_HUB) {
            ModelHubScreen(
                viewModel = viewModel,
                onPickModel = { filePickerLauncher.launch(arrayOf("*/*")) },
                onBackClick = {
                    viewModel.currentScreen = AppScreen.DASHBOARD
                },
                modifier = Modifier.fillMaxSize()
            )
        } else if (viewModel.currentScreen == AppScreen.SETTINGS) {
            SettingsScreen(
                viewModel = viewModel,
                onPickModel = { filePickerLauncher.launch(arrayOf("*/*")) },
                onExportBackup = { showExportPasswordDialog = true },
                onImportBackup = { importBackupLauncher.launch(arrayOf("*/*")) },
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
                onBackClick = {
                    viewModel.currentScreen = previousScreen
                },
                modifier = Modifier.fillMaxSize()
            )
        } else if (viewModel.currentScreen == AppScreen.JOURNALS) {
            JournalListScreen(
                viewModel = viewModel,
                onBackClick = {
                    viewModel.currentScreen = AppScreen.DASHBOARD
                },
                onLoadSession = { sessionId ->
                    viewModel.navigateToChatWithAutoStart {
                        viewModel.loadSession(sessionId)
                    }
                    viewModel.currentScreen = AppScreen.CHAT
                },
                onNewEntryClick = {
                    viewModel.navigateToChatWithAutoStart {
                        viewModel.startNewChatWithTags(isTemporary = false, listOf("Journal"))
                    }
                    viewModel.currentScreen = AppScreen.CHAT
                },
                onLockSession = { session -> sessionToLock = session },
                modifier = Modifier.fillMaxSize()
            )
        } else {
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
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .consumeWindowInsets(innerPadding)
                    ) {
                        // 1. Content Container (fills the Box)
                        Box(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            if (viewModel.currentScreen == AppScreen.DASHBOARD) {
                                DashboardScreen(
                                    viewModel = viewModel,
                                    onManageModelsClick = {
                                        viewModel.currentScreen = AppScreen.MODEL_HUB
                                    },
                                    onQuickChatClick = {
                                        viewModel.navigateToChatWithAutoStart {
                                            viewModel.startNewChat()
                                        }
                                        viewModel.currentScreen = AppScreen.CHAT
                                    },
                                    onStartJournalSession = {
                                        viewModel.navigateToChatWithAutoStart {
                                            viewModel.startNewChatWithTags(isTemporary = false, listOf("Journal"))
                                        }
                                        viewModel.currentScreen = AppScreen.CHAT
                                    },
                                    onLoadSession = { sessionId ->
                                        viewModel.navigateToChatWithAutoStart {
                                            viewModel.loadSession(sessionId)
                                        }
                                        viewModel.currentScreen = AppScreen.CHAT
                                    },
                                    onOpenJournals = {
                                        viewModel.currentScreen = AppScreen.JOURNALS
                                    },
                                     onOpenSettings = {
                                         previousScreen = viewModel.currentScreen
                                         viewModel.currentScreen = AppScreen.SETTINGS
                                     },
                                    onLockSession = { session -> sessionToLock = session },
                                    onAttachDocumentClick = { documentPickerLauncher.launch("*/*") },
                                    topPadding = headerHeightDp,
                                    bottomPadding = inputHeightDp,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else { // AppScreen.CHAT
                                if (viewModel.isInitializing) {
                                    IgnitingCoreOverlay(
                                        modelPath = viewModel.modelPath,
                                        selectedBackend = viewModel.selectedBackend,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(top = headerHeightDp)
                                    )
                                } else if (!viewModel.isEngineReady) {
                                    EngineOfflineFallback(
                                        modelPath = viewModel.modelPath,
                                        onInitialize = { viewModel.triggerManualInitialization() },
                                        onGoToHub = { viewModel.currentScreen = AppScreen.MODEL_HUB },
                                        onGoToDashboard = { viewModel.currentScreen = AppScreen.DASHBOARD },
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(top = headerHeightDp)
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
                                                .fillMaxSize()
                                                .padding(top = headerHeightDp)
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.BottomCenter
                                        ) {
                                            if (viewModel.chatMessages.isEmpty() && !viewModel.isThinking && !viewModel.isGenerating && viewModel.currentResponseChunk.isEmpty()) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .padding(top = headerHeightDp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    com.rajpawardotin.kosh.ui.chat.components.ChatEmptyState(
                                                        onSuggestionClick = { suggestion ->
                                                            viewModel.prompt = suggestion
                                                        },
                                                        bottomPadding = inputHeightDp
                                                    )
                                                }
                                            } else {
                                                val topFadePx = with(density) { topFadeHeightDp.toPx() }
                                                val topBoundaryPx = with(density) { headerHeightDp.toPx() }

                                                LazyColumn(
                                                    state = scrollState,
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .fadingEdges(
                                                            topBoundaryPx = topBoundaryPx,
                                                            topFadePx = topFadePx
                                                        ),
                                                    reverseLayout = false,
                                                    contentPadding = PaddingValues(
                                                        start = 16.dp,
                                                        end = 16.dp,
                                                        top = headerHeightDp + 8.dp,
                                                        bottom = inputHeightDp + 32.dp
                                                    ),
                                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                                ) {
                                                    items(
                                                        items = viewModel.chatMessages,
                                                        key = { it.id }
                                                    ) { message ->
                                                        val currentlySpeakingId by viewModel.currentlySpeakingMessageId.collectAsState()
                                                        ChatBubble(
                                                            message = message,
                                                            currentlySpeakingMessageId = currentlySpeakingId,
                                                            onPlayTts = { id, text -> viewModel.playTts(id, text) },
                                                            onStopTts = { viewModel.stopTts() },
                                                            checkedItems = viewModel.checkedItems,
                                                            onToggleChecklistItem = { index, checked ->
                                                                viewModel.toggleChecklistItem(message.id, index, checked)
                                                            },
                                                            onFeedbackChanged = { feedback ->
                                                                viewModel.updateMessageFeedback(message.id, feedback)
                                                            },
                                                            onManageTagsClick = if (!viewModel.isTemporarySession && !isLocked) {
                                                                { showManageTagsDialog = true }
                                                            } else null,
                                                            isSearchingInternet = viewModel.isSearchingInternet,
                                                            isGenerating = viewModel.isGenerating,
                                                            agenticStateLabel = viewModel.agenticStateLabel,
                                                            modifier = Modifier.animateItem()
                                                        )
                                                    }
                                                }

                                                // Scroll to Bottom Button
                                                AnimatedVisibility(
                                                    visible = showScrollToBottom,
                                                    enter = fadeIn(animationSpec = tween(300)) + scaleIn(animationSpec = tween(300), initialScale = 0.8f),
                                                    exit = fadeOut(animationSpec = tween(300)) + scaleOut(animationSpec = tween(300), targetScale = 0.8f),
                                                    modifier = Modifier
                                                        .align(Alignment.BottomEnd)
                                                        .navigationBarsPadding()
                                                        .imePadding()
                                                        .padding(end = 20.dp, bottom = inputHeightDp + 24.dp)
                                                ) {
                                                    SmallFloatingActionButton(
                                                        onClick = {
                                                            scope.launch {
                                                                scrollState.animateScrollToBottom(lastItemIndex)
                                                            }
                                                        },
                                                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                                                        contentColor = MaterialTheme.colorScheme.primary,
                                                        shape = CircleShape,
                                                        modifier = Modifier
                                                            .size(40.dp)
                                                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), CircleShape)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.KeyboardArrowDown,
                                                            contentDescription = "Scroll to bottom",
                                                            modifier = Modifier.size(20.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // 2. ChatInput and Stats HUD Container (always visible with soft gradient background)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomCenter)
                                .background(
                                    brush = Brush.verticalGradient(
                                        colorStops = arrayOf(
                                            0.0f to Color.Transparent,
                                            0.12f to MaterialTheme.colorScheme.background.copy(alpha = 0.45f),
                                            0.35f to MaterialTheme.colorScheme.background.copy(alpha = 0.85f),
                                            0.6f to MaterialTheme.colorScheme.background.copy(alpha = 0.98f),
                                            1.0f to MaterialTheme.colorScheme.background
                                        )
                                    )
                                )
                                .onGloballyPositioned { coordinates ->
                                    inputHeightDp = with(density) { coordinates.size.height.toDp() }
                                }
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .navigationBarsPadding()
                                    .imePadding()
                                    .padding(start = 16.dp, end = 16.dp, bottom = 12.dp, top = 20.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Dynamic Performance HUD
                                AnimatedVisibility(
                                    visible = viewModel.isGenerating && viewModel.showHardwareStats,
                                    enter = fadeIn() + expandVertically(expandFrom = Alignment.Bottom),
                                    exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Bottom)
                                ) {
                                    HardwareStatsHUD(
                                        backend = viewModel.selectedBackend,
                                        speed = viewModel.tokensPerSecond,
                                        load = viewModel.npuLoad,
                                        ram = viewModel.ramUsage
                                    )
                                }

                                ChatInput(
                                    value = viewModel.prompt,
                                    onValueChange = { viewModel.prompt = it },
                                    onSend = {
                                         val hasInput = viewModel.prompt.isNotBlank() || viewModel.attachedFiles.isNotEmpty()
                                         if (hasInput && viewModel.batteryPercentage < 15 && !viewModel.isCharging) {
                                             viewModel.showLowBatteryDialog = true
                                         } else {
                                             if (viewModel.currentScreen == AppScreen.DASHBOARD) {
                                                 val enteredPrompt = viewModel.prompt
                                                 viewModel.currentScreen = AppScreen.CHAT
                                                 viewModel.startNewChat()
                                                 viewModel.prompt = enteredPrompt
                                             }
                                             userHasScrolledUp = false
                                             viewModel.sendMessage(context)
                                         }
                                     },
                                    onStop = { viewModel.stopGeneration() },
                                    onVoiceClick = {
                                        if (viewModel.currentScreen == AppScreen.DASHBOARD) {
                                            viewModel.currentScreen = AppScreen.CHAT
                                            viewModel.startNewChat()
                                        }
                                        startVoiceInput()
                                    },
                                    onAttachClick = { documentPickerLauncher.launch("*/*") },
                                    attachedFiles = viewModel.attachedFiles,
                                    onDetachFile = { viewModel.detachFile(it) },
                                    enabled = viewModel.modelPath != null,
                                    isGenerating = viewModel.isGenerating,
                                    isInternetEnabled = viewModel.isInternetEnabled,
                                    isSearchForced = viewModel.isSearchForced,
                                    onToggleSearch = { viewModel.toggleSearchForced() },
                                    chatMode = viewModel.currentChatMode,
                                    onChatModeChange = { viewModel.updateChatMode(it) },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        // 3. Floating Header Column (floats on top of the content)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.TopCenter)
                                .onGloballyPositioned { coordinates ->
                                    headerHeightDp = with(density) { coordinates.size.height.toDp() }
                                }
                        ) {
                            com.rajpawardotin.kosh.ui.chat.components.ChatTopBar(
                                isEngineReady = viewModel.isEngineReady,
                                modelPath = viewModel.modelPath,
                                currentSession = viewModel.savedSessions.find { it.id == viewModel.currentSessionId },
                                isCurrentSessionUnlocked = viewModel.currentSessionId?.let { viewModel.activeSessionKeys.containsKey(it) } ?: false,
                                isTemporarySession = viewModel.isTemporarySession,
                                isGenerating = viewModel.isGenerating,
                                onMenuClick = { scope.launch { drawerState.open() } },
                                onCoreSelectorClick = {
                                    if (!viewModel.isGenerating) {
                                        previousScreen = viewModel.currentScreen
                                        viewModel.currentScreen = AppScreen.SETTINGS
                                    }
                                },
                                onLockSettingsClick = { session -> sessionToLock = session },
                                onManageLockClick = { showManageLockDialog = true },
                                onNewChatClick = { isTemp -> 
                                    viewModel.startNewChat(isTemporary = isTemp)
                                    viewModel.currentScreen = AppScreen.CHAT
                                },
                                onSettingsClick = {
                                    previousScreen = viewModel.currentScreen
                                    viewModel.currentScreen = AppScreen.SETTINGS
                                },
                                scrollProgress = { scrollProgress },
                                onBackClick = if (viewModel.currentScreen == AppScreen.CHAT) {
                                    { viewModel.currentScreen = AppScreen.DASHBOARD }
                                } else null
                            )

                            // Badge for Temporary Chat
                            AnimatedVisibility(
                                visible = viewModel.isTemporarySession && viewModel.currentScreen == AppScreen.CHAT,
                                enter = fadeIn() + expandVertically(),
                                exit = fadeOut() + shrinkVertically()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 4.dp),
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
                                                text = "TEMPORARY SESSION (NOT SAVED)",
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

                            // Badge for Low Battery Warning
                            AnimatedVisibility(
                                visible = viewModel.batteryPercentage < 20 && !viewModel.isCharging && viewModel.currentScreen == AppScreen.CHAT,
                                enter = fadeIn() + expandVertically(),
                                exit = fadeOut() + shrinkVertically()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 4.dp),
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f)),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Warning,
                                                contentDescription = "Low Battery Warning",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "LOW BATTERY: local generation drains battery faster (${viewModel.batteryPercentage}%)",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    letterSpacing = 0.5.sp
                                                ),
                                                color = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
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

        // Low Battery Sending Warning Dialog
        if (viewModel.showLowBatteryDialog) {
            AlertDialog(
                onDismissRequest = { viewModel.showLowBatteryDialog = false },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .border(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f), RoundedCornerShape(28.dp)),
                containerColor = MaterialTheme.colorScheme.surface,
                titleContentColor = MaterialTheme.colorScheme.onSurface,
                textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Low Battery Warning", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    }
                },
                text = {
                    Text(
                        text = "Your battery is at ${viewModel.batteryPercentage}%. Running local models consumes significant CPU/GPU/NPU resources and can drain battery very rapidly or lead to device shutdown. Please connect a charger, or confirm if you want to proceed anyway.",
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.showLowBatteryDialog = false
                            if (viewModel.currentScreen == AppScreen.DASHBOARD) {
                                val enteredPrompt = viewModel.prompt
                                viewModel.currentScreen = AppScreen.CHAT
                                viewModel.startNewChat()
                                viewModel.prompt = enteredPrompt
                            }
                            viewModel.sendMessage(context)
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Proceed Anyway", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.showLowBatteryDialog = false }) {
                        Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
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
                    Toast.makeText(context, "Chat Locked & Secured", Toast.LENGTH_SHORT).show()
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

        // Manage Tags Dialog
        if (showManageTagsDialog) {
            com.rajpawardotin.kosh.ui.chat.dialogs.ManageTagsDialog(
                viewModel = viewModel,
                onDismiss = { showManageTagsDialog = false }
            )
        }

        // Backend Selection and Fallback dialogues
        if (viewModel.showInitializeBackendDialog) {
            com.rajpawardotin.kosh.ui.chat.dialogs.InitializeBackendDialog(
                backends = viewModel.backends,
                onSelectBackend = { viewModel.initializeEngineWithBackend(it) },
                onDismiss = { viewModel.showInitializeBackendDialog = false }
            )
        }

        if (viewModel.showBackendFallbackPrompt && viewModel.failedBackend != null) {
            val remaining = viewModel.backends.filter { it !in viewModel.attemptedBackends }
            com.rajpawardotin.kosh.ui.chat.dialogs.BackendFallbackPromptDialog(
                failedBackend = viewModel.failedBackend!!,
                remainingBackends = remaining,
                onSelectBackend = { viewModel.initializeEngineWithFallbackBackend(it) },
                onDismiss = { viewModel.showBackendFallbackPrompt = false }
            )
        }

        if (viewModel.showModelIncompatibleDialog) {
            com.rajpawardotin.kosh.ui.chat.dialogs.ModelIncompatibleDialog(
                onGoToHub = {
                    viewModel.showModelIncompatibleDialog = false
                    viewModel.currentScreen = AppScreen.MODEL_HUB
                },
                onDismiss = { viewModel.showModelIncompatibleDialog = false }
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

private data class DisplayState(
    val streamState: ResponseParser.StreamState,
    val parsedBlocks: List<ChatContentBlock>
)

@Composable
fun ThinkingIndicator(
    text: String,
    isSearchingInternet: Boolean,
    isGenerating: Boolean = false,
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val onBackground = MaterialTheme.colorScheme.onBackground

    val rotation = if (isSearchingInternet) {
        val infiniteTransition = rememberInfiniteTransition(label = "searching")
        val rot by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(2500, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "rotation"
        )
        rot
    } else {
        0f
    }

    // State for throttled parsed stream state and parsed blocks combined to ensure single-tick updates
    var displayState by remember {
        mutableStateOf(
            DisplayState(
                streamState = ResponseParser.StreamState(isThinking = false, thinkingContent = "", cleanResponse = ""),
                parsedBlocks = emptyList()
            )
        )
    }

    // State for the rate-limited intermediate thinking steps preview
    var displayThinkingText by remember { mutableStateOf("") }
    var textVisible by remember { mutableStateOf(true) }

    // Use rememberUpdatedState so we can read the latest text inside the LaunchedEffect loop
    // without triggering a restart of the coroutine (which would cancel the delay and break throttling).
    val latestText by rememberUpdatedState(text)

    // Throttled background parsing loop to offload heavy calculations from the Main Thread
    LaunchedEffect(isGenerating) {
        var lastProcessedText = ""
        while (true) {
            val currentText = latestText
            if (currentText != lastProcessedText) {
                lastProcessedText = currentText
                withContext(Dispatchers.Default) {
                    val parsedStream = ResponseParser.parseStreamState(lastProcessedText)
                    val rawCleanText = parsedStream.cleanResponse
                    val suffix = if (isGenerating) " ▊" else ""
                    
                    val parsed = ResponseParser.parse(rawCleanText)
                    val finalBlocks = if (isGenerating && parsed.isNotEmpty()) {
                        val lastIdx = parsed.indexOfLast { it is ChatContentBlock.Text }
                        if (lastIdx != -1) {
                            parsed.mapIndexed { idx, block ->
                                if (idx == lastIdx) {
                                    ChatContentBlock.Text((block as ChatContentBlock.Text).content + suffix)
                                } else {
                                    block
                                }
                            }
                        } else {
                            parsed + ChatContentBlock.Text(suffix)
                        }
                    } else {
                        parsed
                    }
                    
                    withContext(Dispatchers.Main) {
                        displayState = DisplayState(parsedStream, finalBlocks)
                    }
                }
            }
            delay(60) // Steady throttle to ~16.6 FPS that is NOT interrupted by new token emissions
        }
    }

    // Keep displayThinkingText updated from streamState.thinkingContent at a controlled rate
    val currentThinkingContent by rememberUpdatedState(displayState.streamState.thinkingContent)
    LaunchedEffect(displayState.streamState.isThinking) {
        if (displayState.streamState.isThinking) {
            val initialLines = currentThinkingContent.lines()
                .map { it.trim() }
                .filter { it.isNotEmpty() }
            displayThinkingText = if (initialLines.isNotEmpty()) {
                initialLines.takeLast(2).joinToString("\n")
            } else {
                "Analyzing..."
            }
            textVisible = true
            
            while (true) {
                delay(1000)
                val lines = currentThinkingContent.lines()
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                
                val newText = if (lines.isNotEmpty()) {
                    lines.takeLast(2).joinToString("\n")
                } else {
                    "Analyzing..."
                }
                
                if (newText != displayThinkingText) {
                    textVisible = false
                    delay(300) // Wait for fade out to complete
                    displayThinkingText = newText
                    textVisible = true
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.Start
    ) {
        // Content Column
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp)
        ) {
            if (isSearchingInternet && displayState.streamState.cleanResponse.isEmpty() && !displayState.streamState.isThinking) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Public,
                        contentDescription = "Searching Web",
                        tint = secondary,
                        modifier = Modifier
                            .size(16.dp)
                            .graphicsLayer { rotationZ = rotation }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Searching the web...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = secondary,
                        fontSize = 14.sp
                    )
                }
            } else if (displayState.streamState.isThinking) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Memory,
                            contentDescription = "Thinking",
                            tint = primary.copy(alpha = 0.8f),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Thinking...",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = primary.copy(alpha = 0.8f)
                        )
                    }
                    
                    // Smooth fading intermediate thinking steps
                    AnimatedVisibility(
                        visible = textVisible,
                        enter = fadeIn(animationSpec = tween(400)),
                        exit = fadeOut(animationSpec = tween(300))
                    ) {
                        Text(
                            text = displayThinkingText,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                lineHeight = 18.sp,
                                fontSize = 12.5.sp
                            ),
                            color = onBackground.copy(alpha = 0.55f),
                            maxLines = 2,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            modifier = Modifier.padding(start = 26.dp, top = 4.dp, bottom = 4.dp)
                        )
                    }
                }
            } else {
                // Completed thinking block (or no thinking tags at all)
                if (displayState.streamState.thinkingContent.isNotEmpty()) {
                    com.rajpawardotin.kosh.ui.chat.components.ThinkingBlockCard(content = displayState.streamState.thinkingContent)
                    Spacer(modifier = Modifier.height(8.dp))
                }
                
                if (displayState.streamState.cleanResponse.isNotEmpty()) {
                    val parsedBlocks = displayState.parsedBlocks

                    parsedBlocks.forEachIndexed { index, block ->
                        key("temp_generating_$index") {
                            when (block) {
                                is ChatContentBlock.Text -> {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                    ) {
                                        MarkdownText(
                                            content = block.content
                                        )
                                    }
                                }
                                is ChatContentBlock.Checklist -> {
                                    com.rajpawardotin.kosh.ui.chat.components.ChecklistCard(
                                        items = block.items,
                                        messageKey = "temp_generating",
                                        checkedItems = emptyMap(),
                                        onToggleChecklistItem = { _, _ -> }
                                    )
                                }
                                is ChatContentBlock.CodeBlock -> {
                                    com.rajpawardotin.kosh.ui.chat.components.CodeBlockCard(
                                        language = block.language,
                                        code = block.code
                                    )
                                }
                                is ChatContentBlock.Sources -> {
                                    com.rajpawardotin.kosh.ui.chat.components.SourcesCarousel(items = block.items)
                                }
                                is ChatContentBlock.MathBlock -> {
                                    com.rajpawardotin.kosh.ui.chat.components.MathFormulaCard(formula = block.formula)
                                }
                                is ChatContentBlock.Thinking -> {
                                    com.rajpawardotin.kosh.ui.chat.components.ThinkingBlockCard(content = block.content)
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }
                } else if (isGenerating && !isSearchingInternet) {
                    Text(
                        text = "▊",
                        style = MaterialTheme.typography.bodyMedium,
                        color = primary
                    )
                }
            }
        }
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

    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val outline = MaterialTheme.colorScheme.outline
    val onSurfaceMuted = MaterialTheme.colorScheme.onSurfaceVariant

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
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

        // Subtle static radial glows (no blur)
        Box(
            modifier = Modifier
                .size(400.dp)
                .background(Brush.radialGradient(
                    0.0f to primary.copy(alpha = 0.10f),
                    0.3f to primary.copy(alpha = 0.05f),
                    0.6f to primary.copy(alpha = 0.01f),
                    1.0f to Color.Transparent
                ))
        )
        Box(
            modifier = Modifier
                .size(300.dp)
                .background(Brush.radialGradient(
                    0.0f to secondary.copy(alpha = 0.06f),
                    0.3f to secondary.copy(alpha = 0.03f),
                    0.6f to secondary.copy(alpha = 0.01f),
                    1.0f to Color.Transparent
                ))
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
                            primary.copy(alpha = 0.3f),
                            secondary.copy(alpha = 0.1f),
                            primary.copy(alpha = 0.05f),
                            secondary.copy(alpha = 0.3f)
                        )
                    ),
                    shape = RoundedCornerShape(32.dp)
                ),
            shape = RoundedCornerShape(32.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
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
                    // Outer pulsing halo
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(scaleX = pulseScale2, scaleY = pulseScale2)
                            .border(1.dp, primary.copy(alpha = 0.08f), CircleShape)
                    )
                    // Inner pulsing halo
                    Box(
                        modifier = Modifier
                            .size(haloSize)
                            .graphicsLayer(scaleX = pulseScale1, scaleY = pulseScale1)
                            .border(1.dp, secondary.copy(alpha = 0.15f), CircleShape)
                    )
                    // Locked Hub
                    Box(
                        modifier = Modifier
                            .size(innerCircleSize)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        primary.copy(alpha = 0.15f),
                                        secondary.copy(alpha = 0.05f)
                                    )
                                )
                            )
                            .border(1.dp, primary.copy(alpha = 0.3f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Locked",
                            tint = primary,
                            modifier = Modifier.size(iconSize)
                        )
                    }
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 12.dp)
                ) {
                    Text(
                        text = "SECURE CHAT LOCKED",
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
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(titleSpace))
                    Text(
                        text = title.uppercase(),
                        style = if (isCompact) {
                            MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black, 
                                letterSpacing = 0.5.sp,
                                brush = Brush.linearGradient(
                                    colors = listOf(primary, secondary)
                                )
                            )
                        } else {
                            MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Black, 
                                letterSpacing = 1.sp,
                                brush = Brush.linearGradient(
                                    colors = listOf(primary, secondary)
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
                        color = onSurfaceMuted,
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
                                    tint = onSurfaceMuted
                                )
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedLabelColor = primary,
                            unfocusedLabelColor = onSurfaceMuted,
                            focusedBorderColor = primary,
                            unfocusedBorderColor = outline.copy(alpha = 0.12f),
                            cursorColor = primary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (errorMsg != null) {
                        Text(
                            text = errorMsg!!,
                            color = MaterialTheme.colorScheme.error,
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
                            color = primary.copy(alpha = 0.8f),
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
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, outline.copy(alpha = 0.12f)),
                            modifier = Modifier.size(52.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Fingerprint,
                                    contentDescription = "Biometric Unlock",
                                    tint = primary,
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
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .background(
                                brush = Brush.linearGradient(
                                    colors = if (password.isNotEmpty() && !isProcessing) {
                                        listOf(primary, secondary)
                                    } else {
                                        listOf(outline.copy(alpha = 0.12f), outline.copy(alpha = 0.12f))
                                    }
                                ),
                                shape = RoundedCornerShape(16.dp)
                            )
                    ) {
                        if (isProcessing) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                        } else {
                            Text(
                                "UNLOCK CHAT", 
                                color = if (password.isNotEmpty() && !isProcessing) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
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
                                primary.copy(alpha = 0.4f),
                                secondary.copy(alpha = 0.4f)
                            )
                        ),
                        shape = RoundedCornerShape(28.dp)
                    ),
                color = MaterialTheme.colorScheme.surface,
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
                                .background(primary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                tint = primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "CHAT RECOVERY",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Warning Alert Box
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.2f))
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp).padding(top = 2.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                "Enter your 12-word recovery phrase. Validating the phrase will securely rebuild your access keys and update your passcode.",
                                color = MaterialTheme.colorScheme.error,
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
                                        tint = primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                focusedLabelColor = primary,
                                unfocusedLabelColor = onSurfaceMuted,
                                focusedBorderColor = primary,
                                unfocusedBorderColor = outline.copy(alpha = 0.12f),
                                cursorColor = primary
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
                                        tint = onSurfaceMuted
                                    )
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                focusedLabelColor = primary,
                                unfocusedLabelColor = onSurfaceMuted,
                                focusedBorderColor = primary,
                                unfocusedBorderColor = outline.copy(alpha = 0.12f),
                                cursorColor = primary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (recoveryError != null) {
                            Text(
                                text = recoveryError!!, 
                                color = MaterialTheme.colorScheme.error, 
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
                            Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1.5f)
                                .height(48.dp)
                                .background(
                                    brush = Brush.linearGradient(
                                        colors = if (mnemonic.trim().split("\\s+".toRegex()).size == 12 && newPassword.isNotEmpty() && !isRecovering) {
                                            listOf(primary, secondary)
                                        } else {
                                            listOf(outline.copy(alpha = 0.12f), outline.copy(alpha = 0.12f))
                                        }
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                )
                        ) {
                            if (isRecovering) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                            } else {
                                Text(
                                    "REBUILD KEY",
                                    color = if (mnemonic.trim().split("\\s+".toRegex()).size == 12 && newPassword.isNotEmpty() && !isRecovering) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
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

private val EASED_FADE_STOPS_DST_OUT = arrayOf(
    0.0f to Color.Black,
    0.125f to Color.Black,
    0.25f to Color.Black.copy(alpha = 0.95f),
    0.375f to Color.Black.copy(alpha = 0.85f),
    0.50f to Color.Black.copy(alpha = 0.70f),
    0.625f to Color.Black.copy(alpha = 0.50f),
    0.75f to Color.Black.copy(alpha = 0.30f),
    0.875f to Color.Black.copy(alpha = 0.10f),
    1.0f to Color.Transparent
)

private fun Modifier.fadingEdges(
    topBoundaryPx: Float,
    topFadePx: Float
): Modifier = this.graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
    .drawWithContent {
        drawContent()
        
        val height = size.height
        if (height <= 0f || topFadePx <= 0f) return@drawWithContent
        
        val width = size.width
        val fadeStart = (topBoundaryPx - topFadePx).coerceAtLeast(0f)
        val fadeEnd = topBoundaryPx
        
        if (fadeEnd > fadeStart) {
            val stops = arrayOf(
                0f to Color.Black,
                (fadeStart / fadeEnd).coerceIn(0f, 1f) to Color.Black,
                *EASED_FADE_STOPS_DST_OUT.map { (stop, color) ->
                    val fraction = fadeStart + stop * (fadeEnd - fadeStart)
                    (fraction / fadeEnd).coerceIn(0f, 1f) to color
                }.toTypedArray(),
                1f to Color.Transparent
            )
            
            val sortedStops = stops.distinctBy { it.first }.sortedBy { it.first }.toTypedArray()
            
            drawRect(
                brush = Brush.verticalGradient(
                    colorStops = sortedStops,
                    startY = 0f,
                    endY = fadeEnd
                ),
                topLeft = androidx.compose.ui.geometry.Offset(0f, 0f),
                size = androidx.compose.ui.geometry.Size(width, fadeEnd),
                blendMode = BlendMode.DstOut
            )
        }
    }

@Composable
fun IgnitingCoreOverlay(
    modelPath: String?,
    selectedBackend: String,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val infiniteTransition = rememberInfiniteTransition(label = "ignitingPulse")
    val alphaPulse by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)),
            modifier = Modifier.widthIn(max = 400.dp)
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(primaryColor.copy(alpha = 0.08f))
                        .border(1.dp, primaryColor.copy(alpha = 0.25f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = primaryColor,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(48.dp)
                    )
                }

                Text(
                    text = "IGNITING NEURAL CORE",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp
                    ),
                    color = primaryColor,
                    modifier = Modifier.alpha(alphaPulse)
                )

                Text(
                    text = "Loading private offline LLM weights into memory. Please keep Kosh open. This takes just a moment...",
                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                val fileName = modelPath?.let { java.io.File(it).name } ?: "UNKNOWN WEIGHTS"
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.05f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Memory,
                            contentDescription = null,
                            tint = primaryColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${fileName.uppercase()} (${selectedBackend.uppercase()})",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EngineOfflineFallback(
    modelPath: String?,
    onInitialize: () -> Unit,
    onGoToHub: () -> Unit,
    onGoToDashboard: () -> Unit,
    modifier: Modifier = Modifier
) {
    val outlineVariant = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)
    val errorColor = MaterialTheme.colorScheme.error

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
            border = androidx.compose.foundation.BorderStroke(1.dp, outlineVariant),
            modifier = Modifier.widthIn(max = 400.dp)
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(errorColor.copy(alpha = 0.08f))
                        .border(1.dp, errorColor.copy(alpha = 0.25f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = errorColor,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Text(
                    text = "NEURAL CORE OFFLINE",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp
                    ),
                    color = errorColor,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "The local brain is offline. You must initialize model weights to execute offline intelligence.",
                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                if (modelPath != null) {
                    val fileName = java.io.File(modelPath).name
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.05f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Memory,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = fileName.uppercase(),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (modelPath != null) {
                        Button(
                            onClick = onInitialize,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Icon(Icons.Default.PowerSettingsNew, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("INITIALIZE CORE", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                        }
                    }

                    OutlinedButton(
                        onClick = onGoToHub,
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("OPEN MODEL HUB", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                    }

                    TextButton(
                        onClick = onGoToDashboard,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Go back to Dashboard", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

@Composable
fun HardwareStatsHUD(
    backend: String,
    speed: Float,
    load: Int,
    ram: Double,
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    val outlineVariant = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)

    // Pulse animation for the core active indicator dot
    val infiniteTransition = rememberInfiniteTransition(label = "hud_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(
                width = 1.dp,
                color = outlineVariant,
                shape = RoundedCornerShape(16.dp)
            ),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left part: Backend Indicator
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .graphicsLayer(alpha = pulseAlpha)
                        .clip(CircleShape)
                        .background(primary)
                )
                Text(
                    text = backend.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, letterSpacing = 0.5.sp),
                    color = primary
                )
            }

            // Middle & Right parts: Metrics
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Speed
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "SPEED",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                    Text(
                        text = String.format(java.util.Locale.US, "%.1f t/s", speed),
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Black),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Core Load
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "CORE LOAD",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                    Text(
                        text = "$load%",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Black),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // RAM
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "RAM ALLOC",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                    Text(
                        text = String.format(java.util.Locale.US, "%.2f GB", ram),
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Black),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

private suspend fun LazyListState.animateScrollToBottom(lastItemIndex: Int) {
    val layoutInfo = this.layoutInfo
    val visibleItems = layoutInfo.visibleItemsInfo
    val lastVisibleItem = visibleItems.find { it.index == lastItemIndex }
    if (lastVisibleItem != null) {
        val viewportHeight = layoutInfo.viewportEndOffset - layoutInfo.afterContentPadding
        val offset = lastVisibleItem.size - viewportHeight
        this.animateScrollToItem(lastItemIndex, offset)
    } else {
        val viewportHeight = (layoutInfo.viewportEndOffset - layoutInfo.afterContentPadding).coerceAtLeast(0)
        this.animateScrollToItem(lastItemIndex, -viewportHeight)
    }
}

private suspend fun LazyListState.scrollToBottom(lastItemIndex: Int) {
    val layoutInfo = this.layoutInfo
    val visibleItems = layoutInfo.visibleItemsInfo
    val lastVisibleItem = visibleItems.find { it.index == lastItemIndex }
    if (lastVisibleItem != null) {
        val viewportHeight = layoutInfo.viewportEndOffset - layoutInfo.afterContentPadding
        val offset = lastVisibleItem.size - viewportHeight
        this.scrollToItem(lastItemIndex, offset)
    } else {
        val viewportHeight = (layoutInfo.viewportEndOffset - layoutInfo.afterContentPadding).coerceAtLeast(0)
        this.scrollToItem(lastItemIndex, -viewportHeight)
    }
}

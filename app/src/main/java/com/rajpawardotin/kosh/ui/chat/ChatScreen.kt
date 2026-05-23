package com.rajpawardotin.kosh.ui.chat

import android.content.Context
import android.net.Uri
import android.widget.Toast
import android.speech.RecognizerIntent
import android.content.Intent
import android.content.ActivityNotFoundException
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.material3.*
import com.rajpawardotin.kosh.domain.model.ChatSession
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rajpawardotin.kosh.domain.model.ChatMessage
import com.rajpawardotin.kosh.ui.components.ChatBubble
import com.rajpawardotin.kosh.ui.components.ChatInput
import com.rajpawardotin.kosh.ui.components.ModelConfigCard
import com.rajpawardotin.kosh.ui.components.KoshSplashScreen
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
    val scope = rememberCoroutineScope()
    val scrollState = rememberLazyListState()

    val currentSessionId = viewModel.currentSessionId
    val currentSession = viewModel.savedSessions.find { it.id == currentSessionId }
    val isLocked = currentSession != null && currentSession.passwordHash != null && !viewModel.activeSessionKeys.containsKey(currentSession.id)

    var showSplash by remember { mutableStateOf(true) }

    var sessionToLock by remember { mutableStateOf<ChatSession?>(null) }
    var showManageLockDialog by remember { mutableStateOf(false) }
    
    var backupPasswordToExport by remember { mutableStateOf("") }
    var showExportPasswordDialog by remember { mutableStateOf(false) }
    
    var backupPasswordToImport by remember { mutableStateOf("") }
    var importUri by remember { mutableStateOf<Uri?>(null) }
    var showImportPasswordDialog by remember { mutableStateOf(false) }

    LaunchedEffect(currentSessionId) {
        if (currentSessionId != null && currentSession != null) {
            val isSessionLocked = currentSession.passwordHash != null && !viewModel.activeSessionKeys.containsKey(currentSessionId)
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
        kotlinx.coroutines.delay(2500)
        while (viewModel.isInitializing || viewModel.isCopyingModel) {
            kotlinx.coroutines.delay(100)
        }
        showSplash = false
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            uri?.let {
                scope.launch {
                    viewModel.isCopyingModel = true
                    try {
                        val path = copyFileToInternalStorage(context, it)
                        viewModel.setModel(path)
                    } catch (e: Exception) {
                        Toast.makeText(context, "Failed to copy: ${e.message}", Toast.LENGTH_SHORT).show()
                    } finally {
                        viewModel.isCopyingModel = false
                    }
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
            viewModel.setModel(file.absolutePath)
            viewModel.initializeEngine()
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

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0C0C0F))
    ) {
        // Top-left teal radial glow
        Box(
            modifier = Modifier
                .size(450.dp)
                .offset(x = (-120).dp, y = (-120).dp)
                .background(Brush.radialGradient(listOf(Color(0xFF0F766E).copy(alpha = 0.18f), Color.Transparent)))
                .blur(80.dp)
        )
        // Top-right indigo radial glow
        Box(
            modifier = Modifier
                .size(450.dp)
                .align(Alignment.TopEnd)
                .offset(x = 120.dp, y = (-120).dp)
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
                ModalDrawerSheet(
                    drawerContainerColor = Color(0xFF0F0F12),
                    drawerContentColor = Color.White,
                    modifier = Modifier.width(320.dp).fillMaxHeight()
                ) {
                    var sessionToDelete by remember { mutableStateOf<ChatSession?>(null) }
                    var sessionToRename by remember { mutableStateOf<ChatSession?>(null) }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        // Title / Header
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp)
                        ) {
                            Box(modifier = Modifier.size(36.dp)) {
                                com.rajpawardotin.kosh.ui.components.KoshLogo(
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "COGNITIVE VAULTS",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.5.sp,
                                    fontSize = 14.sp
                                ),
                                color = Color.White
                            )
                        }

                        HorizontalDivider(
                            color = Color.White.copy(alpha = 0.1f),
                            thickness = 1.dp,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        // New Brainstorm button
                        Button(
                            onClick = {
                                viewModel.startNewChat()
                                scope.launch { drawerState.close() }
                            },
                            enabled = !viewModel.isGenerating,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White.copy(alpha = 0.05f),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                Brush.linearGradient(listOf(Color(0xFF03DAC5), Color(0xFF6200EE)))
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = Color(0xFF03DAC5),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "New Brainstorm",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Scrollable List of Saved Chats
                        Text(
                            text = "HISTORY",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color.Gray,
                                letterSpacing = 1.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                        )

                        if (viewModel.savedSessions.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No cognitive vaults yet.",
                                    color = Color.Gray,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(viewModel.savedSessions) { session ->
                                    val isActive = viewModel.currentSessionId == session.id
                                    val relativeTime = remember(session.lastActive) {
                                        val now = System.currentTimeMillis()
                                        val diff = now - session.lastActive
                                        when {
                                            diff < 60_000 -> "Just now"
                                            diff < 3600_000 -> "${diff / 60_000}m ago"
                                            diff < 86400_000 -> "${diff / 3600_000}h ago"
                                            else -> "${diff / 86400_000}d ago"
                                        }
                                    }

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(
                                                if (isActive) Color.White.copy(alpha = 0.08f)
                                                else Color.Transparent
                                            )
                                            .clickable(enabled = !viewModel.isGenerating) {
                                                viewModel.loadSession(session.id)
                                                scope.launch { drawerState.close() }
                                            }
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        val isEncrypted = session.passwordHash != null
                                        val isUnlocked = viewModel.activeSessionKeys.containsKey(session.id)
                                        Icon(
                                            imageVector = if (isEncrypted) {
                                                if (isUnlocked) Icons.Default.LockOpen else Icons.Default.Lock
                                            } else {
                                                Icons.AutoMirrored.Filled.Chat
                                            },
                                            contentDescription = null,
                                            tint = if (isActive) Color(0xFF03DAC5) else if (isEncrypted) Color(0xFF03DAC5).copy(alpha = 0.7f) else Color.Gray,
                                            modifier = Modifier.size(18.dp)
                                        )

                                        Spacer(modifier = Modifier.width(10.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = session.title,
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
                                                ),
                                                color = if (isActive) Color.White else Color.White.copy(alpha = 0.7f),
                                                maxLines = 1,
                                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = relativeTime,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.Gray
                                            )
                                        }

                                        IconButton(
                                            onClick = { sessionToRename = session },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "Rename",
                                                tint = Color.Gray,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(4.dp))

                                        IconButton(
                                            onClick = {
                                                if (!isEncrypted) {
                                                    sessionToLock = session
                                                } else if (isUnlocked) {
                                                    viewModel.activeSessionKeys.remove(session.id)
                                                    if (viewModel.currentSessionId == session.id) {
                                                        viewModel.loadSession(session.id)
                                                    }
                                                    Toast.makeText(context, "Vault Locked", Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (isEncrypted) Icons.Default.Lock else Icons.Default.LockOpen,
                                                contentDescription = "Lock Status",
                                                tint = if (isEncrypted) Color(0xFF03DAC5) else Color.Gray,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(4.dp))

                                        IconButton(
                                            onClick = { sessionToDelete = session },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete",
                                                tint = Color.Gray,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Delete Confirmation Dialog
                    if (sessionToDelete != null) {
                        AlertDialog(
                            onDismissRequest = { sessionToDelete = null },
                            containerColor = Color(0xFF1E1E22),
                            titleContentColor = Color.White,
                            textContentColor = Color.White.copy(alpha = 0.8f),
                            title = { Text("Delete Cognitive Vault", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) },
                            text = { Text("Are you sure you want to permanently delete this chat history?") },
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        sessionToDelete?.let { viewModel.deleteSession(it.id) }
                                        sessionToDelete = null
                                    }
                                ) {
                                    Text("Delete", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { sessionToDelete = null }) {
                                    Text("Cancel", color = Color.White.copy(alpha = 0.6f))
                                }
                            }
                        )
                    }

                    // Rename Dialog
                    if (sessionToRename != null) {
                        var newTitle by remember { mutableStateOf(sessionToRename?.title ?: "") }
                        AlertDialog(
                            onDismissRequest = { sessionToRename = null },
                            containerColor = Color(0xFF1E1E22),
                            titleContentColor = Color.White,
                            title = { Text("Rename Vault", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) },
                            text = {
                                OutlinedTextField(
                                    value = newTitle,
                                    onValueChange = { newTitle = it },
                                    label = { Text("Title") },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedLabelColor = Color(0xFF03DAC5),
                                        unfocusedLabelColor = Color.White.copy(alpha = 0.6f),
                                        focusedBorderColor = Color(0xFF03DAC5),
                                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                        cursorColor = Color(0xFF03DAC5)
                                    )
                                )
                            },
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        sessionToRename?.let { viewModel.renameSession(it.id, newTitle) }
                                        sessionToRename = null
                                    }
                                ) {
                                    Text("Save", color = Color(0xFF03DAC5), fontWeight = FontWeight.Bold)
                                }
                              },
                              dismissButton = {
                                  TextButton(onClick = { sessionToRename = null }) {
                                      Text("Cancel", color = Color.White.copy(alpha = 0.6f))
                                  }
                              }
                          )
                      }
                  }
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
            ) {
                // Header (Stays at the very top)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(top = 12.dp, start = 16.dp, end = 16.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Left: 2-line Hamburger Menu
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.05f))
                            .clickable { scope.launch { drawerState.open() } },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            horizontalAlignment = Alignment.Start,
                            modifier = Modifier.width(18.dp)
                        ) {
                            Box(modifier = Modifier.width(18.dp).height(2.dp).background(Color.White))
                            Box(modifier = Modifier.width(12.dp).height(2.dp).background(Color.White))
                        }
                    }

                    // Center: Dropdown Selector
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                            .clickable { showBottomSheet = true }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = if (viewModel.isEngineReady) {
                                viewModel.modelPath?.let { 
                                    File(it).name
                                        .replace(".litertlm", "")
                                        .replace(".bin", "")
                                        .replace("model", "Neural Core")
                                        .uppercase()
                                } ?: "NEURAL CORE"
                            } else {
                                "SELECT CORE"
                            },
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                fontSize = 13.sp
                            ),
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Select Core",
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Chat Lock Management Button
                        if (viewModel.currentSessionId != null && !viewModel.isTemporarySession) {
                            val currentSession = viewModel.savedSessions.find { it.id == viewModel.currentSessionId }
                            if (currentSession != null) {
                                val isEncrypted = currentSession.passwordHash != null
                                val isUnlocked = viewModel.activeSessionKeys.containsKey(currentSession.id)
                                
                                IconButton(
                                    onClick = {
                                        if (!isEncrypted) {
                                            sessionToLock = currentSession
                                        } else if (isUnlocked) {
                                            showManageLockDialog = true
                                        }
                                    },
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isEncrypted) Color(0xFF03DAC5).copy(alpha = 0.15f)
                                            else Color.White.copy(alpha = 0.05f)
                                        )
                                ) {
                                    Icon(
                                        imageVector = if (isEncrypted) Icons.Default.Lock else Icons.Default.LockOpen,
                                        contentDescription = "Vault Lock Settings",
                                        tint = if (isEncrypted) Color(0xFF03DAC5) else Color.White.copy(alpha = 0.8f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        // Temporary Chat Button
                        IconButton(
                            onClick = {
                                viewModel.startNewChat(isTemporary = true)
                            },
                            enabled = !viewModel.isGenerating,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(
                                    if (viewModel.isTemporarySession) Color(0xFFFF9100).copy(alpha = 0.15f)
                                    else Color.White.copy(alpha = 0.05f)
                                )
                                .graphicsLayer(alpha = if (viewModel.isTemporarySession) 1f else 0.6f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.VisibilityOff,
                                contentDescription = "Temporary Chat",
                                tint = if (viewModel.isTemporarySession) Color(0xFFFF9100) else Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // New Saved Chat Button
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.05f))
                                .clickable(enabled = !viewModel.isGenerating) {
                                    viewModel.startNewChat(isTemporary = false)
                                }
                                .drawWithContent {
                                    drawContent()
                                    drawCircle(
                                        brush = Brush.linearGradient(
                                            colors = listOf(Color(0xFF03DAC5), Color(0xFF6200EE))
                                        ),
                                        radius = size.minDimension / 2f,
                                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5.dp.toPx())
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "New Saved Chat",
                                tint = Color(0xFF03DAC5),
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Settings & Core Configuration Button
                        IconButton(
                            onClick = { showBottomSheet = true },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.05f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

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
                            }
                        }
                    }
                }
                


                if (!viewModel.isEngineReady) {
                    NeuralCoreWizard(
                        modelPath = viewModel.modelPath,
                        isInitializing = viewModel.isInitializing,
                        isCopyingModel = viewModel.isCopyingModel,
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
                            title = currentSession.title,
                            hasBiometricKey = currentSession.encryptedKeyBiometric != null,
                            onUnlockWithPassword = { pwd, onDone ->
                                viewModel.unlockSessionWithPassword(currentSession.id, pwd, onDone)
                            },
                            onUnlockWithBiometrics = { onDone ->
                                viewModel.unlockSessionWithBiometrics(currentSession.id, context, onDone)
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
                            contentAlignment = Alignment.Center
                        ) {
                            if (viewModel.chatMessages.isEmpty() && !viewModel.isThinking && !viewModel.isGenerating && viewModel.currentResponseChunk.isEmpty()) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                    modifier = Modifier.padding(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(80.dp)
                                            .graphicsLayer(alpha = 0.95f)
                                            .drawWithContent {
                                                drawContent()
                                                drawRect(
                                                    brush = Brush.linearGradient(
                                                        colors = listOf(
                                                            Color(0xFF8B5CF6), // Violet
                                                            Color(0xFF06B6D4), // Cyan
                                                            Color(0xFFEC4899)  // Pink
                                                        )
                                                    ),
                                                    blendMode = BlendMode.SrcAtop
                                                )
                                            },
                                        tint = Color.Unspecified
                                    )
                                    Spacer(modifier = Modifier.height(24.dp))
                                    Text(
                                        text = if (viewModel.isTemporarySession) "Temporary Vault: History is disabled." else "What should we focus on?",
                                        style = MaterialTheme.typography.headlineMedium.copy(
                                            fontWeight = FontWeight.Medium,
                                            letterSpacing = (-0.5).sp,
                                            fontSize = 26.sp
                                        ),
                                        color = if (viewModel.isTemporarySession) Color(0xFFFF9100).copy(alpha = 0.8f) else Color.White.copy(alpha = 0.9f),
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            } else {
                                LazyColumn(
                                    state = scrollState,
                                    modifier = Modifier.fillMaxSize(),
                                    reverseLayout = true,
                                    contentPadding = PaddingValues(16.dp),
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
                                        ChatBubble(
                                            message = message,
                                            checkedItems = viewModel.checkedItems,
                                            onToggleChecklistItem = { index, checked ->
                                                viewModel.toggleChecklistItem(message.id, index, checked)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (viewModel.isEngineReady && !isLocked) {
                    ChatInput(
                        value = viewModel.prompt,
                        onValueChange = { viewModel.prompt = it },
                        onSend = { viewModel.sendMessage() },
                        onVoiceClick = { startVoiceInput() },
                        isInternetEnabled = viewModel.isInternetEnabled,
                        onInternetToggle = { viewModel.isInternetEnabled = !viewModel.isInternetEnabled },
                        enabled = viewModel.isEngineReady,
                        isGenerating = viewModel.isGenerating,
                        modifier = Modifier
                            .fillMaxWidth()
                            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))
                            .padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
                    )
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
                        onToggleAppLock = { viewModel.toggleAppLock(it) },
                        onExportBackup = { showExportPasswordDialog = true },
                        onImportBackup = { importBackupLauncher.launch(arrayOf("*/*")) },
                        onTavilyApiKeyChange = { viewModel.updateTavilyApiKey(it) },
                        onBraveApiKeyChange = { viewModel.updateBraveApiKey(it) },
                        onPickModel = { filePickerLauncher.launch(arrayOf("*/*")) },
                        onDeleteModel = { viewModel.deleteModel() },
                        onSelectBackend = { viewModel.selectBackend(it) },
                        onSelectSearchEngine = { viewModel.selectSearchEngine(it) },
                        onStartEngine = { viewModel.initializeEngine() },
                        onToggleInternet = { viewModel.isInternetEnabled = it }
                    )
                }
            }
        }

        // App Lock Overlay
        AnimatedVisibility(
            visible = viewModel.isAppLocked,
            enter = fadeIn(animationSpec = tween(400)),
            exit = fadeOut(animationSpec = tween(400))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF070709).copy(alpha = 0.96f))
                    .clickable(enabled = true) {},
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(300.dp)
                        .background(Brush.radialGradient(listOf(Color(0xFF03DAC5).copy(alpha = 0.15f), Color.Transparent)))
                        .blur(50.dp)
                )
                
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = "App Secured",
                        tint = Color(0xFF03DAC5),
                        modifier = Modifier.size(72.dp)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "KOSH SECURE VAULT",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp,
                            fontSize = 20.sp
                        ),
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Authentication required to access cognitive vaults",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(48.dp))
                    
                    Button(
                        onClick = { triggerAppBiometricUnlock(context, viewModel) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF03DAC5),
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .height(52.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fingerprint,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "UNLOCK VAULT",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }

        // Lock Chat Dialog
        if (sessionToLock != null) {
            var password by remember { mutableStateOf("") }
            var confirmPassword by remember { mutableStateOf("") }
            var enableBiometric by remember { mutableStateOf(false) }
            var passwordVisibility by remember { mutableStateOf(false) }
            var confirmPasswordVisibility by remember { mutableStateOf(false) }
            var isProcessing by remember { mutableStateOf(false) }

            AlertDialog(
                onDismissRequest = { if (!isProcessing) sessionToLock = null },
                containerColor = Color(0xFF1E1E22),
                titleContentColor = Color.White,
                textContentColor = Color.White.copy(alpha = 0.8f),
                title = { Text("Lock Cognitive Vault", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Set a custom password to encrypt this vault. Messages will be encrypted using AES-256-GCM.", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                        
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Password") },
                            singleLine = true,
                            enabled = !isProcessing,
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
                                unfocusedLabelColor = Color.White.copy(alpha = 0.6f),
                                focusedBorderColor = Color(0xFF03DAC5),
                                unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                cursorColor = Color(0xFF03DAC5)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = confirmPassword,
                            onValueChange = { confirmPassword = it },
                            label = { Text("Confirm Password") },
                            singleLine = true,
                            enabled = !isProcessing,
                            visualTransformation = if (confirmPasswordVisibility) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { confirmPasswordVisibility = !confirmPasswordVisibility }) {
                                    Icon(
                                        imageVector = if (confirmPasswordVisibility) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = null,
                                        tint = Color.Gray
                                    )
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedLabelColor = Color(0xFF03DAC5),
                                unfocusedLabelColor = Color.White.copy(alpha = 0.6f),
                                focusedBorderColor = Color(0xFF03DAC5),
                                unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                cursorColor = Color(0xFF03DAC5)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().clickable(enabled = !isProcessing) { enableBiometric = !enableBiometric }
                        ) {
                            Checkbox(
                                checked = enableBiometric,
                                onCheckedChange = { enableBiometric = it },
                                enabled = !isProcessing,
                                colors = CheckboxDefaults.colors(
                                    checkedColor = Color(0xFF03DAC5),
                                    checkmarkColor = Color.Black,
                                    uncheckedColor = Color.White.copy(alpha = 0.4f)
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Enable Fingerprint Unlock", color = Color.White)
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        enabled = password.isNotEmpty() && password == confirmPassword && !isProcessing,
                        onClick = {
                            isProcessing = true
                            viewModel.lockSession(sessionToLock!!.id, password, enableBiometric, context) { success ->
                                isProcessing = false
                                if (success) {
                                    Toast.makeText(context, "Vault Locked Successfully", Toast.LENGTH_SHORT).show()
                                    sessionToLock = null
                                } else {
                                    Toast.makeText(context, "Locking failed", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    ) {
                        if (isProcessing) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color(0xFF03DAC5), strokeWidth = 2.dp)
                        } else {
                            Text("Lock", color = Color(0xFF03DAC5), fontWeight = FontWeight.Bold)
                        }
                    }
                },
                dismissButton = {
                    TextButton(
                        enabled = !isProcessing,
                        onClick = { sessionToLock = null }
                    ) {
                        Text("Cancel", color = Color.White.copy(alpha = 0.6f))
                    }
                }
            )
        }


        // Export Password Dialog
        if (showExportPasswordDialog) {
            var password by remember { mutableStateOf("") }
            var passwordVisibility by remember { mutableStateOf(false) }

            AlertDialog(
                onDismissRequest = { showExportPasswordDialog = false },
                containerColor = Color(0xFF1E1E22),
                titleContentColor = Color.White,
                title = { Text("Backup Password", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Set a password to encrypt your cognitive vault backup file.", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Password") },
                            singleLine = true,
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
                                unfocusedLabelColor = Color.White.copy(alpha = 0.6f),
                                focusedBorderColor = Color(0xFF03DAC5),
                                unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                cursorColor = Color(0xFF03DAC5)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        enabled = password.isNotEmpty(),
                        onClick = {
                            backupPasswordToExport = password
                            showExportPasswordDialog = false
                            exportBackupLauncher.launch("kosh_vault_backup.kosh")
                        }
                    ) {
                        Text("Export", color = Color(0xFF03DAC5), fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showExportPasswordDialog = false }) {
                        Text("Cancel", color = Color.White.copy(alpha = 0.6f))
                    }
                }
            )
        }

        // Import Password Dialog
        if (showImportPasswordDialog) {
            var password by remember { mutableStateOf("") }
            var passwordVisibility by remember { mutableStateOf(false) }
            var isProcessing by remember { mutableStateOf(false) }

            AlertDialog(
                onDismissRequest = { if (!isProcessing) showImportPasswordDialog = false },
                containerColor = Color(0xFF1E1E22),
                titleContentColor = Color.White,
                title = { Text("Restore Password", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Enter the password that was used to encrypt this backup file.", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Password") },
                            singleLine = true,
                            enabled = !isProcessing,
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
                                unfocusedLabelColor = Color.White.copy(alpha = 0.6f),
                                focusedBorderColor = Color(0xFF03DAC5),
                                unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                cursorColor = Color(0xFF03DAC5)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        enabled = password.isNotEmpty() && !isProcessing,
                        onClick = {
                            isProcessing = true
                            viewModel.importBackup(context, importUri!!, password,
                                onSuccess = {
                                    isProcessing = false
                                    Toast.makeText(context, "Backup restored successfully!", Toast.LENGTH_SHORT).show()
                                    showImportPasswordDialog = false
                                    importUri = null
                                },
                                onError = { err ->
                                    isProcessing = false
                                    Toast.makeText(context, "Restore failed: $err", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    ) {
                        if (isProcessing) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color(0xFF03DAC5), strokeWidth = 2.dp)
                        } else {
                            Text("Restore", color = Color(0xFF03DAC5), fontWeight = FontWeight.Bold)
                        }
                    }
                },
                dismissButton = {
                    TextButton(
                        enabled = !isProcessing,
                        onClick = { showImportPasswordDialog = false }
                    ) {
                        Text("Cancel", color = Color.White.copy(alpha = 0.6f))
                    }
                }
            )
        }

        // Manage Vault Lock Dialog (for unlocked encrypted chats)
        if (showManageLockDialog && viewModel.currentSessionId != null) {
            val sessionId = viewModel.currentSessionId!!
            val session = viewModel.savedSessions.find { it.id == sessionId }
            var isProcessing by remember { mutableStateOf(false) }

            if (session != null) {
                AlertDialog(
                    onDismissRequest = { if (!isProcessing) showManageLockDialog = false },
                    containerColor = Color(0xFF1E1E22),
                    titleContentColor = Color.White,
                    textContentColor = Color.White.copy(alpha = 0.8f),
                    title = { Text("Vault Lock Settings", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) },
                    text = {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text("Manage encryption options for this active conversation vault.", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                            
                            // Option 1: Lock Now
                            Button(
                                onClick = {
                                    viewModel.activeSessionKeys.remove(sessionId)
                                    viewModel.loadSession(sessionId)
                                    showManageLockDialog = false
                                    Toast.makeText(context, "Vault Locked", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White.copy(alpha = 0.05f),
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                            ) {
                                Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color(0xFF03DAC5))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Lock Vault Now", fontWeight = FontWeight.Bold)
                            }
                            
                            // Option 2: Remove Password
                            Button(
                                onClick = {
                                    isProcessing = true
                                    viewModel.removeSessionLock(sessionId) { success ->
                                        isProcessing = false
                                        if (success) {
                                            Toast.makeText(context, "Password Lock Removed", Toast.LENGTH_SHORT).show()
                                            showManageLockDialog = false
                                        } else {
                                            Toast.makeText(context, "Failed to remove lock", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                enabled = !isProcessing,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFEF4444).copy(alpha = 0.15f),
                                    contentColor = Color(0xFFEF4444)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                            ) {
                                if (isProcessing) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color(0xFFEF4444), strokeWidth = 2.dp)
                                } else {
                                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color(0xFFEF4444))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Remove Password & Decrypt", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(
                            enabled = !isProcessing,
                            onClick = { showManageLockDialog = false }
                        ) {
                            Text("Close", color = Color.White.copy(alpha = 0.6f))
                        }
                    }
                )
            }
        }

        // Splash Screen Overlay
        AnimatedVisibility(
            visible = showSplash,
            enter = fadeIn(),
            exit = fadeOut(animationSpec = tween(600))
        ) {
            KoshSplashScreen(
                isInitializing = viewModel.isInitializing,
                isCopyingModel = viewModel.isCopyingModel,
                modifier = Modifier.fillMaxSize()
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
    modifier: Modifier = Modifier
) {
    var password by remember { mutableStateOf("") }
    var passwordVisibility by remember { mutableStateOf(false) }
    var isProcessing by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0C0C0F)),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(320.dp)
                .background(Brush.radialGradient(listOf(Color(0xFF03DAC5).copy(alpha = 0.1f), Color.Transparent)))
                .blur(60.dp)
        )

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF1E1E24).copy(alpha = 0.85f),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF03DAC5).copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = Color(0xFF03DAC5),
                        modifier = Modifier.size(28.dp)
                    )
                }

                Text(
                    text = "Vault Sealed",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp),
                    color = Color.White
                )

                Text(
                    text = "Conversation \"$title\" is secured with AES-256 encryption. Provide your passcode to access.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { 
                        password = it
                        errorMsg = null
                    },
                    label = { Text("Passcode") },
                    singleLine = true,
                    enabled = !isProcessing,
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
                        unfocusedLabelColor = Color.White.copy(alpha = 0.6f),
                        focusedBorderColor = Color(0xFF03DAC5),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                        cursorColor = Color(0xFF03DAC5)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMsg != null) {
                    Text(
                        text = errorMsg!!,
                        color = Color(0xFFCF6679),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.align(Alignment.Start)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (hasBiometricKey) {
                        IconButton(
                            onClick = {
                                onUnlockWithBiometrics { success ->
                                    if (!success) {
                                        Toast.makeText(context, "Biometrics verification failed", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            modifier = Modifier
                                .size(50.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFF03DAC5).copy(alpha = 0.1f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Fingerprint,
                                contentDescription = "Biometric Unlock",
                                tint = Color(0xFF03DAC5)
                            )
                        }
                    }

                    Button(
                        onClick = {
                            isProcessing = true
                            onUnlockWithPassword(password) { success ->
                                isProcessing = false
                                if (!success) {
                                    errorMsg = "Incorrect passcode"
                                }
                            }
                        },
                        enabled = password.isNotEmpty() && !isProcessing,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF03DAC5),
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                    ) {
                        if (isProcessing) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.Black, strokeWidth = 2.dp)
                        } else {
                            Text("Unlock Vault", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}




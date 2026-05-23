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
import androidx.compose.material3.*
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
    var showBottomSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberLazyListState()

    var showSplash by remember { mutableStateOf(true) }

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
                            .clickable { showBottomSheet = true },
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

                    // Right: New Chat / Sparkle button
                    IconButton(
                        onClick = {
                            viewModel.chatMessages.clear()
                            viewModel.checkedItems.clear()
                            viewModel.prompt = ""
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.05f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "New Chat",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
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
                                    text = "What should we focus on?",
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontWeight = FontWeight.Medium,
                                        letterSpacing = (-0.5).sp,
                                        fontSize = 26.sp
                                    ),
                                    color = Color.White.copy(alpha = 0.9f),
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
                                            viewModel.toggleChecklistItem(message.hashCode().toString(), index, checked)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                if (viewModel.isEngineReady) {
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




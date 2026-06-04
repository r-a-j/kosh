package com.rajpawardotin.kosh.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rajpawardotin.kosh.ui.chat.ChatViewModel
import com.rajpawardotin.kosh.ui.chat.AppScreen
import java.io.File
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: ChatViewModel,
    onPickModel: () -> Unit,
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    
    val primary = MaterialTheme.colorScheme.primary
    val outlineVariant = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)
    val cardColors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))

    // Local dialog state for tags
    var newTagName by remember { mutableStateOf("") }
    val colorOptions = listOf(
        "#EF4444", "#F97316", "#F59E0B", "#84CC16", "#10B981", 
        "#14B8A6", "#06B6D4", "#0EA5E9", "#3B82F6", "#6366F1", 
        "#8B5CF6", "#D946EF", "#EC4899", "#F43F5E", "#C2410C", 
        "#4D7C0F", "#15803D", "#1D4ED8", "#6B21A8", "#475569"
    )
    var selectedColor by remember { mutableStateOf(colorOptions[9]) } // Default indigo
    var tagWarningDialog by remember { mutableStateOf<com.rajpawardotin.kosh.ui.chat.dialogs.TagWarningInfo?>(null) }
    var editingTag by remember { mutableStateOf<com.rajpawardotin.kosh.domain.model.ChatTag?>(null) }
    var editTagName by remember { mutableStateOf("") }
    var editTagColor by remember { mutableStateOf("") }



    // Local warning dialog
    if (tagWarningDialog != null) {
        AlertDialog(
            onDismissRequest = { tagWarningDialog = null },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Warning", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) },
            text = { Text(tagWarningDialog!!.message) },
            confirmButton = {
                TextButton(
                    onClick = {
                        tagWarningDialog!!.onConfirm()
                        tagWarningDialog = null
                    }
                ) {
                    Text("Confirm", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { tagWarningDialog = null }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }

    // Inline Rename Tag Dialog
    if (editingTag != null) {
        AlertDialog(
            onDismissRequest = { editingTag = null },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Edit Tag", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = editTagName,
                        onValueChange = { editTagName = it },
                        label = { Text("Tag Name") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "LIVE PREVIEW",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        val editColorParsed = try { Color(android.graphics.Color.parseColor(editTagColor)) } catch (e: Exception) { Color.Gray }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = editColorParsed.copy(alpha = 0.12f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, editColorParsed.copy(alpha = 0.3f))
                        ) {
                            Text(
                                text = if (editTagName.isBlank()) "Preview" else editTagName,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = editColorParsed,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        colorOptions.forEach { hex ->
                            val color = Color(android.graphics.Color.parseColor(hex))
                            val isSelected = editTagColor == hex
                            val scale by animateFloatAsState(targetValue = if (isSelected) 1.2f else 1.0f, label = "scale")
                            Box(
                                modifier = Modifier
                                    .padding(4.dp)
                                    .size(28.dp)
                                    .graphicsLayer(scaleX = scale, scaleY = scale)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(
                                        width = if (isSelected) 2.5.dp else 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f),
                                        shape = CircleShape
                                    )
                                    .clickable { editTagColor = hex },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(Color.White)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val targetTag = editingTag!!
                        viewModel.updateTag(targetTag.name, editTagName, editTagColor) { count, proceed ->
                            tagWarningDialog = com.rajpawardotin.kosh.ui.chat.dialogs.TagWarningInfo(
                                title = "Associated Chats Warning",
                                message = "This tag is associated with $count chats. Renaming it will update all associated sessions. Are you sure you want to proceed?",
                                onConfirm = proceed
                            )
                        }
                        editingTag = null
                    }
                ) {
                    Text("Save", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { editingTag = null }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Sleek Background Ambient Gradients
        Box(
            modifier = Modifier
                .size(450.dp)
                .offset(x = (-120).dp, y = (-120).dp)
                .background(Brush.radialGradient(
                    0.0f to primary.copy(alpha = 0.12f),
                    0.5f to primary.copy(alpha = 0.05f),
                    1.0f to Color.Transparent
                ))
        )
        Box(
            modifier = Modifier
                .size(450.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 120.dp, y = 120.dp)
                .background(Brush.radialGradient(
                    0.0f to MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
                    0.5f to MaterialTheme.colorScheme.secondary.copy(alpha = 0.06f),
                    1.0f to Color.Transparent
                ))
        )

        Column(modifier = Modifier.fillMaxSize()) {
            // Header Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .height(64.dp)
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                        .border(1.dp, outlineVariant, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black, fontSize = 22.sp),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
            }



            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding()
                    .navigationBarsPadding(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {


                // CATEGORY 1: Theme & Appearance
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = cardColors,
                        border = androidx.compose.foundation.BorderStroke(1.dp, outlineVariant)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            // Section Header
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Palette, contentDescription = null, tint = primary, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "THEME & APPEARANCE",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black, letterSpacing = 1.sp),
                                    color = primary
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))

                            val themeOptions = listOf(
                                "SYSTEM" to "System",
                                "OLED_OBSIDIAN" to "Obsidian",
                                "MINIMALIST_SAND" to "Sand",
                                "AERO_GLASS" to "Aero"
                            )
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                themeOptions.forEach { (themeKey, label) ->
                                    val isSelected = viewModel.appTheme == themeKey
                                    val borderBrush = if (isSelected) {
                                        Brush.linearGradient(colors = listOf(primary, MaterialTheme.colorScheme.secondary))
                                    } else {
                                        Brush.linearGradient(colors = listOf(outlineVariant, outlineVariant))
                                    }
                                    
                                    Surface(
                                        onClick = { viewModel.updateAppTheme(themeKey) },
                                        shape = RoundedCornerShape(14.dp),
                                        color = if (isSelected) primary.copy(alpha = 0.10f) else Color.Transparent,
                                        border = androidx.compose.foundation.BorderStroke(1.dp, borderBrush),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                val previewColors = when (themeKey) {
                                                    "OLED_OBSIDIAN" -> listOf(Color(0xFF000000), Color(0xFF6366F1), Color(0xFF84CC16))
                                                    "MINIMALIST_SAND" -> listOf(Color(0xFFFAF9F6), Color(0xFFC2410C), Color(0xFF4F46E5))
                                                    "AERO_GLASS" -> listOf(Color(0xFF0B0F19), Color(0xFF14B8A6), Color(0xFF38BDF8))
                                                    else -> listOf(Color.Gray, Color.DarkGray, Color.LightGray)
                                                }
                                                previewColors.forEach { color ->
                                                    Box(
                                                        modifier = Modifier
                                                            .size(6.dp)
                                                            .clip(CircleShape)
                                                            .background(color)
                                                            .border(0.5.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f), CircleShape)
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = label,
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.5.sp),
                                                color = if (isSelected) primary else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // CATEGORY 2: Neural Core Configuration
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = cardColors,
                        border = androidx.compose.foundation.BorderStroke(1.dp, outlineVariant)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            // Section Header
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Memory, contentDescription = null, tint = primary, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "NEURAL CORE",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black, letterSpacing = 1.sp),
                                    color = primary,
                                    modifier = Modifier.weight(1f)
                                )
                                // Link to Hub
                                TextButton(
                                    onClick = { viewModel.currentScreen = AppScreen.MODEL_HUB },
                                    colors = ButtonDefaults.textButtonColors(contentColor = primary),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Icon(Icons.Default.Launch, contentDescription = null, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Model Hub", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))

                            // Model Status Card
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(if (viewModel.modelPath == null) primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.error.copy(alpha = 0.12f))
                                        .clickable { if (viewModel.modelPath == null) onPickModel() else viewModel.deleteModel() },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (viewModel.modelPath == null) Icons.Default.Add else Icons.Default.Delete,
                                        contentDescription = null,
                                        tint = if (viewModel.modelPath == null) primary else MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (viewModel.modelPath == null) "Import Local Model..." else File(viewModel.modelPath!!).name,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = when {
                                            viewModel.isEngineReady -> "Neural Core Online"
                                            viewModel.modelPath != null -> "Standby • Ready for Init"
                                            else -> "No active weights loaded"
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (viewModel.isEngineReady) primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Performance HUD Switch Row
                             Row(
                                 modifier = Modifier
                                     .fillMaxWidth()
                                     .clickable { viewModel.updateShowHardwareStats(!viewModel.showHardwareStats) }
                                     .padding(vertical = 4.dp),
                                 horizontalArrangement = Arrangement.SpaceBetween,
                                 verticalAlignment = Alignment.CenterVertically
                             ) {
                                 Column(modifier = Modifier.weight(1f)) {
                                     Text(
                                         text = "Performance HUD",
                                         style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                         color = MaterialTheme.colorScheme.onSurface
                                     )
                                     Text(
                                         text = "Show generation speed (t/s), memory usage, and core load during chat inference",
                                         style = MaterialTheme.typography.labelSmall,
                                         color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                     )
                                 }
                                 Spacer(modifier = Modifier.width(16.dp))
                                 KoshSwitch(
                                     checked = viewModel.showHardwareStats,
                                     onCheckedChange = { viewModel.updateShowHardwareStats(it) }
                                 )
                             }

                            // Initializer Section
                            AnimatedVisibility(
                                visible = viewModel.modelPath != null && !viewModel.isEngineReady,
                                enter = fadeIn() + expandVertically(),
                                exit = fadeOut() + shrinkVertically()
                            ) {
                                Column {
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "Hardware Backend Accelerator",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    
                                    Row(
                                        Modifier.fillMaxWidth().selectableGroup(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        viewModel.backends.forEach { text ->
                                            val selected = text == viewModel.selectedBackend
                                            val activeColor = when {
                                                text.contains("NPU") -> primary
                                                text == "GPU" -> MaterialTheme.colorScheme.secondary
                                                else -> MaterialTheme.colorScheme.tertiary
                                            }
                                            Surface(
                                                modifier = Modifier.weight(1f),
                                                shape = RoundedCornerShape(12.dp),
                                                color = if (selected) activeColor.copy(alpha = 0.12f) else Color.Transparent,
                                                border = androidx.compose.foundation.BorderStroke(
                                                    1.dp,
                                                    if (selected) activeColor.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                                ),
                                                onClick = { viewModel.selectBackend(text) }
                                            ) {
                                                Box(modifier = Modifier.padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                                                    Text(
                                                        text = text.split(" ")[0],
                                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal),
                                                        color = if (selected) activeColor else MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))

                                    Button(
                                        onClick = { viewModel.triggerManualInitialization() },
                                        enabled = !viewModel.isInitializing,
                                        modifier = Modifier.fillMaxWidth().height(46.dp),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = primary)
                                    ) {
                                        if (viewModel.isInitializing) {
                                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Igniting Core...", fontWeight = FontWeight.Bold)
                                        } else {
                                            Text("INITIALIZE CORES", fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // CATEGORY 3: Search & Intelligence (RAG)
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = cardColors,
                        border = androidx.compose.foundation.BorderStroke(1.dp, outlineVariant)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            // Section Header
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Language, contentDescription = null, tint = primary, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "SEARCH & INTELLIGENCE",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black, letterSpacing = 1.sp),
                                    color = primary
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))

                            // Web Search Switch Row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.isInternetEnabled = !viewModel.isInternetEnabled }
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Web Search Integration",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Let Kosh search the live web for real-time temporal queries",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                KoshSwitch(
                                    checked = viewModel.isInternetEnabled,
                                    onCheckedChange = { viewModel.isInternetEnabled = it }
                                )
                            }

                            // Dynamic inputs for Web Search APIs
                            AnimatedVisibility(
                                visible = viewModel.isInternetEnabled,
                                enter = fadeIn() + expandVertically(),
                                exit = fadeOut() + shrinkVertically()
                            ) {
                                Column {
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "Active Web Search Provider",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))

                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        viewModel.searchEngines.chunked(2).forEach { rowEngines ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                rowEngines.forEach { engine ->
                                                    val selected = engine == viewModel.selectedSearchEngine
                                                    Surface(
                                                        modifier = Modifier.weight(1f),
                                                        shape = RoundedCornerShape(12.dp),
                                                        color = if (selected) primary.copy(alpha = 0.12f) else Color.Transparent,
                                                        border = androidx.compose.foundation.BorderStroke(
                                                            1.dp,
                                                            if (selected) primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                                        ),
                                                        onClick = { viewModel.selectSearchEngine(engine) }
                                                    ) {
                                                        Box(
                                                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Text(
                                                                text = engine,
                                                                style = MaterialTheme.typography.labelMedium,
                                                                color = if (selected) primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    // Tavily API Input
                                    AnimatedVisibility(
                                        visible = viewModel.selectedSearchEngine == "Tavily API",
                                        enter = fadeIn() + expandVertically(),
                                        exit = fadeOut() + shrinkVertically()
                                    ) {
                                        Column {
                                            Spacer(modifier = Modifier.height(14.dp))
                                            var showKey by remember { mutableStateOf(false) }
                                            OutlinedTextField(
                                                value = viewModel.tavilyApiKey,
                                                onValueChange = { viewModel.updateTavilyApiKey(it) },
                                                label = { Text("Tavily API Key", style = MaterialTheme.typography.labelMedium) },
                                                placeholder = { Text("tvly-...", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)) },
                                                modifier = Modifier.fillMaxWidth(),
                                                singleLine = true,
                                                visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                                                trailingIcon = {
                                                    IconButton(onClick = { showKey = !showKey }) {
                                                        Icon(
                                                            imageVector = if (showKey) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                                            contentDescription = "Toggle Visibility",
                                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                },
                                                shape = RoundedCornerShape(14.dp),
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedBorderColor = primary,
                                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                                )
                                            )
                                        }
                                    }

                                    // Brave Search API Input
                                    AnimatedVisibility(
                                        visible = viewModel.selectedSearchEngine == "Brave Search API",
                                        enter = fadeIn() + expandVertically(),
                                        exit = fadeOut() + shrinkVertically()
                                    ) {
                                        Column {
                                            Spacer(modifier = Modifier.height(14.dp))
                                            var showKey by remember { mutableStateOf(false) }
                                            OutlinedTextField(
                                                value = viewModel.braveApiKey,
                                                onValueChange = { viewModel.updateBraveApiKey(it) },
                                                label = { Text("Brave Search API Key", style = MaterialTheme.typography.labelMedium) },
                                                placeholder = { Text("BS-...", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)) },
                                                modifier = Modifier.fillMaxWidth(),
                                                singleLine = true,
                                                visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                                                trailingIcon = {
                                                    IconButton(onClick = { showKey = !showKey }) {
                                                        Icon(
                                                            imageVector = if (showKey) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                                            contentDescription = "Toggle Visibility",
                                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                },
                                                shape = RoundedCornerShape(14.dp),
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedBorderColor = primary,
                                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // CATEGORY 4: Security & Privacy
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = cardColors,
                        border = androidx.compose.foundation.BorderStroke(1.dp, outlineVariant)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            // Section Header
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Security, contentDescription = null, tint = primary, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "SECURITY & DATA",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black, letterSpacing = 1.sp),
                                    color = primary
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))

                            // Biometrics Startup Lock
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.toggleAppLock(!viewModel.isAppLockEnabled) }
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "App Lock on Startup",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Lock Kosh with system biometrics/passcode on startup",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                KoshSwitch(
                                    checked = viewModel.isAppLockEnabled,
                                    onCheckedChange = { viewModel.toggleAppLock(it) }
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Allow Screen Capture
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.toggleScreenshot(!viewModel.isScreenshotEnabled) }
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Allow Screenshots",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Allow system screenshot capture in chat interfaces",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                KoshSwitch(
                                    checked = viewModel.isScreenshotEnabled,
                                    onCheckedChange = { enabled ->
                                        if (!enabled) {
                                            viewModel.toggleScreenshot(false)
                                        } else {
                                            // Handle biometric or passcode confirmation in settings screen
                                            viewModel.toggleScreenshot(true) // Simpler callback route for inline settings screen
                                        }
                                    }
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Start App with New Chat Preference
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.updateStartWithNewChat(!viewModel.startWithNewChat) }
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Start App with New Chat",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Open directly to a blank chat instead of the dashboard",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                KoshSwitch(
                                    checked = viewModel.startWithNewChat,
                                    onCheckedChange = { viewModel.updateStartWithNewChat(it) }
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            HorizontalDivider(color = outlineVariant)
                            Spacer(modifier = Modifier.height(16.dp))

                            // Backups Actions
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = onExportBackup,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                                    contentPadding = PaddingValues(vertical = 12.dp)
                                ) {
                                    Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Export Vault", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                                }

                                OutlinedButton(
                                    onClick = onImportBackup,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                                    contentPadding = PaddingValues(vertical = 12.dp)
                                ) {
                                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Import Vault", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                                }
                            }
                        }
                    }
                }

                // CATEGORY 5: Tags Management
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = cardColors,
                        border = androidx.compose.foundation.BorderStroke(1.dp, outlineVariant)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            // Section Header
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Label, contentDescription = null, tint = primary, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "TAGS MANAGEMENT",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black, letterSpacing = 1.sp),
                                    color = primary
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))

                            // Create tag heading
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "CREATE NEW TAG",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                val selectedColorParsed = remember(selectedColor) {
                                    try { Color(android.graphics.Color.parseColor(selectedColor)) } catch (e: Exception) { Color.Gray }
                                }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = selectedColorParsed.copy(alpha = 0.12f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, selectedColorParsed.copy(alpha = 0.3f))
                                ) {
                                    Text(
                                        text = if (newTagName.isBlank()) "Preview" else newTagName,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = selectedColorParsed,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Text Input Field
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                val selectedColorParsed = remember(selectedColor) {
                                    try { Color(android.graphics.Color.parseColor(selectedColor)) } catch (e: Exception) { Color.Gray }
                                }
                                OutlinedTextField(
                                    value = newTagName,
                                    onValueChange = { newTagName = it },
                                    placeholder = { Text("New tag name...") },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Label,
                                            contentDescription = null,
                                            tint = selectedColorParsed,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    },
                                    shape = RoundedCornerShape(14.dp),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                        focusedBorderColor = primary,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                                        focusedContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.01f),
                                        unfocusedContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.01f)
                                    ),
                                    modifier = Modifier.weight(1f)
                                )

                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(if (newTagName.isNotBlank()) selectedColorParsed else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
                                        .clickable(enabled = newTagName.isNotBlank()) {
                                            viewModel.createTag(newTagName, selectedColor)
                                            newTagName = ""
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Create Tag",
                                        tint = if (newTagName.isNotBlank()) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Horizontal scroll color picker
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                colorOptions.forEach { hex ->
                                    val color = remember(hex) { Color(android.graphics.Color.parseColor(hex)) }
                                    val isSelected = selectedColor == hex
                                    val scale by animateFloatAsState(targetValue = if (isSelected) 1.2f else 1.0f, label = "scale")

                                    Box(
                                        modifier = Modifier
                                            .padding(4.dp)
                                            .size(28.dp)
                                            .graphicsLayer(scaleX = scale, scaleY = scale)
                                            .clip(CircleShape)
                                            .background(color)
                                            .border(
                                                width = if (isSelected) 2.5.dp else 1.dp,
                                                color = if (isSelected) primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f),
                                                shape = CircleShape
                                            )
                                            .clickable { selectedColor = hex },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isSelected) {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .clip(CircleShape)
                                                    .background(Color.White)
                                            )
                                        }
                                    }
                                }
                            }

                            // Existing tag list
                            if (viewModel.allTags.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(16.dp))
                                HorizontalDivider(color = outlineVariant)
                                Spacer(modifier = Modifier.height(12.dp))

                                Column(
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    viewModel.allTags.forEach { tag ->
                                        val tagColor = try { Color(android.graphics.Color.parseColor(tag.colorHex)) } catch (e: Exception) { Color.Gray }
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = tagColor.copy(alpha = 0.12f),
                                                border = androidx.compose.foundation.BorderStroke(1.dp, tagColor.copy(alpha = 0.3f))
                                            ) {
                                                Text(
                                                    text = tag.name,
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = tagColor,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }

                                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                IconButton(
                                                    onClick = {
                                                        editingTag = tag
                                                        editTagName = tag.name
                                                        editTagColor = tag.colorHex
                                                    },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Edit,
                                                        contentDescription = "Edit Tag",
                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                                IconButton(
                                                    onClick = {
                                                        viewModel.deleteTag(tag.name) { count, proceed ->
                                                            tagWarningDialog = com.rajpawardotin.kosh.ui.chat.dialogs.TagWarningInfo(
                                                                title = "Delete Tag confirmation",
                                                                message = "This tag is associated with $count chats. Deleting it will disassociate it from all of them. Are you sure you want to delete it?",
                                                                onConfirm = proceed
                                                            )
                                                        }
                                                    },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Delete,
                                                        contentDescription = "Delete Tag",
                                                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                                                        modifier = Modifier.size(16.dp)
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
            }
        }
    }
}

@Composable
fun KoshSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    val activeTrackColor = primary
    val inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    val inactiveBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)

    val trackColor by animateColorAsState(
        targetValue = if (checked) activeTrackColor else inactiveTrackColor,
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
        label = "trackColor"
    )

    // Smooth bouncy spring offset
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) 22.dp else 2.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "thumbOffset"
    )

    // Scale animation to make it feel tactile when clicked
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "scale"
    )

    Box(
        modifier = modifier
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .size(width = 46.dp, height = 26.dp)
            .clip(CircleShape)
            .background(trackColor)
            .border(
                width = if (checked) 0.dp else 1.dp,
                color = if (checked) Color.Transparent else inactiveBorderColor,
                shape = CircleShape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = { onCheckedChange(!checked) }
            ),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .size(22.dp)
                .padding(2.dp)
                .clip(CircleShape)
                .background(Color.White)
        )
    }
}

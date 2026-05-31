package com.rajpawardotin.kosh.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff

@Composable
fun ModelConfigCard(
    modelPath: String?,
    isInitializing: Boolean,
    isCopyingModel: Boolean,
    isEngineReady: Boolean,
    selectedBackend: String,
    backends: List<String>,
    isInternetEnabled: Boolean,
    tokensPerSecond: Float,
    npuLoad: Int,
    ramUsage: Double,
    selectedSearchEngine: String,
    searchEngines: List<String>,
    tavilyApiKey: String = "",
    braveApiKey: String = "",
    isAppLockEnabled: Boolean = false,
    isScreenshotEnabled: Boolean = false,
    currentTheme: String = "SYSTEM",
    onThemeSelected: (String) -> Unit = {},
    onToggleAppLock: (Boolean) -> Unit = {},
    onToggleScreenshot: (Boolean) -> Unit = {},
    onExportBackup: () -> Unit = {},
    onImportBackup: () -> Unit = {},
    onTavilyApiKeyChange: (String) -> Unit = {},
    onBraveApiKeyChange: (String) -> Unit = {},
    onPickModel: () -> Unit,
    onDeleteModel: () -> Unit,
    onSelectBackend: (String) -> Unit,
    onSelectSearchEngine: (String) -> Unit,
    onStartEngine: () -> Unit,
    onToggleInternet: (Boolean) -> Unit,
    models: List<com.rajpawardotin.kosh.data.ModelProfile> = emptyList(),
    onSelectModel: (com.rajpawardotin.kosh.data.ModelProfile) -> Unit = {},
    onSetModelTag: (String, com.rajpawardotin.kosh.data.ModelTag) -> Unit = { _, _ -> },
    onDeleteModelFile: (String) -> Unit = {},
    allTags: List<com.rajpawardotin.kosh.domain.model.ChatTag> = emptyList(),
    onCreateTag: (String, String) -> Unit = { _, _ -> },
    onRenameTag: (String, String, String, (Int, () -> Unit) -> Unit) -> Unit = { _, _, _, _ -> },
    onDeleteTag: (String, (Int, () -> Unit) -> Unit) -> Unit = { _, _ -> }
) {
    val scrollState = rememberScrollState()
    val primary = MaterialTheme.colorScheme.primary
    val outlineVariant = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)
    val cardColors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section: Header
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
            Text(
                text = "Settings & Config",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold, fontSize = 22.sp),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Configure neural models, web search, and device security",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
        }

        // Category 1: Neural Core
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = cardColors,
            border = androidx.compose.foundation.BorderStroke(1.dp, outlineVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Memory,
                        contentDescription = null,
                        tint = primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "NEURAL CORE",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = primary
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Active Model status row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (modelPath == null) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.error.copy(alpha = 0.12f))
                            .clickable { if (modelPath == null) onPickModel() else onDeleteModel() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (modelPath == null) Icons.Default.Add else Icons.Default.Delete,
                            contentDescription = null,
                            tint = if (modelPath == null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (modelPath == null) "Load Model" else File(modelPath).name,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                        Text(
                            text = when {
                                isEngineReady -> "Model Active"
                                modelPath != null -> "Standby • Ready for Init"
                                else -> "No Active Model"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isEngineReady) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Hardware Dashboard
                HardwareDashboard(
                    isEngineReady = isEngineReady,
                    selectedBackend = selectedBackend,
                    tokensPerSecond = tokensPerSecond,
                    npuLoad = npuLoad,
                    ramUsage = ramUsage,
                    modifier = Modifier.padding(horizontal = 0.dp)
                )

                // Initialize Engine Controls (if model selected but not running)
                AnimatedVisibility(
                    visible = modelPath != null && !isEngineReady,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column {
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Row(
                            Modifier.fillMaxWidth().selectableGroup(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            backends.forEach { text ->
                                val selected = text == selectedBackend
                                val activeColor = when {
                                    text.contains("NPU") -> MaterialTheme.colorScheme.primary
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
                                    onClick = { onSelectBackend(text) }
                                ) {
                                    Box(modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp), contentAlignment = Alignment.Center) {
                                        Text(
                                            text = text.split(" ")[0],
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal),
                                            color = if (selected) activeColor else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Button(
                            onClick = onStartEngine,
                            enabled = !isInitializing,
                            modifier = Modifier.fillMaxWidth().height(44.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            if (isInitializing) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Ignition...", fontWeight = FontWeight.Bold)
                            } else {
                                Text("Initialize Model", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = outlineVariant)
                Spacer(modifier = Modifier.height(12.dp))

                // Model Library list and import button inside Card
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Model Library",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(
                        onClick = { if (!isCopyingModel) onPickModel() },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                        enabled = !isCopyingModel,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        if (isCopyingModel) {
                            CircularProgressIndicator(modifier = Modifier.size(14.dp), color = MaterialTheme.colorScheme.primary, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isCopyingModel) "Importing..." else "Import",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(6.dp))

                if (models.isEmpty()) {
                    Text(
                        text = "No models in library. Import a .litertlm file to begin.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        models.forEach { model ->
                            val isActive = model.filePath == modelPath
                            val activeBorder = if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
                            val activeBg = if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.04f) else Color.Transparent

                            Surface(
                                onClick = { onSelectModel(model) },
                                shape = RoundedCornerShape(12.dp),
                                color = activeBg,
                                border = androidx.compose.foundation.BorderStroke(1.dp, activeBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Memory,
                                        contentDescription = null,
                                        tint = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = model.name,
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal),
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = String.format(java.util.Locale.US, "%.2f GB", model.sizeBytes / (1024.0 * 1024.0 * 1024.0)),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                        )
                                    }
                                    
                                    val tagColor = when (model.tag) {
                                        com.rajpawardotin.kosh.data.ModelTag.GENERAL -> MaterialTheme.colorScheme.primary
                                        com.rajpawardotin.kosh.data.ModelTag.CODER -> MaterialTheme.colorScheme.secondary
                                        com.rajpawardotin.kosh.data.ModelTag.RAG_READER -> MaterialTheme.colorScheme.tertiary
                                    }
                                    Surface(
                                        onClick = {
                                            val nextTag = when (model.tag) {
                                                com.rajpawardotin.kosh.data.ModelTag.GENERAL -> com.rajpawardotin.kosh.data.ModelTag.CODER
                                                com.rajpawardotin.kosh.data.ModelTag.CODER -> com.rajpawardotin.kosh.data.ModelTag.RAG_READER
                                                com.rajpawardotin.kosh.data.ModelTag.RAG_READER -> com.rajpawardotin.kosh.data.ModelTag.GENERAL
                                            }
                                            onSetModelTag(model.name, nextTag)
                                        },
                                        shape = RoundedCornerShape(6.dp),
                                        color = tagColor.copy(alpha = 0.12f),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, tagColor.copy(alpha = 0.25f)),
                                        modifier = Modifier.padding(horizontal = 4.dp)
                                    ) {
                                        Text(
                                            text = model.tag.name,
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.sp),
                                            color = tagColor,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    
                                    Spacer(modifier = Modifier.width(2.dp))
                                    
                                    IconButton(
                                        onClick = { onDeleteModelFile(model.name) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete model",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
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

        // Category 2: Search & Intelligence
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = cardColors,
            border = androidx.compose.foundation.BorderStroke(1.dp, outlineVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = null,
                        tint = primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "SEARCH & INTELLIGENCE",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = primary
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Switch Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggleInternet(!isInternetEnabled) }
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
                            text = "Allow model to run live web queries for up-to-date answers",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Switch(
                        checked = isInternetEnabled,
                        onCheckedChange = onToggleInternet,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            checkedBorderColor = MaterialTheme.colorScheme.primary,
                            uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                            uncheckedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )
                }

                // Expandable Search Settings
                AnimatedVisibility(
                    visible = isInternetEnabled,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Web Search Provider",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Selectable engine rows
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            searchEngines.chunked(2).forEach { rowEngines ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    rowEngines.forEach { engine ->
                                        val selected = engine == selectedSearchEngine
                                        val accentColor = MaterialTheme.colorScheme.primary
                                        Surface(
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(12.dp),
                                            color = if (selected) accentColor.copy(alpha = 0.12f) else Color.Transparent,
                                            border = androidx.compose.foundation.BorderStroke(
                                                1.dp,
                                                if (selected) accentColor.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                            ),
                                            onClick = { onSelectSearchEngine(engine) }
                                        ) {
                                            Box(
                                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = engine,
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = if (selected) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Tavily Key input
                        AnimatedVisibility(
                            visible = selectedSearchEngine == "Tavily API",
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Column {
                                Spacer(modifier = Modifier.height(12.dp))
                                var showKey by remember { mutableStateOf(false) }
                                OutlinedTextField(
                                    value = tavilyApiKey,
                                    onValueChange = onTavilyApiKeyChange,
                                    label = { Text("Tavily API Key", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium) },
                                    placeholder = { Text("tvly-...", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f), style = MaterialTheme.typography.labelMedium) },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                                    trailingIcon = {
                                        IconButton(onClick = { showKey = !showKey }) {
                                            Icon(
                                                imageVector = if (showKey) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                                contentDescription = if (showKey) "Hide" else "Show",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                        cursorColor = MaterialTheme.colorScheme.primary
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    textStyle = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }

                        // Brave Key input
                        AnimatedVisibility(
                            visible = selectedSearchEngine == "Brave Search API",
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Column {
                                Spacer(modifier = Modifier.height(12.dp))
                                var showKey by remember { mutableStateOf(false) }
                                OutlinedTextField(
                                    value = braveApiKey,
                                    onValueChange = onBraveApiKeyChange,
                                    label = { Text("Brave Search API Key", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium) },
                                    placeholder = { Text("BS-...", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f), style = MaterialTheme.typography.labelMedium) },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                                    trailingIcon = {
                                        IconButton(onClick = { showKey = !showKey }) {
                                            Icon(
                                                imageVector = if (showKey) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                                contentDescription = if (showKey) "Hide" else "Show",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                        cursorColor = MaterialTheme.colorScheme.primary
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    textStyle = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }
        }

        // Category 3: Security & Privacy
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = cardColors,
            border = androidx.compose.foundation.BorderStroke(1.dp, outlineVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "SECURITY & DATA",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = primary
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Startup Lock Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggleAppLock(!isAppLockEnabled) }
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
                            text = "Require biometric/passcode on launch",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Switch(
                        checked = isAppLockEnabled,
                        onCheckedChange = onToggleAppLock,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            checkedBorderColor = MaterialTheme.colorScheme.primary,
                            uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                            uncheckedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                
                // Screenshot Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggleScreenshot(!isScreenshotEnabled) }
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
                            text = "Allow system capture inside secure vaults",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Switch(
                        checked = isScreenshotEnabled,
                        onCheckedChange = onToggleScreenshot,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            checkedBorderColor = MaterialTheme.colorScheme.primary,
                            uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                            uncheckedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = outlineVariant)
                Spacer(modifier = Modifier.height(16.dp))
                
                // Vault Backup buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onExportBackup,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
                        contentPadding = PaddingValues(vertical = 10.dp)
                    ) {
                        Text("Export Vault", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                    }
                    
                    OutlinedButton(
                        onClick = onImportBackup,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
                        contentPadding = PaddingValues(vertical = 10.dp)
                    ) {
                        Text("Import Vault", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                    }
                }
            }
        }

        // Category 3.5: Chat Tags Management
        var newTagName by remember { mutableStateOf("") }
        val colorOptions = listOf(
            "#EF4444", // Crimson Red
            "#F97316", // Sunset Orange
            "#F59E0B", // Amber Gold
            "#84CC16", // Lime Zing
            "#10B981", // Emerald Green
            "#14B8A6", // Teal Aurora
            "#06B6D4", // Cyan Wave
            "#0EA5E9", // Sky Blue
            "#3B82F6", // Cobalt Blue
            "#6366F1", // Royal Indigo
            "#8B5CF6", // Electric Purple
            "#D946EF", // Orchid Magenta
            "#EC4899", // Hot Pink
            "#F43F5E", // Rose Coral
            "#C2410C", // Terracotta Rust
            "#4D7C0F", // Olive Green
            "#15803D", // Forest Green
            "#1D4ED8", // Navy Blue
            "#6B21A8", // Plum Purple
            "#475569"  // Slate Gunmetal
        )
        var selectedColor by remember { mutableStateOf(colorOptions[10]) } // default purple
        var tagWarningDialog by remember { mutableStateOf<com.rajpawardotin.kosh.ui.chat.dialogs.TagWarningInfo?>(null) }
        var editingTag by remember { mutableStateOf<com.rajpawardotin.kosh.domain.model.ChatTag?>(null) }
        var editTagName by remember { mutableStateOf("") }
        var editTagColor by remember { mutableStateOf("") }

        // Local inline Warning Dialog
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
                        // Live tag preview for editing
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

                        // Slideable Carousel Color Picker for Edit
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
                                val scale by animateFloatAsState(targetValue = if (isSelected) 1.2f else 1.0f)
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
                            onRenameTag(targetTag.name, editTagName, editTagColor) { count, proceed ->
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

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = cardColors,
            border = androidx.compose.foundation.BorderStroke(1.dp, outlineVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Label,
                        contentDescription = null,
                        tint = primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "TAGS MANAGEMENT",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = primary
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Live preview and label
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "CREATE NEW TAG",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
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

                // Create Tag Input Row
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
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            focusedContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.02f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.02f)
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (newTagName.isNotBlank()) selectedColorParsed
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
                            )
                            .clickable(enabled = newTagName.isNotBlank()) {
                                onCreateTag(newTagName, selectedColor)
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

                // Slideable Carousel Color Picker
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
                        val scale by animateFloatAsState(targetValue = if (isSelected) 1.2f else 1.0f)
                        
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

                if (allTags.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = outlineVariant)
                    Spacer(modifier = Modifier.height(12.dp))

                    // Existing Tags list
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        allTags.forEach { tag ->
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
                                            contentDescription = "Rename Tag",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            onDeleteTag(tag.name) { count, proceed ->
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

        Spacer(modifier = Modifier.height(16.dp))

        // Category 4: Appearance & Theming
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = cardColors,
            border = androidx.compose.foundation.BorderStroke(1.dp, outlineVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = null,
                        tint = primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "THEME & APPEARANCE",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
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
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    themeOptions.forEach { (themeKey, label) ->
                        val isSelected = currentTheme == themeKey
                        val borderBrush = if (isSelected) {
                            Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.secondary
                                )
                            )
                        } else {
                            Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.12f),
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
                                )
                            )
                        }
                        
                        Surface(
                            onClick = { onThemeSelected(themeKey) },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.Transparent,
                            border = androidx.compose.foundation.BorderStroke(
                                width = 1.dp,
                                brush = borderBrush
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 2.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val previewColors = when (themeKey) {
                                        "OLED_OBSIDIAN" -> listOf(Color(0xFF000000), Color(0xFF6366F1), Color(0xFF84CC16))
                                        "MINIMALIST_SAND" -> listOf(Color(0xFFFAF9F6), Color(0xFFC2410C), Color(0xFF4F46E5))
                                        "AERO_GLASS" -> listOf(Color(0xFF0B0F19), Color(0xFF14B8A6), Color(0xFF38BDF8))
                                        else -> listOf(Color.Gray, Color.DarkGray, Color.LightGray) // System
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
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
        
        // Bottom Spacer for BottomSheet layout clearance
        Spacer(modifier = Modifier.height(16.dp))
    }
}


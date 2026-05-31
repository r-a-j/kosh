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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
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
    onDeleteModelFile: (String) -> Unit = {}
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


package com.rajpawardotin.kosh.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Memory
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
    onToggleAppLock: (Boolean) -> Unit = {},
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
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(32.dp),
        color = Color(0xFF1A1A1A).copy(alpha = 0.9f),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Circular Pulse for Status
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(if (isEngineReady) Color(0xFF03DAC5).copy(alpha = 0.1f) else Color.White.copy(alpha = 0.05f))
                        .selectable(selected = false, onClick = { if (modelPath == null) onPickModel() else onDeleteModel() }),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (modelPath == null) Icons.Default.Add else Icons.Default.Delete,
                        contentDescription = null,
                        tint = if (modelPath == null) Color(0xFF03DAC5) else Color(0xFFCF6679),
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (modelPath == null) "Load Intelligence" else File(modelPath).name,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = when {
                            isEngineReady -> "Neural Core Active"
                            modelPath != null -> "Standby • Ready for Init"
                            else -> "No Active Core"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isEngineReady) Color(0xFF03DAC5) else Color.Gray
                    )
                }

                if (isEngineReady) {
                    Surface(
                        onClick = { onToggleInternet(!isInternetEnabled) },
                        shape = CircleShape,
                        color = if (isInternetEnabled) Color(0xFF6200EE).copy(alpha = 0.2f) else Color.Transparent,
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("WEB", style = MaterialTheme.typography.labelSmall, color = Color.White)
                            Spacer(modifier = Modifier.width(4.dp))
                            Box(
                                modifier = Modifier.size(width = 32.dp, height = 20.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Switch(
                                    checked = isInternetEnabled,
                                    onCheckedChange = onToggleInternet,
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color(0xFF03DAC5),
                                        checkedTrackColor = Color(0xFF03DAC5).copy(alpha = 0.2f),
                                        checkedBorderColor = Color(0xFF03DAC5),
                                        uncheckedThumbColor = Color.Gray,
                                        uncheckedTrackColor = Color.White.copy(alpha = 0.05f),
                                        uncheckedBorderColor = Color.White.copy(alpha = 0.15f)
                                    ),
                                    modifier = Modifier.scale(0.6f)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            HardwareDashboard(
                isEngineReady = isEngineReady,
                selectedBackend = selectedBackend,
                tokensPerSecond = tokensPerSecond,
                npuLoad = npuLoad,
                ramUsage = ramUsage,
                modifier = Modifier.padding(horizontal = 0.dp)
            )

            AnimatedVisibility(
                visible = isInternetEnabled,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Web Search Engine",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.Gray,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
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
                                    Surface(
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (selected) Color(0xFF6200EE).copy(alpha = 0.15f) else Color.Transparent,
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.dp,
                                            if (selected) Color(0xFF6200EE).copy(alpha = 0.5f) else Color.White.copy(alpha = 0.05f)
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
                                                color = if (selected) Color(0xFF03DAC5) else Color.Gray,
                                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

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
                                label = { Text("Tavily API Key", color = Color.Gray, style = MaterialTheme.typography.labelMedium) },
                                placeholder = { Text("tvly-...", color = Color.DarkGray, style = MaterialTheme.typography.labelMedium) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                                trailingIcon = {
                                    IconButton(onClick = { showKey = !showKey }) {
                                        Icon(
                                            imageVector = if (showKey) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                            contentDescription = if (showKey) "Hide" else "Show",
                                            tint = Color.Gray
                                        )
                                    }
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF03DAC5),
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    cursorColor = Color(0xFF03DAC5)
                                ),
                                shape = RoundedCornerShape(16.dp),
                                textStyle = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

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
                                label = { Text("Brave Search API Key", color = Color.Gray, style = MaterialTheme.typography.labelMedium) },
                                placeholder = { Text("BS-...", color = Color.DarkGray, style = MaterialTheme.typography.labelMedium) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                                trailingIcon = {
                                    IconButton(onClick = { showKey = !showKey }) {
                                        Icon(
                                            imageVector = if (showKey) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                            contentDescription = if (showKey) "Hide" else "Show",
                                            tint = Color.Gray
                                        )
                                    }
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF03DAC5),
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    cursorColor = Color(0xFF03DAC5)
                                ),
                                shape = RoundedCornerShape(16.dp),
                                textStyle = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = modelPath != null && !isEngineReady,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    Row(
                        Modifier.fillMaxWidth().selectableGroup(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        backends.forEach { text ->
                            val selected = text == selectedBackend
                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(16.dp),
                                color = if (selected) Color(0xFF03DAC5).copy(alpha = 0.15f) else Color.Transparent,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp, 
                                    if (selected) Color(0xFF03DAC5).copy(alpha = 0.5f) else Color.White.copy(alpha = 0.1f)
                                ),
                                onClick = { onSelectBackend(text) }
                            ) {
                                Box(modifier = Modifier.padding(10.dp), contentAlignment = Alignment.Center) {
                                    Text(
                                        text = text.split(" ")[0],
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (selected) Color(0xFF03DAC5) else Color.Gray,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Button(
                        onClick = onStartEngine,
                        enabled = !isInitializing,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF03DAC5),
                            contentColor = Color.Black
                        )
                    ) {
                        if (isInitializing) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.Black, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Ignition...", fontWeight = FontWeight.Bold)
                        } else {
                            Text("Ignite Neural Engine", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Cognitive Library",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.Gray,
                    fontWeight = FontWeight.Bold
                )
                TextButton(
                    onClick = { if (!isCopyingModel) onPickModel() },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF03DAC5)),
                    enabled = !isCopyingModel
                ) {
                    if (isCopyingModel) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color(0xFF03DAC5), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isCopyingModel) "Importing..." else "Import",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            if (models.isEmpty()) {
                Text(
                    text = "No models in library. Import a .litertlm file to begin.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.DarkGray,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    models.forEach { model ->
                        val isActive = model.filePath == modelPath
                        val activeBorder = if (isActive) Color(0xFF03DAC5).copy(alpha = 0.5f) else Color.White.copy(alpha = 0.05f)
                        val activeBg = if (isActive) Color(0xFF03DAC5).copy(alpha = 0.04f) else Color.Transparent

                        Surface(
                            onClick = { onSelectModel(model) },
                            shape = RoundedCornerShape(16.dp),
                            color = activeBg,
                            border = androidx.compose.foundation.BorderStroke(1.dp, activeBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Memory,
                                    contentDescription = null,
                                    tint = if (isActive) Color(0xFF03DAC5) else Color.Gray,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = model.name,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal),
                                        color = if (isActive) Color.White else Color.LightGray,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = String.format(java.util.Locale.US, "%.2f GB", model.sizeBytes / (1024.0 * 1024.0 * 1024.0)),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.Gray
                                    )
                                }
                                
                                val tagColor = when (model.tag) {
                                    com.rajpawardotin.kosh.data.ModelTag.GENERAL -> Color(0xFF03DAC5)
                                    com.rajpawardotin.kosh.data.ModelTag.CODER -> Color(0xFFBB86FC)
                                    com.rajpawardotin.kosh.data.ModelTag.RAG_READER -> Color(0xFFFFB74D)
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
                                    shape = RoundedCornerShape(8.dp),
                                    color = tagColor.copy(alpha = 0.15f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, tagColor.copy(alpha = 0.3f)),
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                ) {
                                    Text(
                                        text = model.tag.name,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = tagColor,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                                
                                Spacer(modifier = Modifier.width(4.dp))
                                
                                IconButton(
                                    onClick = { onDeleteModelFile(model.name) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete model",
                                        tint = Color.Gray.copy(alpha = 0.7f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Security & Data",
                style = MaterialTheme.typography.labelMedium,
                color = Color.Gray,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "App Lock on Startup",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium
                )
                Switch(
                    checked = isAppLockEnabled,
                    onCheckedChange = onToggleAppLock,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color(0xFF03DAC5),
                        checkedTrackColor = Color(0xFF03DAC5).copy(alpha = 0.2f),
                        checkedBorderColor = Color(0xFF03DAC5),
                        uncheckedThumbColor = Color.Gray,
                        uncheckedTrackColor = Color.White.copy(alpha = 0.05f),
                        uncheckedBorderColor = Color.White.copy(alpha = 0.15f)
                    ),
                    modifier = Modifier.scale(0.8f)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onExportBackup,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
                ) {
                    Text("Export Vault", style = MaterialTheme.typography.labelMedium)
                }
                
                OutlinedButton(
                    onClick = onImportBackup,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
                ) {
                    Text("Import Vault", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}


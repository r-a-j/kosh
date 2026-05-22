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
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File

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
    onPickModel: () -> Unit,
    onDeleteModel: () -> Unit,
    onSelectBackend: (String) -> Unit,
    onStartEngine: () -> Unit,
    onToggleInternet: (Boolean) -> Unit
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
                            Switch(
                                checked = isInternetEnabled,
                                onCheckedChange = onToggleInternet,
                                modifier = Modifier.height(18.dp)
                            )
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
        }
    }
}


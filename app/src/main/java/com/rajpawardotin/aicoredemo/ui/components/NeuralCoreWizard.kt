package com.rajpawardotin.aicoredemo.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File

@Composable
fun NeuralCoreWizard(
    modelPath: String?,
    isInitializing: Boolean,
    isCopyingModel: Boolean,
    selectedBackend: String,
    backends: List<String>,
    onPickModel: () -> Unit,
    onSelectBackend: (String) -> Unit,
    onStartEngine: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier.widthIn(max = 400.dp)
        ) {
            // High-Tech Pulsing Logo
            val infiniteTransition = rememberInfiniteTransition(label = "logoPulse")
            val scale by infiniteTransition.animateFloat(
                initialValue = 0.95f,
                targetValue = 1.05f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1200, easing = EaseInOutSine),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "scale"
            )

            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                Color(0xFF03DAC5).copy(alpha = 0.2f),
                                Color.Transparent
                            )
                        )
                    )
                    .border(1.dp, Color(0xFF03DAC5).copy(alpha = 0.3f), CircleShape)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = Color(0xFF03DAC5),
                    modifier = Modifier
                        .size(36.dp)
                        .aspectRatio(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (modelPath == null) {
                // Step 1: Load Core Intelligence
                Step1LoadModel(
                    isCopyingModel = isCopyingModel,
                    onPickModel = onPickModel
                )
            } else {
                // Step 2: Configure & Ignite
                Step2Ignite(
                    modelPath = modelPath,
                    isInitializing = isInitializing,
                    selectedBackend = selectedBackend,
                    backends = backends,
                    onSelectBackend = onSelectBackend,
                    onStartEngine = onStartEngine
                )
            }
        }
    }
}

@Composable
fun Step1LoadModel(
    isCopyingModel: Boolean,
    onPickModel: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E22).copy(alpha = 0.8f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "NEURAL OS OFFLINE",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp
                ),
                color = Color(0xFFFFB74D), // Warning Orange
                textAlign = TextAlign.Center
            )

            Text(
                text = "The system requires a LiteRT LLM model file (.litertlm) to initialize local NPU / GPU cognitive pathways.",
                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                color = Color.Gray,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onPickModel,
                enabled = !isCopyingModel,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF03DAC5),
                    contentColor = Color.Black
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                if (isCopyingModel) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.Black,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Copying Core...", fontWeight = FontWeight.Bold)
                } else {
                    Icon(Icons.Default.FolderOpen, contentDescription = null)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("LOAD CORE MODEL", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun Step2Ignite(
    modelPath: String,
    isInitializing: Boolean,
    selectedBackend: String,
    backends: List<String>,
    onSelectBackend: (String) -> Unit,
    onStartEngine: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E22).copy(alpha = 0.8f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "CORE READY FOR SYNC",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp
                ),
                color = Color(0xFF03DAC5),
                textAlign = TextAlign.Center
            )

            // Display loaded file name as a chip
            val fileName = File(modelPath).name
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.White.copy(alpha = 0.04f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Memory,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = fileName,
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Backend selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                backends.forEach { text ->
                    val selected = text == selectedBackend
                    val activeColor = when {
                        text.contains("NPU") -> Color(0xFF03DAC5)
                        text == "GPU" -> Color(0xFFBB86FC)
                        else -> Color(0xFFFFB74D)
                    }

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (selected) activeColor.copy(alpha = 0.15f) else Color.Transparent,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (selected) activeColor.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.1f)
                        ),
                        onClick = { onSelectBackend(text) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier.padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = text.split(" ")[0],
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (selected) activeColor else Color.Gray
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Large Ignite Button
            Button(
                onClick = onStartEngine,
                enabled = !isInitializing,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF03DAC5),
                    contentColor = Color.Black
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                if (isInitializing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.Black,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("SYNCHRONIZING CORE...", fontWeight = FontWeight.Bold)
                } else {
                    Text("IGNITE NEURAL CORE", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

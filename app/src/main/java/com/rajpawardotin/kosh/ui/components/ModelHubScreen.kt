package com.rajpawardotin.kosh.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rajpawardotin.kosh.data.ModelProfile
import com.rajpawardotin.kosh.data.ModelTag
import com.rajpawardotin.kosh.ui.chat.ChatViewModel
import java.io.File

data class MatrixModel(
    val name: String,
    val type: String,
    val size: String,
    val targetHardware: String,
    val prefillSpeed: String,
    val decodeSpeed: String,
    val bestFor: String,
    val url: String,
    val description: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelHubScreen(
    viewModel: ChatViewModel,
    onPickModel: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val outlineVariant = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary

    val openUrl = { url: String ->
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Cannot open browser", Toast.LENGTH_SHORT).show()
        }
    }

    val modelsMatrix = remember {
        listOf(
            MatrixModel(
                name = "Gemma-4-E2B",
                type = "Chat",
                size = "2.58 GB",
                targetHardware = "Samsung S26 Ultra / Snapdragon 8 Gen 4",
                prefillSpeed = "3808 tk/s",
                decodeSpeed = "52 tk/s",
                bestFor = "Best Overall Balance (Speed, Reasoning, and MTP support)",
                url = "https://huggingface.co/PeppX/gemma-4-e2b-uncensored-litertlm",
                description = "Official Google Gemma-4 model fine-tuned for on-device applications. Native support for Multi-token Prediction (MTP) allows near-instant prefill and highly fluent decode speeds. Our recommended pick."
            ),
            MatrixModel(
                name = "Qwen2.5-1.5B",
                type = "Chat",
                size = "1.60 GB",
                targetHardware = "Samsung S25 Ultra / Mid-range devices",
                prefillSpeed = "1668 tk/s",
                decodeSpeed = "31 tk/s",
                bestFor = "Best for Multilingual & Low RAM devices",
                url = "https://huggingface.co/litert-community/Qwen2.5-1.5B-Instruct",
                description = "Exceptional multilingual reasoning model. Excels in East Asian, Middle Eastern, and European languages. Balanced memory footprints and fast generation."
            ),
            MatrixModel(
                name = "Gemma3-1B",
                type = "Chat",
                size = "1.00 GB",
                targetHardware = "Samsung S24 Ultra / Flagship & budget",
                prefillSpeed = "1191 tk/s",
                decodeSpeed = "24 tk/s",
                bestFor = "Ultra-lightweight chat for entry-level devices",
                url = "https://huggingface.co/litert-community/Gemma3-1B-IT",
                description = "Highly optimized, extremely small footprint model. Ideal for devices with limited memory headroom or budget configurations. Low battery draw."
            ),
            MatrixModel(
                name = "Gemma-3n-E2B",
                type = "Chat",
                size = "2.96 GB",
                targetHardware = "Samsung S24 Ultra / Snapdragon 8 Gen 3",
                prefillSpeed = "816 tk/s",
                decodeSpeed = "16 tk/s",
                bestFor = "Good compatibility with Gemini Nano workflows",
                url = "https://huggingface.co/models?search=litert-gemma-3n-2b",
                description = "Google Gemma 3n (Nano) quantized model. Delivers balanced reasoning on older chipsets."
            ),
            MatrixModel(
                name = "Gemma4-E4B",
                type = "Chat",
                size = "3.65 GB",
                targetHardware = "Samsung S26 Ultra / Flagships",
                prefillSpeed = "1293 tk/s",
                decodeSpeed = "22 tk/s",
                bestFor = "Higher reasoning power (at the cost of speed)",
                url = "https://huggingface.co/models?search=litert-gemma-4b",
                description = "Google Gemma-4 4B model. Drastically higher score in math and multi-turn logic at the cost of processing latency."
            ),
            MatrixModel(
                name = "Gemma-3n-E4B",
                type = "Chat",
                size = "4.23 GB",
                targetHardware = "Samsung S24 Ultra / Flagships",
                prefillSpeed = "548 tk/s",
                decodeSpeed = "9 tk/s",
                bestFor = "Heavy task processing",
                url = "https://huggingface.co/models?search=litert-gemma-3n-4b",
                description = "Google Gemma 3n 4B model. Robust task following, requires flagship processors."
            ),
            MatrixModel(
                name = "phi-4-mini",
                type = "Chat",
                size = "3.90 GB",
                targetHardware = "Samsung S24 Ultra / Flagships",
                prefillSpeed = "314 tk/s",
                decodeSpeed = "10 tk/s",
                bestFor = "Coding & complex logic (runs slow)",
                url = "https://huggingface.co/models?search=litert-phi-4-mini",
                description = "Microsoft Phi-4-mini model. High coding ability and reasoning benchmark scores. Runs slow on standard mobile hardware."
            ),
            MatrixModel(
                name = "Qwen2.5-0.5B",
                type = "Chat",
                size = "521 MB",
                targetHardware = "Samsung S24 Ultra / Constrained RAM",
                prefillSpeed = "—",
                decodeSpeed = "—",
                bestFor = "Highly constrained devices (runs at 30 tk/s on CPU)",
                url = "https://huggingface.co/litert-community/Qwen2.5-0.5B-Instruct",
                description = "Extremely small Qwen model. Can run entirely on low-tier CPUs for background processing."
            ),
            MatrixModel(
                name = "FunctionGemma",
                type = "Action",
                size = "289 MB",
                targetHardware = "Samsung S25 Ultra / Flagship & Mid-range",
                prefillSpeed = "—",
                decodeSpeed = "—",
                bestFor = "Constrained tool use / Function calling only (154 tk/s on CPU)",
                url = "https://huggingface.co/models?search=litert-function-gemma",
                description = "Specialized model designed only to generate structured JSON commands for native device functions. Fast and efficient background worker."
            )
        )
    }

    var selectedTab by remember { mutableStateOf(0) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Aesthetics Radial Glows
        Box(
            modifier = Modifier
                .size(350.dp)
                .offset(x = (-80).dp, y = (-80).dp)
                .background(Brush.radialGradient(
                    0.0f to primary.copy(alpha = 0.10f),
                    0.5f to primary.copy(alpha = 0.04f),
                    1.0f to Color.Transparent
                ))
        )
        Box(
            modifier = Modifier
                .size(350.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 80.dp, y = 80.dp)
                .background(Brush.radialGradient(
                    0.0f to secondary.copy(alpha = 0.12f),
                    0.5f to secondary.copy(alpha = 0.05f),
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
                        contentDescription = "Back to Dashboard",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Text(
                    text = "Model Hub",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
            }

            // Contextual Guidance Callout Banner
            val modelsCount = viewModel.models.size
            val isEngineReady = viewModel.isEngineReady

            AnimatedContent(
                targetState = Pair(modelsCount, isEngineReady),
                label = "guidanceCallout",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp)
            ) { (count, online) ->
                val (color, title, desc, icon) = when {
                    count == 0 -> Quadruple(
                        MaterialTheme.colorScheme.error,
                        "NO MODELS IMPORTED",
                        "Kosh runs 100% offline. Follow Step 1 or Step 2 below to find and download a compatible model file, then import it.",
                        Icons.Default.Warning
                    )
                    !online -> Quadruple(
                        MaterialTheme.colorScheme.tertiary,
                        "NEURAL CORE STANDBY",
                        "Your model is imported but the engine is offline. Return to the Dashboard or go to the Library tab to click 'Initialize Core'.",
                        Icons.Default.PowerSettingsNew
                    )
                    else -> Quadruple(
                        MaterialTheme.colorScheme.primary,
                        "NEURAL CORE ONLINE",
                        "Active weights: ${viewModel.modelPath?.let { File(it).name.uppercase() } ?: "UNKNOWN"}. Core is ready to respond.",
                        Icons.Default.CheckCircle
                    )
                }

                Surface(
                    color = color.copy(alpha = 0.07f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.25f)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = color,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                                color = color
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = desc,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (count > 0 && !online) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = { viewModel.triggerManualInitialization() },
                                enabled = !viewModel.isInitializing,
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = color),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.height(36.dp)
                            ) {
                                if (viewModel.isInitializing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Text(
                                        text = "INITIALIZE",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Guidance-numbered navigation tabs
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.primary,
                divider = { HorizontalDivider(color = outlineVariant) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("1. DISCOVER", fontWeight = FontWeight.Bold, fontSize = 11.sp) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("2. FIND FIT", fontWeight = FontWeight.Bold, fontSize = 11.sp) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("3. LIBRARY", fontWeight = FontWeight.Bold, fontSize = 11.sp) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Main Tab Content
            Box(modifier = Modifier.weight(1f)) {
                when (selectedTab) {
                    0 -> DiscoverTab(models = modelsMatrix, onDownloadClick = openUrl)
                    1 -> FindFitTab(onDownloadClick = openUrl)
                    2 -> LibraryTab(viewModel = viewModel, onPickModel = onPickModel)
                }
            }
        }
    }
}

// Tab 1: Discover / Performance Matrix
@Composable
fun DiscoverTab(
    models: List<MatrixModel>,
    onDownloadClick: (String) -> Unit
) {
    val scrollState = rememberScrollState()
    val outlineVariant = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)
    val primary = MaterialTheme.colorScheme.primary

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Table Horizontal Scroll Header Info
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Speeds based on flagship hardware running GPU acceleration. Swipe left/right on rows to view matrix specs.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Models List
        items(models) { model ->
            var isExpanded by remember { mutableStateOf(false) }

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.dp,
                    color = if (isExpanded) primary.copy(alpha = 0.3f) else outlineVariant
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Header Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = model.name,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = primary.copy(alpha = 0.1f)
                                ) {
                                    Text(
                                        text = model.type.uppercase(),
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, fontWeight = FontWeight.Bold),
                                        color = primary,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                                Text(
                                    text = model.size,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                        }

                        IconButton(onClick = { isExpanded = !isExpanded }) {
                            Icon(
                                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = "Expand details",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Horizontal Scrolling Key Specs Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(scrollState),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        SpecPill(label = "TARGET HARDWARE", value = model.targetHardware)
                        SpecPill(label = "PREFILL (TK/S)", value = model.prefillSpeed, highlight = primary)
                        SpecPill(label = "DECODE (TK/S)", value = model.decodeSpeed, highlight = MaterialTheme.colorScheme.secondary)
                        SpecPill(label = "BEST FOR", value = model.bestFor)
                    }

                    // Expanded explanation & download button
                    AnimatedVisibility(
                        visible = isExpanded,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Spacer(modifier = Modifier.height(14.dp))
                            HorizontalDivider(color = outlineVariant)
                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = model.description,
                                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            Button(
                                onClick = { onDownloadClick(model.url) },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = primary),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                            ) {
                                Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("DOWNLOAD FROM HUGGING FACE", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SpecPill(
    label: String,
    value: String,
    highlight: Color? = null
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.03f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.05f))
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 7.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                color = highlight ?: MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
        }
    }
}

// Tab 2: Find Fit / Scenario Guide Recommender
@Composable
fun FindFitTab(
    onDownloadClick: (String) -> Unit
) {
    val outlineVariant = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Introduction card
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = primary.copy(alpha = 0.05f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, primary.copy(alpha = 0.15f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Lightbulb, contentDescription = null, tint = primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Find Your Fit Guide",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = primary
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Match your device's RAM and your specific assistant priorities below to download the absolute best model for your needs.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Scenario 1: Best Overall
        item {
            ScenarioCard(
                title = "1. Best Overall Speed & Reasoning",
                description = "Recommended for flagship processors (Snapdragon 8 Gen 3/4, Google Tensor G4) with at least 8GB of system RAM. Delivers near-instant responses with high language capabilities.",
                modelName = "Gemma-4-E2B (2.58 GB)",
                badgeText = "3808 tk/s Prefill • 52 tk/s Decode",
                onDownload = { onDownloadClick("https://huggingface.co/PeppX/gemma-4-e2b-uncensored-litertlm") }
            )
        }

        // Scenario 2: Coding & Logic
        item {
            ScenarioCard(
                title = "2. Writing Code & Complex Reasoning",
                description = "If you need Kosh to analyze code snippets, write logic algorithms, or solve math puzzles, these larger models score highest. Note: speed will feel noticeably slower.",
                modelName = "phi-4-mini (3.90 GB) / Gemma4-E4B (3.65 GB)",
                badgeText = "10–22 tk/s Decode • Requires 4GB+ Free RAM",
                tintColor = secondary,
                onDownload = { onDownloadClick("https://huggingface.co/models?search=litert-phi-4-mini") }
            )
        }

        // Scenario 3: Budget Devices
        item {
            ScenarioCard(
                title = "3. Low RAM / Older & Budget Devices",
                description = "On budget configurations with 4GB–6GB total RAM, larger models will crash the engine. These models are ultra-lightweight, have minor power draw, and generate files reliably.",
                modelName = "Gemma3-1B (1.00 GB) / Qwen2.5-1.5B (1.60 GB)",
                badgeText = "Fast Init • 24–31 tk/s Decode • Low Battery Draw",
                tintColor = MaterialTheme.colorScheme.tertiary,
                onDownload = { onDownloadClick("https://huggingface.co/litert-community/Gemma3-1B-IT") }
            )
        }

        // Scenario 4: Multilingual
        item {
            ScenarioCard(
                title = "4. Multilingual & International Support",
                description = "Gemma is heavily English-centric. If you need fluent multi-turn conversations in East Asian, European, or Middle Eastern languages, select the Qwen series.",
                modelName = "Qwen2.5-1.5B (1.60 GB)",
                badgeText = "Excellent Multilingual reasoning • 31 tk/s Decode",
                tintColor = primary,
                onDownload = { onDownloadClick("https://huggingface.co/litert-community/Qwen2.5-1.5B-Instruct") }
            )
        }

        // Scenario 5: Automation
        item {
            ScenarioCard(
                title = "5. Constrained Tool Use & Device Automation",
                description = "A specialized micro-model trained strictly to parse user intent and output structured JSON commands for native device functions. Runs completely on CPU at minimal load.",
                modelName = "FunctionGemma (289 MB)",
                badgeText = "154 tk/s on CPU • Constrained Tool-Use only",
                tintColor = MaterialTheme.colorScheme.outline,
                onDownload = { onDownloadClick("https://huggingface.co/models?search=litert-function-gemma") }
            )
        }
    }
}

@Composable
fun ScenarioCard(
    title: String,
    description: String,
    modelName: String,
    badgeText: String,
    tintColor: Color = MaterialTheme.colorScheme.primary,
    onDownload: () -> Unit
) {
    val outlineVariant = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            Surface(
                color = tintColor.copy(alpha = 0.08f),
                border = androidx.compose.foundation.BorderStroke(1.dp, tintColor.copy(alpha = 0.25f)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Memory,
                        contentDescription = null,
                        tint = tintColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = modelName,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = badgeText,
                            style = MaterialTheme.typography.labelSmall,
                            color = tintColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = onDownload,
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, tintColor.copy(alpha = 0.5f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = tintColor),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
            ) {
                Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("DOWNLOAD MODEL FILE", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
            }
        }
    }
}

// Tab 3: Local Library Manager & File Import
@Composable
fun LibraryTab(
    viewModel: ChatViewModel,
    onPickModel: () -> Unit
) {
    val outlineVariant = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)
    val primary = MaterialTheme.colorScheme.primary

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (viewModel.models.isEmpty()) {
            // Step-by-Step checklist when library is empty
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "GET STARTED: STEP-BY-STEP",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                            color = primary
                        )

                        Text(
                            text = "Follow these instructions to configure your offline assistant:",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        ChecklistRow(stepNum = "1", desc = "Download a model file (.litertlm / .bin) from the Discover or Find Fit tabs.", checked = false)
                        ChecklistRow(stepNum = "2", desc = "Click 'IMPORT LOCAL MODEL FILE' below and select the downloaded file.", checked = false)
                        ChecklistRow(stepNum = "3", desc = "Tap the imported model to select it, then return to the Dashboard to initialize the Core.", checked = false)
                    }
                }
            }
        } else {
            // Subtitle
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "IMPORTED MODEL WEIGHTS",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = primary.copy(alpha = 0.08f)
                    ) {
                        Text(
                            text = "${viewModel.models.size} FILE(S)",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.sp),
                            color = primary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Local models files lists
            items(viewModel.models) { model ->
                val isActive = model.filePath == viewModel.modelPath
                val activeBorder = if (isActive) primary.copy(alpha = 0.4f) else outlineVariant
                val activeBg = if (isActive) primary.copy(alpha = 0.04f) else Color.Transparent

                Surface(
                    onClick = { viewModel.selectModel(model.filePath) },
                    shape = RoundedCornerShape(16.dp),
                    color = activeBg,
                    border = androidx.compose.foundation.BorderStroke(1.dp, activeBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Pulse online/offline model indicator
                        if (isActive && viewModel.isEngineReady) {
                            val infiniteTransition = rememberInfiniteTransition(label = "activePulse")
                            val pulseAlpha by infiniteTransition.animateFloat(
                                initialValue = 0.3f,
                                targetValue = 1f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(800),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "alpha"
                            )
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(primary)
                                    .alpha(pulseAlpha)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Memory,
                                contentDescription = null,
                                tint = if (isActive) primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = model.name,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = String.format(java.util.Locale.US, "%.2f GB", model.sizeBytes / (1024.0 * 1024.0 * 1024.0)),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }

                        if (isActive && !viewModel.isEngineReady) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = { viewModel.triggerManualInitialization() },
                                enabled = !viewModel.isInitializing,
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = primary),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(30.dp)
                            ) {
                                if (viewModel.isInitializing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(12.dp),
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Text(
                                        text = "INITIALIZE",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    )
                                }
                            }
                        }

                        // Re-tag profile capsule
                        val tagColor = when (model.tag) {
                            ModelTag.GENERAL -> primary
                            ModelTag.CODER -> MaterialTheme.colorScheme.secondary
                            ModelTag.RAG_READER -> MaterialTheme.colorScheme.tertiary
                        }
                        Surface(
                            onClick = {
                                val nextTag = when (model.tag) {
                                    ModelTag.GENERAL -> ModelTag.CODER
                                    ModelTag.CODER -> ModelTag.RAG_READER
                                    ModelTag.RAG_READER -> ModelTag.GENERAL
                                }
                                viewModel.setModelTag(model.name, nextTag)
                            },
                            shape = RoundedCornerShape(8.dp),
                            color = tagColor.copy(alpha = 0.12f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, tagColor.copy(alpha = 0.25f)),
                            modifier = Modifier.padding(horizontal = 4.dp)
                        ) {
                            Text(
                                text = model.tag.name,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.sp),
                                color = tagColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        IconButton(
                            onClick = { viewModel.deleteModelFile(model.name) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete model",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        // Import Local Model Button (always at bottom)
        item {
            Button(
                onClick = onPickModel,
                enabled = !viewModel.isCopyingModel,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = primary),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                if (viewModel.isCopyingModel) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Copying file...", style = MaterialTheme.typography.labelMedium)
                } else {
                    Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("IMPORT LOCAL MODEL FILE", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                }
            }
        }
    }
}

@Composable
fun ChecklistRow(
    stepNum: String,
    desc: String,
    checked: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(
                    if (checked) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stepNum,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.sp),
                color = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = desc,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private data class Quadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)

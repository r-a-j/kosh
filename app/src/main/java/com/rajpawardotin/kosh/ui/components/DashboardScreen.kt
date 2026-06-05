package com.rajpawardotin.kosh.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.saveable.rememberSaveable
import kotlinx.coroutines.delay
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rajpawardotin.kosh.domain.model.ChatSession
import com.rajpawardotin.kosh.domain.model.ChatTag
import com.rajpawardotin.kosh.ui.chat.ChatViewModel
import java.io.File
import android.widget.Toast
import com.rajpawardotin.kosh.ui.chat.dialogs.DeleteSessionDialog
import com.rajpawardotin.kosh.ui.chat.dialogs.RenameSessionDialog

@Composable
fun DashboardScreen(
    viewModel: ChatViewModel,
    onManageModelsClick: () -> Unit,
    onQuickChatClick: () -> Unit,
    onStartJournalSession: () -> Unit,
    onLoadSession: (String) -> Unit,
    onOpenJournals: () -> Unit,
    onOpenSettings: () -> Unit,
    onLockSession: (ChatSession) -> Unit,
    onAttachDocumentClick: () -> Unit,
    topPadding: Dp = 0.dp,
    bottomPadding: Dp = 100.dp,
    modifier: Modifier = Modifier
) {
    val outlineVariant = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val cardColors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    val density = LocalDensity.current
    val topFadeHeightDp = 28.dp

    val selectedTags = remember { mutableStateListOf<String>() }
    val isFirstLaunch = rememberSaveable { mutableStateOf(true) }
    var sessionToDelete by remember { mutableStateOf<ChatSession?>(null) }
    var sessionToRename by remember { mutableStateOf<ChatSession?>(null) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val context = androidx.compose.ui.platform.LocalContext.current
    val animateEntrance = isFirstLaunch.value

    LaunchedEffect(Unit) {
        if (animateEntrance) {
            delay(1000)
            isFirstLaunch.value = false
        }
    }

    val filteredSessions = remember(viewModel.savedSessions.toList(), selectedTags.toList(), searchQuery) {
        val base = if (selectedTags.isEmpty()) {
            viewModel.savedSessions.toList()
        } else {
            viewModel.savedSessions.filter { session ->
                selectedTags.all { selectedId -> session.tags.any { it.id == selectedId } }
            }
        }
        if (searchQuery.isBlank()) {
            base
        } else {
            base.filter { session ->
                session.title.contains(searchQuery, ignoreCase = true) ||
                session.tags.any { it.name.contains(searchQuery, ignoreCase = true) }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Aesthetic ambient glows
        Box(
            modifier = Modifier
                .size(350.dp)
                .offset(x = (-80).dp, y = (-80).dp)
                .background(Brush.radialGradient(
                    0.0f to primary.copy(alpha = 0.12f),
                    0.4f to primary.copy(alpha = 0.05f),
                    1.0f to Color.Transparent
                ))
        )
        Box(
            modifier = Modifier
                .size(350.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 80.dp, y = 80.dp)
                .background(Brush.radialGradient(
                    0.0f to secondary.copy(alpha = 0.15f),
                    0.4f to secondary.copy(alpha = 0.06f),
                    1.0f to Color.Transparent
                ))
        )

        val scrollState = rememberLazyListState()
        var isSearchFocused by remember { mutableStateOf(false) }

        LaunchedEffect(isSearchFocused) {
            if (isSearchFocused) {
                delay(150)
                val searchBarIndex = if (viewModel.allTags.isNotEmpty()) 5 else 4
                val viewportHeight = scrollState.layoutInfo.viewportSize.height
                if (viewportHeight > 0) {
                    val itemHeight = with(density) { 64.dp.roundToPx() }
                    val targetOffset = (viewportHeight - itemHeight) / 2
                    scrollState.animateScrollToItem(index = searchBarIndex, scrollOffset = -targetOffset)
                } else {
                    scrollState.animateScrollToItem(index = searchBarIndex)
                }
            }
        }

        LazyColumn(
            state = scrollState,
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .fadingEdges(
                    topBoundaryPx = with(density) { topPadding.toPx() },
                    topFadePx = with(density) { topFadeHeightDp.toPx() }
                ),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = topPadding + 12.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. HERO CARD: Encrypted Personal Journal
            item {
                val journalGradient = Brush.linearGradient(
                    colors = listOf(
                        primary.copy(alpha = 0.25f),
                        secondary.copy(alpha = 0.08f)
                    )
                )

                StaggeredEntrance(enabled = animateEntrance, delayMillis = 0) { animationModifier ->
                    Card(
                        modifier = animationModifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .border(1.dp, primary.copy(alpha = 0.25f), RoundedCornerShape(24.dp)),
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                    ) {
                        Column(
                            modifier = Modifier
                                .background(journalGradient)
                                .padding(20.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.Top,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(primary.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = null,
                                        tint = primary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Personal Journal Vault",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold, fontSize = 16.sp),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Secure your thoughts, goals, and daily reflections offline. Fully encrypted locally with AES-GCM.",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp, lineHeight = 16.sp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val journalSessionsCount = remember(viewModel.savedSessions.toList()) {
                                    viewModel.savedSessions.count { it.tags.any { tag -> tag.id == "journal" } }
                                }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, outlineVariant)
                                ) {
                                    Text(
                                        text = "$journalSessionsCount entries secured",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }

                                Button(
                                    onClick = onStartJournalSession,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = primary),
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                                ) {
                                    Text("Write Entry", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                                }
                            }
                        }
                    }
                }
            }

            // 2. MIDDLE ROW: Vault Security Card (Action) & Model Engine Panel (Information)
            item {
                StaggeredEntrance(enabled = animateEntrance, delayMillis = 60) { animationModifier ->
                    Row(
                        modifier = animationModifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Card A: Vault Security Mode (Interactive Action Card)
                        Card(
                            modifier = Modifier
                                .weight(1.1f)
                                .height(136.dp),
                            shape = RoundedCornerShape(20.dp),
                            colors = cardColors,
                            border = androidx.compose.foundation.BorderStroke(1.dp, outlineVariant)
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.Top
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (viewModel.isTemporarySession) Icons.Default.VisibilityOff else Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = if (viewModel.isTemporarySession) MaterialTheme.colorScheme.tertiary else primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (viewModel.isTemporarySession) "RAM Sandbox" else "Active Vault",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                    )
                                }
                                
                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = if (viewModel.isTemporarySession) "Incognito. No device logs." else "AES-GCM encrypted database.",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp, lineHeight = 13.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    modifier = Modifier.heightIn(min = 26.dp)
                                )

                                Spacer(modifier = Modifier.weight(1f))

                                Button(
                                    onClick = {
                                        if (viewModel.isTemporarySession) {
                                            viewModel.startNewChat(isTemporary = false)
                                        } else {
                                            viewModel.lockAppOnBackground()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (viewModel.isTemporarySession) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
                                        contentColor = if (viewModel.isTemporarySession) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.height(28.dp).fillMaxWidth()
                                ) {
                                    Icon(
                                        imageVector = if (viewModel.isTemporarySession) Icons.Default.ExitToApp else Icons.Default.PowerSettingsNew,
                                        contentDescription = null,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (viewModel.isTemporarySession) "Exit Sandbox" else "Seal Vault",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                    )
                                }
                            }
                        }

                        // Card B: Journal Vault Gateway Card
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .height(136.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .clickable { onOpenJournals() },
                            shape = RoundedCornerShape(20.dp),
                            colors = cardColors,
                            border = androidx.compose.foundation.BorderStroke(1.dp, outlineVariant)
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.Top
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Book,
                                        contentDescription = null,
                                        tint = primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Journal Vault",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = "Read or browse your private thoughts and daily reflections.",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp, lineHeight = 13.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    modifier = Modifier.heightIn(min = 26.dp)
                                )

                                Spacer(modifier = Modifier.weight(1f))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val journalSessionsCount = remember(viewModel.savedSessions.toList()) {
                                        viewModel.savedSessions.count { it.tags.any { tag -> tag.id == "journal" } }
                                    }
                                    Text(
                                        text = "$journalSessionsCount entries",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold, fontSize = 9.sp),
                                        color = primary
                                    )
                                    Icon(
                                        imageVector = Icons.Default.ArrowForward,
                                        contentDescription = null,
                                        tint = primary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3. SECURE DOCUMENT LIBRARY (Sleek Compact Card)
            item {
                StaggeredEntrance(enabled = animateEntrance, delayMillis = 120) { animationModifier ->
                    Card(
                        modifier = animationModifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onAttachDocumentClick() },
                        shape = RoundedCornerShape(16.dp),
                        colors = cardColors,
                        border = androidx.compose.foundation.BorderStroke(1.dp, outlineVariant)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(primary.copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Folder,
                                        contentDescription = null,
                                        tint = primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Secure Document Library",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (viewModel.attachedFiles.isEmpty()) "No files staged" else "${viewModel.attachedFiles.size} files staged for chat",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                    )
                                }
                            }

                            Button(
                                onClick = onAttachDocumentClick,
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = primary.copy(alpha = 0.12f),
                                    contentColor = primary
                                ),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Import", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                            }
                        }
                    }
                }
            }

            // 4. INTERACTIVE CHAT HUB
            item {
                StaggeredEntrance(enabled = animateEntrance, delayMillis = 180) { animationModifier ->
                    Column(modifier = animationModifier) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "SECURE CONVERSATIONS",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                    fontSize = 11.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(0.5.dp)
                                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                            )
                        }
                    }
                }
            }

            // Horizontal Tag Pills Scroll
            if (viewModel.allTags.isNotEmpty()) {
                item {
                    StaggeredEntrance(enabled = animateEntrance, delayMillis = 240) { animationModifier ->
                        Row(
                            modifier = animationModifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            viewModel.allTags.forEach { tag ->
                                val isSelected = selectedTags.contains(tag.id)
                                val tagColor = try { Color(android.graphics.Color.parseColor(tag.colorHex)) } catch (e: Exception) { Color.Gray }

                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = if (isSelected) tagColor else Color.Transparent,
                                    border = androidx.compose.foundation.BorderStroke(
                                        width = 1.dp,
                                        color = if (isSelected) Color.Transparent else tagColor.copy(alpha = 0.4f)
                                    ),
                                    modifier = Modifier.clickable {
                                        if (isSelected) {
                                            selectedTags.remove(tag.id)
                                        } else {
                                            selectedTags.add(tag.id)
                                        }
                                    }
                                ) {
                                    Text(
                                        text = tag.name,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (isSelected) Color.White else tagColor,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (viewModel.savedSessions.size > 10) {
                item(key = "search_bar") {
                    StaggeredEntrance(enabled = animateEntrance, delayMillis = 300) { animationModifier ->
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search chats...", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(
                                        onClick = { searchQuery = "" },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Clear",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f),
                                focusedBorderColor = primary.copy(alpha = 0.5f),
                                unfocusedBorderColor = outlineVariant,
                                cursorColor = primary
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = animationModifier
                                .fillMaxWidth()
                                .onFocusChanged { isSearchFocused = it.isFocused }
                                .padding(vertical = 4.dp)
                        )
                    }
                }
            }

            // List of Filtered Chats
            if (filteredSessions.isEmpty()) {
                item {
                    StaggeredEntrance(enabled = animateEntrance, delayMillis = 300) { animationModifier ->
                        Card(
                            modifier = animationModifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = cardColors,
                            border = androidx.compose.foundation.BorderStroke(0.5.dp, outlineVariant)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (selectedTags.isNotEmpty()) "No chats match the selected tags." else "No conversations found. Write a journal entry or tap the '+' icon in the top right to start a new chat.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            } else {
                itemsIndexed(filteredSessions) { index, session ->
                    val isEncrypted = session.encryptedKeyPassword != null
                    val isLocked = isEncrypted && !viewModel.activeSessionKeys.containsKey(session.id)
                    val itemDelay = 300 + (index * 40).coerceAtMost(160)
                    StaggeredEntrance(enabled = animateEntrance, delayMillis = itemDelay) { animationModifier ->
                        Card(
                            modifier = animationModifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { onLoadSession(session.id) },
                            shape = RoundedCornerShape(16.dp),
                            colors = cardColors,
                            border = androidx.compose.foundation.BorderStroke(
                                width = 0.5.dp,
                                color = outlineVariant
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        if (isEncrypted) {
                                            Icon(
                                                imageVector = if (isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                                                contentDescription = if (isLocked) "Encrypted Locked" else "Encrypted Unlocked",
                                                tint = if (isLocked) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f) else Color(0xFFF59E0B),
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                        }
                                        Text(
                                            text = session.title,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    val relativeTime = remember(session.lastActive) {
                                        val diff = System.currentTimeMillis() - session.lastActive
                                        when {
                                            diff < 60_000 -> "Just now"
                                            diff < 3600_000 -> "${diff / 60_000}m ago"
                                            diff < 86400_000 -> "${diff / 3600_000}h ago"
                                            else -> "${diff / 86400_000}d ago"
                                        }
                                    }
                                    Row(
                                         verticalAlignment = Alignment.CenterVertically,
                                         horizontalArrangement = Arrangement.spacedBy(6.dp)
                                     ) {
                                         Text(
                                             text = relativeTime,
                                             style = MaterialTheme.typography.labelSmall,
                                             color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                         )
                                         
                                         Spacer(modifier = Modifier.width(4.dp))
                                         
                                         val renameEnabled = !viewModel.isGenerating && !isLocked
                                         IconButton(
                                             onClick = { sessionToRename = session },
                                             enabled = renameEnabled,
                                             modifier = Modifier.size(24.dp)
                                         ) {
                                             Icon(
                                                 imageVector = Icons.Default.Edit,
                                                 contentDescription = "Rename",
                                                 tint = if (renameEnabled) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.22f),
                                                 modifier = Modifier.size(14.dp)
                                             )
                                         }
                                         
                                         if (isEncrypted && !isLocked) {
                                             IconButton(
                                                 onClick = {
                                                     viewModel.activeSessionKeys.remove(session.id)
                                                     if (viewModel.currentSessionId == session.id) {
                                                         viewModel.loadSession(session.id)
                                                     }
                                                     Toast.makeText(context, "Chat Locked", Toast.LENGTH_SHORT).show()
                                                 },
                                                 modifier = Modifier.size(24.dp)
                                             ) {
                                                 Icon(
                                                     imageVector = Icons.Default.Lock,
                                                     contentDescription = "Lock Chat",
                                                     tint = Color(0xFFF59E0B),
                                                     modifier = Modifier.size(14.dp)
                                                 )
                                             }
                                         } else if (!isEncrypted) {
                                             IconButton(
                                                 onClick = { onLockSession(session) },
                                                 modifier = Modifier.size(24.dp)
                                             ) {
                                                 Icon(
                                                     imageVector = Icons.Default.LockOpen,
                                                     contentDescription = "Encrypt Chat",
                                                     tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                                     modifier = Modifier.size(14.dp)
                                                 )
                                             }
                                         } else {
                                             Spacer(modifier = Modifier.size(24.dp))
                                         }
                                         
                                         IconButton(
                                             onClick = { sessionToDelete = session },
                                             modifier = Modifier.size(24.dp)
                                         ) {
                                             Icon(
                                                 imageVector = Icons.Default.Delete,
                                                 contentDescription = "Delete",
                                                 tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                                 modifier = Modifier.size(14.dp)
                                             )
                                         }
                                     }
                                }

                                if (session.tags.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        session.tags.forEach { tag ->
                                            val tagColor = try { Color(android.graphics.Color.parseColor(tag.colorHex)) } catch (e: Exception) { Color.Gray }
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(tagColor.copy(alpha = 0.1f))
                                                    .border(0.5.dp, tagColor.copy(alpha = 0.25f), RoundedCornerShape(6.dp))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = tag.name,
                                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                                                    color = tagColor
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

            // Bottom Spacing to clear the ChatInput
            item {
                Spacer(modifier = Modifier.height(bottomPadding + 16.dp))
            }
        }

        if (sessionToDelete != null) {
            DeleteSessionDialog(
                session = sessionToDelete!!,
                viewModel = viewModel,
                context = context,
                onDismiss = { sessionToDelete = null }
            )
        }

        if (sessionToRename != null) {
            RenameSessionDialog(
                session = sessionToRename!!,
                viewModel = viewModel,
                onDismiss = { sessionToRename = null }
            )
        }
    }
}

private fun Modifier.fadingEdges(
    topBoundaryPx: Float,
    topFadePx: Float
): Modifier = this.graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
    .drawWithContent {
        drawContent()
        
        val height = size.height
        if (height <= 0f || topFadePx <= 0f) return@drawWithContent
        
        val fadeEnd = topBoundaryPx + topFadePx
        
        drawRect(
            brush = Brush.verticalGradient(
                0f to Color.Transparent,
                (topBoundaryPx / height).coerceIn(0f, 1f) to Color.Transparent,
                (fadeEnd / height).coerceIn(0f, 1f) to Color.Black,
                1f to Color.Black
            ),
            blendMode = androidx.compose.ui.graphics.BlendMode.DstIn
        )
    }

@Composable
private fun StaggeredEntrance(
    enabled: Boolean,
    delayMillis: Int,
    content: @Composable (modifier: Modifier) -> Unit
) {
    // Keep composition path identical to prevent node recreation and focus loss.
    // If initially not enabled, start animProgress at 1f.
    val animProgress = remember { Animatable(if (enabled) 0f else 1f) }

    LaunchedEffect(enabled) {
        if (enabled && animProgress.value < 1f) {
            delay(delayMillis.toLong())
            animProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = 350,
                    easing = CubicBezierEasing(0.215f, 0.61f, 0.355f, 1f)
                )
            )
        }
    }

    val alpha = animProgress.value
    val density = LocalDensity.current
    val slideY = with(density) { (-20).dp.toPx() * (1f - animProgress.value) }

    content(
        Modifier
            .alpha(alpha)
            .graphicsLayer {
                translationY = slideY
            }
    )
}


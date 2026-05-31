package com.rajpawardotin.kosh.ui.chat.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Label
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rajpawardotin.kosh.domain.model.ChatTag
import com.rajpawardotin.kosh.ui.chat.ChatViewModel

@Composable
fun ManageTagsDialog(
    viewModel: ChatViewModel,
    onDismiss: () -> Unit
) {
    var newTagName by remember { mutableStateOf("") }
    
    // 20 Curated distinctive color options for tags
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
    var selectedColor by remember { mutableStateOf(colorOptions[10]) } // default to Electric Purple (#8B5CF6)

    val activeSessionTags = viewModel.activeSessionTags
    val allTags = viewModel.allTags

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        title = { 
            Text(
                text = "Manage Session Tags", 
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            ) 
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Section: Existing Tags Checkbox List
                Text(
                    text = "SELECT TAGS FOR THIS CHAT",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                if (allTags.isEmpty()) {
                    Text(
                        text = "No custom tags created yet. Create one below.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                } else {
                    Box(modifier = Modifier.heightIn(max = 160.dp)) {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(allTags) { tag ->
                                val isAssociated = activeSessionTags.any { it.id == tag.id }
                                val tagColor = remember(tag.colorHex) {
                                    try { Color(android.graphics.Color.parseColor(tag.colorHex)) } catch (e: Exception) { Color.Gray }
                                }
                                
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            if (isAssociated) {
                                                viewModel.removeTagFromActiveSession(tag.name)
                                            } else {
                                                viewModel.addTagToActiveSession(tag.name)
                                            }
                                        }
                                        .padding(vertical = 4.dp, horizontal = 8.dp)
                                ) {
                                    Checkbox(
                                        checked = isAssociated,
                                        onCheckedChange = { checked ->
                                            if (checked) {
                                                viewModel.addTagToActiveSession(tag.name)
                                            } else {
                                                viewModel.removeTagFromActiveSession(tag.name)
                                            }
                                        },
                                        colors = CheckboxDefaults.colors(
                                            checkedColor = tagColor,
                                            checkmarkColor = Color.White
                                        )
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
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
                                }
                            }
                        }
                    }
                }
                
                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                
                // Section: Create Tag on the Fly with Live Preview
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "CREATE NEW TAG",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    val selectedColorParsed = remember(selectedColor) {
                        try { Color(android.graphics.Color.parseColor(selectedColor)) } catch (e: Exception) { Color.Gray }
                    }
                    
                    // Premium dynamic tag preview
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
                
                OutlinedTextField(
                    value = newTagName,
                    onValueChange = { newTagName = it },
                    placeholder = { Text("Enter tag name...") },
                    leadingIcon = {
                        val selectedColorParsed = remember(selectedColor) {
                            try { Color(android.graphics.Color.parseColor(selectedColor)) } catch (e: Exception) { Color.Gray }
                        }
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
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Slideable Carousel Color Picker Row
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
                
                val selectedColorParsed = remember(selectedColor) {
                    try { Color(android.graphics.Color.parseColor(selectedColor)) } catch (e: Exception) { Color.Gray }
                }
                Button(
                    onClick = {
                        if (newTagName.isNotBlank()) {
                            viewModel.createTag(newTagName, selectedColor)
                            // Auto associate the newly created tag
                            viewModel.addTagToActiveSession(newTagName)
                            newTagName = ""
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = selectedColorParsed,
                        contentColor = Color.White,
                        disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                        disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = newTagName.isNotBlank()
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add & Associate Tag", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
        }
    )
}

data class TagWarningInfo(
    val title: String,
    val message: String,
    val onConfirm: () -> Unit
)


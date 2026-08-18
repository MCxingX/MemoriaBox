package com.memoriabox.ui.screen.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.memoriabox.data.model.Event
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.Alignment

@Composable
fun BatchSelectDialog(
    events: List<Event>,
    selectedEvents: Set<String>,
    onSelectionChange: (Set<String>) -> Unit,
    onDismiss: () -> Unit,
    onBatchDelete: () -> Unit,
    onBatchMove: () -> Unit,
    onBatchEdit: () -> Unit
) {
    val allEventIds = remember(events) { events.map { it.id }.toSet() }
    val allSelected = events.isNotEmpty() && selectedEvents.containsAll(allEventIds)
    var showDeleteConfirm by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("批量操作 (${selectedEvents.size} 个已选)") },
        text = {
            Column {
                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ActionChip(
                        icon = if (allSelected) Icons.Default.Clear else Icons.Default.SelectAll,
                        label = if (allSelected) "取消全选" else "全选",
                        onClick = {
                            onSelectionChange(if (allSelected) emptySet() else allEventIds)
                        },
                        enabled = events.isNotEmpty()
                    )
                    ActionChip(
                        icon = Icons.Default.Delete,
                        label = "删除",
                        onClick = { showDeleteConfirm = true },
                        enabled = selectedEvents.isNotEmpty()
                    )
                    ActionChip(
                        icon = Icons.Default.FolderOpen,
                        label = "移动",
                        onClick = onBatchMove,
                        enabled = selectedEvents.isNotEmpty()
                    )
                    ActionChip(
                        icon = Icons.Default.Edit,
                        label = "编辑",
                        onClick = onBatchEdit,
                        enabled = selectedEvents.size == 1
                    )
                }
                
                Spacer(Modifier.height(16.dp))
                
                // Event list
                Text("选择事件", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(8.dp))
                
                LazyColumn(
                    modifier = Modifier.heightIn(max = 300.dp)
                ) {
                    items(events) { event ->
                        SelectableEventItem(
                            event = event,
                            isSelected = selectedEvents.contains(event.id),
                            onToggle = {
                                val newSet = if (selectedEvents.contains(event.id)) {
                                    selectedEvents - event.id
                                } else {
                                    selectedEvents + event.id
                                }
                                onSelectionChange(newSet)
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text("完成") }
        }
    )

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("批量删除") },
            text = { Text("确定要删除选中的 ${selectedEvents.size} 个事件吗？此操作不可恢复。") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    onBatchDelete()
                }) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("取消") }
            }
        )
    }
}

@Composable
fun ActionChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    enabled: Boolean
) {
    AssistChip(
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = {
            Icon(
                icon,
                null,
                modifier = Modifier.size(16.dp),
                tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        },
        enabled = enabled
    )
}

@Composable
fun SelectableEventItem(
    event: Event,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = isSelected,
            onCheckedChange = { onToggle() }
        )
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(event.name, style = MaterialTheme.typography.bodyLarge)
            Text(
                "${event.type} · ${com.memoriabox.ui.screen.components.formatDate(event.date)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun MoveToBoxDialog(
    boxes: List<com.memoriabox.data.model.Box>,
    onDismiss: () -> Unit,
    onBoxSelected: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("移动到分类") },
        text = {
            LazyColumn {
                items(boxes) { box ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { onBoxSelected(box.id) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(MaterialTheme.shapes.small)
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (box.icon.isImageUri()) {
                                        AsyncImage(
                                            model = box.icon,
                                            contentDescription = box.name,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Text(box.icon, style = MaterialTheme.typography.headlineSmall)
                                    }
                                }
                                Text(box.name, style = MaterialTheme.typography.bodyLarge)
                            }
                            Icon(
                                Icons.Default.ChevronRight,
                                null,
                                tint = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text("取消") }
        }
    )
}

private fun String.isImageUri(): Boolean = startsWith("content://") || startsWith("file://")

package com.memoriabox.ui.screen.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
    var showActionMenu by remember { mutableStateOf(false) }

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
                        icon = Icons.Default.Delete,
                        label = "删除",
                        onClick = onBatchDelete,
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

    if (showActionMenu) {
        // Action menu dialog
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
        title = { Text("移动到盒子") },
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
                                Text(
                                    box.icon,
                                    style = MaterialTheme.typography.headlineSmall
                                )
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

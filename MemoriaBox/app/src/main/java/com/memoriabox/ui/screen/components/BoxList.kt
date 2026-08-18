package com.memoriabox.ui.screen.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.memoriabox.data.model.Box
import com.memoriabox.data.model.Event
import com.memoriabox.data.model.LogEntry
import com.memoriabox.data.model.TodoPriority
import com.memoriabox.data.model.TodoStatus
import com.memoriabox.data.model.TodoSubtask
import com.memoriabox.utils.ColorUtils
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun BoxList(
    boxes: List<Box>,
    onBoxClick: (String) -> Unit,
    onCreateBox: () -> Unit = {},
    modifier: Modifier = Modifier,
    showCreateButton: Boolean = true
) {
    Column(modifier = modifier.padding(horizontal = 16.dp)) {
        boxes.forEach { box ->
            BoxCard(box = box, onClick = { onBoxClick(box.id) })
            Spacer(Modifier.height(8.dp))
        }
        if (showCreateButton) {
            Button(onClick = onCreateBox, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Add, null)
                Spacer(Modifier.width(8.dp))
                Text("新建分类")
            }
        }
    }
}

@Composable
fun BoxCard(box: Box, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = ColorUtils.hexToColor(box.bgValue).copy(alpha = 0.15f)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(MaterialTheme.shapes.medium)
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
                    Text(box.icon, style = MaterialTheme.typography.headlineMedium)
                }
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(box.name, style = MaterialTheme.typography.titleLarge)
                Text("查看这一类日子", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

private fun String.isImageUri(): Boolean = startsWith("content://") || startsWith("file://")

@Composable
fun TodoListView(
    events: List<Event>,
    subtaskMap: Map<String, List<TodoSubtask>>,
    onToggleStatus: (Event) -> Unit,
    onUpdatePriority: (Event, TodoPriority) -> Unit,
    onAddSubtask: (String, String) -> Unit,
    onToggleSubtask: (TodoSubtask) -> Unit,
    onDeleteSubtask: (TodoSubtask) -> Unit,
    isOverdue: (Event) -> Boolean,
    modifier: Modifier = Modifier
) {
    if (events.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(64.dp), tint = ColorUtils.hexToColor("#CCCCCC"))
                Spacer(Modifier.height(16.dp))
                Text("暂无待办事项", style = MaterialTheme.typography.bodyLarge, color = ColorUtils.hexToColor("#CCCCCC"))
            }
        }
    } else {
        LazyColumn(modifier = modifier) {
            items(events, key = { it.id }) { event ->
                TodoCard(
                    event = event,
                    subtasks = subtaskMap[event.id].orEmpty(),
                    onToggleStatus = { onToggleStatus(event) },
                    onUpdatePriority = { onUpdatePriority(event, it) },
                    onAddSubtask = { onAddSubtask(event.id, it) },
                    onToggleSubtask = onToggleSubtask,
                    onDeleteSubtask = onDeleteSubtask,
                    overdue = isOverdue(event)
                )
            }
        }
    }
}

private val priorityTint: Map<TodoPriority, Color> = mapOf(
    TodoPriority.HIGH to Color(0xFFE53935),
    TodoPriority.MEDIUM to Color(0xFFFB8C00),
    TodoPriority.LOW to Color(0xFF43A047)
)

private val priorityLabel: Map<TodoPriority, String> = mapOf(
    TodoPriority.HIGH to "高",
    TodoPriority.MEDIUM to "中",
    TodoPriority.LOW to "低"
)

@Composable
private fun TodoCard(
    event: Event,
    subtasks: List<TodoSubtask>,
    onToggleStatus: () -> Unit,
    onUpdatePriority: (TodoPriority) -> Unit,
    onAddSubtask: (String) -> Unit,
    onToggleSubtask: (TodoSubtask) -> Unit,
    onDeleteSubtask: (TodoSubtask) -> Unit,
    overdue: Boolean
) {
    var expanded by remember { mutableStateOf(false) }
    var showPriorityMenu by remember { mutableStateOf(false) }
    var newSubtask by remember { mutableStateOf("") }
    var pendingSubtask by remember { mutableStateOf<TodoSubtask?>(null) }
    val completed = event.todoStatus == TodoStatus.COMPLETED

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(checked = completed, onCheckedChange = { onToggleStatus() }, modifier = Modifier.semantics { contentDescription = if (completed) "标记为未完成" else "标记为已完成" })
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = event.name,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (completed) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        else MaterialTheme.colorScheme.onSurface
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        event.dueDate?.let { due ->
                            val overdueNow = overdue
                            Text(
                                "截止: ${SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(due))}",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (overdueNow) Color(0xFFE53935) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (overdue) {
                            Text(
                                "已逾期",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                modifier = Modifier
                                    .clip(MaterialTheme.shapes.small)
                                    .background(Color(0xFFE53935))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Box {
                    IconButton(onClick = { showPriorityMenu = true }) {
                        Text(
                            priorityLabel[event.todoPriority] ?: "中",
                            style = MaterialTheme.typography.labelMedium,
                            color = priorityTint[event.todoPriority] ?: MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    DropdownMenu(expanded = showPriorityMenu, onDismissRequest = { showPriorityMenu = false }) {
                        TodoPriority.entries.forEach { p ->
                            DropdownMenuItem(
                                text = { Text("${priorityLabel[p]} 优先级") },
                                onClick = {
                                    onUpdatePriority(p)
                                    showPriorityMenu = false
                                }
                            )
                        }
                    }
                }
                Spacer(Modifier.width(4.dp))
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = "展开子任务"
                    )
                }
            }
            if (expanded) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    subtasks.forEach { subtask ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = subtask.done,
                                onCheckedChange = { onToggleSubtask(subtask) }
                            )
                            Text(
                                subtask.title,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (subtask.done) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { pendingSubtask = subtask }) {
                                Icon(Icons.Default.Close, contentDescription = "删除子任务", tint = MaterialTheme.colorScheme.outline)
                            }
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = newSubtask,
                            onValueChange = { newSubtask = it },
                            placeholder = { Text("添加子任务") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            textStyle = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(Modifier.width(8.dp))
                        IconButton(onClick = {
                            if (newSubtask.isNotBlank()) {
                                onAddSubtask(newSubtask.trim())
                                newSubtask = ""
                            }
                        }) {
                            Icon(Icons.Default.Add, contentDescription = "添加子任务")
                        }
                    }
                }
            }
        }
    }
    pendingSubtask?.let { subtask ->
        AlertDialog(
            onDismissRequest = { pendingSubtask = null },
            title = { Text("删除子任务") },
            text = { Text("确定要删除「${subtask.title}」吗？此操作不可恢复。") },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteSubtask(subtask)
                    pendingSubtask = null
                }) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { pendingSubtask = null }) { Text("取消") }
            }
        )
    }
}

@Composable
fun LogsList(logs: List<LogEntry>) {
    LazyColumn {
        items(logs) { log ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(log.operation, style = MaterialTheme.typography.titleSmall)
                    Text(
                        "${log.targetName} - ${log.result}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    log.extra?.let { extra ->
                        Text(extra, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

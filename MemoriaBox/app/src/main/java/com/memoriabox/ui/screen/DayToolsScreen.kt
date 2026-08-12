package com.memoriabox.ui.screen

import android.app.Application
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.*
import com.memoriabox.ui.navigation.Screen
import com.memoriabox.ui.screen.components.*
import com.memoriabox.ui.screen.dialogs.EventDialog
import com.memoriabox.data.model.*
import com.memoriabox.ui.theme.group
import com.memoriabox.viewmodel.*
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date

@Composable
fun DayToolsScreen(
    application: Application,
    onNavigateBack: () -> Unit,
    onNavigateToCalendar: () -> Unit,
    onNavigateToPhotoWall: () -> Unit,
    onNavigateToExport: () -> Unit
) {
    val viewModel = remember { createMainViewModel(application) }
    val boxes by viewModel.boxes.collectAsState(initial = emptyList())
    val events by viewModel.allEvents.collectAsState(initial = emptyList())
    val logs by viewModel.recentLogs.collectAsState(initial = emptyList())
    val snackbarHostState = remember { SnackbarHostState() }
    val snackbarScope = rememberCoroutineScope()
    var showBatchImport by remember { mutableStateOf(false) }
    var selectedEvent by remember { mutableStateOf<Event?>(null) }
    var editingEvent by remember { mutableStateOf<Event?>(null) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val defaultBoxId = boxes.firstOrNull()?.id ?: ""
    val archivedSuggestions = remember(events) {
        val cutoff = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -30) }.timeInMillis
        events.filter { it.date < cutoff && !it.isPinned }
            .sortedByDescending { it.date }
            .take(8)
    }
    val filteredEvents = remember(events, searchQuery) {
        if (searchQuery.isBlank()) emptyList() else events.filter {
            it.name.contains(searchQuery, ignoreCase = true) || it.note.contains(searchQuery, ignoreCase = true)
        }.take(20)
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("日子工具箱") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            ToolSection("批量导入", "把多条日期一次性加入日子列表") {
                ToolActionRow(
                    title = "打开批量导入",
                    subtitle = "支持：木羽生日 6月24日 / 考试 2026-07-10",
                    icon = Icons.Default.Upload,
                    onClick = {
                        if (defaultBoxId.isBlank()) {
                            snackbarScope.launch {
                                snackbarHostState.showSnackbar("请先创建一个分类，再使用批量导入。")
                            }
                        } else {
                            showBatchImport = true
                        }
                    },
                    featured = true
                )
            }
            ToolSection("搜索和筛选", "按名称或备注查找日子") {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("搜索日子") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                when {
                    searchQuery.isBlank() -> Text("输入名称或备注后开始查找。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    filteredEvents.isEmpty() -> Text("未找到相关日子", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    else -> filteredEvents.forEach { event -> CompactToolEventRow(event, onClick = { selectedEvent = event }) }
                }
            }
            ToolSection("较早未置顶", "查看过去较久且仍未置顶的日子") {
                if (archivedSuggestions.isEmpty()) {
                    Text("暂无较早未置顶的日子", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    archivedSuggestions.forEach { event -> CompactToolEventRow(event, onClick = { selectedEvent = event }) }
                }
            }
            ToolSection("分享和看板", "分享海报、照片墙、导出和日历看板入口") {
                ToolActionRow("分享海报", "生成社交分享图片", Icons.Default.PhotoLibrary, onNavigateToPhotoWall)
                ToolActionRow("导出分享", "导出数据和分享内容", Icons.Default.Share, onNavigateToExport)
                ToolActionRow("月历看板", "查看所有事件分布", Icons.Default.CalendarToday, onNavigateToCalendar)
            }
        }
    }

    if (showBatchImport) {
        BatchImportDialog(
            onDismiss = { showBatchImport = false },
            onImport = { text ->
                val parsedEvents = parseBatchEvents(text, defaultBoxId)
                when {
                    defaultBoxId.isBlank() -> {
                        snackbarScope.launch {
                            snackbarHostState.showSnackbar("请先创建一个分类，再使用批量导入。")
                        }
                    }
                    parsedEvents.isEmpty() -> {
                        snackbarScope.launch {
                            snackbarHostState.showSnackbar("没有识别到可导入的日期。")
                        }
                    }
                    else -> {
                        parsedEvents.forEach { viewModel.createQuickEvent(it) }
                        snackbarScope.launch {
                            snackbarHostState.showSnackbar("已导入 ${parsedEvents.size} 条日子。")
                        }
                    }
                }
                showBatchImport = false
            }
        )
    }

    selectedEvent?.let { event ->
        EventDetailDialog(
            event = event,
            logs = logs.filter { it.targetId == event.id },
            onDismiss = { selectedEvent = null },
            onEdit = {
                selectedEvent = null
                editingEvent = event
            },
            onTogglePin = {
                viewModel.togglePinned(event)
                selectedEvent = null
            },
            onDelete = {
                viewModel.deleteQuickEvent(event)
                selectedEvent = null
            },
            onOpenCategory = {
                selectedEvent = null
            }
        )
    }

    editingEvent?.let { event ->
        EventDialog(
            existingEvent = event,
            availableBoxes = boxes,
            onDismiss = { editingEvent = null },
            onSave = { updated ->
                viewModel.updateQuickEvent(updated)
                editingEvent = null
            }
        )
    }
}

@Composable
private fun ToolSection(title: String, subtitle: String, content: @Composable ColumnScope.() -> Unit) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            content()
        }
    }
}

@Composable
private fun ToolActionRow(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit, featured: Boolean = false) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = if (featured) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f) else Color.Transparent
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = if (featured) 10.dp else 0.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyMedium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null)
        }
    }
}

@Composable
private fun CompactToolEventRow(event: Event, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 9.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(event.name, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                Text(eventTypeLabel(event.type), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(com.memoriabox.ui.screen.components.formatDate(event.date), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun BatchImportDialog(onDismiss: () -> Unit, onImport: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("批量导入日子") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("每行一个日子，例如：木羽生日 6月24日", style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    minLines = 6,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("旅行 2026-08-01\n木羽生日 6月24日") }
                )
            }
        },
        confirmButton = { TextButton(onClick = { onImport(text) }) { Text("导入") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

private fun parseBatchEvents(text: String, boxId: String): List<Event> {
    return text.lines().mapNotNull { line ->
        val trimmed = line.trim()
        if (trimmed.isBlank()) return@mapNotNull null
        val dateRegex = Regex("(\\d{4}-\\d{1,2}-\\d{1,2}|\\d{1,2}月\\d{1,2}日)")
        val match = dateRegex.find(trimmed) ?: return@mapNotNull null
        val name = trimmed.removeRange(match.range).trim().ifBlank { "新日子" }
        Event(
            boxId = boxId,
            name = name,
            date = parseFlexibleDate(match.value) ?: return@mapNotNull null,
            type = if (name.contains("生日")) EventType.BIRTHDAY else EventType.COUNTDOWN,
            reminderEnabled = true,
            reminderOffsets = "0,1,7"
        )
    }
}

private fun parseFlexibleDate(value: String): Long? {
    val cal = Calendar.getInstance()
    val year: Int
    val month: Int
    val day: Int
    try {
        if (value.contains("-")) {
            val parts = value.split("-").map { it.toInt() }
            if (parts.size != 3) return null
            year = parts[0]
            month = parts[1]
            day = parts[2]
        } else {
            val parts = Regex("(\\d{1,2})月(\\d{1,2})日").find(value)?.groupValues ?: return null
            year = cal.get(Calendar.YEAR)
            month = parts[1].toInt()
            day = parts[2].toInt()
        }
    } catch (_: NumberFormatException) {
        return null
    }
    if (month !in 1..12 || day !in 1..31) return null
    cal.isLenient = false
    return try {
        cal.set(year, month - 1, day, 9, 0, 0)
        cal.set(Calendar.MILLISECOND, 0)
        cal.timeInMillis
    } catch (_: IllegalArgumentException) {
        null
    }
}


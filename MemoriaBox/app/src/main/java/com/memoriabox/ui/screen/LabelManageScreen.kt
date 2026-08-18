package com.memoriabox.ui.screen

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Label
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.memoriabox.data.model.Event
import com.memoriabox.data.model.Label
import com.memoriabox.utils.ColorUtils
import com.memoriabox.viewmodel.createLabelViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class LabelTab(val label: String) {
    MANAGE("标签管理"),
    EVENTS("给日子打标签")
}

@Composable
fun LabelManageScreen(
    application: Application,
    onNavigateBack: () -> Unit
) {
    val viewModel = remember { createLabelViewModel(application) }
    val labels by viewModel.labels.collectAsState(initial = emptyList())
    val allEvents by viewModel.allEvents.collectAsState(initial = emptyList())
    val eventLabelsMap by viewModel.eventLabelsMap.collectAsState(initial = emptyMap())
    var selectedTab by rememberSaveable { mutableStateOf(LabelTab.MANAGE) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var deletingLabel by remember { mutableStateOf<Label?>(null) }
    var taggingEvent by remember { mutableStateOf<Event?>(null) }
    LaunchedEffect(Unit) { viewModel.refreshEventLabels() }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("标签管理") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        floatingActionButton = {
            if (selectedTab == LabelTab.MANAGE) {
                FloatingActionButton(onClick = { showCreateDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "新建标签")
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LabelTab.entries.forEach { tab ->
                    FilterChip(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        label = { Text(tab.label) }
                    )
                }
            }
            when (selectedTab) {
                LabelTab.MANAGE -> LabelManageList(
                    labels = labels,
                    onDelete = { deletingLabel = it },
                    onCreate = { showCreateDialog = true }
                )
                LabelTab.EVENTS -> EventLabelList(
                    events = allEvents,
                    eventLabelsMap = eventLabelsMap,
                    onClick = { taggingEvent = it }
                )
            }
        }
    }

    if (showCreateDialog) {
        LabelCreateDialog(
            existingColors = labels.map { it.color }.toSet(),
            onDismiss = { showCreateDialog = false },
            onSave = { name, color ->
                viewModel.createLabel(name, color)
                showCreateDialog = false
            }
        )
    }

    deletingLabel?.let { label ->
        AlertDialog(
            onDismissRequest = { deletingLabel = null },
            title = { Text("删除标签") },
            text = { Text("确认删除“${label.name}”？会同时移除该标签与日子的关联。") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteLabel(label)
                    deletingLabel = null
                }) { Text("删除") }
            },
            dismissButton = {
                TextButton(onClick = { deletingLabel = null }) { Text("取消") }
            }
        )
    }

    taggingEvent?.let { event ->
        EventTagDialog(
            event = event,
            labels = labels,
            currentLabels = eventLabelsMap[event.id].orEmpty().toSet(),
            onDismiss = { taggingEvent = null },
            onSave = { selected ->
                viewModel.setEventLabels(event.id, selected)
                taggingEvent = null
            }
        )
    }
}

@Composable
private fun LabelManageList(
    labels: List<Label>,
    onDelete: (Label) -> Unit,
    onCreate: () -> Unit
) {
    if (labels.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Text(
                    "还没有标签，点右下角新建标签，之后可以在给日子打标签时使用。",
                    modifier = Modifier.padding(20.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(labels, key = { it.name }) { label ->
                LabelManageRow(
                    label = label,
                    onDelete = { onDelete(label) }
                )
            }
        }
    }
}

@Composable
private fun EventLabelList(
    events: List<Event>,
    eventLabelsMap: Map<String, List<String>>,
    onClick: (Event) -> Unit
) {
    if (events.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Text(
                    "还没有任何日子，先在日子页添加后再来打标签。",
                    modifier = Modifier.padding(20.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(events, key = { it.id }) { event ->
                EventLabelRow(
                    event = event,
                    labels = eventLabelsMap[event.id].orEmpty(),
                    onClick = { onClick(event) }
                )
            }
        }
    }
}

@Composable
private fun LabelManageRow(label: Label, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(runCatching { ColorUtils.hexToColor(label.color) }.getOrDefault(Color(0xFF7C4DFF)))
                )
                Text(label.name, style = MaterialTheme.typography.titleMedium)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "删除", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun EventLabelRow(event: Event, labels: List<String>, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(event.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    SimpleDateFormat("yyyy年M月d日", Locale.getDefault()).format(Date(event.date)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (labels.isEmpty()) {
                    Text("未打标签", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        labels.take(4).forEach { name ->
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer
                            ) {
                                Text(
                                    name,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        if (labels.size > 4) {
                            Text("+${labels.size - 4}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            Icon(Icons.Default.Label, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EventTagDialog(
    event: Event,
    labels: List<Label>,
    currentLabels: Set<String>,
    onDismiss: () -> Unit,
    onSave: (Set<String>) -> Unit
) {
    val selected = remember(event.id, currentLabels) { currentLabels.toMutableStateList() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("给日子打标签") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(event.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    SimpleDateFormat("yyyy年M月d日", Locale.getDefault()).format(Date(event.date)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (labels.isEmpty()) {
                    Text(
                        "还没有可用标签，请先在标签管理页新建标签。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        labels.forEach { label ->
                            val isSelected = selected.contains(label.name)
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    if (isSelected) selected.remove(label.name)
                                    else selected.add(label.name)
                                },
                                label = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(runCatching { ColorUtils.hexToColor(label.color) }.getOrDefault(Color(0xFF7C4DFF)))
                                        )
                                        Text(label.name)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(selected.toSet()) }) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LabelCreateDialog(
    existingColors: Set<String>,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var color by remember { mutableStateOf("#7C4DFF") }
    val palette = listOf(
        "#7C4DFF", "#F44336", "#E91E63", "#9C27B0", "#673AB7",
        "#2196F3", "#03A9F4", "#00BCD4", "#009688", "#4CAF50",
        "#8BC34A", "#FFEB3B", "#FFC107", "#FF9800", "#FF5722", "#795548"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("新建标签") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("标签名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    palette.forEach { hex ->
                        val selected = color == hex
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .border(2.dp, if (selected) MaterialTheme.colorScheme.primary else Color.Transparent, CircleShape)
                                .background(runCatching { ColorUtils.hexToColor(hex) }.getOrDefault(Color.Gray))
                                .clickable { color = hex }
                                .semantics { contentDescription = "选择颜色 $hex" }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(name.trim(), color) }, enabled = name.isNotBlank()) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

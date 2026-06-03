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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.memoriabox.data.model.Box
import com.memoriabox.data.model.Event
import com.memoriabox.data.model.CardLayoutMode
import com.memoriabox.data.model.LogEntry
import com.memoriabox.data.model.TodoStatus
import com.memoriabox.data.model.Friend
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
    onToggleStatus: (Event) -> Unit,
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
            items(events) { event ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = event.todoStatus == TodoStatus.COMPLETED,
                            onCheckedChange = { onToggleStatus(event) }
                        )
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(
                                text = event.name,
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (event.todoStatus == TodoStatus.COMPLETED) {
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                }
                            )
                            event.dueDate?.let { due ->
                                val cal = Calendar.getInstance()
                                cal.timeInMillis = due
                                Text(
                                    "截止: ${SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FriendListView(
    friends: List<Pair<Friend, List<String>>>,
    onFriendClick: (Friend) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier) {
        items(friends) { (friend, labels) ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .clickable { onFriendClick(friend) }
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(MaterialTheme.shapes.large)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!friend.avatarUri.isNullOrBlank()) {
                            AsyncImage(
                                model = friend.avatarUri,
                                contentDescription = friend.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Text(
                                text = friend.name.firstOrNull()?.toString() ?: "F",
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(friend.name, style = MaterialTheme.typography.titleMedium)
                        if (labels.isNotEmpty()) {
                            Row {
                                labels.forEach { label ->
                                    Surface(
                                        color = MaterialTheme.colorScheme.secondaryContainer,
                                        shape = MaterialTheme.shapes.small,
                                        modifier = Modifier.padding(end = 4.dp)
                                    ) {
                                        Text(
                                            text = label,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            style = MaterialTheme.typography.labelSmall
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

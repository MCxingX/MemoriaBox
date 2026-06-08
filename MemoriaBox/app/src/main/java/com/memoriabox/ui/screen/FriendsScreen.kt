package com.memoriabox.ui.screen

import android.app.Application
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.memoriabox.data.model.Friend
import com.memoriabox.ui.theme.MemoriaDesign
import com.memoriabox.viewmodel.createFriendViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsScreen(
    application: Application,
    onNavigateBack: () -> Unit
) {
    val viewModel = remember { createFriendViewModel(application) }
    val friends by viewModel.friends.collectAsState()
    var editingFriend by remember { mutableStateOf<Friend?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var deletingFriend by remember { mutableStateOf<Friend?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("好友管理") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                editingFriend = null
                showEditor = true
            }) {
                Icon(Icons.Default.Add, contentDescription = "新增好友")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            FriendBirthdayGuide()
            Spacer(Modifier.height(16.dp))
            if (friends.isEmpty()) {
                EmptyFriendsCard(onAdd = {
                    editingFriend = null
                    showEditor = true
                })
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(friends, key = { it.id }) { friend ->
                        FriendCard(
                            friend = friend,
                            onEdit = {
                                editingFriend = friend
                                showEditor = true
                            },
                            onDelete = { deletingFriend = friend }
                        )
                    }
                }
            }
        }
    }

    if (showEditor) {
        FriendEditorDialog(
            friend = editingFriend,
            onDismiss = { showEditor = false },
            onSave = { name, birthday ->
                viewModel.saveFriend(editingFriend, name, birthday)
                showEditor = false
            }
        )
    }

    deletingFriend?.let { friend ->
        AlertDialog(
            onDismissRequest = { deletingFriend = null },
            title = { Text("删除好友") },
            text = { Text("确认删除“${friend.name}”？删除后好友管理列表会立即更新。") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteFriend(friend)
                    deletingFriend = null
                }) { Text("删除") }
            },
            dismissButton = {
                TextButton(onClick = { deletingFriend = null }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun FriendBirthdayGuide() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.58f)
    ) {
        Column(modifier = Modifier.padding(MemoriaDesign.spacing.cardPadding), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Cake, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("生日小册子", style = MaterialTheme.typography.titleMedium)
            }
            Text(
                "一个月内生日越近越靠前，超过一个月和未设置生日的好友会继续保留。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun FriendCard(friend: Friend, onEdit: () -> Unit, onDelete: () -> Unit) {
    val daysUntilBirthday = remember(friend.birthdayDate) { friend.birthdayDate?.let { nextBirthdayDistance(it) } }
    val isSoon = daysUntilBirthday != null && daysUntilBirthday in 0..30
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSoon) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f) else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(MemoriaDesign.spacing.cardPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.primaryContainer) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(8.dp)
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(friend.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(friendBirthdayLabel(friend), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, contentDescription = "编辑") }
            IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = "删除") }
        }
    }
}

@Composable
private fun EmptyFriendsCard(onAdd: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(modifier = Modifier.padding(MemoriaDesign.spacing.cardPadding), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Default.Cake, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text("还没有好友", style = MaterialTheme.typography.titleMedium)
            Text("记录亲友生日后，临近一个月的生日会自动排到前面。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(onClick = onAdd) { Text("新增好友") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FriendEditorDialog(friend: Friend?, onDismiss: () -> Unit, onSave: (String, Long?) -> Unit) {
    var name by remember(friend?.id) { mutableStateOf(friend?.name ?: "") }
    var birthday by remember(friend?.id) { mutableStateOf(friend?.birthdayDate) }
    var showDatePicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (friend == null) "新增好友" else "编辑好友") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("好友名称") }, singleLine = true)
                OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(birthday?.let { "生日：${formatBirthday(it)}" } ?: "选择生日（可选）")
                }
                if (birthday != null) {
                    TextButton(onClick = { birthday = null }) { Text("清除生日") }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(name, birthday) }, enabled = name.isNotBlank()) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )

    if (showDatePicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = birthday ?: System.currentTimeMillis())
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    birthday = state.selectedDateMillis
                    showDatePicker = false
                }) { Text("确定") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("取消") } }
        ) { DatePicker(state = state) }
    }
}

private fun friendBirthdayLabel(friend: Friend): String {
    val birthday = friend.birthdayDate ?: return "未设置生日，已保留在列表中"
    val distance = nextBirthdayDistance(birthday)
    val prefix = when {
        distance == 0 -> "今天生日"
        distance <= 30 -> "还有 ${distance} 天生日"
        else -> "超过一个月，还有 ${distance} 天"
    }
    return "$prefix · ${formatBirthday(birthday)}"
}

private fun nextBirthdayDistance(birthday: Long): Int {
    val today = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val source = Calendar.getInstance().apply { timeInMillis = birthday }
    val next = Calendar.getInstance().apply {
        timeInMillis = today.timeInMillis
        set(Calendar.MONTH, source.get(Calendar.MONTH))
        set(Calendar.DAY_OF_MONTH, source.get(Calendar.DAY_OF_MONTH))
    }
    if (next.before(today)) next.add(Calendar.YEAR, 1)
    return TimeUnit.MILLISECONDS.toDays(next.timeInMillis - today.timeInMillis).toInt()
}

private fun formatBirthday(timestamp: Long): String = SimpleDateFormat("M月d日", Locale.getDefault()).format(Date(timestamp))

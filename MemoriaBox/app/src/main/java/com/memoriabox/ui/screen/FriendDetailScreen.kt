package com.memoriabox.ui.screen

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.memoriabox.data.model.Friend
import com.memoriabox.data.model.FriendBirthdayRecord
import com.memoriabox.data.model.FriendGift
import com.memoriabox.data.model.GiftStatus
import com.memoriabox.viewmodel.createFriendDetailViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val giftStatusLabel: Map<GiftStatus, String> = mapOf(
    GiftStatus.PLANNED to "计划中",
    GiftStatus.PURCHASED to "已购买",
    GiftStatus.GIVEN to "已送出"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendDetailScreen(
    application: Application,
    friendId: String,
    onNavigateBack: () -> Unit
) {
    val viewModel = remember { createFriendDetailViewModel(application) }
    LaunchedEffect(friendId) { viewModel.load(friendId) }

    val friend by viewModel.friend.collectAsState()
    val relations by viewModel.relations.collectAsState(initial = emptyList())
    val gifts by viewModel.gifts.collectAsState(initial = emptyList())
    val birthdayRecords by viewModel.birthdayRecords.collectAsState(initial = emptyList())

    var showEditDialog by remember { mutableStateOf(false) }
    var showAddGift by remember { mutableStateOf(false) }
    var showAddRecord by remember { mutableStateOf(false) }
    var deletingGift by remember { mutableStateOf<FriendGift?>(null) }
    var deletingRecord by remember { mutableStateOf<FriendBirthdayRecord?>(null) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(friend?.name ?: "好友详情") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { showEditDialog = true }) {
                        Icon(Icons.Default.Edit, contentDescription = "编辑")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { paddingValues ->
        if (friend == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    FriendHeaderCard(friend = friend!!, relations = relations)
                }
                item {
                    SectionCard(
                        title = "礼物清单",
                        subtitle = "记录送过的礼物和计划",
                        onAdd = { showAddGift = true },
                        addLabel = "记礼物"
                    ) {
                        if (gifts.isEmpty()) {
                            Text("还没有礼物记录。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            gifts.sortedByDescending { it.year }.forEach { gift ->
                                GiftRow(gift = gift, onDelete = { deletingGift = gift })
                            }
                        }
                    }
                }
                item {
                    SectionCard(
                        title = "历年生日记录",
                        subtitle = "每一年生日怎么度过的",
                        onAdd = { showAddRecord = true },
                        addLabel = "记一笔"
                    ) {
                        if (birthdayRecords.isEmpty()) {
                            Text("还没有生日记录。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            birthdayRecords.sortedByDescending { it.year }.forEach { record ->
                                BirthdayRecordRow(record = record, onDelete = { deletingRecord = record })
                            }
                        }
                    }
                }
            }
        }
    }

    if (showEditDialog) {
        friend?.let { f ->
            FriendDetailEditDialog(
                friend = f,
                relations = relations,
                onDismiss = { showEditDialog = false },
                onSave = { name, birthday, newRelations ->
                    viewModel.updateFriend(name, birthday, f.avatarUri, newRelations)
                    showEditDialog = false
                }
            )
        }
    }
    if (showAddGift) {
        AddGiftDialog(
            onDismiss = { showAddGift = false },
            onSave = { name, price, status, year ->
                viewModel.addGift(name, price, status, year)
                showAddGift = false
            }
        )
    }
    if (showAddRecord) {
        AddBirthdayRecordDialog(
            onDismiss = { showAddRecord = false },
            onSave = { note ->
                viewModel.addBirthdayRecord(note)
                showAddRecord = false
            }
        )
    }
    deletingGift?.let { gift ->
        AlertDialog(
            onDismissRequest = { deletingGift = null },
            title = { Text("删除礼物") },
            text = { Text("确认删除礼物“${gift.name}”？") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteGift(gift)
                    deletingGift = null
                }) { Text("删除") }
            },
            dismissButton = {
                TextButton(onClick = { deletingGift = null }) { Text("取消") }
            }
        )
    }
    deletingRecord?.let { record ->
        AlertDialog(
            onDismissRequest = { deletingRecord = null },
            title = { Text("删除记录") },
            text = { Text("确认删除 ${record.year} 年的生日记录？") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteBirthdayRecord(record)
                    deletingRecord = null
                }) { Text("删除") }
            },
            dismissButton = {
                TextButton(onClick = { deletingRecord = null }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun FriendHeaderCard(friend: Friend, relations: List<String>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (friend.avatarUri != null) {
                AsyncImage(
                    model = friend.avatarUri,
                    contentDescription = friend.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(36.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
            } else {
                Surface(shape = RoundedCornerShape(36.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(72.dp)
                            .padding(18.dp)
                    )
                }
            }
            Text(friend.name, style = MaterialTheme.typography.titleLarge)
            Text(
                friend.birthdayDate?.let { "生日：${friendBirthdayLabel(friend)}" } ?: "未设置生日",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (relations.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    relations.forEach { label ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                label,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    subtitle: String,
    addLabel: String,
    onAdd: () -> Unit,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(title, style = MaterialTheme.typography.titleMedium)
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                TextButton(onClick = onAdd) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(addLabel)
                }
            }
            HorizontalDivider()
            content()
        }
    }
}

@Composable
private fun GiftRow(gift: FriendGift, onDelete: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(gift.name, style = MaterialTheme.typography.bodyLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (gift.price > 0) {
                    Text("¥%.0f".format(gift.price), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (gift.year > 0) {
                    Text("${gift.year}年", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = giftStatusColor(gift.status).copy(alpha = 0.15f)
                ) {
                    Text(
                        giftStatusLabel[gift.status] ?: "",
                        style = MaterialTheme.typography.labelSmall,
                        color = giftStatusColor(gift.status),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, contentDescription = "删除礼物", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun BirthdayRecordRow(record: FriendBirthdayRecord, onDelete: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("${record.year} 年", style = MaterialTheme.typography.bodyLarge)
            if (record.note.isNotBlank()) {
                Text(record.note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, contentDescription = "删除记录", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FriendDetailEditDialog(
    friend: Friend,
    relations: List<String>,
    onDismiss: () -> Unit,
    onSave: (String, Long?, List<String>) -> Unit
) {
    var name by remember(friend.id) { mutableStateOf(friend.name) }
    val initialBirthday = remember(friend.id) {
        friend.birthdayDate?.let { timestamp -> Calendar.getInstance().apply { timeInMillis = timestamp } }
    }
    var month by remember(friend.id) { mutableStateOf(initialBirthday?.get(Calendar.MONTH)?.plus(1)?.toString() ?: "") }
    var day by remember(friend.id) { mutableStateOf(initialBirthday?.get(Calendar.DAY_OF_MONTH)?.toString() ?: "") }
    var year by remember(friend.id) { mutableStateOf(initialBirthday?.get(Calendar.YEAR)?.takeIf { it != BIRTHDAY_UNKNOWN_YEAR }?.toString() ?: "") }
    var relationInput by remember(friend.id) { mutableStateOf(relations.joinToString("，")) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑好友") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("好友名称") }, singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(value = month, onValueChange = { month = it.filter(Char::isDigit).take(2) }, label = { Text("月份*") }, modifier = Modifier.weight(1f), singleLine = true)
                    OutlinedTextField(value = day, onValueChange = { day = it.filter(Char::isDigit).take(2) }, label = { Text("日期*") }, modifier = Modifier.weight(1f), singleLine = true)
                }
                OutlinedTextField(value = year, onValueChange = { year = it.filter(Char::isDigit).take(4) }, label = { Text("年份（可选）") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                TextButton(onClick = { month = ""; day = ""; year = "" }) { Text("清除生日") }
                OutlinedTextField(
                    value = relationInput,
                    onValueChange = { relationInput = it },
                    label = { Text("关系标签（逗号分隔）") },
                    placeholder = { Text("如：发小、同事、家人") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val newRelations = relationInput.split(",", "，").map { it.trim() }.filter { it.isNotBlank() }
                    onSave(name, birthdayTimestamp(year.toIntOrNull(), month.toIntOrNull(), day.toIntOrNull()), newRelations)
                },
                enabled = name.isNotBlank() && ((month.isBlank() && day.isBlank()) || birthdayTimestamp(year.toIntOrNull(), month.toIntOrNull(), day.toIntOrNull()) != null)
            ) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )

}

@Composable
private fun AddGiftDialog(
    onDismiss: () -> Unit,
    onSave: (String, Double, GiftStatus, Int) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var status by remember { mutableStateOf(GiftStatus.PLANNED) }
    var year by remember { mutableStateOf(Calendar.getInstance().get(Calendar.YEAR)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("记录礼物") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("礼物名称") }, singleLine = true)
                OutlinedTextField(
                    value = price,
                    onValueChange = { price = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("价格（元）") },
                    singleLine = true
                )
                Text("状态", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GiftStatus.entries.forEach { s ->
                        FilterChip(
                            selected = status == s,
                            onClick = { status = s },
                            label = { Text(giftStatusLabel[s] ?: "") }
                        )
                    }
                }
                OutlinedTextField(
                    value = year.toString(),
                    onValueChange = { year = it.filter { c -> c.isDigit() }.toIntOrNull() ?: year },
                    label = { Text("年份") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(name.trim(), price.toDoubleOrNull() ?: 0.0, status, year) }, enabled = name.isNotBlank()) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
private fun AddBirthdayRecordDialog(
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var note by remember { mutableStateOf("") }
    val currentYear = Calendar.getInstance().get(Calendar.YEAR)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("记录 $currentYear 年生日") },
        text = {
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("备注") },
                placeholder = { Text("今年生日怎么过的？") },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = { onSave(note.trim()) }) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

private fun friendBirthdayLabel(friend: Friend): String {
    val birthday = friend.birthdayDate ?: return "未设置"
    return formatBirthdayWithYear(birthday)
}

private fun giftStatusColor(status: GiftStatus): androidx.compose.ui.graphics.Color = when (status) {
    GiftStatus.PLANNED -> androidx.compose.ui.graphics.Color(0xFFFB8C00)
    GiftStatus.PURCHASED -> androidx.compose.ui.graphics.Color(0xFF1E88E5)
    GiftStatus.GIVEN -> androidx.compose.ui.graphics.Color(0xFF43A047)
}

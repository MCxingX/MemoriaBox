package com.memoriabox.ui.screen

import android.app.DatePickerDialog
import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.memoriabox.data.model.*
import com.memoriabox.ui.screen.components.FriendListView
import com.memoriabox.ui.screen.dialogs.ColorPickerDialog
import com.memoriabox.viewmodel.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsScreen(application: Application) {
    val friendVM = remember { createFriendViewModel(application) }
    val labelVM = remember { createLabelViewModel(application) }
    val friends by friendVM.friends.collectAsState(initial = emptyList())
    val friendRelations by friendVM.friendRelations.collectAsState(initial = emptyList())
    val labels by labelVM.labels.collectAsState(initial = emptyList())
    
    var showAddFriend by remember { mutableStateOf(false) }
    var showManageLabels by remember { mutableStateOf(false) }
    var friendForEdit by remember { mutableStateOf<Friend?>(null) }
    var selectedFilterLabel by remember { mutableStateOf<String?>(null) }

    // Load friends with labels from database
    LaunchedEffect(Unit) {
        // Initialize data loading
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("生日分组") },
                actions = {
                    IconButton(onClick = { showManageLabels = true }) {
                        Icon(Icons.Default.LocalOffer, contentDescription = "管理标签")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddFriend = true }) {
                Icon(Icons.Default.PersonAdd, contentDescription = "添加成员")
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            // Filter chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedFilterLabel == null,
                    onClick = { selectedFilterLabel = null },
                    label = { Text("全部") }
                )
                labels.forEach { label ->
                    FilterChip(
                        selected = selectedFilterLabel == label.name,
                        onClick = { selectedFilterLabel = label.name },
                        label = { Text(label.name) }
                    )
                }
            }
            
            // Friend list
            if (friends.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.PeopleOutline,
                            null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(Modifier.height(16.dp))
                        Text("暂无成员，点击右下角添加")
                    }
                }
            } else {
                val friendsWithLabels = friends.map { friend ->
                    friend to friendRelations.filter { it.friendId == friend.id }.map { it.label }
                }.filter { (_, friendLabels) ->
                    selectedFilterLabel == null || friendLabels.contains(selectedFilterLabel)
                }
                FriendListView(
                    friends = friendsWithLabels,
                    onFriendClick = { friendForEdit = it },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    if (showAddFriend) {
        AddFriendDialog(
            title = "添加成员",
            onDismiss = { showAddFriend = false },
            onSave = { name, birthdayDate, avatarUri, selectedLabels ->
                friendVM.createFriend(name, avatarUri, birthdayDate, selectedLabels)
                showAddFriend = false
            },
            availableLabels = labels,
            onAddLabel = { name, color ->
                labelVM.createLabel(name, color)
            }
        )
    }

    friendForEdit?.let { friend ->
        val existingLabels = friendRelations.filter { it.friendId == friend.id }.map { it.label }
        AddFriendDialog(
            title = "编辑成员",
            existingFriend = friend,
            existingLabels = existingLabels,
            onDismiss = { friendForEdit = null },
            onSave = { name, birthdayDate, avatarUri, selectedLabels ->
                friendVM.updateFriend(
                    friend.copy(name = name, birthdayDate = birthdayDate, avatarUri = avatarUri),
                    selectedLabels,
                    existingLabels
                )
                friendForEdit = null
            },
            availableLabels = labels,
            onAddLabel = { name, color -> labelVM.createLabel(name, color) }
        )
    }

    if (showManageLabels) {
        ManageLabelsDialog(
            labels = labels,
            onDismiss = { showManageLabels = false },
            onDeleteLabel = { labelVM.deleteLabel(it) },
            onCreateLabel = { name, color -> labelVM.createLabel(name, color) }
        )
    }
}

@Composable
fun AddFriendDialog(
    title: String = "添加成员",
    existingFriend: Friend? = null,
    existingLabels: List<String> = emptyList(),
    onDismiss: () -> Unit,
    onSave: (String, Long?, String?, List<String>) -> Unit,
    availableLabels: List<Label>,
    onAddLabel: (String, String) -> Unit
) {
    var name by remember(existingFriend?.id) { mutableStateOf(existingFriend?.name.orEmpty()) }
    var birthdayDate by remember(existingFriend?.id) { mutableStateOf(existingFriend?.birthdayDate) }
    var birthdayInput by remember(existingFriend?.id) { mutableStateOf(existingFriend?.birthdayDate?.let { formatFriendDate(it) }.orEmpty()) }
    var avatarUri by remember(existingFriend?.id) { mutableStateOf<String?>(existingFriend?.avatarUri) }
    var selectedLabels by remember(existingFriend?.id, existingLabels) { mutableStateOf(existingLabels) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showColorPicker by remember { mutableStateOf(false) }
    var newLabelName by remember { mutableStateOf("") }
    var newLabelColor by remember { mutableStateOf("#7C4DFF") }

    val context = LocalContext.current
    val avatarPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            try {
                context.contentResolver.takePersistableUriPermission(
                    it,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                avatarUri = copyFriendImageToPrivateStorage(context, it) ?: it.toString()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Avatar upload
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .clickable { avatarPicker.launch(arrayOf("image/*")) }
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    if (avatarUri != null) {
                        AsyncImage(
                            model = avatarUri,
                            contentDescription = "头像",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            Icons.Default.AddPhotoAlternate,
                            null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Spacer(Modifier.height(16.dp))
                
                // Name input
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("姓名") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(Modifier.height(16.dp))
                
                // Birthday picker
                OutlinedTextField(
                    value = birthdayInput,
                    onValueChange = {
                        birthdayInput = it
                        parseFriendDate(it)?.let { parsed -> birthdayDate = parsed }
                    },
                    label = { Text("生日 (可选)") },
                    placeholder = { Text("例如 1998-05-20 或 5/20") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(Icons.Default.CalendarToday, null)
                        }
                    }
                )
                
                Spacer(Modifier.height(16.dp))
                
                // Label selection
                Text("标签", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    availableLabels.forEach { label ->
                        val isSelected = selectedLabels.contains(label.name)
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedLabels = if (isSelected) {
                                    selectedLabels - label.name
                                } else {
                                    selectedLabels + label.name
                                }
                            },
                            label = { Text(label.name) },
                            modifier = Modifier.fillMaxWidth(),
                            border = if (isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null
                        )
                    }
                }
                
                // Add new label
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = newLabelName,
                    onValueChange = { newLabelName = it },
                    label = { Text("新建标签名称") },
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        IconButton(onClick = { 
                            if (newLabelName.isNotBlank()) {
                                onAddLabel(newLabelName, newLabelColor)
                                selectedLabels = selectedLabels + newLabelName
                                newLabelName = ""
                            }
                        }) {
                            Icon(Icons.Default.Add, null)
                        }
                    }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(name, birthdayDate, avatarUri, selectedLabels) },
                enabled = name.isNotBlank()
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )

    if (showDatePicker) {
        android.app.DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val cal = java.util.Calendar.getInstance()
                cal.set(year, month, dayOfMonth)
                birthdayDate = cal.timeInMillis
                birthdayInput = formatFriendDate(cal.timeInMillis)
                showDatePicker = false
            },
            java.util.Calendar.getInstance().get(java.util.Calendar.YEAR),
            java.util.Calendar.getInstance().get(java.util.Calendar.MONTH),
            java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_MONTH)
        ).show()
    }

    if (showColorPicker) {
        ColorPickerDialog(
            initialColor = newLabelColor,
            onDismiss = { showColorPicker = false },
            onSelected = { 
                newLabelColor = it
                showColorPicker = false
            }
        )
    }
}

private fun formatFriendDate(timeMillis: Long): String =
    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(java.util.Date(timeMillis))

private fun parseFriendDate(input: String): Long? {
    val trimmed = input.trim()
    if (trimmed.isBlank()) return null
    val patterns = listOf("yyyy-MM-dd", "yyyy/M/d", "yyyy.M.d", "M-d", "M/d", "M.d")
    return patterns.firstNotNullOfOrNull { pattern ->
        runCatching {
            val normalizedInput = if (pattern.startsWith("M")) "2000-$trimmed" else trimmed
            val normalizedPattern = if (pattern.startsWith("M")) "yyyy-$pattern" else pattern
            SimpleDateFormat(normalizedPattern, Locale.getDefault()).apply { isLenient = false }.parse(normalizedInput)?.time
        }.getOrNull()
    }
}

private fun safeComposeColor(value: String) = runCatching {
    com.memoriabox.utils.ColorUtils.hexToColor(value)
}.getOrElse { androidx.compose.ui.graphics.Color(0xFF7C4DFF) }

private fun copyFriendImageToPrivateStorage(context: Context, uri: Uri): String? {
    return runCatching {
        val dir = File(context.filesDir, "friend_images").apply { mkdirs() }
        val target = File(dir, "friend_${System.currentTimeMillis()}.jpg")
        context.contentResolver.openInputStream(uri)?.use { input ->
            target.outputStream().use { output -> input.copyTo(output) }
        }
        Uri.fromFile(target).toString()
    }.getOrNull()
}

@Composable
fun ManageLabelsDialog(
    labels: List<Label>,
    onDismiss: () -> Unit,
    onDeleteLabel: (Label) -> Unit,
    onCreateLabel: (String, String) -> Unit
) {
    var newLabelName by remember { mutableStateOf("") }
    var newLabelColor by remember { mutableStateOf("#7C4DFF") }
    var showColorPicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("管理标签") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Existing labels
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    labels.forEach { label ->
                        val labelColor = safeComposeColor(label.color)
                        Card(
                            modifier = Modifier
                                .fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = labelColor.copy(alpha = 0.15f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(labelColor)
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Text(label.name, style = MaterialTheme.typography.bodyLarge)
                                }
                                IconButton(onClick = { onDeleteLabel(label) }) {
                                    Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
                
                Spacer(Modifier.height(16.dp))
                
                // Create new label
                Text("新建标签", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .clickable { showColorPicker = true }
                            .background(safeComposeColor(newLabelColor))
                    )
                    OutlinedTextField(
                        value = newLabelName,
                        onValueChange = { newLabelName = it },
                        label = { Text("标签名称") },
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = {
                            if (newLabelName.isNotBlank()) {
                                onCreateLabel(newLabelName, newLabelColor)
                                newLabelName = ""
                            }
                        },
                        enabled = newLabelName.isNotBlank()
                    ) {
                        Icon(Icons.Default.Add, null)
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text("完成") }
        }
    )

    if (showColorPicker) {
        ColorPickerDialog(
            initialColor = newLabelColor,
            onDismiss = { showColorPicker = false },
            onSelected = { 
                newLabelColor = it
                showColorPicker = false
            }
        )
    }
}

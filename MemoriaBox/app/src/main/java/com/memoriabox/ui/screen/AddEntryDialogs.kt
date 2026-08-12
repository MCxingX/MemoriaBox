package com.memoriabox.ui.screen

import android.app.Application
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.*
import com.memoriabox.ui.screen.components.*
import com.memoriabox.ui.screen.dialogs.EventDialog
import com.memoriabox.data.model.*
import com.memoriabox.utils.NotificationHelper
import com.memoriabox.viewmodel.*
import java.util.Date

@Composable
fun AddTypePickerDialog(
    onDismiss: () -> Unit,
    onTypeSelected: (EventType) -> Unit,
    onAddDiary: (() -> Unit)? = null,
    onCreateBox: (() -> Unit)? = null
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("新增") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                onAddDiary?.let { addDiary ->
                    AddActionOption(Icons.Default.Edit, "写日记", "记录今天发生了什么", onClick = addDiary)
                }
                AddTypeOption(Icons.Default.Timer, "倒数日", "考试、旅行、纪念日", EventType.COUNTDOWN, onTypeSelected)
                AddTypeOption(Icons.Default.Favorite, "纪念日", "恋爱、结婚、相识", EventType.ANNIVERSARY, onTypeSelected)
                AddTypeOption(Icons.Default.History, "正计时", "记录已坚持多久", EventType.ELAPSED, onTypeSelected)
                AddTypeOption(Icons.Default.Cake, "生日", "支持提前提醒", EventType.BIRTHDAY, onTypeSelected)
                AddTypeOption(Icons.Default.CheckCircle, "待办", "把事项放进时间轴", EventType.TODO, onTypeSelected)
                onCreateBox?.let { createBox ->
                    AddActionOption(Icons.Default.CreateNewFolder, "新建分组", "收纳不同类型日子", onClick = createBox)
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
fun AddActionOption(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.secondaryContainer) {
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.padding(10.dp).size(24.dp),
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null)
        }
    }
}

@Composable
fun AddTypeOption(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    type: EventType,
    onTypeSelected: (EventType) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onTypeSelected(type) },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.primaryContainer) {
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.padding(10.dp).size(24.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null)
        }
    }
}

@Composable
fun QuickAddEventDialog(
    boxes: List<com.memoriabox.data.model.Box>,
    initialType: EventType,
    application: Application,
    defaultBoxId: String? = null,
    onDismiss: () -> Unit,
    onSave: (Event) -> Unit
) {
    val notificationHelper = remember { com.memoriabox.utils.NotificationHelper(application) }
    EventDialog(
        availableBoxes = boxes,
        defaultBoxId = defaultBoxId ?: boxes.firstOrNull()?.id ?: "",
        defaultType = initialType,
        defaultDate = System.currentTimeMillis(),
        defaultReminderEnabled = true,
        defaultPushPlusEnabled = notificationHelper.isPushPlusEnabled(),
        allowTypeChange = false,
        showFixedTypeLabel = false,
        onPushPlusEnabledChange = { notificationHelper.setPushPlusEnabled(it) },
        onDismiss = onDismiss,
        onSave = onSave
    )
}


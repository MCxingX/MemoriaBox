package com.memoriabox.ui.screen

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import androidx.navigation.compose.*
import com.memoriabox.ui.screen.components.*
import com.memoriabox.data.model.*
import com.memoriabox.viewmodel.*
import java.util.Date

@Composable
fun EventDetailDialog(
    event: Event,
    logs: List<LogEntry>,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onTogglePin: () -> Unit,
    onDelete: () -> Unit,
    onOpenCategory: () -> Unit
) {
    val context = LocalContext.current
    val days = com.memoriabox.ui.screen.components.calculateDays(event)
    var showMenu by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(event.name) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    com.memoriabox.utils.ColorUtils.hexToColor(event.gradientStart),
                                    com.memoriabox.utils.ColorUtils.hexToColor(event.gradientEnd)
                                )
                            ),
                            shape = MaterialTheme.shapes.extraLarge
                        )
                ) {
                    if (event.avatarUri != null) {
                        AsyncImage(
                            model = event.avatarUri,
                            contentDescription = event.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.matchParentSize()
                        )
                        Box(modifier = Modifier.matchParentSize().background(Color.Black.copy(alpha = 0.36f)))
                    }
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(16.dp)
                    ) {
                        Text(days.toString(), style = MaterialTheme.typography.displayMedium, color = com.memoriabox.utils.ColorUtils.hexToColor(event.textColor))
                        Text("还剩 $days 天 · ${eventTypeLabel(event.type)}", style = MaterialTheme.typography.titleMedium, color = com.memoriabox.utils.ColorUtils.hexToColor(event.textColor))
                    }
                }
                DetailLine("类型", eventTypeLabel(event.type))
                DetailLine("日期", com.memoriabox.ui.screen.components.formatDate(event.date))
                event.lunar?.let { DetailLine("农历", it) }
                if (event.note.isNotBlank()) DetailLine("备注", event.note)
                DetailLine("提醒", if (event.reminderEnabled) "提前 ${event.reminderDays} 天" else "关闭")
                DetailLine("重复", repeatModeLabel(event))
                if (event.pushPlusEnabled) DetailLine("PushPlus", "开启")
                if (event.isPinned) DetailLine("状态", "已置顶")
                DetailLine("模板", cardTemplateLabel(event.cardTemplate))
                DetailLine("心情标签", eventMoodLabel(event))
                Spacer(Modifier.height(8.dp))
                Text("提示：可在编辑资料中选择背景、渐变色和卡片模板；也可分享图片/文本或在桌面添加小组件。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                HorizontalDivider()
                Text("时间线记录", style = MaterialTheme.typography.titleSmall)
                if (event.note.isNotBlank()) {
                    Text(event.note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                HorizontalDivider()
                Text("操作历史", style = MaterialTheme.typography.titleSmall)
                if (logs.isEmpty()) {
                    Text("暂无历史记录", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    logs.take(5).forEach { log ->
                        DetailLine(log.operation, com.memoriabox.ui.screen.components.formatDate(log.timestamp))
                    }
                }
            }
        },
        confirmButton = {
            Box {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { showMenu = true }) { Text("更多") }
                    TextButton(onClick = onEdit) { Text("编辑") }
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("分享图片") },
                        onClick = {
                            showMenu = false
                            shareBitmap(context, generateEventCardBitmap(context, event, days))
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("分享文本") },
                        onClick = {
                            showMenu = false
                            val shareText = "${event.name}\n${eventTypeLabel(event.type)}：还剩 $days 天\n日期：${com.memoriabox.ui.screen.components.formatDate(event.date)}\n${event.note}"
                            context.startActivity(
                                Intent.createChooser(
                                    Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, shareText)
                                    },
                                    "分享日子"
                                )
                            )
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("分类") },
                        onClick = {
                            showMenu = false
                            onOpenCategory()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(if (event.isPinned) "取消置顶" else "置顶") },
                        onClick = {
                            showMenu = false
                            onTogglePin()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("删除", color = MaterialTheme.colorScheme.error) },
                        onClick = {
                            showMenu = false
                            showDeleteConfirm = true
                        }
                    )
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("删除事件") },
            text = { Text("确定要删除「${event.name}」吗？此操作不可恢复。") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    onDelete()
                }) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("取消") }
            }
        )
    }
}

fun repeatModeLabel(event: Event): String = when {
    event.repeatMode == RepeatMode.YEARLY || event.repeatYearly -> "每年重复"
    event.repeatMode == RepeatMode.MONTHLY -> "每月重复"
    event.repeatMode == RepeatMode.CUSTOM_DAYS -> if (event.repeatInterval <= 1) "每日重复" else "每 ${event.repeatInterval} 天重复"
    event.repeatMode == RepeatMode.CUSTOM_WEEKS -> if (event.repeatInterval <= 1) "每周重复" else "每 ${event.repeatInterval} 周重复"
    event.repeatMode == RepeatMode.CUSTOM_MONTHS -> if (event.repeatInterval <= 1) "每月重复" else "每 ${event.repeatInterval} 个月重复"
    event.type == EventType.BIRTHDAY -> "每年重复"
    else -> "不重复"
}

fun cardTemplateLabel(template: String): String = when (template) {
    "POSTER" -> "海报"
    "GLASS" -> "玻璃"
    "SPLIT" -> "分栏"
    "NEON" -> "光轨"
    "MINIMAL" -> "徽章"
    else -> "大图"
}

fun eventMoodLabel(event: Event): String = when (event.type) {
    EventType.COUNTDOWN -> "期待"
    EventType.ANNIVERSARY -> "心动"
    EventType.ELAPSED -> "坚持"
    EventType.BIRTHDAY -> "祝福"
    EventType.TODO -> "重要"
}

@Composable
fun DetailLine(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun EventActionDialog(
    event: Event,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onTogglePin: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(event.name) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("请选择要执行的操作。", style = MaterialTheme.typography.bodyMedium)
                Button(onClick = onEdit, modifier = Modifier.fillMaxWidth()) { Text("编辑资料") }
                OutlinedButton(onClick = onTogglePin, modifier = Modifier.fillMaxWidth()) {
                    Text(if (event.isPinned) "取消置顶" else "置顶")
                }
                OutlinedButton(onClick = { showDeleteConfirm = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("删除事件") },
            text = { Text("确定要删除「${event.name}」吗？此操作不可恢复。") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    onDelete()
                }) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("取消") }
            }
        )
    }
}


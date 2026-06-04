package com.memoriabox.ui.screen.components

import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.memoriabox.ui.utils.rememberAdaptiveUiSize

@Composable
fun SettingsList(
    onBackupSettingsClick: () -> Unit = {},
    onWebDavSettingsClick: () -> Unit = {},
    onDiarySettingsClick: () -> Unit = {}
) {
    Column(modifier = Modifier.fillMaxSize()) {
        SettingsItem(
            icon = Icons.Default.Backup,
            title = "备份设置",
            description = "本地备份、导入导出、自动备份",
            onClick = onBackupSettingsClick
        )
        SettingsItem(
            icon = Icons.Default.Cloud,
            title = "WebDAV 同步",
            description = "配置云端同步服务",
            onClick = onWebDavSettingsClick
        )
        SettingsItem(
            icon = Icons.Default.Edit,
            title = "日记设置",
            description = "滚动动画速度、开关",
            onClick = onDiarySettingsClick
        )
        SettingsItem(
            icon = Icons.Default.NotificationImportant,
            title = "提醒设置",
            description = "PushPlus 推送、通知管理"
        )
        SettingsItem(
            icon = Icons.Default.Info,
            title = "关于",
            description = "版本 3.2.2"
        )
    }
}

@Composable
fun SettingsItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit = {}
) {
    val adaptiveUi = rememberAdaptiveUiSize()
    val gradient = Brush.linearGradient(
        listOf(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.92f),
            MaterialTheme.colorScheme.secondary.copy(alpha = 0.88f),
            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.84f)
        )
    )
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = adaptiveUi.screenPadding, vertical = adaptiveUi.sectionSpacing / 2f)
            .clickable(onClick = onClick),
        shape = if (adaptiveUi.compact) MaterialTheme.shapes.medium else MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.84f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(if (adaptiveUi.compact) 12.dp else 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(if (adaptiveUi.compact) 42.dp else 48.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(gradient),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = title, modifier = Modifier.size(if (adaptiveUi.compact) 22.dp else 25.dp), tint = Color.White)
            }
            Spacer(Modifier.width(if (adaptiveUi.compact) 12.dp else 16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
        }
    }
}

@Composable
fun BackupSettingsContent(
    modifier: Modifier = Modifier,
    onSelectDir: () -> Unit = {},
    onManualBackup: () -> Unit = {},
    onImport: () -> Unit = {}
) {
    Column(
        modifier = modifier
            .padding(16.dp)
            .fillMaxWidth()
    ) {
        Text("备份设置", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(16.dp))
        
        OutlinedCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("本地备份", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Button(onClick = onSelectDir, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Folder, null)
                    Spacer(Modifier.width(8.dp))
                    Text("选择备份目录")
                }
                Spacer(Modifier.height(8.dp))
                Button(onClick = onManualBackup, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Backup, null)
                    Spacer(Modifier.width(8.dp))
                    Text("立即备份")
                }
                Spacer(Modifier.height(8.dp))
                Button(onClick = onImport, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Upload, null)
                    Spacer(Modifier.width(8.dp))
                    Text("导入备份")
                }
            }
        }
    }
}

@Composable
fun WebDavSettingsContent(
    modifier: Modifier = Modifier
) {
    var serverUrl by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var statusText by remember { mutableStateOf<String?>(null) }
    
    Column(
        modifier = modifier
            .padding(16.dp)
            .fillMaxWidth()
    ) {
        Text("WebDAV 设置", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(16.dp))
        
        OutlinedTextField(
            value = serverUrl,
            onValueChange = { serverUrl = it },
            label = { Text("服务器地址") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("用户名") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("密码") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = {
                statusText = if (serverUrl.isBlank()) {
                    "请先填写 WebDAV 服务器地址。"
                } else {
                    "配置已填写。请在同步页面执行同步检查。"
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("检查配置")
        }
        statusText?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun DiarySettingsDialog(
    onDismiss: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var scrollEnabled by remember { mutableStateOf(com.memoriabox.utils.AppSettings.getDiaryScrollEnabled(context)) }
    var scrollSpeed by remember { mutableIntStateOf(com.memoriabox.utils.AppSettings.getDiaryScrollSpeed(context)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("日记设置") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("滚动动画", style = MaterialTheme.typography.titleSmall)
                        Text("关闭后直接显示全部文字", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = scrollEnabled,
                        onCheckedChange = {
                            scrollEnabled = it
                            com.memoriabox.utils.AppSettings.setDiaryScrollEnabled(context, it)
                        }
                    )
                }

                if (scrollEnabled) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("滚动速度", style = MaterialTheme.typography.titleSmall)
                            Text("${scrollSpeed}ms/字", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Slider(
                            value = scrollSpeed.toFloat(),
                            onValueChange = {
                                scrollSpeed = it.toInt()
                                com.memoriabox.utils.AppSettings.setDiaryScrollSpeed(context, scrollSpeed)
                            },
                            valueRange = 10f..200f,
                            steps = 18,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("快", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("慢", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("完成") }
        }
    )
}

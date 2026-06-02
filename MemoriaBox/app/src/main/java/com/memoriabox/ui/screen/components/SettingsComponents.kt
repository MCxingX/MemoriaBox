package com.memoriabox.ui.screen.components

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
import androidx.compose.ui.unit.dp

@Composable
fun SettingsList(
    onBackupSettingsClick: () -> Unit = {},
    onWebDavSettingsClick: () -> Unit = {}
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
            icon = Icons.Default.NotificationImportant,
            title = "提醒设置",
            description = "PushPlus 推送、通知管理"
        )
        SettingsItem(
            icon = Icons.Default.Info,
            title = "关于",
            description = "版本 1.0.0"
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
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = title, modifier = Modifier.size(32.dp))
            Spacer(Modifier.width(16.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(description, style = MaterialTheme.typography.bodySmall)
            }
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
        Button(onClick = { /* TODO: Test connection */ }, modifier = Modifier.fillMaxWidth()) {
            Text("保存并测试连接")
        }
    }
}

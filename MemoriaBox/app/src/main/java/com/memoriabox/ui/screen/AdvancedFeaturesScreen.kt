package com.memoriabox.ui.screen

import android.app.Application
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.memoriabox.viewmodel.createCalendarViewModel

@Composable
fun SyncStatusScreen(application: Application) {
    val vm = remember { createCalendarViewModel(application) }
    val events by vm.allEvents.collectAsState(initial = emptyList())

    Scaffold(topBar = { TopAppBar(title = { Text("多设备同步") }) }) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FeatureCard(
                icon = Icons.Default.CloudSync,
                title = "同步状态",
                description = "当前设备保存了 ${events.size} 个事件。WebDAV 已可用于跨设备手动同步。"
            )
            FeatureCard(
                icon = Icons.Default.TaskAlt,
                title = "同步建议",
                description = "在两台设备中配置同一个 WebDAV 目录，然后通过备份导入导出保持数据一致。"
            )
            FeatureCard(
                icon = Icons.Default.CloudSync,
                title = "后续增强",
                description = "自动冲突合并和增量同步已列入后续版本，当前版本优先保证数据可恢复。"
            )
        }
    }
}

@Composable
private fun FeatureCard(icon: ImageVector, title: String, description: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
            Column(modifier = Modifier.padding(start = 12.dp)) {
                Text(title, fontWeight = FontWeight.Bold)
                Text(description, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

package com.memoriabox.ui.screen

import android.app.Application
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import com.memoriabox.data.model.Event
import com.memoriabox.data.model.EventType
import com.memoriabox.viewmodel.createCalendarViewModel

@Composable
fun AiSuggestionsScreen(application: Application) {
    val vm = remember { createCalendarViewModel(application) }
    val events by vm.allEvents.collectAsState(initial = emptyList())
    val suggestions = remember(events) { buildAiSuggestions(events) }

    Scaffold(topBar = { TopAppBar(title = { Text("AI 智能建议") }) }) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(suggestions.size) { index ->
                FeatureCard(
                    icon = Icons.Default.AutoAwesome,
                    title = suggestions[index].first,
                    description = suggestions[index].second
                )
            }
        }
    }
}

@Composable
fun AchievementsScreen(application: Application) {
    val vm = remember { createCalendarViewModel(application) }
    val events by vm.allEvents.collectAsState(initial = emptyList())
    val achievements = remember(events) { buildAchievements(events) }

    Scaffold(topBar = { TopAppBar(title = { Text("成就系统") }) }) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(achievements.size) { index ->
                val item = achievements[index]
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(item.icon, null, tint = MaterialTheme.colorScheme.primary)
                            Column(modifier = Modifier.padding(start = 12.dp)) {
                                Text(item.title, fontWeight = FontWeight.Bold)
                                Text(item.description, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        LinearProgressIndicator(
                            progress = { item.progress.coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

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

private data class AchievementItem(
    val icon: ImageVector,
    val title: String,
    val description: String,
    val progress: Float
)

private fun buildAiSuggestions(events: List<Event>): List<Pair<String, String>> {
    val birthdays = events.count { it.type == EventType.BIRTHDAY }
    val todos = events.count { it.type == EventType.TODO }
    val missingReminder = events.count { !it.reminderEnabled }
    return listOf(
        "提醒优化" to "有 $missingReminder 个事件还没有开启提醒，可为重要纪念日开启提前提醒。",
        "生日管家" to "已记录 $birthdays 个生日，建议为亲友生日设置 7 天提前提醒。",
        "待办整理" to "当前有 $todos 个待办事件，可以按截止日期优先处理。",
        "照片补全" to "为重要事件添加头像或照片后，照片墙会更完整。"
    )
}

private fun buildAchievements(events: List<Event>): List<AchievementItem> {
    val boxesProgress = (events.map { it.boxId }.distinct().size / 3f)
    val eventProgress = events.size / 20f
    val birthdayProgress = events.count { it.type == EventType.BIRTHDAY } / 10f
    val todoProgress = events.count { it.type == EventType.TODO } / 10f
    val photoProgress = events.count { it.avatarUri != null } / 8f
    return listOf(
        AchievementItem(Icons.Default.Event, "事件收藏家", "记录 20 个事件", eventProgress),
        AchievementItem(Icons.Default.Folder, "盒子规划师", "使用 3 个盒子整理生活", boxesProgress),
        AchievementItem(Icons.Default.LocalOffer, "生日守护者", "记录 10 个生日", birthdayProgress),
        AchievementItem(Icons.Default.TaskAlt, "行动派", "创建 10 个待办", todoProgress),
        AchievementItem(Icons.Default.PhotoLibrary, "回忆摄影师", "为 8 个事件添加照片", photoProgress),
        AchievementItem(Icons.Default.EmojiEvents, "长期陪伴", "持续记录生活里的重要日子", eventProgress)
    )
}

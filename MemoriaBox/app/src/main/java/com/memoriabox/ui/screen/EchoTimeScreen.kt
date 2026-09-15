package com.memoriabox.ui.screen

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.memoriabox.data.model.DiaryEntry
import com.memoriabox.data.model.DiaryMedia
import com.memoriabox.data.model.Event
import com.memoriabox.data.model.EventType
import com.memoriabox.viewmodel.createEchoTimeViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class EchoTimeTab(val label: String) {
    TODAY("往年今日"),
    PHOTOS("历史照片"),
    NODES("重要节点")
}

@Composable
fun EchoTimeScreen(
    application: Application,
    onNavigateBack: () -> Unit
) {
    val viewModel = remember { createEchoTimeViewModel(application) }
    val diaries by viewModel.allDiaries.collectAsState(initial = emptyList())
    val events by viewModel.allEvents.collectAsState(initial = emptyList())
    val media by viewModel.allDiaryMedia.collectAsState(initial = emptyList())
    var selectedTab by rememberSaveable { mutableStateOf(EchoTimeTab.TODAY) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("时光回响") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                EchoTimeTab.entries.forEach { tab ->
                    FilterChip(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        label = { Text(tab.label) }
                    )
                }
            }
            when (selectedTab) {
                EchoTimeTab.TODAY -> TodaySections(diaries = diaries, events = events, media = media)
                EchoTimeTab.PHOTOS -> HistoricalPhotos(media = viewModel.historicalPhotos(media), diaries = diaries)
                EchoTimeTab.NODES -> ImportantNodes(events = events)
            }
        }
    }
}

@Composable
private fun TodaySections(diaries: List<DiaryEntry>, events: List<Event>, media: List<DiaryMedia>) {
    val today = Calendar.getInstance()
    val monthDayRecords = diaries.filter { diary ->
        val cal = Calendar.getInstance().apply { timeInMillis = diary.dateStart }
        cal.get(Calendar.MONTH) == today.get(Calendar.MONTH) && cal.get(Calendar.DAY_OF_MONTH) == today.get(Calendar.DAY_OF_MONTH)
    }
    val year = today.get(Calendar.YEAR)
    val mediaMap = media.groupBy { it.diaryId }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            EchoSectionHeader(
                icon = Icons.Default.History,
                title = "往年今日",
                subtitle = "${today.get(Calendar.MONTH) + 1}月${today.get(Calendar.DAY_OF_MONTH)}日，你曾留下 ${monthDayRecords.size} 篇记录"
            )
        }
        if (monthDayRecords.isEmpty()) {
            item { EchoEmptyCard("今天还没有历史记录，去日历写下第一篇吧。") }
        } else {
            items(monthDayRecords) { diary ->
                DiaryEchoCard(diary = diary, media = mediaMap[diary.id].orEmpty())
            }
        }
    }
}

@Composable
private fun EchoSectionHeader(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Column {
            Text(title, style = MaterialTheme.typography.titleLarge)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun DiaryEchoCard(diary: DiaryEntry, media: List<DiaryMedia>) {
    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                SimpleDateFormat("yyyy年M月d日", Locale.getDefault()).format(Date(diary.dateStart)),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            if (diary.content.isNotBlank()) {
                Text(diary.content, style = MaterialTheme.typography.bodyMedium, maxLines = 4, overflow = TextOverflow.Ellipsis)
            }
            if (media.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(media.take(6)) { item ->
                        AsyncImage(
                            model = item.mediaUri,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(80.dp)
                                .clip(MaterialTheme.shapes.medium)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoricalPhotos(media: List<DiaryMedia>, diaries: List<DiaryEntry>) {
    val diaryById = diaries.associateBy { it.id }
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            EchoSectionHeader(
                icon = Icons.Default.PhotoLibrary,
                title = "历史照片",
                subtitle = "共 ${media.size} 张照片"
            )
        }
        if (media.isEmpty()) {
            item { EchoEmptyCard("还没有历史照片，写日记时可以添加照片。") }
        } else {
            val byMonth = media.mapNotNull { item ->
                val diary = diaryById[item.diaryId] ?: return@mapNotNull null
                item to diary.dateStart
            }.groupBy { (_, ts) ->
                SimpleDateFormat("yyyy年M月", Locale.getDefault()).format(Date(ts))
            }
            byMonth.forEach { (month, items) ->
                item {
                    Text(month, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(vertical = 4.dp))
                }
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(items) { (item, _) ->
                            AsyncImage(
                                model = item.mediaUri,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(96.dp)
                                    .clip(MaterialTheme.shapes.medium)
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ImportantNodes(events: List<Event>) {
    val meaningful = events
        .filter { it.type != EventType.TODO }
        .filter { it.date < System.currentTimeMillis() }
        .sortedByDescending { it.date }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            EchoSectionHeader(
                icon = Icons.Default.Event,
                title = "重要节点",
                subtitle = "共 ${meaningful.size} 个已发生的纪念日与生日"
            )
        }
        if (meaningful.isEmpty()) {
            item { EchoEmptyCard("还没有已发生的重要节点。") }
        } else {
            items(meaningful.take(100)) { event ->
                Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(event.name, style = MaterialTheme.typography.titleMedium)
                            Text(
                                SimpleDateFormat("yyyy年M月d日", Locale.getDefault()).format(Date(event.date)),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (event.isBirthday) {
                            Text(
                                "生日",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier
                                    .clip(MaterialTheme.shapes.small)
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EchoEmptyCard(message: String) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Text(
            message,
            modifier = Modifier.padding(20.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

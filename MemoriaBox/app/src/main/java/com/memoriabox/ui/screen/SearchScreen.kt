package com.memoriabox.ui.screen

import android.app.Application
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import com.memoriabox.data.model.Box
import com.memoriabox.data.model.Event
import com.memoriabox.data.model.EventType
import com.memoriabox.data.model.RepeatMode
import com.memoriabox.viewmodel.createCalendarViewModel
import com.memoriabox.viewmodel.createMainViewModel
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import com.memoriabox.utils.AppSettings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    application: Application,
    onNavigateBack: () -> Unit = {},
    onOpenBox: (String) -> Unit = {},
    onOpenEvent: (com.memoriabox.data.model.Event) -> Unit = {}
) {
    val boxVM = remember { createMainViewModel(application) }
    val eventVM = remember { createCalendarViewModel(application) }
    val boxes by boxVM.boxes.collectAsState(initial = emptyList())
    val events by eventVM.allEvents.collectAsState(initial = emptyList())
    
    var searchQuery by remember { mutableStateOf("") }
    var searchType by remember { mutableStateOf(SearchType.ALL) }
    var eventFilter by remember { mutableStateOf(EventFilter.ALL) }
    
    val filteredBoxes = remember(searchQuery, searchType) {
        if (searchType == SearchType.ALL || searchType == SearchType.BOXES) {
            boxes.filter { it.name.contains(searchQuery, ignoreCase = true) }
        } else emptyList()
    }
    
    val filteredEvents = remember(searchQuery, searchType, eventFilter, events) {
        if (searchType == SearchType.ALL || searchType == SearchType.EVENTS) {
            events.filter { 
                (it.name.contains(searchQuery, ignoreCase = true) ||
                it.note.contains(searchQuery, ignoreCase = true)) && matchesEventFilter(it, eventFilter)
            }
        } else emptyList()
    }

    val focusManager = LocalFocusManager.current

    // 最近搜索持久化到 AppSettings，退出页面或杀进程后仍然保留
    var recentSearches by remember { mutableStateOf(AppSettings.getRecentSearches(application)) }

    fun commitSearch(keyword: String) {
        if (keyword.isBlank()) return
        AppSettings.addRecentSearch(application, keyword)
        recentSearches = AppSettings.getRecentSearches(application)
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("搜索") },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                        }
                    }
                )
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("搜索事件、分类、备注...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "清除搜索")
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            commitSearch(searchQuery)
                            focusManager.clearFocus()
                        }
                    ),
                    singleLine = true,
                    shape = MaterialTheme.shapes.extraLarge,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            // Search type filter
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SearchType.values().forEach { type ->
                    FilterChip(
                        selected = searchType == type,
                        onClick = { searchType = type },
                        label = { Text(type.label) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            if (searchType == SearchType.ALL || searchType == SearchType.EVENTS) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    EventFilter.entries.take(4).forEach { filter ->
                        FilterChip(
                            selected = eventFilter == filter,
                            onClick = { eventFilter = filter },
                            label = { Text(filter.label) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    EventFilter.entries.drop(4).forEach { filter ->
                        FilterChip(
                            selected = eventFilter == filter,
                            onClick = { eventFilter = filter },
                            label = { Text(filter.label) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
             
            HorizontalDivider()
            
            if (searchQuery.isEmpty()) {
                // Show recent searches or trending
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("最近搜索", style = MaterialTheme.typography.titleMedium)
                        if (recentSearches.isNotEmpty()) {
                            TextButton(onClick = {
                                AppSettings.clearRecentSearches(application)
                                recentSearches = emptyList()
                            }) { Text("清空") }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    if (recentSearches.isEmpty()) {
                        Text("暂无搜索历史", style = MaterialTheme.typography.bodyMedium)
                    } else {
                        recentSearches.forEach { query ->
                            AssistChip(
                                onClick = {
                                    searchQuery = query
                                    commitSearch(query)
                                },
                                label = { Text(query) },
                                leadingIcon = { Icon(Icons.Default.History, null) },
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }
                    }
                }
            } else {
                // Show results
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (searchType == SearchType.ALL || searchType == SearchType.BOXES) {
                        items(filteredBoxes) { box ->
                            SearchResultCard(
                                title = box.name,
                                subtitle = "分类 · ${if (box.icon.isImageUri()) "自定义图标" else box.icon}",
                                icon = Icons.Default.Folder,
                                onClick = {
                                    commitSearch(searchQuery)
                                    onOpenBox(box.id)
                                }
                            )
                        }
                    }
                    
                    if (searchType == SearchType.ALL || searchType == SearchType.EVENTS) {
                        items(filteredEvents) { event ->
                            SearchResultCard(
                                title = event.name,
                                subtitle = buildEventSubtitle(event),
                                icon = getEventIcon(event),
                                onClick = {
                                    commitSearch(searchQuery)
                                    onOpenEvent(event)
                                }
                            )
                        }
                    }
                    
                    if (filteredBoxes.isEmpty() && filteredEvents.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = androidx.compose.ui.Alignment.Center
                            ) {
                                Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                                    Icon(
                                        Icons.Default.SearchOff,
                                        null,
                                        modifier = Modifier.size(64.dp),
                                        tint = MaterialTheme.colorScheme.outline
                                    )
                                    Spacer(Modifier.height(16.dp))
                                    Text("未找到匹配结果", style = MaterialTheme.typography.bodyLarge)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SearchResultCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Icon(
                icon,
                null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (onClick != null) {
                Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.outline)
            }
        }
    }
}

enum class SearchType(val label: String) {
    ALL("全部"),
    BOXES("分类"),
    EVENTS("事件")
}

enum class EventFilter(val label: String) {
    ALL("全部"),
    PINNED("置顶"),
    REPEATING("重复"),
    WITH_IMAGE("有图"),
    REMINDER("提醒"),
    BIRTHDAY("生日"),
    TODO("待办")
}

fun matchesEventFilter(event: Event, filter: EventFilter): Boolean = when (filter) {
    EventFilter.ALL -> true
    EventFilter.PINNED -> event.isPinned
    EventFilter.REPEATING -> event.repeatMode != RepeatMode.NONE || event.repeatYearly || event.type == EventType.BIRTHDAY
    EventFilter.WITH_IMAGE -> event.avatarUri != null
    EventFilter.REMINDER -> event.reminderEnabled
    EventFilter.BIRTHDAY -> event.type == EventType.BIRTHDAY
    EventFilter.TODO -> event.type == EventType.TODO
}

fun buildEventSubtitle(event: Event): String {
    val days = when (event.type) {
        com.memoriabox.data.model.EventType.COUNTDOWN -> "还剩${com.memoriabox.ui.screen.components.calculateDays(event)}天"
        com.memoriabox.data.model.EventType.ANNIVERSARY -> "已过${com.memoriabox.ui.screen.components.calculateDays(event)}天"
        com.memoriabox.data.model.EventType.ELAPSED -> "已过${com.memoriabox.ui.screen.components.calculateDays(event)}天"
        com.memoriabox.data.model.EventType.BIRTHDAY -> "生日"
        com.memoriabox.data.model.EventType.TODO -> "待办"
    }
    return "$days · ${event.type.name}"
}

fun getEventIcon(event: Event) = when (event.type) {
    com.memoriabox.data.model.EventType.COUNTDOWN -> Icons.Default.Timer
    com.memoriabox.data.model.EventType.ANNIVERSARY -> Icons.Default.Favorite
    com.memoriabox.data.model.EventType.ELAPSED -> Icons.Default.History
    com.memoriabox.data.model.EventType.BIRTHDAY -> Icons.Default.Cake
    com.memoriabox.data.model.EventType.TODO -> Icons.Default.CheckCircle
}

private fun String.isImageUri(): Boolean = startsWith("content://") || startsWith("file://")

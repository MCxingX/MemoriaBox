package com.memoriabox.ui.screen

import android.app.Application
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.memoriabox.data.model.Box
import com.memoriabox.data.model.Event
import com.memoriabox.viewmodel.createCalendarViewModel
import com.memoriabox.viewmodel.createMainViewModel
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.clickable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(application: Application) {
    val boxVM = remember { createMainViewModel(application) }
    val eventVM = remember { createCalendarViewModel(application) }
    val boxes by boxVM.boxes.collectAsState(initial = emptyList())
    val events by eventVM.allEvents.collectAsState(initial = emptyList())
    
    var searchQuery by remember { mutableStateOf("") }
    var searchType by remember { mutableStateOf(SearchType.ALL) }
    
    val filteredBoxes = remember(searchQuery, searchType) {
        if (searchType == SearchType.ALL || searchType == SearchType.BOXES) {
            boxes.filter { it.name.contains(searchQuery, ignoreCase = true) }
        } else emptyList()
    }
    
    val filteredEvents = remember(searchQuery, searchType) {
        if (searchType == SearchType.ALL || searchType == SearchType.EVENTS) {
            events.filter { 
                it.name.contains(searchQuery, ignoreCase = true) ||
                it.note.contains(searchQuery, ignoreCase = true)
            }
        } else emptyList()
    }

    // Recent searches
    var recentSearches by remember { mutableStateOf(listOf<String>()) }

    Scaffold(
        topBar = {
            SearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                onSearch = { },
                active = false,
                onActiveChange = {},
                placeholder = { Text("搜索事件、盒子、备注...") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, null)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                // Search suggestions dropdown
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
            
            Divider()
            
            if (searchQuery.isEmpty()) {
                // Show recent searches or trending
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Text("最近搜索", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(12.dp))
                    if (recentSearches.isEmpty()) {
                        Text("暂无搜索历史", style = MaterialTheme.typography.bodyMedium)
                    } else {
                        recentSearches.forEach { query ->
                            AssistChip(
                                onClick = { searchQuery = query },
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
                                subtitle = "盒子 · ${box.icon}",
                                icon = Icons.Default.Folder,
                                onClick = { /* Navigate to box */ }
                            )
                        }
                    }
                    
                    if (searchType == SearchType.ALL || searchType == SearchType.EVENTS) {
                        items(filteredEvents) { event ->
                            SearchResultCard(
                                title = event.name,
                                subtitle = buildEventSubtitle(event),
                                icon = getEventIcon(event),
                                onClick = { /* Navigate to event */ }
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
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
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
            Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.outline)
        }
    }
}

enum class SearchType(val label: String) {
    ALL("全部"),
    BOXES("盒子"),
    EVENTS("事件")
}

fun buildEventSubtitle(event: Event): String {
    val days = when (event.type) {
        com.memoriabox.data.model.EventType.COUNTDOWN -> "还剩${(event.date - System.currentTimeMillis()) / (1000 * 60 * 60 * 24)}天"
        com.memoriabox.data.model.EventType.ANNIVERSARY -> "已过${(System.currentTimeMillis() - event.date) / (1000 * 60 * 60 * 24)}天"
        com.memoriabox.data.model.EventType.ELAPSED -> "已过${(System.currentTimeMillis() - event.date) / (1000 * 60 * 60 * 24)}天"
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


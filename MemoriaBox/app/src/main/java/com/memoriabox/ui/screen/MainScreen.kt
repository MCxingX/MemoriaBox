package com.memoriabox.ui.screen

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import androidx.navigation.compose.rememberNavController
import com.memoriabox.ui.navigation.Screen
import com.memoriabox.ui.navigation.bottomNavItems
import com.memoriabox.ui.screen.components.*
import com.memoriabox.ui.screen.dialogs.BatchSelectDialog
import com.memoriabox.ui.screen.dialogs.BoxDialog
import com.memoriabox.ui.screen.dialogs.EventDialog
import com.memoriabox.ui.screen.dialogs.MoveToBoxDialog
import com.memoriabox.data.model.*
import com.memoriabox.viewmodel.*

@Composable
fun MainScreen(
    application: Application,
    navController: NavHostController = rememberNavController()
) {
    var selectedTab by remember { mutableStateOf(0) }
    val mainViewModel = remember { createMainViewModel(application) }
    val boxes by mainViewModel.boxes.collectAsState(initial = emptyList())
    var showQuickAdd by remember { mutableStateOf(false) }
    var pendingQuickAddType by remember { mutableStateOf<EventType?>(null) }
    fun navigateToRootTab(index: Int, route: String) {
        selectedTab = index
        navController.navigate(route) {
            popUpTo(Screen.Boxes.route) {
                saveState = false
            }
            launchSingleTop = true
            restoreState = false
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar(tonalElevation = 4.dp) {
                bottomNavItems.take(2).forEachIndexed { index, item ->
                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) },
                        selected = selectedTab == index,
                        onClick = {
                            navigateToRootTab(index, item.route)
                        }
                    )
                }
                NavigationBarItem(
                    selected = false,
                    onClick = {
                        showQuickAdd = true
                        pendingQuickAddType = null
                    },
                    icon = {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = MaterialTheme.shapes.large,
                            modifier = Modifier
                                .heightIn(min = 44.dp)
                                .fillMaxWidth(0.92f)
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 10.dp)) {
                                Text(
                                    "(=^.^=)",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    maxLines = 1
                                )
                            }
                        }
                    },
                    label = { Text("", maxLines = 1) }
                )
                bottomNavItems.drop(2).forEachIndexed { offset, item ->
                    val index = offset + 2
                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) },
                        selected = selectedTab == index,
                        onClick = {
                            navigateToRootTab(index, item.route)
                        }
                    )
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.Boxes.route,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(Screen.Boxes.route) {
                BoxesScreen(
                    application = application,
                    onBoxClick = { navController.navigate(Screen.BoxDetail.createRoute(it)) },
                    onNavigateToCalendar = { navController.navigate(Screen.Calendar.route) },
                    onNavigateToFriends = { navController.navigate(Screen.Friends.route) },
                    onNavigateToPhotoWall = { navController.navigate(Screen.PhotoWall.route) },
                    onNavigateToStatistics = { navController.navigate(Screen.Statistics.route) },
                    onNavigateToAiSuggestions = { navController.navigate(Screen.AiSuggestions.route) }
                )
            }
            composable(Screen.BoxDetail.route) { backStackEntry ->
                val boxId = backStackEntry.arguments?.getString("boxId") ?: return@composable
                BoxDetailScreen(
                    application = application,
                    boxId = boxId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(Screen.Calendar.route) {
                val calendarVM = remember { createCalendarViewModel(application) }
                val events by calendarVM.allEvents.collectAsState(initial = emptyList())
                CalendarViewScreen(events = events)
            }
            composable(Screen.Todo.route) {
                TodoScreen(application)
            }
            composable(Screen.Logs.route) {
                LogsScreen(application)
            }
            composable(Screen.Settings.route) {
                SettingsScreen(
                    application = application,
                    onNavigateToStatistics = { navController.navigate(Screen.Statistics.route) },
                    onNavigateToFriends = { navController.navigate(Screen.Friends.route) },
                    onNavigateToPhotoWall = { navController.navigate(Screen.PhotoWall.route) },
                    onNavigateToExport = { navController.navigate(Screen.Export.route) },
                    onNavigateToAiSuggestions = { navController.navigate(Screen.AiSuggestions.route) },
                    onNavigateToAchievements = { navController.navigate(Screen.Achievements.route) },
                    onNavigateToSyncStatus = { navController.navigate(Screen.SyncStatus.route) },
                    onBackupSettingsClick = { navController.navigate(Screen.BackupSettings.route) },
                    onWebDavSettingsClick = { navController.navigate(Screen.WebDavSettings.route) }
                )
            }
            composable(Screen.Statistics.route) {
                StatisticsScreen(application)
            }
            composable(Screen.Friends.route) {
                FriendsScreen(application)
            }
            composable(Screen.PhotoWall.route) {
                PhotoWallScreen(application)
            }
            composable(Screen.Export.route) {
                ExportScreen(application)
            }
            composable(Screen.BackupSettings.route) {
                BackupSettingsScreen(
                    application = application,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(Screen.WebDavSettings.route) {
                WebDavSettingsScreen(
                    application = application,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(Screen.Birthday.route) {
                BirthdayScreen(application)
            }
            composable(Screen.Timeline.route) {
                TimelineScreen(application)
            }
            composable(Screen.AiSuggestions.route) {
                AiSuggestionsScreen(application)
            }
            composable(Screen.Achievements.route) {
                AchievementsScreen(application)
            }
            composable(Screen.SyncStatus.route) {
                SyncStatusScreen(application)
            }
        }
    }

    if (showQuickAdd) {
        val selectedType = pendingQuickAddType
        if (selectedType == null) {
            AddTypePickerDialog(
                onDismiss = { showQuickAdd = false },
                onTypeSelected = { type -> pendingQuickAddType = type }
            )
        } else {
            QuickAddEventDialog(
                boxes = boxes,
                initialType = selectedType,
                application = application,
                onDismiss = {
                    showQuickAdd = false
                    pendingQuickAddType = null
                },
                onSave = { event ->
                    mainViewModel.createQuickEvent(event)
                    pendingQuickAddType = null
                    showQuickAdd = false
                }
            )
        }
    }
}

@Composable
fun AddTypePickerDialog(
    onDismiss: () -> Unit,
    onTypeSelected: (EventType) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择要新增的类型") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AddTypeOption(Icons.Default.Timer, "倒数日", "适合考试、旅行、纪念节点", EventType.COUNTDOWN, onTypeSelected)
                AddTypeOption(Icons.Default.Favorite, "纪念日", "记录恋爱、结婚、相识等重要日子", EventType.ANNIVERSARY, onTypeSelected)
                AddTypeOption(Icons.Default.History, "正计时", "记录已经坚持了多久", EventType.ELAPSED, onTypeSelected)
                AddTypeOption(Icons.Default.Cake, "生日", "支持提前提醒和 PushPlus 推送", EventType.BIRTHDAY, onTypeSelected)
                AddTypeOption(Icons.Default.CheckCircle, "待办", "把重要事项放进时间轴", EventType.TODO, onTypeSelected)
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
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
            modifier = Modifier.padding(14.dp),
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
fun BoxesScreen(
    application: Application,
    onBoxClick: (String) -> Unit,
    onNavigateToCalendar: () -> Unit,
    onNavigateToFriends: () -> Unit,
    onNavigateToPhotoWall: () -> Unit,
    onNavigateToStatistics: () -> Unit,
    onNavigateToAiSuggestions: () -> Unit
) {
    val viewModel = remember { createMainViewModel(application) }
    val friendViewModel = remember { createFriendViewModel(application) }
    val notificationHelper = remember { com.memoriabox.utils.NotificationHelper(application) }
    val boxes by viewModel.boxes.collectAsState(initial = emptyList())
    val events by viewModel.allEvents.collectAsState(initial = emptyList())
    val logs by viewModel.recentLogs.collectAsState(initial = emptyList())
    val friends by friendViewModel.friends.collectAsState(initial = emptyList())
    var showCreateDialog by remember { mutableStateOf(false) }
    var homeTab by remember { mutableIntStateOf(0) }
    var selectedEvent by remember { mutableStateOf<Event?>(null) }
    var eventForActions by remember { mutableStateOf<Event?>(null) }
    var eventForEdit by remember { mutableStateOf<Event?>(null) }
    var eventForDelete by remember { mutableStateOf<Event?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("我的日子") },
                actions = {
                    IconButton(onClick = { showCreateDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "添加分类")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreateDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "添加分类")
            }
        }
    ) { paddingValues ->
        HomeDashboard(
            boxes = boxes,
            events = events,
            friends = friends,
            selectedTab = homeTab,
            onTabSelected = { homeTab = it },
            onBoxClick = onBoxClick,
            onCreateBox = { showCreateDialog = true },
            onNavigateToCalendar = onNavigateToCalendar,
            onNavigateToFriends = onNavigateToFriends,
            onNavigateToPhotoWall = onNavigateToPhotoWall,
            onNavigateToStatistics = onNavigateToStatistics,
            onNavigateToAiSuggestions = onNavigateToAiSuggestions,
            onEventClick = { event -> selectedEvent = event },
            onEventLongClick = { event -> eventForActions = event },
            modifier = Modifier.padding(paddingValues)
        )
    }

    if (showCreateDialog) {
        BoxDialog(
            onDismiss = { showCreateDialog = false },
            onSave = { name, icon, bgType, bgValue ->
                viewModel.createBox(name, icon, bgType, bgValue)
                showCreateDialog = false
            }
        )
    }

    selectedEvent?.let { event ->
        EventDetailDialog(
            event = event,
            logs = logs.filter { it.targetId == event.id },
            onDismiss = { selectedEvent = null },
            onEdit = {
                eventForEdit = event
                selectedEvent = null
            },
            onTogglePin = {
                viewModel.togglePinned(event)
                selectedEvent = null
            },
            onDelete = {
                eventForDelete = event
                selectedEvent = null
            },
            onOpenCategory = {
                selectedEvent = null
                onBoxClick(event.boxId)
            }
        )
    }

    eventForActions?.let { event ->
        EventActionDialog(
            event = event,
            onDismiss = { eventForActions = null },
            onEdit = {
                eventForEdit = event
                eventForActions = null
            },
            onTogglePin = {
                viewModel.togglePinned(event)
                eventForActions = null
            },
            onDelete = {
                eventForDelete = event
                eventForActions = null
            }
        )
    }

    eventForEdit?.let { event ->
        EventDialog(
            existingEvent = event,
            availableBoxes = boxes,
            defaultPushPlusEnabled = notificationHelper.isPushPlusEnabled(),
            onPushPlusEnabledChange = { notificationHelper.setPushPlusEnabled(it) },
            onDismiss = { eventForEdit = null },
            onSave = { updatedEvent ->
                viewModel.updateQuickEvent(updatedEvent)
                eventForEdit = null
            }
        )
    }

    eventForDelete?.let { event ->
        AlertDialog(
            onDismissRequest = { eventForDelete = null },
            title = { Text("删除日子") },
            text = { Text("确认删除“${event.name}”？") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteQuickEvent(event)
                    eventForDelete = null
                }) { Text("删除") }
            },
            dismissButton = {
                TextButton(onClick = { eventForDelete = null }) { Text("取消") }
            }
        )
    }
}

@Composable
fun HomeDashboard(
    boxes: List<com.memoriabox.data.model.Box>,
    events: List<Event>,
    friends: List<com.memoriabox.data.model.Friend>,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    onBoxClick: (String) -> Unit,
    onCreateBox: () -> Unit,
    onNavigateToCalendar: () -> Unit,
    onNavigateToFriends: () -> Unit,
    onNavigateToPhotoWall: () -> Unit,
    onNavigateToStatistics: () -> Unit,
    onNavigateToAiSuggestions: () -> Unit,
    onEventClick: (Event) -> Unit,
    onEventLongClick: (Event) -> Unit,
    modifier: Modifier = Modifier
) {
    val sortedEvents = remember(events) {
        events.sortedBy { kotlin.math.abs(it.date - System.currentTimeMillis()) }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("常用功能", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(10.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                HomeShortcutCard(Icons.Default.CalendarToday, "日历", "按月查看所有提醒", onNavigateToCalendar, Modifier.weight(1f))
                HomeShortcutCard(Icons.Default.People, "好友", "生日和好友标签", onNavigateToFriends, Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                HomeShortcutCard(Icons.Default.PhotoLibrary, "照片墙", "查看照片回忆", onNavigateToPhotoWall, Modifier.weight(1f))
                HomeShortcutCard(Icons.Default.BarChart, "统计", "记录趋势总览", onNavigateToStatistics, Modifier.weight(1f))
            }
            HomeShortcutCard(Icons.Default.AutoAwesome, "智能建议", "提醒、生日、待办整理建议", onNavigateToAiSuggestions, Modifier.fillMaxWidth())
        }

        Spacer(Modifier.height(20.dp))
        TabRow(selectedTabIndex = selectedTab) {
            listOf("全部", "我的分类", "好友组").forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { onTabSelected(index) },
                    text = { Text(title) }
                )
            }
        }

        Spacer(Modifier.height(24.dp))
        when (selectedTab) {
            0 -> AllEventsTab(events = sortedEvents, onEventClick = onEventClick, onEventLongClick = onEventLongClick)
            1 -> CategoryFoldersTab(boxes = boxes, onBoxClick = onBoxClick, onCreateBox = onCreateBox)
            2 -> FriendGroupsTab(friends = friends, onNavigateToFriends = onNavigateToFriends)
        }
    }
}

@Composable
fun AllEventsTab(events: List<Event>, onEventClick: (Event) -> Unit, onEventLongClick: (Event) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("全部日子", style = MaterialTheme.typography.titleMedium)
        Text("${events.size} 个", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    Spacer(Modifier.height(8.dp))
    if (events.isEmpty()) {
        EmptyEventListHint()
    } else {
        events.take(12).forEach { event ->
            HomeEventRow(event = event, onClick = { onEventClick(event) }, onLongClick = { onEventLongClick(event) })
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
fun CategoryFoldersTab(
    boxes: List<com.memoriabox.data.model.Box>,
    onBoxClick: (String) -> Unit,
    onCreateBox: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("我的分类", style = MaterialTheme.typography.titleMedium)
        TextButton(onClick = onCreateBox) { Text("新建分类") }
    }
    Spacer(Modifier.height(8.dp))
    if (boxes.isEmpty()) {
        Text("还没有分类，新增日子会自动保存到默认分类。", style = MaterialTheme.typography.bodyMedium)
    } else {
        BoxList(
            boxes = boxes,
            onBoxClick = onBoxClick,
            onCreateBox = onCreateBox,
            modifier = Modifier.fillMaxWidth(),
            showCreateButton = false
        )
    }
}

@Composable
fun FriendGroupsTab(
    friends: List<com.memoriabox.data.model.Friend>,
    onNavigateToFriends: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("好友组", style = MaterialTheme.typography.titleMedium)
        TextButton(onClick = onNavigateToFriends) { Text("管理好友") }
    }
    Spacer(Modifier.height(8.dp))
    if (friends.isEmpty()) {
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("还没有好友组", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(4.dp))
                Text("添加好友后，可以按 Bestie、Colleague 等标签整理生日和纪念日。", style = MaterialTheme.typography.bodyMedium)
            }
        }
    } else {
        friends.take(8).forEach { friend ->
            Card(modifier = Modifier.fillMaxWidth().clickable { onNavigateToFriends() }) {
                Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(48.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(friend.name.firstOrNull()?.toString() ?: "F", style = MaterialTheme.typography.titleLarge)
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(friend.name, style = MaterialTheme.typography.titleMedium)
                        Text(
                            if (friend.birthdayDate == null) "未设置生日" else "生日已记录",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
fun HomeShortcutCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.primaryContainer) {
                Icon(icon, contentDescription = null, modifier = Modifier.padding(8.dp).size(20.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, maxLines = 1)
                Text(description, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
        }
    }
}

@Composable
fun EmptyEventListHint() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("还没有倒数日", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(4.dp))
            Text("点击底部中间的 (=^.^=)，记录纪念日、生日、倒数日或待办。", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeEventRow(event: Event, onClick: () -> Unit, onLongClick: () -> Unit) {
    val days = com.memoriabox.ui.screen.components.calculateDays(event.date, event.type)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(116.dp)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.secondaryContainer
                        )
                    )
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.82f),
                    modifier = Modifier.size(64.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Text(days.toString(), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                        Text("天", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(event.name, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                    Text(
                        text = listOfNotNull(
                            if (event.isPinned) "置顶" else null,
                            eventTypeLabel(event.type),
                            com.memoriabox.ui.screen.components.formatDate(event.date)
                        ).joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (event.reminderEnabled) {
                        Text(
                            text = "提前 ${event.reminderDays} 天提醒，PushPlus 按设置同步推送",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

fun eventTypeLabel(type: EventType): String = when (type) {
    EventType.COUNTDOWN -> "倒数日"
    EventType.ANNIVERSARY -> "纪念日"
    EventType.ELAPSED -> "正计时"
    EventType.BIRTHDAY -> "生日"
    EventType.TODO -> "待办"
}

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
    val days = com.memoriabox.ui.screen.components.calculateDays(event.date, event.type)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(event.name) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
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
                            .padding(18.dp)
                    ) {
                        Text(days.toString(), style = MaterialTheme.typography.displayMedium, color = com.memoriabox.utils.ColorUtils.hexToColor(event.textColor))
                        Text("天 · ${eventTypeLabel(event.type)}", style = MaterialTheme.typography.titleMedium, color = com.memoriabox.utils.ColorUtils.hexToColor(event.textColor))
                    }
                }
                DetailLine("类型", eventTypeLabel(event.type))
                DetailLine("日期", com.memoriabox.ui.screen.components.formatDate(event.date))
                event.lunar?.let { DetailLine("农历", it) }
                if (event.note.isNotBlank()) DetailLine("备注", event.note)
                DetailLine("提醒", if (event.reminderEnabled) "提前 ${event.reminderDays} 天" else "关闭")
                DetailLine("重复", repeatModeLabel(event))
                DetailLine("PushPlus", if (event.pushPlusEnabled) "开启" else "关闭")
                if (event.isPinned) DetailLine("状态", "已置顶")
                DetailLine("模板", cardTemplateLabel(event.cardTemplate))
                HorizontalDivider()
                Text("历史记录", style = MaterialTheme.typography.titleSmall)
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
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = {
                    shareBitmap(context, generateEventCardBitmap(context, event, days.coerceAtLeast(0)))
                }) { Text("分享图") }
                TextButton(onClick = {
                    val shareText = "${event.name}\n${eventTypeLabel(event.type)}：${days} 天\n日期：${com.memoriabox.ui.screen.components.formatDate(event.date)}\n${event.note}"
                    context.startActivity(
                        Intent.createChooser(
                            Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, shareText)
                            },
                            "分享日子"
                        )
                    )
                }) { Text("分享") }
                TextButton(onClick = onEdit) { Text("编辑") }
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = onOpenCategory) { Text("分类") }
                TextButton(onClick = onTogglePin) { Text(if (event.isPinned) "取消置顶" else "置顶") }
                TextButton(onClick = onDelete) { Text("删除") }
            }
        }
    )
}

fun repeatModeLabel(event: Event): String = when {
    event.repeatMode == RepeatMode.YEARLY || event.repeatYearly -> "每年重复"
    event.repeatMode == RepeatMode.MONTHLY -> "每月重复"
    event.repeatMode == RepeatMode.CUSTOM_DAYS -> "每 ${event.repeatInterval} 天重复"
    event.repeatMode == RepeatMode.CUSTOM_WEEKS -> "每 ${event.repeatInterval} 周重复"
    event.repeatMode == RepeatMode.CUSTOM_MONTHS -> "每 ${event.repeatInterval} 个月重复"
    event.type == EventType.BIRTHDAY -> "每年重复"
    else -> "不重复"
}

fun cardTemplateLabel(template: String): String = when (template) {
    "POSTER" -> "海报"
    "GLASS" -> "玻璃"
    "SPLIT" -> "分栏"
    else -> "大图"
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
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(event.name) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("选择要执行的操作", style = MaterialTheme.typography.bodyMedium)
                Button(onClick = onEdit, modifier = Modifier.fillMaxWidth()) { Text("编辑") }
                OutlinedButton(onClick = onTogglePin, modifier = Modifier.fillMaxWidth()) {
                    Text(if (event.isPinned) "取消置顶" else "置顶")
                }
                OutlinedButton(onClick = onDelete, modifier = Modifier.fillMaxWidth()) { Text("删除") }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
fun QuickAddEventDialog(
    boxes: List<com.memoriabox.data.model.Box>,
    initialType: EventType,
    application: Application,
    onDismiss: () -> Unit,
    onSave: (Event) -> Unit
) {
    val notificationHelper = remember { com.memoriabox.utils.NotificationHelper(application) }
    EventDialog(
        availableBoxes = boxes,
        defaultBoxId = boxes.firstOrNull()?.id ?: "",
        defaultType = initialType,
        defaultDate = System.currentTimeMillis(),
        defaultReminderEnabled = true,
        defaultPushPlusEnabled = notificationHelper.isPushPlusEnabled(),
        allowTypeChange = false,
        onPushPlusEnabledChange = { notificationHelper.setPushPlusEnabled(it) },
        onDismiss = onDismiss,
        onSave = onSave
    )
}

@Composable
fun BoxDetailScreen(
    application: Application,
    boxId: String,
    onNavigateBack: () -> Unit
) {
    val viewModel = remember { createBoxDetailViewModel(application) }
    val events by viewModel.events.collectAsState(initial = emptyList())
    val box by viewModel.box.collectAsState()
    val layoutMode by viewModel.layoutMode.collectAsState()
    val notificationHelper = remember { com.memoriabox.utils.NotificationHelper(application) }

    var showCreateEvent by remember { mutableStateOf(false) }
    var showEditEvent by remember { mutableStateOf<Event?>(null) }
    var showBatchDialog by remember { mutableStateOf(false) }
    var showMoveDialog by remember { mutableStateOf(false) }
    var selectedEvents by remember { mutableStateOf<Set<String>>(emptySet()) }

    LaunchedEffect(boxId) {
        viewModel.loadBox(boxId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(box?.name ?: "日子详情") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { showBatchDialog = true }) {
                        Icon(Icons.Default.Checklist, contentDescription = "批量操作")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreateEvent = true }) {
                Icon(Icons.Default.Add, contentDescription = "添加事件")
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            Text(
                text = "拖拽卡片可切换展示样式，松手后自动应用",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            EnhancedEventGrid(
                events = events,
                layoutMode = layoutMode,
                onEventClick = { showEditEvent = it },
                onEventEdit = { showEditEvent = it }
            )
        }
    }

    if (showCreateEvent) {
        val currentBox = box
        EventDialog(
            existingEvent = null,
            availableBoxes = if (currentBox != null) listOf(currentBox) else emptyList(),
            defaultPushPlusEnabled = notificationHelper.isPushPlusEnabled(),
            onPushPlusEnabledChange = { notificationHelper.setPushPlusEnabled(it) },
            onDismiss = { showCreateEvent = false },
            onSave = { event ->
                viewModel.createEvent(event)
                showCreateEvent = false
            }
        )
    }

    showEditEvent?.let { event ->
        val currentBox = box
        EventDialog(
            existingEvent = event,
            availableBoxes = if (currentBox != null) listOf(currentBox) else emptyList(),
            defaultPushPlusEnabled = notificationHelper.isPushPlusEnabled(),
            onPushPlusEnabledChange = { notificationHelper.setPushPlusEnabled(it) },
            onDismiss = { showEditEvent = null },
            onSave = { updatedEvent ->
                viewModel.updateEvent(updatedEvent)
                showEditEvent = null
            }
        )
    }

    if (showBatchDialog) {
        BatchSelectDialog(
            events = events,
            selectedEvents = selectedEvents,
            onSelectionChange = { selectedEvents = it },
            onDismiss = { showBatchDialog = false },
            onBatchDelete = {
                viewModel.deleteEvents(selectedEvents)
                selectedEvents = emptySet()
                showBatchDialog = false
            },
            onBatchMove = { showMoveDialog = true },
            onBatchEdit = {
                showEditEvent = events.firstOrNull { selectedEvents.contains(it.id) }
                showBatchDialog = false
            }
        )
    }

    if (showMoveDialog) {
        MoveToBoxDialog(
            boxes = box?.let { listOf(it) } ?: emptyList(),
            onDismiss = { showMoveDialog = false },
            onBoxSelected = { targetBoxId ->
                viewModel.moveEvents(selectedEvents, targetBoxId)
                selectedEvents = emptySet()
                showMoveDialog = false
                showBatchDialog = false
            }
        )
    }
}

@Composable
fun TodoScreen(application: Application) {
    val todoVM = remember { createTodoViewModel(application) }
    val todoEvents by todoVM.todoEvents.collectAsState(initial = emptyList())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("待办事项") },
            )
        }
    ) { paddingValues ->
        TodoListView(
            events = todoEvents,
            onToggleStatus = { todoVM.toggleTodoStatus(it) },
            modifier = Modifier.padding(paddingValues)
        )
    }
}

@Composable
fun LogsScreen(application: Application) {
    val viewModel = remember { createLogViewModel(application) }
    val logs by viewModel.logs.collectAsState(initial = emptyList())
    var filter by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        LogFilterBar(
            onFilterChange = {
                filter = it
                viewModel.setFilter(it)
            },
            onDateRangeChange = { }
        )
        LogsList(logs = logs)
    }
}

@Composable
fun SettingsScreen(
    application: Application,
    onNavigateToStatistics: () -> Unit,
    onNavigateToFriends: () -> Unit,
    onNavigateToPhotoWall: () -> Unit,
    onNavigateToExport: () -> Unit,
    onNavigateToAiSuggestions: () -> Unit,
    onNavigateToAchievements: () -> Unit,
    onNavigateToSyncStatus: () -> Unit,
    onBackupSettingsClick: () -> Unit,
    onWebDavSettingsClick: () -> Unit
) {
    val pushPlusHelper = remember { com.memoriabox.utils.NotificationHelper(application) }
    var pushPlusToken by remember { mutableStateOf(pushPlusHelper.getPushPlusToken()) }
    var pushPlusEnabled by remember { mutableStateOf(pushPlusHelper.isPushPlusEnabled()) }
    var pushPlusChannel by remember { mutableStateOf(pushPlusHelper.getPushPlusChannel()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Data section
        SettingsItem(
            icon = Icons.Default.BarChart,
            title = "数据统计",
            description = "查看事件统计和趋势",
            onClick = onNavigateToStatistics
        )
        SettingsItem(
            icon = Icons.Default.People,
            title = "好友管理",
            description = "管理好友信息和标签",
            onClick = onNavigateToFriends
        )
        SettingsItem(
            icon = Icons.Default.PhotoLibrary,
            title = "照片墙",
            description = "查看所有事件照片",
            onClick = onNavigateToPhotoWall
        )
        SettingsItem(
            icon = Icons.Default.Share,
            title = "导出分享",
            description = "导出数据和分享图片",
            onClick = onNavigateToExport
        )
        SettingsItem(
            icon = Icons.Default.AutoAwesome,
            title = "AI 智能建议",
            description = "根据事件和提醒状态给出整理建议",
            onClick = onNavigateToAiSuggestions
        )
        SettingsItem(
            icon = Icons.Default.EmojiEvents,
            title = "成就系统",
            description = "查看记录事件、生日、照片等成就进度",
            onClick = onNavigateToAchievements
        )
        SettingsItem(
            icon = Icons.Default.CloudSync,
            title = "多设备同步",
            description = "查看 WebDAV 跨设备同步状态和建议",
            onClick = onNavigateToSyncStatus
        )
        
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        
        SettingsItem(
            icon = Icons.Default.Backup,
            title = "备份设置",
            description = "本地备份、导入导出",
            onClick = onBackupSettingsClick
        )
        SettingsItem(
            icon = Icons.Default.Cloud,
            title = "WebDAV 同步",
            description = "配置云端同步服务",
            onClick = onWebDavSettingsClick
        )
        
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        
        // PushPlus settings inline
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("PushPlus 推送", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("启用 PushPlus")
                    Switch(
                        checked = pushPlusEnabled,
                        onCheckedChange = { 
                            pushPlusEnabled = it
                            pushPlusHelper.setPushPlusEnabled(it)
                        }
                    )
                }
                if (pushPlusEnabled) {
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = pushPlusToken,
                        onValueChange = { 
                            pushPlusToken = it
                            pushPlusHelper.setPushPlusToken(it)
                        },
                        label = { Text("Token") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    Row {
                        listOf("wechat", "webhook", "mail", "sms").forEach { ch ->
                            FilterChip(
                                selected = pushPlusChannel == ch,
                                onClick = { 
                                    pushPlusChannel = ch
                                    pushPlusHelper.setPushPlusChannel(ch)
                                },
                                label = { Text(ch.take(3)) }
                            )
                        }
                    }
                }
            }
        }
        
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        
        SettingsItem(
            icon = Icons.Default.Info,
            title = "关于",
            description = "版本 1.0.0"
        )
    }
}

@Composable
fun BackupSettingsScreen(
    application: Application,
    onNavigateBack: () -> Unit
) {
    val viewModel = remember { createBackupViewModel(application) }
    var showDirPicker by remember { mutableStateOf(false) }
    var showExportPicker by remember { mutableStateOf(false) }
    var showImportPicker by remember { mutableStateOf(false) }

    val dirPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let { viewModel.saveBackupDirUri(it) }
    }

    val exportPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let { 
            viewModel.triggerManualBackup(it)
        }
    }

    val importPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.importBackup(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("备份设置") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { paddingValues ->
        BackupSettingsContent(
            modifier = Modifier.padding(paddingValues),
            onSelectDir = { showDirPicker = true },
            onManualBackup = { showExportPicker = true },
            onImport = { showImportPicker = true }
        )
    }
}

@Composable
fun WebDavSettingsScreen(
    application: Application,
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("WebDAV 设置") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { paddingValues ->
        WebDavSettingsContent(
            modifier = Modifier.padding(paddingValues)
        )
    }
}

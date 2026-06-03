package com.memoriabox.ui.screen

import android.app.Application
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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
    var mascotIndex by remember { mutableIntStateOf(0) }
    val mascotMessages = remember {
        listOf(
            "(^_^) 记一个小日子",
            "(o.o) 今天也重要",
            "(^_-) 新回忆来啦",
            "(=^.^=) 喵，添加吧",
            "(*_*) 珍藏这一刻",
            "(>_<) 别忘记呀",
            "(^o^) 特别的一天",
            "(•_•) 认真记下"
        )
    }

    fun nextMascot() {
        mascotIndex = (mascotIndex + 1) % mascotMessages.size
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
                            nextMascot()
                            selectedTab = index
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true }
                        }
                    )
                }
                NavigationBarItem(
                    selected = false,
                    onClick = {
                        nextMascot()
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
                            Column(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Default.AddCircle,
                                    contentDescription = "新增日子",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(22.dp)
                                )
                                Text(
                                    mascotMessages[mascotIndex],
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    maxLines = 2,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    },
                    label = { Text("新增", maxLines = 1) }
                )
                bottomNavItems.drop(2).forEachIndexed { offset, item ->
                    val index = offset + 2
                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) },
                        selected = selectedTab == index,
                        onClick = {
                            nextMascot()
                            selectedTab = index
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true }
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
    val boxes by viewModel.boxes.collectAsState(initial = emptyList())
    val events by viewModel.allEvents.collectAsState(initial = emptyList())
    var showCreateDialog by remember { mutableStateOf(false) }

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
            onBoxClick = onBoxClick,
            onCreateBox = { showCreateDialog = true },
            onNavigateToCalendar = onNavigateToCalendar,
            onNavigateToFriends = onNavigateToFriends,
            onNavigateToPhotoWall = onNavigateToPhotoWall,
            onNavigateToStatistics = onNavigateToStatistics,
            onNavigateToAiSuggestions = onNavigateToAiSuggestions,
            onEventClick = { event -> onBoxClick(event.boxId) },
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
}

@Composable
fun HomeDashboard(
    boxes: List<com.memoriabox.data.model.Box>,
    events: List<Event>,
    onBoxClick: (String) -> Unit,
    onCreateBox: () -> Unit,
    onNavigateToCalendar: () -> Unit,
    onNavigateToFriends: () -> Unit,
    onNavigateToPhotoWall: () -> Unit,
    onNavigateToStatistics: () -> Unit,
    onNavigateToAiSuggestions: () -> Unit,
    onEventClick: (Event) -> Unit,
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
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            HomeShortcutCard(Icons.Default.CalendarToday, "日历", onNavigateToCalendar)
            HomeShortcutCard(Icons.Default.People, "好友", onNavigateToFriends)
            HomeShortcutCard(Icons.Default.PhotoLibrary, "照片墙", onNavigateToPhotoWall)
            HomeShortcutCard(Icons.Default.BarChart, "统计", onNavigateToStatistics)
            HomeShortcutCard(Icons.Default.AutoAwesome, "建议", onNavigateToAiSuggestions)
        }

        Spacer(Modifier.height(24.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("我的日子", style = MaterialTheme.typography.titleMedium)
            Text("${events.size} 个", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(8.dp))
        if (sortedEvents.isEmpty()) {
            EmptyEventListHint()
        } else {
            sortedEvents.take(8).forEach { event ->
                HomeEventRow(event = event, onClick = { onEventClick(event) })
                Spacer(Modifier.height(8.dp))
            }
        }

        Spacer(Modifier.height(24.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("分类文件夹", style = MaterialTheme.typography.titleMedium)
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
}

@Composable
fun HomeShortcutCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    onClick: () -> Unit
) {
    AssistChip(
        onClick = onClick,
        leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp)) },
        label = { Text(title) }
    )
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
            Text("点击底部中间的“新增”，记录纪念日、生日、倒数日或待办。", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun HomeEventRow(event: Event, onClick: () -> Unit) {
    val days = com.memoriabox.ui.screen.components.calculateDays(event.date, event.type)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(116.dp)
            .clickable(onClick = onClick)
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
                        text = "${eventTypeLabel(event.type)} · ${com.memoriabox.ui.screen.components.formatDate(event.date)}",
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

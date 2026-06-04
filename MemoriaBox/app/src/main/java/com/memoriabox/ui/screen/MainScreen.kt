package com.memoriabox.ui.screen

import android.app.Application
import android.content.Context
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.memoriabox.ui.navigation.BottomNavigationItem
import com.memoriabox.ui.navigation.Screen
import com.memoriabox.ui.navigation.bottomNavItems
import com.memoriabox.ui.screen.components.*
import com.memoriabox.ui.screen.dialogs.BatchSelectDialog
import com.memoriabox.ui.screen.dialogs.BoxDialog
import com.memoriabox.ui.screen.dialogs.EventDialog
import com.memoriabox.ui.screen.dialogs.MoveToBoxDialog
import com.memoriabox.ui.utils.AdaptiveUiSize
import com.memoriabox.ui.utils.rememberAdaptiveUiSize
import com.memoriabox.data.model.*
import com.memoriabox.ui.theme.AppThemeMode
import com.memoriabox.utils.AppSettings
import com.memoriabox.viewmodel.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun MainScreen(
    application: Application,
    currentThemeMode: AppThemeMode = AppThemeMode.BLUE_WHITE,
    onThemeModeChange: (AppThemeMode) -> Unit = {},
    navController: NavHostController = rememberNavController()
) {
    val context = LocalContext.current
    val settingsVersion = AppSettings.settingsVersion
    var selectedTab by remember { mutableStateOf(0) }
    val mainViewModel = remember { createMainViewModel(application) }
    val boxes by mainViewModel.boxes.collectAsState(initial = emptyList())
    val logs by mainViewModel.recentLogs.collectAsState(initial = emptyList())
    var showQuickAdd by remember { mutableStateOf(false) }
    var pendingQuickAddType by remember { mutableStateOf<EventType?>(null) }
    val cuteTexts = remember {
        listOf(
            "(=^.^=)", "(˶ᵔ ᵕ ᵔ˶)", "ฅ^•ﻌ•^ฅ", "(｡•̀ᴗ-)✧",
            "(≧▽≦)", "(๑˃̵ᴗ˂̵)", "(｡･ω･｡)", "(ง •̀_•́)ง",
            "(づ｡◕‿‿◕｡)づ", "(๑•̀ㅂ•́)و✧", "(＾▽＾)", "(｡♥‿♥｡)",
            "(っ˘ω˘ς )", "(ﾉ◕ヮ◕)ﾉ*:･ﾟ", "(๑>◡<๑)", "(๑˘︶˘๑)",
            "(´｡• ᵕ •｡`)", "(ฅ'ω'ฅ)", "(๑• . •๑)", "(｡･∀･)ﾉﾞ",
            "(ง˙∇˙)ว", "(ﾉ´ヮ`)ﾉ*:･ﾟ", "(๑¯ω¯๑)", "(＾ω＾)",
            "(◍•ᴗ•◍)", "(๑´ㅂ`๑)", "(｡･ω･｡)ﾉ", "(｀・ω・´)"
        )
    }
    var cuteTextIndex by rememberSaveable { mutableIntStateOf(0) }
    fun nextCuteText() {
        cuteTextIndex = (cuteTextIndex + 1) % cuteTexts.size
    }
    fun navigateToRootTab(index: Int, route: String) {
        selectedTab = index
        nextCuteText()
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
                        icon = { CustomBottomNavIcon(item = item, context = context, version = settingsVersion) },
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
                        nextCuteText()
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
                                    cuteTexts[cuteTextIndex],
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
                        icon = { CustomBottomNavIcon(item = item, context = context, version = settingsVersion) },
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
                val diaries by calendarVM.allDiaries.collectAsState(initial = emptyList())
                val diaryMedia by calendarVM.selectedDiaryMedia.collectAsState(initial = emptyList())
                var addDateFromCalendar by remember { mutableStateOf<Long?>(null) }
                var selectedCalendarEvent by remember { mutableStateOf<Event?>(null) }
                var editCalendarEvent by remember { mutableStateOf<Event?>(null) }

                val diaryMediaMap = remember(diaryMedia) {
                    diaryMedia.groupBy { it.diaryId }
                }

                ScreenBgWrapper(context = androidx.compose.ui.platform.LocalContext.current, page = "CALENDAR") {
                    CalendarViewScreen(
                        events = events,
                        diaries = diaries,
                        diaryMediaMap = diaryMediaMap,
                        onAddEvent = { date -> addDateFromCalendar = date },
                        onEventClick = { event -> selectedCalendarEvent = event },
                        onSaveDiary = { date, content, mediaUris, backgroundUri ->
                            calendarVM.saveDiary(date, content, mediaUris, backgroundUri)
                        },
                        onLoadDiaryMedia = { diary -> calendarVM.loadDiaryMedia(diary.id) }
                    )
                }
                addDateFromCalendar?.let { date ->
                    EventDialog(
                        availableBoxes = boxes,
                        defaultDate = date,
                        defaultType = EventType.COUNTDOWN,
                        onDismiss = { addDateFromCalendar = null },
                        onSave = { event ->
                            mainViewModel.createQuickEvent(event)
                            addDateFromCalendar = null
                        }
                    )
                }
                editCalendarEvent?.let { event ->
                    EventDialog(
                        existingEvent = event,
                        availableBoxes = boxes,
                        onDismiss = { editCalendarEvent = null },
                        onSave = { updated ->
                            mainViewModel.updateQuickEvent(updated)
                            editCalendarEvent = null
                        }
                    )
                }
                selectedCalendarEvent?.let { event ->
                    EventDetailDialog(
                        event = event,
                        logs = logs.filter { it.targetId == event.id },
                        onDismiss = { selectedCalendarEvent = null },
                        onEdit = {
                            selectedCalendarEvent = null
                            editCalendarEvent = event
                        },
                        onTogglePin = {
                            mainViewModel.togglePinned(event)
                            selectedCalendarEvent = null
                        },
                        onDelete = {
                            mainViewModel.deleteQuickEvent(event)
                            selectedCalendarEvent = null
                        },
                        onOpenCategory = {
                            selectedCalendarEvent = null
                            navController.navigate(Screen.BoxDetail.createRoute(event.boxId))
                        }
                    )
                }
            }
            composable(Screen.Todo.route) {
                ScreenBgWrapper(context = androidx.compose.ui.platform.LocalContext.current, page = "TODO") {
                    TodoScreen(application)
                }
            }
            composable(Screen.Logs.route) {
                ScreenBgWrapper(context = androidx.compose.ui.platform.LocalContext.current, page = "LOGS") {
                    LogsScreen(application)
                }
            }
            composable(Screen.Settings.route) {
                ScreenBgWrapper(context = androidx.compose.ui.platform.LocalContext.current, page = "SETTINGS") {
                    SettingsScreen(
                    application = application,
                    currentThemeMode = currentThemeMode,
                    onThemeModeChange = onThemeModeChange,
                    onNavigateToStatistics = { navController.navigate(Screen.Statistics.route) },
                    onNavigateToFriends = { navController.navigate(Screen.Friends.route) },
                    onNavigateToPhotoWall = { navController.navigate(Screen.PhotoWall.route) },
                    onNavigateToExport = { navController.navigate(Screen.Export.route) },
                    onNavigateToAiSuggestions = { navController.navigate(Screen.AiSuggestions.route) },
                    onNavigateToAchievements = { navController.navigate(Screen.Achievements.route) },
                    onNavigateToSyncStatus = { navController.navigate(Screen.SyncStatus.route) },
                    onNavigateToDayTools = { navController.navigate(Screen.DayTools.route) },
                    onNavigateToCustomization = { navController.navigate(Screen.CustomizationSettings.route) },
                    onBackupSettingsClick = { navController.navigate(Screen.BackupSettings.route) },
                    onWebDavSettingsClick = { navController.navigate(Screen.WebDavSettings.route) }
                )
                }
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
            composable(Screen.DayTools.route) {
                DayToolsScreen(
                    application = application,
                    onNavigateToCalendar = { navController.navigate(Screen.Calendar.route) },
                    onNavigateToPhotoWall = { navController.navigate(Screen.PhotoWall.route) },
                    onNavigateToExport = { navController.navigate(Screen.Export.route) }
                )
            }
            composable(Screen.CustomizationSettings.route) {
                CustomizationSettingsScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
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
private fun CustomBottomNavIcon(item: BottomNavigationItem, context: Context, version: Int) {
    val iconUri = remember(version, item.route) {
        when (item.route) {
            Screen.Boxes.route -> AppSettings.getHomeIconUri(context)
            Screen.Calendar.route -> AppSettings.getCalendarIconUri(context)
            Screen.Todo.route -> AppSettings.getTodoIconUri(context)
            Screen.Settings.route -> AppSettings.getSettingsIconUri(context)
            else -> null
        }
    }
    if (iconUri.isNullOrBlank()) {
        Icon(item.icon, contentDescription = item.label)
    } else {
        AsyncImage(
            model = iconUri,
            contentDescription = item.label,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(24.dp).clip(MaterialTheme.shapes.small)
        )
    }
}

@Composable
fun AddTypePickerDialog(
    onDismiss: () -> Unit,
    onTypeSelected: (EventType) -> Unit,
    onCreateBox: (() -> Unit)? = null
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择要新增的类型") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                onCreateBox?.let { createBox ->
                    AddActionOption(Icons.Default.CreateNewFolder, "新增分类分组", "用来收纳不同类型的小日子", onClick = createBox)
                }
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
fun AddActionOption(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.secondaryContainer) {
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.padding(10.dp).size(24.dp),
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
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
    var showQuickAdd by remember { mutableStateOf(false) }
    var pendingQuickAddType by remember { mutableStateOf<EventType?>(null) }
    var selectedBoxId by rememberSaveable { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val adaptiveUi = rememberAdaptiveUiSize()
    val settingsVersion = AppSettings.settingsVersion
    val homeBgUri = remember(settingsVersion) { AppSettings.getHomeBgUri(context) }

    Box(modifier = Modifier.fillMaxSize()) {
        if (!homeBgUri.isNullOrBlank()) {
            AsyncImage(model = homeBgUri, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.matchParentSize())
            Box(modifier = Modifier.matchParentSize().background(Color.Black.copy(alpha = 0.32f)))
        } else {
            Box(
                modifier = Modifier.matchParentSize().background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
            )
        }

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    modifier = Modifier.height(adaptiveUi.topBarHeight),
                    title = { Text("我的日子") },
                    windowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp),
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                    actions = {
                        IconButton(onClick = { showCreateDialog = true }) {
                            Icon(Icons.Default.Add, contentDescription = "添加分类")
                        }
                    }
                )
            },
            floatingActionButton = {
                FloatingActionButton(onClick = {
                    showQuickAdd = true
                    pendingQuickAddType = null
                }) {
                    Icon(Icons.Default.Add, contentDescription = "添加日子")
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
                selectedBoxId = selectedBoxId,
                onBoxSelected = { selectedBoxId = it },
                adaptiveUi = adaptiveUi,
                modifier = Modifier.padding(paddingValues)
            )
        }
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
                defaultBoxId = selectedBoxId,
                onDismiss = {
                    showQuickAdd = false
                    pendingQuickAddType = null
                },
                onSave = { event ->
                    viewModel.createQuickEvent(event)
                    showQuickAdd = false
                    pendingQuickAddType = null
                }
            )
        }
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
            text = { Text("删除后无法在应用内恢复。确认删除“${event.name}”？") },
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
    selectedBoxId: String?,
    onBoxSelected: (String?) -> Unit,
    onBoxClick: (String) -> Unit,
    onCreateBox: () -> Unit,
    onNavigateToCalendar: () -> Unit,
    onNavigateToFriends: () -> Unit,
    onNavigateToPhotoWall: () -> Unit,
    onNavigateToStatistics: () -> Unit,
    onNavigateToAiSuggestions: () -> Unit,
    onEventClick: (Event) -> Unit,
    onEventLongClick: (Event) -> Unit,
    adaptiveUi: AdaptiveUiSize,
    modifier: Modifier = Modifier
) {
    val sortedEvents = remember(events) {
        events.sortedWith(
            compareByDescending<Event> { it.isPinned }
                .thenBy { kotlin.math.abs(it.date - System.currentTimeMillis()) }
        )
    }
    val visibleEvents = remember(sortedEvents, selectedBoxId) {
        selectedBoxId?.let { boxId -> sortedEvents.filter { it.boxId == boxId } } ?: sortedEvents
    }
    val selectedBoxName = selectedBoxId?.let { id -> boxes.firstOrNull { it.id == id }?.name } ?: "全部分组"

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = adaptiveUi.screenPadding, vertical = adaptiveUi.sectionSpacing)
        ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("全部日子", style = MaterialTheme.typography.titleMedium)
                Text("${visibleEvents.size} 个 · $selectedBoxName", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            HomeBoxFilter(
                boxes = boxes,
                selectedBoxId = selectedBoxId,
                onBoxSelected = onBoxSelected,
                onCreateBox = onCreateBox
            )
        }
        Spacer(Modifier.height(adaptiveUi.sectionSpacing))
        HomeHeroCard(
            onEventsClick = { onTabSelected(0) },
            onFriendsClick = onNavigateToFriends,
            onBoxesClick = { onTabSelected(1) },
            adaptiveUi = adaptiveUi
        )
        Spacer(Modifier.height(adaptiveUi.sectionSpacing))
        AllEventsTab(events = visibleEvents, onEventClick = onEventClick, onEventLongClick = onEventLongClick)
        }
    }
}

@Composable
fun HomeHeroCard(
    onEventsClick: () -> Unit = {},
    onFriendsClick: () -> Unit = {},
    onBoxesClick: () -> Unit = {},
    adaptiveUi: AdaptiveUiSize
) {
    val context = LocalContext.current
    val settingsVersion = AppSettings.settingsVersion
    val useCustom = remember(settingsVersion) { AppSettings.getUseCustomQuote(context) }
    val customQuotes = remember(settingsVersion) { AppSettings.getCustomDailyQuotes(context) }
    val dailyQuote = if (useCustom && customQuotes.isNotEmpty()) {
        val dayOfYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
        DailyQuote("每日语录", customQuotes[dayOfYear % customQuotes.size])
    } else {
        val quotes = listOf(
            DailyQuote("晨光收藏家", "把今天第一束光，放进值得纪念的小盒子。"),
            DailyQuote("小日子巡航", "慢慢走，也能抵达很多闪闪发亮的时刻。"),
            DailyQuote("温柔提醒员", "重要的日子会来，温柔的准备也会来。"),
            DailyQuote("今天有糖", "给平凡的一天加一点甜，再记下一点心动。"),
            DailyQuote("好运存档中", "每一次认真记录，都是给未来留一枚彩蛋。"),
            DailyQuote("心愿打卡日", "愿望有了日期，就开始悄悄靠近现实。"),
            DailyQuote("月光备忘录", "今晚也把在意的人和事，轻轻放在心上。"),
            DailyQuote("生活发光体", "把小事过好，日子就会自己亮起来。"),
            DailyQuote("倒数也可爱", "期待会让时间变软，等待也变得有形状。"),
            DailyQuote("纪念日小船", "一起经过的日子，会在记忆里慢慢靠岸。"),
            DailyQuote("今日元气格", "先照顾好自己，再拥抱今天安排的小惊喜。"),
            DailyQuote("灵感小邮差", "有些想念需要提醒，有些喜欢值得准时送达。"),
            DailyQuote("清爽计划表", "把复杂收起来，留一条清清楚楚的今天。"),
            DailyQuote("甜梦导航", "梦里有方向，醒来也能把日子过得稳稳当当。"),
            DailyQuote("星星排班表", "重要的时刻已经排好队，等你一一遇见。"),
            DailyQuote("心动保鲜盒", "喜欢要记录，快乐要保鲜，今天也要认真生活。"),
            DailyQuote("晴天收藏夹", "天气会变，值得期待的事情一直在路上。"),
            DailyQuote("温暖续航中", "把一点耐心留给自己，把一点期待留给明天。"),
            DailyQuote("今天很有盼头", "一个提醒，一次准备，一份靠近未来的安心。"),
            DailyQuote("小确幸雷达", "今天也去发现一件轻轻发光的小事。")
        )
        val dayOfYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
        quotes[dayOfYear % quotes.size]
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(adaptiveUi.cardRadius),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = adaptiveUi.heroMinHeight)
                .background(
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.secondary,
                            MaterialTheme.colorScheme.tertiary
                        )
                    )
                )
                .padding(if (adaptiveUi.compact) 12.dp else 16.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                Surface(color = Color.White.copy(alpha = 0.22f), shape = MaterialTheme.shapes.large) {
                    Text(dailyQuote.title, modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp), color = Color.White, style = MaterialTheme.typography.labelSmall)
                }
                Spacer(Modifier.height(14.dp))
                Text(
                    text = dailyQuote.content,
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}

private data class DailyQuote(val title: String, val content: String)

@Composable
fun HomeBoxFilter(
    boxes: List<com.memoriabox.data.model.Box>,
    selectedBoxId: String?,
    onBoxSelected: (String?) -> Unit,
    onCreateBox: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { expanded = true }) {
            Text(selectedBoxId?.let { id -> boxes.firstOrNull { it.id == id }?.name } ?: "全部分组", maxLines = 1)
            Spacer(Modifier.width(4.dp))
            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("全部分组") },
                onClick = {
                    onBoxSelected(null)
                    expanded = false
                }
            )
            boxes.forEach { box ->
                DropdownMenuItem(
                    text = { Text(box.name) },
                    onClick = {
                        onBoxSelected(box.id)
                        expanded = false
                    }
                )
            }
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text("新增分类分组") },
                leadingIcon = { Icon(Icons.Default.CreateNewFolder, contentDescription = null) },
                onClick = {
                    expanded = false
                    onCreateBox()
                }
            )
        }
    }
}

@Composable
fun HeroStatPill(label: String, value: String, modifier: Modifier = Modifier, onClick: () -> Unit = {}) {
    Surface(
        color = Color.White.copy(alpha = 0.2f),
        shape = MaterialTheme.shapes.large,
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, color = Color.White, style = MaterialTheme.typography.titleMedium)
            Text(label, color = Color.White.copy(alpha = 0.82f), style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
fun AllEventsTab(events: List<Event>, onEventClick: (Event) -> Unit, onEventLongClick: (Event) -> Unit) {
    val pinnedEvents = events.filter { it.isPinned }
    val normalEvents = events.filter { !it.isPinned }
    
    if (events.isEmpty()) {
        EmptyEventListHint()
    } else if (pinnedEvents.isNotEmpty()) {
        Text("置顶", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(6.dp))
        pinnedEvents.forEach { event ->
            HomeEventRow(event = event, onClick = { onEventClick(event) }, onLongClick = { onEventLongClick(event) })
            Spacer(Modifier.height(6.dp))
        }
        Spacer(Modifier.height(12.dp))
    }
    if (events.isNotEmpty()) {
        normalEvents.forEach { event ->
            HomeEventRow(event = event, onClick = { onEventClick(event) }, onLongClick = { onEventLongClick(event) })
            Spacer(Modifier.height(6.dp))
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
    val sortedFriends = remember(friends) {
        friends.sortedWith(
            compareBy<com.memoriabox.data.model.Friend> { friend ->
                friend.birthdayDate?.let { daysUntilNextBirthday(it) } ?: Int.MAX_VALUE
            }.thenBy { it.name }
        )
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("生日分组", style = MaterialTheme.typography.titleMedium)
        TextButton(onClick = onNavigateToFriends) { Text("管理分组") }
    }
    Spacer(Modifier.height(8.dp))
    if (friends.isEmpty()) {
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("还没有生日分组", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(4.dp))
                Text("添加成员后，可以按标签管理生日、纪念日和重要联系人。", style = MaterialTheme.typography.bodyMedium)
            }
        }
    } else {
        sortedFriends.take(8).forEach { friend ->
            val birthdayDays = friend.birthdayDate?.let { daysUntilNextBirthday(it) }
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
                            friend.birthdayDate?.let { "生日 ${formatFriendBirthdayMonthDay(it)}" } ?: "未设置生日",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    birthdayDays?.let { days ->
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = if (days == 0) "今天生日" else "还有 ${days} 天",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

private fun formatFriendBirthdayMonthDay(timestamp: Long): String {
    return SimpleDateFormat("M月d日", Locale.getDefault()).format(timestamp)
}

private fun daysUntilNextBirthday(timestamp: Long): Int {
    val today = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val birthday = Calendar.getInstance().apply { timeInMillis = timestamp }
    val nextBirthday = Calendar.getInstance().apply {
        set(Calendar.YEAR, today.get(Calendar.YEAR))
        set(Calendar.MONTH, birthday.get(Calendar.MONTH))
        set(Calendar.DAY_OF_MONTH, birthday.get(Calendar.DAY_OF_MONTH))
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        if (before(today)) add(Calendar.YEAR, 1)
    }
    return ((nextBirthday.timeInMillis - today.timeInMillis) / (24L * 60L * 60L * 1000L)).toInt()
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
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = Color.Transparent,
                modifier = Modifier.background(
                    Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)),
                    shape = MaterialTheme.shapes.medium
                )
            ) {
                Icon(icon, contentDescription = null, modifier = Modifier.padding(8.dp).size(20.dp), tint = Color.White)
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
                            Color(0xFFFFE1E4),
                            Color(0xFFE7E0FF),
                            Color(0xFFC6F6FF)
                        )
                    )
                )
        ) {
            if (!event.avatarUri.isNullOrBlank()) {
                AsyncImage(
                    model = event.avatarUri,
                    contentDescription = event.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize()
                )
                Box(modifier = Modifier.matchParentSize().background(Color.Black.copy(alpha = 0.38f)))
            }
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = if (event.avatarUri.isNullOrBlank()) Color.White.copy(alpha = 0.72f) else Color.White.copy(alpha = 0.18f),
                    modifier = Modifier.size(64.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Text(days.toString(), style = MaterialTheme.typography.titleLarge, color = if (event.avatarUri.isNullOrBlank()) MaterialTheme.colorScheme.primary else Color.White)
                        Text("天", style = MaterialTheme.typography.labelSmall, color = if (event.avatarUri.isNullOrBlank()) MaterialTheme.colorScheme.primary else Color.White)
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    val foreground = if (event.avatarUri.isNullOrBlank()) MaterialTheme.colorScheme.onSurface else Color.White
                    Text(event.name, style = MaterialTheme.typography.titleMedium, color = foreground, maxLines = 1)
                    Text(
                        text = listOfNotNull(
                            if (event.isPinned) "置顶" else null,
                            eventTypeLabel(event.type),
                            com.memoriabox.ui.screen.components.formatDate(event.date)
                        ).joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = foreground.copy(alpha = 0.82f)
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
                DetailLine("心情标签", eventMoodLabel(event))
                DetailLine("封面主题", "可在编辑资料中选择背景、渐变色和卡片模板")
                DetailLine("分享和小组件", "可分享图片/文本，也可在桌面添加小组件查看重要日子")
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
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = {
                    shareBitmap(context, generateEventCardBitmap(context, event, days.coerceAtLeast(0)))
                }) { Text("图片") }
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
                }) { Text("文本") }
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
    event.repeatMode == RepeatMode.CUSTOM_DAYS -> if (event.repeatInterval <= 1) "每日重复" else "每 ${event.repeatInterval} 天重复"
    event.repeatMode == RepeatMode.CUSTOM_WEEKS -> if (event.repeatInterval <= 1) "每周重复" else "每 ${event.repeatInterval} 周重复"
    event.repeatMode == RepeatMode.CUSTOM_MONTHS -> "每 ${event.repeatInterval} 个月重复"
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
fun DayToolsScreen(
    application: Application,
    onNavigateToCalendar: () -> Unit,
    onNavigateToPhotoWall: () -> Unit,
    onNavigateToExport: () -> Unit
) {
    val viewModel = remember { createMainViewModel(application) }
    val boxes by viewModel.boxes.collectAsState(initial = emptyList())
    val events by viewModel.allEvents.collectAsState(initial = emptyList())
    var showBatchImport by remember { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val defaultBoxId = boxes.firstOrNull()?.id ?: ""
    val today = remember { Calendar.getInstance() }
    val todayEvents = remember(events) { events.filter { isSameMonthDay(it.date, System.currentTimeMillis()) } }
    val upcomingEvents = remember(events) {
        events.filter { it.date >= System.currentTimeMillis() }
            .sortedBy { it.date }
            .take(8)
    }
    val archivedSuggestions = remember(events) {
        val cutoff = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -30) }.timeInMillis
        events.filter { it.date < cutoff && !it.isPinned }
            .sortedByDescending { it.date }
            .take(8)
    }
    val filteredEvents = remember(events, searchQuery) {
        if (searchQuery.isBlank()) emptyList() else events.filter {
            it.name.contains(searchQuery, ignoreCase = true) || it.note.contains(searchQuery, ignoreCase = true)
        }.take(20)
    }

    Scaffold(topBar = { TopAppBar(title = { Text("日子工具箱") }) }) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            ToolSection("日子模板", "一键创建常用类型，自动带提醒和文案") {
                dayTemplates().forEach { template ->
                    ToolActionRow(template.title, template.description, Icons.Default.AddCircle) {
                        viewModel.createQuickEvent(template.toEvent(defaultBoxId))
                    }
                }
            }
            ToolSection("节日库", "常见节日可一键加入我的日子") {
                holidayTemplates(today.get(Calendar.YEAR)).forEach { template ->
                    ToolActionRow(template.title, template.description, Icons.Default.EventAvailable) {
                        viewModel.createQuickEvent(template.toEvent(defaultBoxId))
                    }
                }
            }
            ToolSection("批量导入", "按行输入名称和日期，快速生成多个日子") {
                ToolActionRow("打开批量导入", "支持：木羽生日 6月24日 / 考试 2026-07-10", Icons.Default.Upload) {
                    showBatchImport = true
                }
            }
            ToolSection("提醒中心", "集中查看接下来要发生的日子") {
                if (upcomingEvents.isEmpty()) {
                    Text("暂无即将到来的日子", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    upcomingEvents.forEach { event -> CompactToolEventRow(event) }
                }
            }
            ToolSection("今日回忆", "查看历史今天和今天相关的日子") {
                if (todayEvents.isEmpty()) {
                    Text("今天还没有对应回忆", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    todayEvents.forEach { event -> CompactToolEventRow(event) }
                }
            }
            ToolSection("搜索和筛选", "按名称或备注查找日子") {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("搜索日子") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                filteredEvents.forEach { event -> CompactToolEventRow(event) }
            }
            ToolSection("归档建议", "过去较久且未置顶的日子可移入历史视角") {
                if (archivedSuggestions.isEmpty()) {
                    Text("暂无需要归档的日子", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    archivedSuggestions.forEach { event -> CompactToolEventRow(event) }
                }
            }
            ToolSection("纪念节点", "自动节点保持轻量生成，后续可继续细化开关") {
                Text("纪念日会自动生成 100天、520天、666天、999天、1/2/3/5/10周年。", style = MaterialTheme.typography.bodySmall)
            }
            ToolSection("分享和小组件", "分享海报、照片墙和桌面小组件集中入口") {
                ToolActionRow("分享海报", "生成社交分享图片", Icons.Default.PhotoLibrary, onNavigateToPhotoWall)
                ToolActionRow("导出分享", "导出数据和分享内容", Icons.Default.Share, onNavigateToExport)
                ToolActionRow("月历看板", "查看所有事件分布", Icons.Default.CalendarToday, onNavigateToCalendar)
                Text("桌面小组件可在手机桌面长按后添加 MemoriaBox 小组件。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }

    if (showBatchImport) {
        BatchImportDialog(
            onDismiss = { showBatchImport = false },
            onImport = { text ->
                parseBatchEvents(text, defaultBoxId).forEach { viewModel.createQuickEvent(it) }
                showBatchImport = false
            }
        )
    }
}

@Composable
private fun ToolSection(title: String, subtitle: String, content: @Composable ColumnScope.() -> Unit) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            content()
        }
    }
}

@Composable
private fun ToolActionRow(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null)
    }
}

@Composable
private fun CompactToolEventRow(event: Event) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Column(modifier = Modifier.weight(1f)) {
            Text(event.name, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
            Text(eventTypeLabel(event.type), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(com.memoriabox.ui.screen.components.formatDate(event.date), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun BatchImportDialog(onDismiss: () -> Unit, onImport: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("批量导入日子") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("每行一个日子，例如：木羽生日 6月24日", style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    minLines = 6,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("旅行 2026-08-01\n木羽生日 6月24日") }
                )
            }
        },
        confirmButton = { TextButton(onClick = { onImport(text) }) { Text("导入") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

private data class DayTemplate(val title: String, val description: String, val type: EventType, val offsetDays: Int, val note: String)

private fun DayTemplate.toEvent(boxId: String): Event = Event(
    boxId = boxId,
    name = title,
    date = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, offsetDays) }.timeInMillis,
    type = type,
    note = note,
    reminderEnabled = true,
    reminderOffsets = "0,1,7",
    gradientStart = if (type == EventType.BIRTHDAY) "#FF8A80" else "#1677FF",
    gradientEnd = if (type == EventType.BIRTHDAY) "#FFC069" else "#13C2C2"
)

private fun dayTemplates(): List<DayTemplate> = listOf(
    DayTemplate("重要考试", "默认 30 天后，适合考试倒计时", EventType.COUNTDOWN, 30, "认真准备，稳稳发挥。"),
    DayTemplate("旅行出发", "默认 60 天后，适合旅行计划", EventType.COUNTDOWN, 60, "把期待装进行李箱。"),
    DayTemplate("恋爱纪念日", "默认今天，自动生成纪念节点", EventType.ANNIVERSARY, 0, "把心动的日子好好保存。"),
    DayTemplate("还款提醒", "默认 7 天后，适合财务提醒", EventType.TODO, 7, "提前安排，安心一点。"),
    DayTemplate("体检提醒", "默认 14 天后，适合健康事项", EventType.TODO, 14, "照顾好自己。"),
    DayTemplate("宠物生日", "默认 90 天后，适合毛孩子生日", EventType.BIRTHDAY, 90, "今天也要给小可爱加餐。")
)

private fun holidayTemplates(year: Int): List<DayTemplate> = listOf(
    fixedDateTemplate("元旦", year, 1, 1, "新的一年开始啦。"),
    fixedDateTemplate("情人节", year, 2, 14, "把喜欢准时送达。"),
    fixedDateTemplate("劳动节", year, 5, 1, "给认真生活的自己放个假。"),
    fixedDateTemplate("儿童节", year, 6, 1, "保持一点童心。"),
    fixedDateTemplate("国庆节", year, 10, 1, "假期和重要安排都值得记录。"),
    fixedDateTemplate("圣诞节", year, 12, 25, "冬天也要有一点仪式感。")
)

private fun fixedDateTemplate(title: String, year: Int, month: Int, day: Int, note: String): DayTemplate {
    val date = Calendar.getInstance().apply { set(year, month - 1, day, 9, 0, 0) }
    val offset = ((date.timeInMillis - System.currentTimeMillis()) / 86_400_000L).toInt().coerceAtLeast(0)
    return DayTemplate(title, "一键添加 $month 月 $day 日", EventType.ANNIVERSARY, offset, note)
}

private fun parseBatchEvents(text: String, boxId: String): List<Event> {
    return text.lines().mapNotNull { line ->
        val trimmed = line.trim()
        if (trimmed.isBlank()) return@mapNotNull null
        val dateRegex = Regex("(\\d{4}-\\d{1,2}-\\d{1,2}|\\d{1,2}月\\d{1,2}日)")
        val match = dateRegex.find(trimmed) ?: return@mapNotNull null
        val name = trimmed.removeRange(match.range).trim().ifBlank { "新日子" }
        Event(boxId = boxId, name = name, date = parseFlexibleDate(match.value), type = if (name.contains("生日")) EventType.BIRTHDAY else EventType.COUNTDOWN, reminderEnabled = true, reminderOffsets = "0,1,7")
    }
}

private fun parseFlexibleDate(value: String): Long {
    val cal = Calendar.getInstance()
    if (value.contains("-")) {
        val parts = value.split("-").map { it.toInt() }
        cal.set(parts[0], parts[1] - 1, parts[2], 9, 0, 0)
    } else {
        val parts = Regex("(\\d{1,2})月(\\d{1,2})日").find(value)?.groupValues ?: return System.currentTimeMillis()
        cal.set(cal.get(Calendar.YEAR), parts[1].toInt() - 1, parts[2].toInt(), 9, 0, 0)
    }
    return cal.timeInMillis
}

private fun isSameMonthDay(left: Long, right: Long): Boolean {
    val leftCal = Calendar.getInstance().apply { timeInMillis = left }
    val rightCal = Calendar.getInstance().apply { timeInMillis = right }
    return leftCal.get(Calendar.MONTH) == rightCal.get(Calendar.MONTH) && leftCal.get(Calendar.DAY_OF_MONTH) == rightCal.get(Calendar.DAY_OF_MONTH)
}

@Composable
fun QuickAddEventDialog(
    boxes: List<com.memoriabox.data.model.Box>,
    initialType: EventType,
    application: Application,
    defaultBoxId: String? = null,
    onDismiss: () -> Unit,
    onSave: (Event) -> Unit
) {
    val notificationHelper = remember { com.memoriabox.utils.NotificationHelper(application) }
    EventDialog(
        availableBoxes = boxes,
        defaultBoxId = defaultBoxId ?: boxes.firstOrNull()?.id ?: "",
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
    val adaptiveUi = rememberAdaptiveUiSize()

    LaunchedEffect(boxId) {
        viewModel.loadBox(boxId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.height(adaptiveUi.topBarHeight),
                windowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp),
                title = { Text(box?.name ?: "日子详情") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
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
                modifier = Modifier.padding(horizontal = adaptiveUi.screenPadding, vertical = adaptiveUi.sectionSpacing),
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
fun ScreenBgWrapper(context: android.content.Context, page: String, content: @Composable () -> Unit) {
    val settingsVersion = AppSettings.settingsVersion
    val bgUri = remember(settingsVersion, page) {
        when (page) {
            "CALENDAR" -> AppSettings.getCalendarBgUri(context)
            "TODO" -> AppSettings.getTodoBgUri(context)
            "SETTINGS" -> AppSettings.getSettingsBgUri(context)
            "LOGS" -> AppSettings.getSettingsBgUri(context)
            else -> null
        }
    }
    Box(modifier = Modifier.fillMaxSize()) {
        if (!bgUri.isNullOrBlank()) {
            coil.compose.AsyncImage(model = bgUri, contentDescription = null, contentScale = androidx.compose.ui.layout.ContentScale.Crop, modifier = Modifier.matchParentSize())
            Box(modifier = Modifier.matchParentSize().background(Color.Black.copy(alpha = 0.32f)))
        } else {
            Box(
                modifier = Modifier.matchParentSize().background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f),
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.24f)
                        )
                    )
                )
            )
        }
        content()
    }
}

@Composable
fun TodoScreen(application: Application) {
    val todoVM = remember { createTodoViewModel(application) }
    val todoEvents by todoVM.todoEvents.collectAsState(initial = emptyList())

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("待办事项") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
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
    currentThemeMode: AppThemeMode,
    onThemeModeChange: (AppThemeMode) -> Unit,
    onNavigateToStatistics: () -> Unit,
    onNavigateToFriends: () -> Unit,
    onNavigateToPhotoWall: () -> Unit,
    onNavigateToExport: () -> Unit,
    onNavigateToAiSuggestions: () -> Unit,
    onNavigateToAchievements: () -> Unit,
    onNavigateToSyncStatus: () -> Unit,
    onNavigateToDayTools: () -> Unit,
    onNavigateToCustomization: () -> Unit,
    onBackupSettingsClick: () -> Unit,
    onWebDavSettingsClick: () -> Unit
) {
    val pushPlusHelper = remember { com.memoriabox.utils.NotificationHelper(application) }
    var pushPlusToken by remember { mutableStateOf(pushPlusHelper.getPushPlusToken()) }
    var pushPlusEnabled by remember { mutableStateOf(pushPlusHelper.isPushPlusEnabled()) }
    var pushPlusChannel by remember { mutableStateOf(pushPlusHelper.getPushPlusChannel()) }
    var showAboutDialog by remember { mutableStateOf(false) }
    val adaptiveUi = rememberAdaptiveUiSize()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(top = adaptiveUi.sectionSpacing, bottom = adaptiveUi.screenPadding)
    ) {
        SettingsHeroCard()
        SettingsSectionTitle("主题设置", "默认蓝白，也可以切换深色、护眼和彩色")
        ThemeModeCard(currentThemeMode = currentThemeMode, onThemeModeChange = onThemeModeChange)
        SettingsSectionTitle("常用工具", "高频功能放前面，少找一步")
        SettingsItem(
            icon = Icons.Default.AutoAwesome,
            title = "日子工具箱",
            description = "模板、节日库、提醒中心、今日回忆和批量导入",
            onClick = onNavigateToDayTools
        )
        SettingsItem(
            icon = Icons.Default.Palette,
            title = "个性化设置",
            description = "自定义页面背景、固定每日语录",
            onClick = onNavigateToCustomization
        )
        SettingsItem(
            icon = Icons.Default.BarChart,
            title = "数据统计",
            description = "查看事件统计和趋势",
            onClick = onNavigateToStatistics
        )
        SettingsItem(
            icon = Icons.Default.People,
            title = "生日分组",
            description = "管理生日资料和分组标签",
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
        
        SettingsSectionTitle("数据和同步", "备份、WebDAV 和跨设备管理")
        
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
        
        SettingsSectionTitle("推送提醒", "生日、纪念日和待办都能乖乖提醒")
        
        // PushPlus settings inline
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = adaptiveUi.screenPadding, vertical = adaptiveUi.sectionSpacing)
                .background(Color.Transparent),
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(if (adaptiveUi.compact) 12.dp else 16.dp)) {
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
        
        SettingsSectionTitle("关于", "版本和应用信息")
        
        SettingsItem(
            icon = Icons.Default.Info,
            title = "关于",
            description = "版本 3.2.1 · MemoriaBox",
            onClick = { showAboutDialog = true }
        )
    }

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text("关于 MemoriaBox") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("版本：3.2.1", style = MaterialTheme.typography.bodyMedium)
                    Text("MemoriaBox 是一个本地优先的日子、纪念日、待办和照片记录工具。", style = MaterialTheme.typography.bodyMedium)
                    Text("数据默认保存在本机，可通过备份和 WebDAV 功能进行迁移或同步。", style = MaterialTheme.typography.bodyMedium)
                    Text("著名木羽制作", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) { Text("知道了") }
            }
        )
    }
}

@Composable
fun SettingsHeroCard() {
    val adaptiveUi = rememberAdaptiveUiSize()
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = adaptiveUi.screenPadding, vertical = adaptiveUi.sectionSpacing),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.18f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary, MaterialTheme.colorScheme.tertiary)))
                .padding(if (adaptiveUi.compact) 16.dp else 20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("我的设置", color = Color.White, style = MaterialTheme.typography.headlineSmall)
                Text("功能收纳清楚，颜色轻快一点，操作更顺手。", color = Color.White.copy(alpha = 0.88f), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ThemeModeCard(currentThemeMode: AppThemeMode, onThemeModeChange: (AppThemeMode) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.84f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("当前主题：${currentThemeMode.label}", style = MaterialTheme.typography.titleMedium)
            Text(currentThemeMode.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AppThemeMode.entries.forEach { mode ->
                    FilterChip(
                        selected = currentThemeMode == mode,
                        onClick = { onThemeModeChange(mode) },
                        label = { Text(mode.label) },
                        leadingIcon = {
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .background(themePreviewBrush(mode), shape = MaterialTheme.shapes.small)
                            )
                        }
                    )
                }
            }
        }
    }
}

private fun themePreviewBrush(mode: AppThemeMode): Brush {
    return when (mode) {
        AppThemeMode.BLUE_WHITE -> Brush.linearGradient(listOf(Color(0xFF1677FF), Color(0xFFFFFFFF)))
        AppThemeMode.DARK -> Brush.linearGradient(listOf(Color(0xFF17121A), Color(0xFFB8A6FF)))
        AppThemeMode.EYE_CARE -> Brush.linearGradient(listOf(Color(0xFF2E7D32), Color(0xFFFAFCF4)))
        AppThemeMode.PLAYFUL -> Brush.linearGradient(listOf(Color(0xFFFF6B6B), Color(0xFF7C5CFF)))
        AppThemeMode.WARM -> Brush.linearGradient(listOf(Color(0xFFFF7A00), Color(0xFFFFF6D8)))
    }
}

@Composable
fun SettingsSectionTitle(title: String, description: String) {
    val adaptiveUi = rememberAdaptiveUiSize()
    Column(
        modifier = Modifier.padding(
            start = adaptiveUi.screenPadding + 4.dp,
            end = adaptiveUi.screenPadding + 4.dp,
            top = if (adaptiveUi.compact) 12.dp else 18.dp,
            bottom = adaptiveUi.sectionSpacing / 2f
        )
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
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
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
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

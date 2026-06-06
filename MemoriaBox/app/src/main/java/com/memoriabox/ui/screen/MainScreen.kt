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
import com.memoriabox.utils.MonthlySummaryHelper
import com.memoriabox.utils.NotificationHelper
import com.memoriabox.utils.LunarDateUtils
import com.memoriabox.utils.startOfMonth
import com.memoriabox.viewmodel.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

@Composable
fun MainScreen(
    application: Application,
    initialMonthlySummaryMonth: Long? = null,
    onMonthlySummaryIntentConsumed: () -> Unit = {},
    currentThemeMode: AppThemeMode = AppThemeMode.BLUE_WHITE,
    onThemeModeChange: (AppThemeMode) -> Unit = {},
    navController: NavHostController = rememberNavController()
) {
    val context = LocalContext.current
    val settingsVersion = AppSettings.settingsVersion
    var selectedTab by remember { mutableStateOf(0) }
    var showNewMonthPrompt by remember { mutableStateOf(false) }
    var newMonthTarget by remember { mutableStateOf<Long?>(null) }
    var autoOpenCalendarSummary by remember { mutableStateOf<Long?>(null) }
    val mainViewModel = remember { createMainViewModel(application) }
    LaunchedEffect(Unit) {
        val now = System.currentTimeMillis()
        if (AppSettings.getMonthlySummaryEnabled(context) &&
            MonthlySummaryHelper.isNewMonthFirstOpenCandidate(now) &&
            AppSettings.getMonthlySummaryAutoPromptEnabled(context)
        ) {
            val lastPrompt = AppSettings.getMonthlySummaryLastPromptMonth(context)
            val currentMonthKey = MonthlySummaryHelper.monthKey(startOfMonth(now))
            if (lastPrompt != currentMonthKey) {
                showNewMonthPrompt = true
                newMonthTarget = startOfMonth(now)
                NotificationHelper(context).showMonthlySummaryNotification(MonthlySummaryHelper.previousMonthStart(now))
                AppSettings.setMonthlySummaryLastPromptMonth(context, currentMonthKey)
            }
        }
    }

    val boxes by mainViewModel.boxes.collectAsState(initial = emptyList())
    val logs by mainViewModel.recentLogs.collectAsState(initial = emptyList())
    var showQuickAdd by remember { mutableStateOf(false) }
    var pendingQuickAddType by remember { mutableStateOf<EventType?>(null) }
    var showDiaryEditor by remember { mutableStateOf(false) }
    var quickExistingDiary by remember { mutableStateOf<DiaryEntry?>(null) }
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

    LaunchedEffect(initialMonthlySummaryMonth) {
        initialMonthlySummaryMonth?.let { targetMonth ->
            if (AppSettings.getMonthlySummaryEnabled(context)) {
                autoOpenCalendarSummary = targetMonth
                navigateToRootTab(1, Screen.Calendar.route)
            }
            onMonthlySummaryIntentConsumed()
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
                val monthlySummaryState by calendarVM.monthlySummary.collectAsState(initial = com.memoriabox.utils.MonthlySummaryUiState())
                var addDateFromCalendar by remember { mutableStateOf<Long?>(null) }
                var selectedCalendarEvent by remember { mutableStateOf<Event?>(null) }
                var editCalendarEvent by remember { mutableStateOf<Event?>(null) }
                var showSummaryOverride by remember { mutableStateOf(false) }
                val calendarContext = androidx.compose.ui.platform.LocalContext.current
                val monthlySummaryEnabled = remember(settingsVersion) { AppSettings.getMonthlySummaryEnabled(calendarContext) }

                LaunchedEffect(autoOpenCalendarSummary) {
                    autoOpenCalendarSummary?.let { targetMonth ->
                        calendarVM.loadMonthlySummary(targetMonth)
                        showSummaryOverride = true
                        autoOpenCalendarSummary = null
                    }
                }

                LaunchedEffect(showSummaryOverride) {
                    if (showSummaryOverride) {
                        kotlinx.coroutines.delay(100)
                        showSummaryOverride = false
                    }
                }

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
                        onSaveDiary = { existingDiary, date, content, media, backgroundUri ->
                            calendarVM.saveDiaryWithMedia(existingDiary, date, content, media, backgroundUri)
                        },
                        onDeleteDiary = { diary -> calendarVM.deleteDiary(diary) },
                        onLoadDiaryMedia = { diary -> calendarVM.loadDiaryMedia(diary.id) },
                        monthlySummaryEnabled = monthlySummaryEnabled,
                        monthlySummaryState = monthlySummaryState,
                        initialShowSummary = showSummaryOverride,
                        onSummaryPlayModeChange = { /* play mode is local to panel */ },
                        onSummarySpeedChange = { speed ->
                            AppSettings.setMonthlySummaryPlaySpeedFactor(calendarContext, speed)
                        },
                        onSummaryTextEnabledChange = { enabled ->
                            AppSettings.setMonthlySummaryTextEnabled(calendarContext, enabled)
                        },
                        onLoadMonthlySummary = { monthStart ->
                            calendarVM.loadMonthlySummary(monthStart)
                        }
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
            composable(Screen.PhotoWall.route) {
                PhotoWallScreen(application)
            }
            composable(Screen.Export.route) {
                ExportScreen(
                    application = application,
                    onNavigateBack = { navController.popBackStack() }
                )
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
                    onNavigateBack = { navController.popBackStack() },
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

    if (showNewMonthPrompt) {
        val promptMonthTarget = newMonthTarget
        val monthText = SimpleDateFormat("yyyy年M月", Locale.getDefault()).format(Date(promptMonthTarget ?: System.currentTimeMillis()))
        AlertDialog(
            onDismissRequest = {
                promptMonthTarget?.let {
                    AppSettings.setMonthlySummaryLastPromptMonth(context, MonthlySummaryHelper.monthKey(it))
                }
                showNewMonthPrompt = false
            },
            title = { Text("新月度总结") },
            text = {
                Column {
                    Text("欢迎进入 $monthText！")
                    Spacer(Modifier.height(6.dp))
                    Text("查看上个月的日记与照片总结，回顾属于你自己的时光。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showNewMonthPrompt = false
                    promptMonthTarget?.let {
                        AppSettings.setMonthlySummaryLastPromptMonth(context, MonthlySummaryHelper.monthKey(it))
                    }
                }) { Text("稍后") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showNewMonthPrompt = false
                    promptMonthTarget?.let {
                        AppSettings.setMonthlySummaryLastPromptMonth(context, MonthlySummaryHelper.monthKey(it))
                        autoOpenCalendarSummary = MonthlySummaryHelper.previousMonthStart()
                    }
                    navigateToRootTab(1, Screen.Calendar.route)
                }) { Text("查看总结") }
            }
        )
    }

    if (showQuickAdd) {
        AddTypePickerDialog(
            onDismiss = {
                showQuickAdd = false
                pendingQuickAddType = null
            },
            onTypeSelected = { type ->
                showQuickAdd = false
                pendingQuickAddType = type
            },
            onAddDiary = {
                showQuickAdd = false
                quickExistingDiary = null
                showDiaryEditor = true
            }
        )
    }

    pendingQuickAddType?.let { type ->
        QuickAddEventDialog(
            boxes = boxes,
            initialType = type,
            application = application,
            onDismiss = { pendingQuickAddType = null },
            onSave = { event ->
                mainViewModel.createQuickEvent(event)
                pendingQuickAddType = null
            }
        )
    }

    if (showDiaryEditor) {
        val calendarVM = remember { createCalendarViewModel(application) }
        val allDiaries by calendarVM.allDiaries.collectAsState(initial = emptyList())
        val selectedDiaryMedia by calendarVM.selectedDiaryMedia.collectAsState(initial = emptyList())
        DiaryEditorDialog(
            existingDiary = quickExistingDiary,
            existingMedia = quickExistingDiary?.let { diary -> selectedDiaryMedia.filter { it.diaryId == diary.id } }.orEmpty(),
            allDiaries = allDiaries,
            dateStart = System.currentTimeMillis(),
            onDismiss = {
                showDiaryEditor = false
                quickExistingDiary = null
            },
            onSave = { selectedDate, content, media, bgUri ->
                calendarVM.saveDiaryWithMedia(quickExistingDiary, selectedDate, content, media, bgUri)
                showDiaryEditor = false
                quickExistingDiary = null
            },
            onOpenExistingDiary = { diary ->
                quickExistingDiary = diary
                calendarVM.loadDiaryMedia(diary.id)
            }
        )
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
    onAddDiary: (() -> Unit)? = null,
    onCreateBox: (() -> Unit)? = null
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("新增") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                onAddDiary?.let { addDiary ->
                    AddActionOption(Icons.Default.Edit, "写日记", "记录今天发生了什么", onClick = addDiary)
                }
                AddTypeOption(Icons.Default.Timer, "倒数日", "考试、旅行、纪念日", EventType.COUNTDOWN, onTypeSelected)
                AddTypeOption(Icons.Default.Favorite, "纪念日", "恋爱、结婚、相识", EventType.ANNIVERSARY, onTypeSelected)
                AddTypeOption(Icons.Default.History, "正计时", "记录已坚持多久", EventType.ELAPSED, onTypeSelected)
                AddTypeOption(Icons.Default.Cake, "生日", "支持提前提醒", EventType.BIRTHDAY, onTypeSelected)
                AddTypeOption(Icons.Default.CheckCircle, "待办", "把事项放进时间轴", EventType.TODO, onTypeSelected)
                onCreateBox?.let { createBox ->
                    AddActionOption(Icons.Default.CreateNewFolder, "新建分组", "收纳不同类型日子", onClick = createBox)
                }
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
    onNavigateToPhotoWall: () -> Unit,
    onNavigateToStatistics: () -> Unit,
    onNavigateToAiSuggestions: () -> Unit
) {
    val viewModel = remember { createMainViewModel(application) }
    val notificationHelper = remember { com.memoriabox.utils.NotificationHelper(application) }
    val boxes by viewModel.boxes.collectAsState(initial = emptyList())
    val events by viewModel.allEvents.collectAsState(initial = emptyList())
    val logs by viewModel.recentLogs.collectAsState(initial = emptyList())
    var showCreateDialog by remember { mutableStateOf(false) }
    var homeTab by remember { mutableIntStateOf(0) }
    var selectedEvent by remember { mutableStateOf<Event?>(null) }
    var eventForActions by remember { mutableStateOf<Event?>(null) }
    var eventForEdit by remember { mutableStateOf<Event?>(null) }
    var eventForDelete by remember { mutableStateOf<Event?>(null) }
    var showQuickAdd by remember { mutableStateOf(false) }
    var pendingQuickAddType by remember { mutableStateOf<EventType?>(null) }
    var showDiaryEditor by remember { mutableStateOf(false) }
    var quickExistingDiary by remember { mutableStateOf<DiaryEntry?>(null) }
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
                    title = { Text("日子") },
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
                selectedTab = homeTab,
                onTabSelected = { homeTab = it },
                onBoxClick = onBoxClick,
                onCreateBox = { showCreateDialog = true },
                onNavigateToCalendar = onNavigateToCalendar,
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
        AddTypePickerDialog(
            onDismiss = { showQuickAdd = false },
            onTypeSelected = { type ->
                showQuickAdd = false
                pendingQuickAddType = type
            },
            onAddDiary = {
                showQuickAdd = false
                quickExistingDiary = null
                showDiaryEditor = true
            }
        )
    }

    pendingQuickAddType?.let { type ->
        QuickAddEventDialog(
            boxes = boxes,
            initialType = type,
            application = application,
            defaultBoxId = selectedBoxId,
            onDismiss = {
                pendingQuickAddType = null
            },
            onSave = { event ->
                viewModel.createQuickEvent(event)
                pendingQuickAddType = null
            }
        )
    }

    if (showDiaryEditor) {
        val calendarVM = remember { createCalendarViewModel(application) }
        val allDiaries by calendarVM.allDiaries.collectAsState(initial = emptyList())
        val selectedDiaryMedia by calendarVM.selectedDiaryMedia.collectAsState(initial = emptyList())
        DiaryEditorDialog(
            existingDiary = quickExistingDiary,
            existingMedia = quickExistingDiary?.let { diary -> selectedDiaryMedia.filter { it.diaryId == diary.id } }.orEmpty(),
            allDiaries = allDiaries,
            dateStart = System.currentTimeMillis(),
            onDismiss = {
                showDiaryEditor = false
                quickExistingDiary = null
            },
            onSave = { selectedDate, content, media, bgUri ->
                calendarVM.saveDiaryWithMedia(quickExistingDiary, selectedDate, content, media, bgUri)
                showDiaryEditor = false
                quickExistingDiary = null
            },
            onOpenExistingDiary = { diary ->
                quickExistingDiary = diary
                calendarVM.loadDiaryMedia(diary.id)
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
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    selectedBoxId: String?,
    onBoxSelected: (String?) -> Unit,
    onBoxClick: (String) -> Unit,
    onCreateBox: () -> Unit,
    onNavigateToCalendar: () -> Unit,
    onNavigateToPhotoWall: () -> Unit,
    onNavigateToStatistics: () -> Unit,
    onNavigateToAiSuggestions: () -> Unit,
    onEventClick: (Event) -> Unit,
    onEventLongClick: (Event) -> Unit,
    adaptiveUi: AdaptiveUiSize,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val settingsVersion = AppSettings.settingsVersion
    val upcomingEnabled = remember(settingsVersion) { AppSettings.getUpcomingEventsEnabled(context) }
    val upcomingDays = remember(settingsVersion) { AppSettings.getUpcomingEventsDays(context) }
    val upcomingReminderEnabled = remember(settingsVersion) { AppSettings.getUpcomingEventsReminderEnabled(context) }
    val now = remember(events, settingsVersion) { System.currentTimeMillis() }
    val sortedEvents = remember(events, upcomingEnabled, upcomingDays, now) {
        if (upcomingEnabled) {
            events.sortedWith(
                compareBy<Event> { event ->
                    val daysLeft = daysUntilNextOccurrence(event, now)
                    when {
                        daysLeft != null && daysLeft in 0..upcomingDays -> 0
                        daysLeft != null && daysLeft > upcomingDays -> 1
                        else -> 2
                    }
                }
                    .thenBy { event -> daysUntilNextOccurrence(event, now) ?: Long.MAX_VALUE }
                    .thenByDescending { it.isPinned }
                    .thenBy { it.date }
            )
        } else {
            events.sortedWith(
                compareByDescending<Event> { it.isPinned }
                    .thenBy { kotlin.math.abs(it.date - System.currentTimeMillis()) }
            )
        }
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
                Text(if (upcomingEnabled) "即将到来" else "今天先看这些", style = MaterialTheme.typography.titleMedium)
                Text(
                    if (upcomingEnabled) {
                        val reminderText = if (upcomingReminderEnabled) "提醒开启" else "提醒关闭"
                        "${visibleEvents.size} 个 · ${upcomingDays} 天内优先 · $reminderText · $selectedBoxName"
                    } else {
                        "${visibleEvents.size} 个 · $selectedBoxName"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
            totalEvents = visibleEvents.size,
            boxCount = boxes.size,
            upcomingDays = upcomingDays,
            upcomingEnabled = upcomingEnabled,
            onEventsClick = { onTabSelected(0) },
            onBoxesClick = { onTabSelected(1) },
            adaptiveUi = adaptiveUi
        )
        Spacer(Modifier.height(adaptiveUi.sectionSpacing))
        AllEventsTab(
            events = visibleEvents,
            upcomingEnabled = upcomingEnabled,
            upcomingDays = upcomingDays,
            now = now,
            onEventClick = onEventClick,
            onEventLongClick = onEventLongClick,
            adaptiveUi = adaptiveUi
        )
        }
    }
}

@Composable
fun HomeHeroCard(
    totalEvents: Int,
    boxCount: Int,
    upcomingDays: Int,
    upcomingEnabled: Boolean,
    onEventsClick: () -> Unit = {},
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
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = if (adaptiveUi.compact) 2.dp else 4.dp)
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
                .padding(if (adaptiveUi.compact) 14.dp else 18.dp)
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 34.dp, y = (-38).dp)
                    .size(if (adaptiveUi.compact) 104.dp else 132.dp)
                    .background(Color.White.copy(alpha = 0.12f), RoundedCornerShape(999.dp))
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .offset(x = (-44).dp, y = 38.dp)
                    .size(if (adaptiveUi.compact) 90.dp else 118.dp)
                    .background(Color.Black.copy(alpha = 0.08f), RoundedCornerShape(999.dp))
            )
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(if (adaptiveUi.compact) 12.dp else 14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(color = Color.White.copy(alpha = 0.22f), shape = MaterialTheme.shapes.large) {
                        Text(dailyQuote.title, modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp), color = Color.White, style = MaterialTheme.typography.labelSmall)
                    }
                    Text(
                        if (upcomingEnabled) "${upcomingDays} 天内优先" else "完整日子流",
                        color = Color.White.copy(alpha = 0.84f),
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1
                    )
                }
                Text(
                    text = dailyQuote.content,
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(if (adaptiveUi.compact) 8.dp else 10.dp)
                ) {
                    HeroStatPill("日子", totalEvents.toString(), Modifier.weight(1f), onEventsClick)
                    HeroStatPill("分组", boxCount.toString(), Modifier.weight(1f), onBoxesClick)
                    HeroStatPill("节奏", if (upcomingEnabled) "${upcomingDays}天" else "全部", Modifier.weight(1f), onEventsClick)
                }
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
fun AllEventsTab(
    events: List<Event>,
    upcomingEnabled: Boolean,
    upcomingDays: Int,
    now: Long,
    onEventClick: (Event) -> Unit,
    onEventLongClick: (Event) -> Unit,
    adaptiveUi: AdaptiveUiSize
) {
    val pinnedEvents = events.filter { it.isPinned }
    val normalEvents = events.filter { !it.isPinned }
    val eventSpacing = if (adaptiveUi.compact) 7.dp else if (adaptiveUi.roomy) 10.dp else 8.dp
    
    if (events.isEmpty()) {
        if (upcomingEnabled) {
            EmptyUpcomingEventHint(upcomingDays)
        } else {
            EmptyEventListHint()
        }
    } else if (upcomingEnabled) {
        events.forEach { event ->
            EnhancedEventCard(event = event, onClick = { onEventClick(event) }, onLongPress = { onEventLongClick(event) })
            Spacer(Modifier.height(eventSpacing))
        }
    } else if (pinnedEvents.isNotEmpty()) {
        Text("置顶", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(eventSpacing))
        pinnedEvents.forEach { event ->
            EnhancedEventCard(event = event, onClick = { onEventClick(event) }, onLongPress = { onEventLongClick(event) })
            Spacer(Modifier.height(eventSpacing))
        }
        Spacer(Modifier.height(eventSpacing))
    }
    if (events.isNotEmpty() && !upcomingEnabled) {
        normalEvents.forEach { event ->
            EnhancedEventCard(event = event, onClick = { onEventClick(event) }, onLongPress = { onEventLongClick(event) })
            Spacer(Modifier.height(eventSpacing))
        }
    }
}

@Composable
fun EmptyUpcomingEventHint(upcomingDays: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("近期很轻松", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(4.dp))
            Text("${upcomingDays} 天内没有需要特别留意的日子。", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(4.dp))
            Text("所有日子都会显示，临近的排前面，较远或已过的排后面。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
            Text("这里还很清爽", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(4.dp))
            Text("点底部中间按钮，先记录一个真正重要的日子。", style = MaterialTheme.typography.bodyMedium)
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

private fun daysUntilNextOccurrence(event: Event, nowMillis: Long): Long? {
    if (event.type == EventType.ELAPSED) return null
    val targetDate = nextOccurrenceMillis(event, nowMillis)
    val startToday = startOfDay(nowMillis)
    val startTarget = startOfDay(targetDate)
    return TimeUnit.MILLISECONDS.toDays(startTarget - startToday)
}

private fun nextOccurrenceMillis(event: Event, nowMillis: Long): Long {
    if (event.type == EventType.BIRTHDAY && !event.lunar.isNullOrBlank()) {
        LunarDateUtils.nextOccurrenceMillis(event.lunar, nowMillis)?.let { return it }
    }
    if (event.repeatYearly || event.type == EventType.BIRTHDAY || event.type == EventType.ANNIVERSARY) {
        val now = Calendar.getInstance().apply { timeInMillis = nowMillis }
        val target = Calendar.getInstance().apply { timeInMillis = event.date }
        val next = Calendar.getInstance().apply {
            timeInMillis = nowMillis
            set(Calendar.MONTH, target.get(Calendar.MONTH))
            set(Calendar.DAY_OF_MONTH, target.get(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (next.before(startCalendar(nowMillis))) {
            next.add(Calendar.YEAR, 1)
        }
        return next.timeInMillis
    }
    return event.date
}

private fun startOfDay(timeMillis: Long): Long = startCalendar(timeMillis).timeInMillis

private fun startCalendar(timeMillis: Long): Calendar = Calendar.getInstance().apply {
    timeInMillis = timeMillis
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}

private fun upcomingDisplayDate(event: Event): String {
    val date = if (event.repeatYearly || event.type == EventType.BIRTHDAY || event.type == EventType.ANNIVERSARY) {
        nextOccurrenceMillis(event, System.currentTimeMillis())
    } else {
        event.date
    }
    return SimpleDateFormat("M月d日", Locale.getDefault()).format(Date(date))
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
    val days = com.memoriabox.ui.screen.components.calculateDays(event)
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
                if (event.pushPlusEnabled) DetailLine("PushPlus", "开启")
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
                TextButton(onClick = onDismiss) { Text("取消") }
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
    onNavigateBack: () -> Unit,
    onNavigateToCalendar: () -> Unit,
    onNavigateToPhotoWall: () -> Unit,
    onNavigateToExport: () -> Unit
) {
    val viewModel = remember { createMainViewModel(application) }
    val boxes by viewModel.boxes.collectAsState(initial = emptyList())
    val events by viewModel.allEvents.collectAsState(initial = emptyList())
    val logs by viewModel.recentLogs.collectAsState(initial = emptyList())
    val snackbarHostState = remember { SnackbarHostState() }
    val snackbarScope = rememberCoroutineScope()
    var showBatchImport by remember { mutableStateOf(false) }
    var selectedEvent by remember { mutableStateOf<Event?>(null) }
    var editingEvent by remember { mutableStateOf<Event?>(null) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val defaultBoxId = boxes.firstOrNull()?.id ?: ""
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

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("日子工具箱") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            ToolSection("批量导入", "把多条日期一次性加入日子列表") {
                ToolActionRow(
                    title = "打开批量导入",
                    subtitle = "支持：木羽生日 6月24日 / 考试 2026-07-10",
                    icon = Icons.Default.Upload,
                    onClick = {
                        if (defaultBoxId.isBlank()) {
                            snackbarScope.launch {
                                snackbarHostState.showSnackbar("请先创建一个分类，再使用批量导入。")
                            }
                        } else {
                            showBatchImport = true
                        }
                    },
                    featured = true
                )
            }
            ToolSection("搜索和筛选", "按名称或备注查找日子") {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("搜索日子") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                when {
                    searchQuery.isBlank() -> Text("输入名称或备注后开始查找。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    filteredEvents.isEmpty() -> Text("未找到相关日子", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    else -> filteredEvents.forEach { event -> CompactToolEventRow(event, onClick = { selectedEvent = event }) }
                }
            }
            ToolSection("较早未置顶", "查看过去较久且仍未置顶的日子") {
                if (archivedSuggestions.isEmpty()) {
                    Text("暂无较早未置顶的日子", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    archivedSuggestions.forEach { event -> CompactToolEventRow(event, onClick = { selectedEvent = event }) }
                }
            }
            ToolSection("分享和看板", "分享海报、照片墙、导出和日历看板入口") {
                ToolActionRow("分享海报", "生成社交分享图片", Icons.Default.PhotoLibrary, onNavigateToPhotoWall)
                ToolActionRow("导出分享", "导出数据和分享内容", Icons.Default.Share, onNavigateToExport)
                ToolActionRow("月历看板", "查看所有事件分布", Icons.Default.CalendarToday, onNavigateToCalendar)
            }
        }
    }

    if (showBatchImport) {
        BatchImportDialog(
            onDismiss = { showBatchImport = false },
            onImport = { text ->
                val parsedEvents = parseBatchEvents(text, defaultBoxId)
                when {
                    defaultBoxId.isBlank() -> {
                        snackbarScope.launch {
                            snackbarHostState.showSnackbar("请先创建一个分类，再使用批量导入。")
                        }
                    }
                    parsedEvents.isEmpty() -> {
                        snackbarScope.launch {
                            snackbarHostState.showSnackbar("没有识别到可导入的日期。")
                        }
                    }
                    else -> {
                        parsedEvents.forEach { viewModel.createQuickEvent(it) }
                        snackbarScope.launch {
                            snackbarHostState.showSnackbar("已导入 ${parsedEvents.size} 条日子。")
                        }
                    }
                }
                showBatchImport = false
            }
        )
    }

    selectedEvent?.let { event ->
        EventDetailDialog(
            event = event,
            logs = logs.filter { it.targetId == event.id },
            onDismiss = { selectedEvent = null },
            onEdit = {
                selectedEvent = null
                editingEvent = event
            },
            onTogglePin = {
                viewModel.togglePinned(event)
                selectedEvent = null
            },
            onDelete = {
                viewModel.deleteQuickEvent(event)
                selectedEvent = null
            },
            onOpenCategory = {
                selectedEvent = null
            }
        )
    }

    editingEvent?.let { event ->
        EventDialog(
            existingEvent = event,
            availableBoxes = boxes,
            onDismiss = { editingEvent = null },
            onSave = { updated ->
                viewModel.updateQuickEvent(updated)
                editingEvent = null
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
private fun ToolActionRow(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit, featured: Boolean = false) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = if (featured) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f) else Color.Transparent
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = if (featured) 10.dp else 0.dp, vertical = 8.dp),
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
}

@Composable
private fun CompactToolEventRow(event: Event, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 9.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(event.name, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                Text(eventTypeLabel(event.type), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(com.memoriabox.ui.screen.components.formatDate(event.date), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
        }
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

private fun parseBatchEvents(text: String, boxId: String): List<Event> {
    return text.lines().mapNotNull { line ->
        val trimmed = line.trim()
        if (trimmed.isBlank()) return@mapNotNull null
        val dateRegex = Regex("(\\d{4}-\\d{1,2}-\\d{1,2}|\\d{1,2}月\\d{1,2}日)")
        val match = dateRegex.find(trimmed) ?: return@mapNotNull null
        val name = trimmed.removeRange(match.range).trim().ifBlank { "新日子" }
        Event(
            boxId = boxId,
            name = name,
            date = parseFlexibleDate(match.value) ?: return@mapNotNull null,
            type = if (name.contains("生日")) EventType.BIRTHDAY else EventType.COUNTDOWN,
            reminderEnabled = true,
            reminderOffsets = "0,1,7"
        )
    }
}

private fun parseFlexibleDate(value: String): Long? {
    val cal = Calendar.getInstance()
    val year: Int
    val month: Int
    val day: Int
    try {
        if (value.contains("-")) {
            val parts = value.split("-").map { it.toInt() }
            if (parts.size != 3) return null
            year = parts[0]
            month = parts[1]
            day = parts[2]
        } else {
            val parts = Regex("(\\d{1,2})月(\\d{1,2})日").find(value)?.groupValues ?: return null
            year = cal.get(Calendar.YEAR)
            month = parts[1].toInt()
            day = parts[2].toInt()
        }
    } catch (_: NumberFormatException) {
        return null
    }
    if (month !in 1..12 || day !in 1..31) return null
    cal.isLenient = false
    return try {
        cal.set(year, month - 1, day, 9, 0, 0)
        cal.set(Calendar.MILLISECOND, 0)
        cal.timeInMillis
    } catch (_: IllegalArgumentException) {
        null
    }
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
        showFixedTypeLabel = false,
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
    val allBoxes by viewModel.allBoxes.collectAsState(initial = emptyList())
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
                text = "左右拖动卡片切换排版，松手后自动应用",
                modifier = Modifier.padding(horizontal = adaptiveUi.screenPadding, vertical = adaptiveUi.sectionSpacing),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            EnhancedEventGrid(
                events = events,
                onEventClick = { showEditEvent = it },
                onEventEdit = { showEditEvent = it },
                onCardTemplateChange = { event, newTemplate ->
                    viewModel.updateEvent(event.copy(cardTemplate = newTemplate))
                },
                boxBgType = box?.bgType ?: com.memoriabox.data.model.BgType.COLOR,
                boxBgValue = box?.bgValue ?: "#F5F5F5"
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
            boxes = allBoxes.filter { it.id != boxId },
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
    var showDiarySettings by remember { mutableStateOf(false) }
    var showMonthlySummarySettings by remember { mutableStateOf(false) }
    var showUpcomingSettings by remember { mutableStateOf(false) }
    var showMoreToolsDialog by remember { mutableStateOf(false) }
    val adaptiveUi = rememberAdaptiveUiSize()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(top = adaptiveUi.sectionSpacing, bottom = adaptiveUi.screenPadding)
    ) {
        SettingsHeroCard()
        SettingsSectionTitle("轻松一点", "先选一个舒服的颜色，再整理常用入口")
        ThemeModeCard(currentThemeMode = currentThemeMode, onThemeModeChange = onThemeModeChange)
        SettingsSectionTitle("常用", "每天会用到的功能放在这里")
        SettingsItem(
            icon = Icons.Default.AutoAwesome,
            title = "日子工具箱",
            description = "批量导入、搜索筛选、归档建议和分享入口",
            onClick = onNavigateToDayTools
        )
        SettingsItem(
            icon = Icons.Default.Palette,
            title = "个性化设置",
            description = "自定义页面背景、固定每日语录",
            onClick = onNavigateToCustomization
        )
        SettingsItem(
            icon = Icons.Default.MoreHoriz,
            title = "更多工具",
            description = "统计、照片墙、导出、AI、成就和同步",
            onClick = { showMoreToolsDialog = true }
        )

        SettingsSectionTitle("记录和数据", "备份、同步、日记和月度总结")
        
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
        SettingsItem(
            icon = Icons.Default.Edit,
            title = "日记设置",
            description = "滚动动画速度、开关",
            onClick = { showDiarySettings = true }
        )
        SettingsItem(
            icon = Icons.Default.AutoStories,
            title = "月度总结",
            description = "开关、自动推送、播放速度",
            onClick = { showMonthlySummarySettings = true }
        )
        
        SettingsSectionTitle("提醒", "需要跨平台推送时再开启")

        SettingsItem(
            icon = Icons.Default.NotificationsActive,
            title = "即将到来",
            description = "首页显示、天数范围、颜色和提醒",
            onClick = { showUpcomingSettings = true }
        )

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
            description = "版本 3.2.11 · MemoriaBox",
            onClick = { showAboutDialog = true }
        )
    }

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text("关于 MemoriaBox") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("版本：3.2.11", style = MaterialTheme.typography.bodyMedium)
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

    if (showDiarySettings) {
        DiarySettingsDialog(onDismiss = { showDiarySettings = false })
    }
    if (showMonthlySummarySettings) {
        MonthlySummarySettingsDialog(onDismiss = { showMonthlySummarySettings = false })
    }
    if (showUpcomingSettings) {
        UpcomingEventsSettingsDialog(onDismiss = { showUpcomingSettings = false })
    }
    if (showMoreToolsDialog) {
        MoreToolsDialog(
            onDismiss = { showMoreToolsDialog = false },
            onNavigateToStatistics = {
                showMoreToolsDialog = false
                onNavigateToStatistics()
            },
            onNavigateToPhotoWall = {
                showMoreToolsDialog = false
                onNavigateToPhotoWall()
            },
            onNavigateToExport = {
                showMoreToolsDialog = false
                onNavigateToExport()
            },
            onNavigateToAiSuggestions = {
                showMoreToolsDialog = false
                onNavigateToAiSuggestions()
            },
            onNavigateToAchievements = {
                showMoreToolsDialog = false
                onNavigateToAchievements()
            },
            onNavigateToSyncStatus = {
                showMoreToolsDialog = false
                onNavigateToSyncStatus()
            }
        )
    }
}

@Composable
fun MoreToolsDialog(
    onDismiss: () -> Unit,
    onNavigateToStatistics: () -> Unit,
    onNavigateToPhotoWall: () -> Unit,
    onNavigateToExport: () -> Unit,
    onNavigateToAiSuggestions: () -> Unit,
    onNavigateToAchievements: () -> Unit,
    onNavigateToSyncStatus: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("更多工具") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                MoreToolActionRow(icon = Icons.Default.BarChart, title = "数据统计", description = "查看事件统计和趋势", onClick = onNavigateToStatistics)
                MoreToolActionRow(icon = Icons.Default.PhotoLibrary, title = "照片墙", description = "查看所有事件照片", onClick = onNavigateToPhotoWall)
                MoreToolActionRow(icon = Icons.Default.Share, title = "导出分享", description = "导出数据和分享图片", onClick = onNavigateToExport)
                MoreToolActionRow(icon = Icons.Default.AutoAwesome, title = "AI 智能建议", description = "整理事件和提醒状态", onClick = onNavigateToAiSuggestions)
                MoreToolActionRow(icon = Icons.Default.EmojiEvents, title = "成就系统", description = "查看记录进度", onClick = onNavigateToAchievements)
                MoreToolActionRow(icon = Icons.Default.CloudSync, title = "多设备同步", description = "查看同步状态和建议", onClick = onNavigateToSyncStatus)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("关闭") }
        }
    )
}

@Composable
fun MoreToolActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(MaterialTheme.shapes.large)
            .clickable(onClick = onClick),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = title, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
        }
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
                Text("轻松设置", color = Color.White, style = MaterialTheme.typography.headlineSmall)
                Text("把常用的留下，把低频工具收起来，界面就清爽很多。", color = Color.White.copy(alpha = 0.88f), style = MaterialTheme.typography.bodyMedium)
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
        AppThemeMode.CREAM -> Brush.linearGradient(listOf(Color(0xFFD97706), Color(0xFFFFF8EC)))
        AppThemeMode.MINT -> Brush.linearGradient(listOf(Color(0xFF0F9F8E), Color(0xFFF7FFFC)))
        AppThemeMode.LAVENDER -> Brush.linearGradient(listOf(Color(0xFF8B5CF6), Color(0xFFFCF8FF)))
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
    val operationState by viewModel.operationState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val snackbarScope = rememberCoroutineScope()

    LaunchedEffect(operationState.message) {
        operationState.message?.let { message ->
            snackbarHostState.showSnackbar(
                message = message,
                duration = if (operationState.importRestored) SnackbarDuration.Long else SnackbarDuration.Short
            )
            viewModel.clearOperationMessage()
        }
    }

    val dirPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            viewModel.saveBackupDirUri(uri)
            snackbarScope.launch { snackbarHostState.showSnackbar("已选择备份目录") }
        } else {
            snackbarScope.launch { snackbarHostState.showSnackbar("未选择备份目录") }
        }
    }

    val exportPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            viewModel.triggerManualBackup(uri)
        } else {
            snackbarScope.launch { snackbarHostState.showSnackbar("未选择备份目录") }
        }
    }

    val importPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.importBackup(uri)
        } else {
            snackbarScope.launch { snackbarHostState.showSnackbar("未选择备份文件") }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
            onSelectDir = { dirPicker.launch(null) },
            onManualBackup = { exportPicker.launch(null) },
            onImport = { importPicker.launch(arrayOf("application/octet-stream", "application/x-sqlite3", "application/vnd.sqlite3", "*/*")) },
            isBusy = operationState.inProgress
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

package com.memoriabox.ui.screen

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.memoriabox.BuildConfig
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import androidx.navigation.compose.rememberNavController
import com.memoriabox.ui.navigation.BottomNavigationItem
import com.memoriabox.ui.navigation.Screen
import com.memoriabox.ui.navigation.bottomNavItems
import com.memoriabox.ui.screen.components.*
import com.memoriabox.ui.screen.dialogs.EventDialog
import com.memoriabox.data.model.*
import com.memoriabox.ui.theme.AppThemeMode
import com.memoriabox.ui.theme.group
import com.memoriabox.update.ApkInstaller
import com.memoriabox.update.UpdateInfo
import com.memoriabox.update.UpdateManager
import com.memoriabox.update.UpdateState
import com.memoriabox.utils.AppSettings
import com.memoriabox.utils.MonthlySummaryHelper
import com.memoriabox.utils.NotificationHelper
import com.memoriabox.utils.startOfMonth
import com.memoriabox.viewmodel.*
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.random.Random

@Composable
fun MainScreen(
    application: Application,
    initialMonthlySummaryMonth: Long? = null,
    onMonthlySummaryIntentConsumed: () -> Unit = {},
    initialOpenUpdate: Boolean = false,
    onOpenUpdateConsumed: () -> Unit = {},
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
    val updateState by UpdateManager.state.collectAsState()
    var showUpdateDialog by remember { mutableStateOf(false) }
    var dismissedUpdateVersion by rememberSaveable { mutableStateOf<String?>(null) }
    var showInstallConfirmation by remember { mutableStateOf(false) }
    var pendingInstallPath by rememberSaveable { mutableStateOf<String?>(null) }
    val installPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        val path = pendingInstallPath
        if (path != null && ApkInstaller.canInstallPackages(context)) {
            ApkInstaller.install(context, Uri.parse(path))
            pendingInstallPath = null
        }
    }
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
        if (cuteTexts.size <= 1) return
        var next = Random.nextInt(cuteTexts.size)
        if (next == cuteTextIndex) next = (next + 1) % cuteTexts.size
        cuteTextIndex = next
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

    LaunchedEffect(initialOpenUpdate) {
        if (initialOpenUpdate) {
            showUpdateDialog = true
            dismissedUpdateVersion = null
            onOpenUpdateConsumed()
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
                    onNavigateToStatistics = { navController.navigate(Screen.Statistics.route) }
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
                val diaryMedia by calendarVM.allDiaryMedia.collectAsState(initial = emptyList())
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
                    onNavigateToSyncStatus = { navController.navigate(Screen.SyncStatus.route) },
                    onNavigateToDayTools = { navController.navigate(Screen.DayTools.route) },
                    onNavigateToCustomization = { navController.navigate(Screen.CustomizationSettings.route) },
                    onNavigateToFriends = { navController.navigate(Screen.Friends.route) },
                    onNavigateToEchoTime = { navController.navigate(Screen.EchoTime.route) },
                    onNavigateToMood = { navController.navigate(Screen.Mood.route) },
                    onNavigateToLabels = { navController.navigate(Screen.Labels.route) },
                    onBackupSettingsClick = { navController.navigate(Screen.BackupSettings.route) },
                    onWebDavSettingsClick = { navController.navigate(Screen.WebDavSettings.route) },
                    onCheckUpdate = {
                        showUpdateDialog = true
                        dismissedUpdateVersion = null
                        UpdateManager.check(context, manual = true)
                    }
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
            composable(Screen.Friends.route) {
                FriendsScreen(
                    application = application,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToFriendDetail = { friendId ->
                        navController.navigate(Screen.FriendDetail.createRoute(friendId))
                    }
                )
            }
            composable(Screen.EchoTime.route) {
                EchoTimeScreen(
                    application = application,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(Screen.Mood.route) {
                MoodScreen(
                    application = application,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(Screen.Labels.route) {
                LabelManageScreen(
                    application = application,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(
                route = Screen.FriendDetail.route,
                arguments = listOf(androidx.navigation.navArgument("friendId") { type = androidx.navigation.NavType.StringType })
            ) { backStackEntry ->
                val friendId = backStackEntry.arguments?.getString("friendId").orEmpty()
                FriendDetailScreen(
                    application = application,
                    friendId = friendId,
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

    if (showUpdateDialog) {
        val visibleUpdateInfo = when (val state = updateState) {
            is UpdateState.Available -> state.info
            is UpdateState.Downloading -> state.info
            is UpdateState.Ready -> state.info
            is UpdateState.Error -> state.info
            else -> null
        }
        if (visibleUpdateInfo != null && dismissedUpdateVersion != visibleUpdateInfo.versionName) {
            UpdateAvailableDialog(
                info = visibleUpdateInfo,
                state = updateState,
                onDismiss = {
                    dismissedUpdateVersion = visibleUpdateInfo.versionName
                    showUpdateDialog = false
                },
                onUpdate = {
                    when (val state = updateState) {
                        is UpdateState.Available -> UpdateManager.download(context, state.info)
                        is UpdateState.Ready -> showInstallConfirmation = true
                        is UpdateState.Error -> UpdateManager.retry(context, state.info)
                        else -> Unit
                    }
                }
            )
        } else if (updateState is UpdateState.Checking || updateState is UpdateState.UpToDate || updateState is UpdateState.Error) {
            val isChecking = updateState is UpdateState.Checking
            val errorMessage = (updateState as? UpdateState.Error)?.message
            AlertDialog(
                onDismissRequest = { showUpdateDialog = false },
                title = { Text("检查更新") },
                text = {
                    when {
                        isChecking -> Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(22.dp))
                            Spacer(Modifier.width(12.dp))
                            Text("正在检查 GitHub Release…")
                        }
                        errorMessage != null -> Text(errorMessage)
                        else -> Text("当前已是最新版本 v${BuildConfig.VERSION_NAME}。")
                    }
                },
                confirmButton = {
                    TextButton(
                        enabled = !isChecking,
                        onClick = { showUpdateDialog = false }
                    ) { Text("知道了") }
                }
            )
        }
    }

    if (showInstallConfirmation) {
        val ready = updateState as? UpdateState.Ready
        AlertDialog(
            onDismissRequest = { showInstallConfirmation = false },
            title = { Text("确认安装更新") },
            text = {
                Text("更新包已通过官方 SHA-256、版本、包名和签名校验。继续后将打开 Android 系统安装器覆盖安装 v${ready?.info?.versionName.orEmpty()}。")
            },
            dismissButton = {
                TextButton(onClick = { showInstallConfirmation = false }) { Text("稍后") }
            },
            confirmButton = {
                Button(
                    enabled = ready != null,
                    onClick = {
                        val apkPath = ready?.apkUri ?: return@Button
                        showInstallConfirmation = false
                        if (ApkInstaller.canInstallPackages(context)) {
                            ApkInstaller.install(context, Uri.parse(apkPath))
                        } else {
                            pendingInstallPath = apkPath
                            installPermissionLauncher.launch(ApkInstaller.permissionIntent(context))
                        }
                    }
                ) { Text("继续安装") }
            }
        )
    }
}

@Composable
private fun UpdateAvailableDialog(
    info: UpdateInfo,
    state: UpdateState,
    onDismiss: () -> Unit,
    onUpdate: () -> Unit
) {
    AlertDialog(
        onDismissRequest = {
            onDismiss()
        },
        title = { Text("发现新版本 v${info.versionName}") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (info.releaseName.isNotBlank()) {
                    Text(info.releaseName, style = MaterialTheme.typography.titleSmall)
                }
                Text(info.releaseNotes.ifBlank { "本次更新未提供说明。" })
                when (state) {
                    is UpdateState.Available -> Text("是否下载此版本？下载仅在你确认后开始。")
                    is UpdateState.Downloading -> {
                        LinearProgressIndicator(
                            progress = { state.progress / 100f },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text("正在下载并校验：${state.progress}%", style = MaterialTheme.typography.bodySmall)
                    }
                    is UpdateState.Ready -> Text("更新包已下载并完成全部校验。", color = MaterialTheme.colorScheme.primary)
                    is UpdateState.Error -> Text(state.message, color = MaterialTheme.colorScheme.error)
                    else -> Unit
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) { Text(if (state is UpdateState.Downloading) "后台下载" else "稍后") }
        },
        confirmButton = {
            Button(
                enabled = state !is UpdateState.Downloading,
                onClick = onUpdate
            ) {
                Text(
                    when (state) {
                        is UpdateState.Available -> "下载更新"
                        is UpdateState.Downloading -> "正在下载"
                        is UpdateState.Ready -> "安装更新"
                        is UpdateState.Error -> "重新下载"
                        else -> "下载更新"
                    }
                )
            }
        }
    )
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

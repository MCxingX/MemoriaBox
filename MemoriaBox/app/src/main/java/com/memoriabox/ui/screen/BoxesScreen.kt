package com.memoriabox.ui.screen

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import androidx.navigation.compose.*
import com.memoriabox.ui.navigation.Screen
import com.memoriabox.ui.screen.components.*
import com.memoriabox.ui.screen.dialogs.BoxDialog
import com.memoriabox.ui.screen.dialogs.EventDialog
import com.memoriabox.ui.utils.AdaptiveUiSize
import com.memoriabox.ui.utils.rememberAdaptiveUiSize
import com.memoriabox.data.model.*
import com.memoriabox.ui.theme.NianJiLogoMark
import com.memoriabox.ui.theme.MemoriaDesign
import com.memoriabox.utils.AppSettings
import com.memoriabox.utils.Header
import com.memoriabox.utils.NotificationHelper
import com.memoriabox.utils.LunarDateUtils
import com.memoriabox.utils.HolidayUtils
import com.memoriabox.viewmodel.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

@Composable
fun BoxesScreen(
    application: Application,
    onBoxClick: (String) -> Unit,
    onNavigateToCalendar: () -> Unit,
    onNavigateToPhotoWall: () -> Unit,
    onNavigateToStatistics: () -> Unit
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

    LaunchedEffect(boxes, events) {
        if (selectedBoxId != null && boxes.none { it.id == selectedBoxId }) {
            selectedBoxId = null
        }
    }

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
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            NianJiLogoMark(size = 32.dp)
                            Text("今天", style = MaterialTheme.typography.titleMedium)
                        }
                    },
                    windowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp),
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                    actions = {
                        IconButton(onClick = { showCreateDialog = true }) {
                            Icon(Icons.Default.CreateNewFolder, contentDescription = "添加分组")
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
                .widthIn(max = adaptiveUi.maxContentWidth)
                .align(Alignment.TopCenter)
                .padding(horizontal = adaptiveUi.screenPadding, vertical = adaptiveUi.sectionSpacing)
        ) {
        TodayHeader(
            visibleCount = visibleEvents.size,
            selectedBoxName = selectedBoxName,
            upcomingDays = upcomingDays,
            upcomingEnabled = upcomingEnabled,
            reminderEnabled = upcomingReminderEnabled,
            adaptiveUi = adaptiveUi,
            trailing = {
                HomeBoxFilter(
                    boxes = boxes,
                    selectedBoxId = selectedBoxId,
                    onBoxSelected = onBoxSelected,
                    onCreateBox = onCreateBox
                )
            }
        )
        if (!adaptiveUi.compact) {
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
        }
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
private fun TodayHeader(
    visibleCount: Int,
    selectedBoxName: String,
    upcomingDays: Int,
    upcomingEnabled: Boolean,
    reminderEnabled: Boolean,
    adaptiveUi: AdaptiveUiSize,
    trailing: @Composable () -> Unit
) {
    val now = remember { Date() }
    val dateText = remember(now) { SimpleDateFormat("M月d日 EEEE", Locale.getDefault()).format(now) }
    val lunarText = remember(now) { LunarDateUtils.dayLabelForGregorian(now.time) }
    val holidayText = remember(now) { HolidayUtils.holidayForDay(now.time) }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(adaptiveUi.cardRadius),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(adaptiveUi.cardPadding),
            verticalArrangement = Arrangement.spacedBy(MemoriaDesign.spacing.xs)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text("今天", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                    Text(
                        text = if (holidayText != null) "$dateText · $lunarText · $holidayText" else "$dateText · $lunarText",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (holidayText != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                trailing()
            }
            val reminderText = if (reminderEnabled) "提醒开启" else "提醒关闭"
            Text(
                text = if (upcomingEnabled) {
                    "$visibleCount 个日子 · $upcomingDays 天内优先 · $reminderText · $selectedBoxName"
                } else {
                    "$visibleCount 个日子 · $selectedBoxName"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
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
                .padding(MemoriaDesign.spacing.cardPadding)
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 32.dp, y = (-40).dp)
                    .size(if (adaptiveUi.compact) 104.dp else 136.dp)
                    .background(Color.White.copy(alpha = 0.12f), RoundedCornerShape(999.dp))
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .offset(x = (-40).dp, y = 40.dp)
                    .size(if (adaptiveUi.compact) 88.dp else 120.dp)
                    .background(Color.Black.copy(alpha = 0.08f), RoundedCornerShape(999.dp))
            )
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(color = Color.White.copy(alpha = 0.22f), shape = MaterialTheme.shapes.large) {
                        Text(dailyQuote.title, modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp), color = Color.White, style = MaterialTheme.typography.labelSmall)
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
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
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
            Spacer(Modifier.width(8.dp))
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
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
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
    val eventSpacing = adaptiveUi.sectionSpacing
    
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
            Spacer(Modifier.height(8.dp))
            Text("${upcomingDays} 天内没有需要特别留意的日子。", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))
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
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
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
            Spacer(Modifier.height(8.dp))
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


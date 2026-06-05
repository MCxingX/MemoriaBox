package com.memoriabox.ui.screen.components

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import coil.compose.AsyncImage
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.memoriabox.data.model.*
import com.memoriabox.ui.utils.rememberAdaptiveUiSize
import com.memoriabox.utils.ColorUtils
import com.memoriabox.utils.LunarDateUtils
import com.memoriabox.utils.MonthlySummaryUiState
import com.memoriabox.utils.startOfMonth
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.abs

@Composable
fun EnhancedEventGrid(
    events: List<Event>,
    onEventClick: (Event) -> Unit,
    onEventEdit: (Event) -> Unit,
    onCardTemplateChange: (Event, String) -> Unit = { _, _ -> },
    boxBgType: BgType = BgType.COLOR,
    boxBgValue: String = "#F5F5F5"
) {
    Box(
        modifier = Modifier.fillMaxSize()
            .background(if (boxBgType == BgType.COLOR) ColorUtils.hexToColor(boxBgValue) else Color.Gray)
    ) {
        if (events.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Event, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.Gray)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("暂无事件，点击 + 添加", color = Color.Gray)
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(events) { event ->
                    EnhancedEventCard(
                        event = event,
                        onClick = { onEventClick(event) },
                        onLongPress = { onEventEdit(event) },
                        onStyleChange = { newTemplate -> onCardTemplateChange(event, newTemplate) }
                    )
                }
            }
        }
    }
}

@Composable
fun EnhancedEventCard(event: Event, onClick: () -> Unit, onLongPress: () -> Unit, onStyleChange: (String) -> Unit = {}) {
    val daysRemaining = calculateDays(event)
    val styleOptions = remember {
        listOf(
            CardVisualStyle.HeroWide,
            CardVisualStyle.PosterTall,
            CardVisualStyle.GlassCompact,
            CardVisualStyle.SplitPanel,
            CardVisualStyle.NeonRail,
            CardVisualStyle.MinimalBadge
        )
    }
    val initialStyle = when (event.cardTemplate) {
        "POSTER" -> CardVisualStyle.PosterTall
        "GLASS" -> CardVisualStyle.GlassCompact
        "SPLIT" -> CardVisualStyle.SplitPanel
        "NEON" -> CardVisualStyle.NeonRail
        "MINIMAL" -> CardVisualStyle.MinimalBadge
        else -> CardVisualStyle.HeroWide
    }
    var styleIndex by remember(event.id, event.cardTemplate) { mutableIntStateOf(styleOptions.indexOf(initialStyle).coerceAtLeast(0)) }
    var isDragging by remember { mutableStateOf(false) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    val style = styleOptions[styleIndex]
    val dragProgress = (dragOffset / 180f).coerceIn(-1f, 1f)
    val animatedScale by animateFloatAsState(targetValue = if (isDragging) 1.04f else 1f, label = "eventCardScale")
    val animatedElevation by animateDpAsState(targetValue = if (isDragging) 16.dp else 4.dp, label = "eventCardElevation")
    val hasImage = event.avatarUri != null
    val displayFields = event.displayFields.split(",").map { it.trim() }.toSet()
    val eventTextColor = ColorUtils.hexToColor(event.textColor)
    val cardHeight = when (style) {
        CardVisualStyle.HeroWide -> 172.dp
        CardVisualStyle.PosterTall -> 236.dp
        CardVisualStyle.GlassCompact -> 138.dp
        CardVisualStyle.SplitPanel -> 196.dp
        CardVisualStyle.NeonRail -> 184.dp
        CardVisualStyle.MinimalBadge -> 152.dp
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(cardHeight)
            .graphicsLayer {
                scaleX = animatedScale
                scaleY = animatedScale
                rotationZ = dragProgress * 4f
                translationX = dragOffset * 0.22f
            }
            .shadow(elevation = animatedElevation, shape = RoundedCornerShape(22.dp))
            .pointerInput(event.id) {
                detectDragGestures(
                    onDragStart = { isDragging = true },
                    onDragCancel = {
                        isDragging = false
                        dragOffset = 0f
                    },
                    onDragEnd = {
                        isDragging = false
                        if (abs(dragOffset) > 44f) {
                            val newIndex = if (dragOffset > 0f) {
                                (styleIndex + 1) % styleOptions.size
                            } else {
                                (styleIndex - 1 + styleOptions.size) % styleOptions.size
                            }
                            styleIndex = newIndex
                            val newTemplate = when (styleOptions[newIndex]) {
                                CardVisualStyle.PosterTall -> "POSTER"
                                CardVisualStyle.GlassCompact -> "GLASS"
                                CardVisualStyle.SplitPanel -> "SPLIT"
                                CardVisualStyle.NeonRail -> "NEON"
                                CardVisualStyle.MinimalBadge -> "MINIMAL"
                                else -> "HERO"
                            }
                            onStyleChange(newTemplate)
                        }
                        dragOffset = 0f
                    },
                    onDrag = { change, dragAmount ->
                        dragOffset += dragAmount.x
                        change.consume()
                    }
                )
            }
            .combinedClickable(onClick = onClick, onLongClick = onLongPress),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = if (isDragging) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.72f)) else null
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(22.dp))
        ) {
            if (hasImage) {
                if (style == CardVisualStyle.HeroWide) {
                    AsyncImage(
                        model = event.avatarUri,
                        contentDescription = event.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .matchParentSize()
                            .blur(18.dp)
                    )
                }
                AsyncImage(
                    model = event.avatarUri,
                    contentDescription = event.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    ColorUtils.hexToColor(event.gradientStart),
                                    ColorUtils.hexToColor(event.gradientEnd),
                                    MaterialTheme.colorScheme.tertiaryContainer
                                )
                            )
                        )
                )
            }

            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(cardOverlayBrush(style))
            )

            if (style == CardVisualStyle.NeonRail) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .fillMaxHeight()
                        .width(8.dp)
                        .background(Brush.verticalGradient(listOf(ColorUtils.hexToColor(event.gradientStart), ColorUtils.hexToColor(event.gradientEnd))))
                )
            }

            if (isDragging) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                )
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp),
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(999.dp)
                ) {
                    Text(
                        if (dragOffset < -44f) "松手切上一款" else if (dragOffset > 44f) "松手切下一款" else "左右拖动换排版",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }

            when (style) {
                CardVisualStyle.SplitPanel -> EventCardSplitContent(event, daysRemaining, displayFields, eventTextColor)
                CardVisualStyle.GlassCompact -> EventCardGlassContent(event, daysRemaining, displayFields, eventTextColor)
                CardVisualStyle.PosterTall -> EventCardPosterContent(event, daysRemaining, displayFields, eventTextColor)
                CardVisualStyle.MinimalBadge -> EventCardMinimalContent(event, daysRemaining, displayFields, eventTextColor)
                else -> EventCardHeroContent(event, daysRemaining, displayFields, eventTextColor, style == CardVisualStyle.NeonRail)
            }

            Surface(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp),
                color = Color.White.copy(alpha = if (isDragging) 0.30f else 0.16f),
                shape = RoundedCornerShape(999.dp)
            ) {
                Text(
                    text = cardStyleLabel(style),
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                    color = eventTextColor,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun EventCardHeroContent(event: Event, daysRemaining: Long, displayFields: Set<String>, color: Color, insetForRail: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth(0.82f)
            .padding(if (insetForRail) 20.dp else 16.dp)
            .wrapContentHeight()
    ) {
        EventTypePill(event.type, color)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = event.name, style = MaterialTheme.typography.titleLarge, color = color, maxLines = 2)
        Text(text = "$daysRemaining 天", style = MaterialTheme.typography.headlineMedium, color = color)
        EventMetaLines(event, displayFields, color)
    }
}

@Composable
private fun EventCardPosterContent(event: Event, daysRemaining: Long, displayFields: Set<String>, color: Color) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(18.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            EventTypePill(event.type, color)
            Text(formatDate(event.date), style = MaterialTheme.typography.labelSmall, color = color.copy(alpha = 0.86f), maxLines = 1)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Text(text = "$daysRemaining", style = MaterialTheme.typography.displayMedium, color = color)
            Text(text = "天", style = MaterialTheme.typography.titleMedium, color = color.copy(alpha = 0.88f))
        }
        Column {
            Text(text = event.name, style = MaterialTheme.typography.titleLarge, color = color, maxLines = 2)
            EventMetaLines(event, displayFields - "date", color)
        }
    }
}

@Composable
private fun BoxScope.EventCardGlassContent(event: Event, daysRemaining: Long, displayFields: Set<String>, color: Color) {
    Surface(
        modifier = Modifier
            .align(Alignment.BottomStart)
            .padding(14.dp)
            .fillMaxWidth(0.88f),
        color = Color.White.copy(alpha = 0.18f),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = event.name, style = MaterialTheme.typography.titleMedium, color = color, maxLines = 1)
            Text(text = "$daysRemaining 天", style = MaterialTheme.typography.headlineSmall, color = color)
            EventMetaLines(event, displayFields, color)
        }
    }
}

@Composable
private fun EventCardSplitContent(event: Event, daysRemaining: Long, displayFields: Set<String>, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Surface(color = Color.White.copy(alpha = 0.22f), shape = RoundedCornerShape(18.dp), modifier = Modifier.size(76.dp)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center, modifier = Modifier.fillMaxSize()) {
                Text(text = "$daysRemaining", style = MaterialTheme.typography.headlineMedium, color = color, maxLines = 1)
                Text(text = "天", style = MaterialTheme.typography.labelSmall, color = color.copy(alpha = 0.82f))
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            EventTypePill(event.type, color)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = event.name, style = MaterialTheme.typography.titleMedium, color = color, maxLines = 2)
            EventMetaLines(event, displayFields, color)
        }
    }
}

@Composable
private fun BoxScope.EventCardMinimalContent(event: Event, daysRemaining: Long, displayFields: Set<String>, color: Color) {
    Column(modifier = Modifier.align(Alignment.CenterStart).padding(18.dp).fillMaxWidth(0.74f)) {
        Text(text = "$daysRemaining 天", style = MaterialTheme.typography.titleLarge, color = color)
        Text(text = event.name, style = MaterialTheme.typography.titleMedium, color = color, maxLines = 2)
        EventMetaLines(event, displayFields, color)
    }
}

@Composable
private fun EventTypePill(type: EventType, color: Color) {
    Surface(color = Color.White.copy(alpha = 0.22f), shape = RoundedCornerShape(999.dp)) {
        Text(text = eventTypeText(type), modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), color = color, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun EventMetaLines(event: Event, displayFields: Set<String>, color: Color) {
    if ("date" in displayFields) Text(text = formatDate(event.date), style = MaterialTheme.typography.bodySmall, color = color.copy(alpha = 0.86f), maxLines = 1)
    if ("lunar" in displayFields && event.lunar != null) Text(text = event.lunar, style = MaterialTheme.typography.labelSmall, color = color.copy(alpha = 0.82f), maxLines = 1)
    if ("note" in displayFields && event.note.isNotBlank()) Text(text = event.note, style = MaterialTheme.typography.labelSmall, color = color.copy(alpha = 0.82f), maxLines = 1)
    if ("reminder" in displayFields && event.reminderEnabled) Text(text = "提前 ${event.reminderDays} 天提醒", style = MaterialTheme.typography.labelSmall, color = color.copy(alpha = 0.82f), maxLines = 1)
}

private enum class CardVisualStyle { HeroWide, PosterTall, GlassCompact, SplitPanel, NeonRail, MinimalBadge }

private fun cardStyleLabel(style: CardVisualStyle): String = when (style) {
    CardVisualStyle.HeroWide -> "封面"
    CardVisualStyle.PosterTall -> "海报"
    CardVisualStyle.GlassCompact -> "玻璃"
    CardVisualStyle.SplitPanel -> "分栏"
    CardVisualStyle.NeonRail -> "光轨"
    CardVisualStyle.MinimalBadge -> "徽章"
}

private fun cardOverlayBrush(style: CardVisualStyle): Brush = when (style) {
    CardVisualStyle.GlassCompact -> Brush.linearGradient(listOf(Color.White.copy(alpha = 0.18f), Color.Black.copy(alpha = 0.50f)))
    CardVisualStyle.SplitPanel -> Brush.horizontalGradient(listOf(Color.Black.copy(alpha = 0.70f), Color.Black.copy(alpha = 0.18f)))
    CardVisualStyle.NeonRail -> Brush.linearGradient(listOf(Color.Black.copy(alpha = 0.68f), Color.Black.copy(alpha = 0.18f), Color.Black.copy(alpha = 0.60f)))
    CardVisualStyle.MinimalBadge -> Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.18f), Color.Black.copy(alpha = 0.46f)))
    else -> Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.06f), Color.Black.copy(alpha = 0.58f)))
}

private fun eventTypeText(type: EventType): String = when (type) {
    EventType.COUNTDOWN -> "还剩"
    EventType.ANNIVERSARY -> "纪念日"
    EventType.ELAPSED -> "已过"
    EventType.BIRTHDAY -> "生日"
    EventType.TODO -> "待办"
}

@Composable
fun CalendarViewScreen(
    events: List<Event>,
    modifier: Modifier = Modifier,
    onAddEvent: (Long) -> Unit = {},
    onEventClick: (Event) -> Unit = {},
    diaries: List<DiaryEntry> = emptyList(),
    onSaveDiary: (DiaryEntry?, Long, String, List<DiaryMedia>, String?) -> Unit = { _, _, _, _, _ -> },
    onDeleteDiary: (DiaryEntry) -> Unit = {},
    onLoadDiaryMedia: (DiaryEntry) -> Unit = {},
    diaryMediaMap: Map<String, List<DiaryMedia>> = emptyMap(),
    monthlySummaryEnabled: Boolean = true,
    monthlySummaryState: MonthlySummaryUiState = MonthlySummaryUiState(),
    initialShowSummary: Boolean = false,
    onOpenMonthlySummary: () -> Unit = {},
    onSummaryPlayModeChange: (Boolean) -> Unit = {},
    onSummarySpeedChange: (Float) -> Unit = {},
    onSummaryTextEnabledChange: (Boolean) -> Unit = {},
    onLoadMonthlySummary: (Long) -> Unit = {}
) {
    val adaptiveUi = rememberAdaptiveUiSize()
    var currentMonthYear by remember { mutableStateOf("${Calendar.getInstance().get(Calendar.YEAR)}-${Calendar.getInstance().get(Calendar.MONTH) + 1}") }
    var showMonthlySummary by remember { mutableStateOf(initialShowSummary) }
    LaunchedEffect(initialShowSummary) {
        if (initialShowSummary) showMonthlySummary = true
    }
    val monthFormat = SimpleDateFormat("yyyy年MM月", Locale.getDefault())
    val cal = remember(currentMonthYear) {
        Calendar.getInstance().apply {
            val parts = currentMonthYear.split("-").map { it.toInt() }
            set(parts[0], parts[1] - 1, 1)
        }
    }
    val monthEvents = remember(events, currentMonthYear) {
        events.filter { event ->
            (1..cal.getActualMaximum(Calendar.DAY_OF_MONTH)).any { day ->
                val dayCal = cal.clone() as Calendar
                dayCal.set(Calendar.DAY_OF_MONTH, day)
                occursOnDay(event, dayCal)
            }
        }
    }
    val today = Calendar.getInstance()
    val nearestEvent = remember(monthEvents) {
        monthEvents.minByOrNull { kotlin.math.abs(it.date - System.currentTimeMillis()) }
    }
    var selectedDay by remember { mutableStateOf<Pair<Long, List<Event>>?>(null) }
    var selectedDiaryForView by remember { mutableStateOf<DiaryEntry?>(null) }
    var editingDiary by remember { mutableStateOf<DiaryEntry?>(null) }
    var editingDiaryDate by remember { mutableStateOf<Long?>(null) }

    val diaryMap = remember(diaries) {
        diaries.groupBy { startOfDayMillis(it.dateStart) }
    }

    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            modifier = Modifier.height(adaptiveUi.topBarHeight),
            title = { Text("日历视图") },
            windowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp),
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            actions = {
                if (monthlySummaryEnabled) {
                    IconButton(onClick = {
                        showMonthlySummary = true
                        val parts = currentMonthYear.split("-").map { it.toInt() }
                        val monthCal = Calendar.getInstance().apply {
                            set(parts[0], parts[1] - 1, 1, 0, 0, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                        onLoadMonthlySummary(startOfMonth(monthCal.timeInMillis))
                    }) { Icon(Icons.Default.AutoStories, contentDescription = "月度总结") }
                }
            }
        )
        CalendarBoardSummary(
            totalCount = events.size,
            monthCount = monthEvents.size,
            todayCount = events.count { occursOnDay(it, today) },
            nearestEvent = nearestEvent
        )
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = adaptiveUi.screenPadding, vertical = adaptiveUi.sectionSpacing),
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.linearGradient(listOf(Color(0xFFFF6B6B), Color(0xFF7C5CFF), Color(0xFF00B8D9))))
                    .padding(horizontal = 10.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    val updated = cal.clone() as Calendar
                    updated.add(Calendar.MONTH, -1)
                    currentMonthYear = "${updated.get(Calendar.YEAR)}-${updated.get(Calendar.MONTH) + 1}"
                }) { Icon(Icons.Default.ChevronLeft, contentDescription = "上月", tint = Color.White) }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = monthFormat.format(cal.time), style = MaterialTheme.typography.titleLarge, color = Color.White, maxLines = 1)
                    Text("看板/月历一体查看事件分布，本月 ${monthEvents.size} 个日子", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.82f))
                }
                IconButton(onClick = {
                    val updated = cal.clone() as Calendar
                    updated.add(Calendar.MONTH, 1)
                    currentMonthYear = "${updated.get(Calendar.YEAR)}-${updated.get(Calendar.MONTH) + 1}"
                }) { Icon(Icons.Default.ChevronRight, contentDescription = "下月", tint = Color.White) }
            }
        }
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val horizontalPadding = if (adaptiveUi.compact) 6.dp else adaptiveUi.screenPadding
            val widthCell = (maxWidth - horizontalPadding * 2) / 7
            val fontScale = LocalDensity.current.fontScale
            val cellSize = (widthCell * (1f + (fontScale - 1f).coerceAtLeast(0f) * 0.35f))
                .coerceIn(38.dp, 72.dp)
            CalendarGrid(
                currentMonth = cal,
                events = events,
                cellSize = cellSize,
                horizontalPadding = horizontalPadding,
                diaryMap = diaryMap,
                onDayClick = { dayCal, dayEvents ->
                    selectedDay = dayCal.timeInMillis to dayEvents
                }
            )
        }
    }

    selectedDay?.let { (date, dayEvents) ->
        val dayDiaries = diaryMap[startOfDayMillis(date)] ?: emptyList()
        CalendarDayDetailDialog(
            date = date,
            events = dayEvents,
            diaries = dayDiaries,
            diaryMediaMap = diaryMediaMap,
            onDismiss = { selectedDay = null },
            onAddEvent = {
                selectedDay = null
                onAddEvent(date)
            },
            onEventClick = { event ->
                selectedDay = null
                onEventClick(event)
            },
            onWriteDiary = {
                editingDiaryDate = date
            },
            onViewDiary = { targetDiary ->
                onLoadDiaryMedia(targetDiary)
                selectedDiaryForView = targetDiary
            },
            onEditDiary = { targetDiary ->
                editingDiary = targetDiary
            },
            onDeleteDiary = { targetDiary ->
                onDeleteDiary(targetDiary)
            }
        )
    }

    selectedDiaryForView?.let { diary ->
        DiaryDetailDialog(
            diary = diary,
            mediaList = diaryMediaMap[diary.id] ?: emptyList(),
            onDismiss = { selectedDiaryForView = null },
            onEdit = {
                selectedDiaryForView = null
                editingDiary = diary
            },
            onDelete = {
                selectedDiaryForView = null
                onDeleteDiary(diary)
            }
        )
    }

    editingDiary?.let { diary ->
        DiaryEditorDialog(
            existingDiary = diary,
            existingMedia = diaryMediaMap[diary.id] ?: emptyList(),
            allDiaries = diaries,
            dateStart = diary.dateStart,
            onDismiss = { editingDiary = null },
            onSave = { selectedDate, content, media, bgUri ->
                onSaveDiary(diary, selectedDate, content, media, bgUri)
                editingDiary = null
            },
            onDelete = {
                editingDiary = null
                onDeleteDiary(diary)
            },
            onOpenExistingDiary = { targetDiary ->
                onLoadDiaryMedia(targetDiary)
                editingDiary = targetDiary
            }
        )
    }

    editingDiaryDate?.let { date ->
        DiaryEditorDialog(
            allDiaries = diaries,
            dateStart = date,
            onDismiss = { editingDiaryDate = null },
            onSave = { selectedDate, content, media, bgUri ->
                onSaveDiary(null, selectedDate, content, media, bgUri)
                editingDiaryDate = null
            },
            onOpenExistingDiary = { targetDiary ->
                onLoadDiaryMedia(targetDiary)
                editingDiaryDate = null
                editingDiary = targetDiary
            }
        )
    }

    if (monthlySummaryEnabled && showMonthlySummary) {
        MonthlySummaryPanel(
            state = monthlySummaryState,
            onDismiss = { showMonthlySummary = false },
            onMonthChange = { newMonth ->
                onLoadMonthlySummary(newMonth)
            },
            onPlayModeChange = onSummaryPlayModeChange,
            onSpeedChange = onSummarySpeedChange,
            onTextEnabledChange = onSummaryTextEnabledChange
        )
    }
}

@Composable
private fun CalendarBoardSummary(
    totalCount: Int,
    monthCount: Int,
    todayCount: Int,
    nearestEvent: Event?
) {
    val adaptiveUi = rememberAdaptiveUiSize()
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = adaptiveUi.screenPadding, vertical = adaptiveUi.sectionSpacing),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(if (adaptiveUi.compact) 10.dp else 14.dp), verticalArrangement = Arrangement.spacedBy(adaptiveUi.sectionSpacing)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CalendarBoardMetric("全部", totalCount.toString(), Modifier.weight(1f))
                CalendarBoardMetric("本月", monthCount.toString(), Modifier.weight(1f))
                CalendarBoardMetric("今天", todayCount.toString(), Modifier.weight(1f))
            }
            Text(
                nearestEvent?.let { "最近日子：${it.name} · ${SimpleDateFormat("M月d日", Locale.getDefault()).format(Date(it.date))}" } ?: "最近日子：暂无",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun CalendarBoardMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.62f)
    ) {
        Column(modifier = Modifier.padding(vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun CalendarGrid(
    currentMonth: Calendar,
    events: List<Event>,
    cellSize: androidx.compose.ui.unit.Dp,
    horizontalPadding: androidx.compose.ui.unit.Dp,
    diaryMap: Map<Long, List<DiaryEntry>> = emptyMap(),
    onDayClick: (Calendar, List<Event>) -> Unit = { _, _ -> }
) {
    val daysOfWeek = listOf("一", "二", "三", "四", "五", "六", "日")
    val calendar = currentMonth.clone() as Calendar
    calendar.set(Calendar.DAY_OF_MONTH, 1)
    val firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
    val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    val today = Calendar.getInstance()
    val todayDay = today.get(Calendar.DAY_OF_MONTH)
    val todayMonth = today.get(Calendar.MONTH)
    val todayYear = today.get(Calendar.YEAR)

    Column(
        modifier = Modifier
            .padding(horizontal = horizontalPadding)
            .verticalScroll(rememberScrollState())
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            daysOfWeek.forEach { day ->
                Box(modifier = Modifier.weight(1f).padding(horizontal = 2.dp), contentAlignment = Alignment.Center) {
                    Text(text = day, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(vertical = 5.dp))
                }
            }
        }

        val daysToFirst = (firstDayOfWeek - Calendar.MONDAY + 7) % 7
        val totalCells = daysToFirst + daysInMonth
        val rows = (totalCells + 6) / 7

        for (row in 0 until rows) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                for (col in 0 until 7) {
                    val cellIndex = row * 7 + col
                    val dayNumber = cellIndex - daysToFirst + 1
                    if (dayNumber in 1..daysInMonth) {
                        val dayCal = calendar.clone() as Calendar
                        dayCal.set(Calendar.DAY_OF_MONTH, dayNumber)
                        dayCal.set(Calendar.HOUR_OF_DAY, 0)
                        dayCal.set(Calendar.MINUTE, 0)
                        dayCal.set(Calendar.SECOND, 0)
                        dayCal.set(Calendar.MILLISECOND, 0)
                        val isToday = dayNumber == todayDay && currentMonth.get(Calendar.MONTH) == todayMonth && currentMonth.get(Calendar.YEAR) == todayYear

                        val dayEvents = events.filter { event -> occursOnDay(event, dayCal) }
                        val dayDiaries = diaryMap[startOfDayMillis(dayCal.timeInMillis)] ?: emptyList()
                        CalendarDayCell(
                            day = dayNumber,
                            isToday = isToday,
                            events = dayEvents,
                            diaries = dayDiaries,
                            modifier = Modifier.weight(1f).height(cellSize),
                            onClick = { onDayClick(dayCal, dayEvents) }
                        )
                    } else {
                        Box(modifier = Modifier.weight(1f).height(cellSize))
                    }
                }
            }
        }
    }
}

@Composable
fun CalendarDayCell(
    day: Int,
    isToday: Boolean,
    events: List<Event>,
    diaries: List<DiaryEntry> = emptyList(),
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Column(
        modifier = modifier
            .padding(2.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .background(
                if (isToday) Brush.linearGradient(listOf(Color(0xFFFF6B6B).copy(alpha = 0.22f), Color(0xFF7C5CFF).copy(alpha = 0.18f)))
                else Brush.linearGradient(listOf(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.52f)))
            )
            .padding(horizontal = 2.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterVertically)
    ) {
        Text(
            text = day.toString(),
            style = MaterialTheme.typography.bodyMedium,
            color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
        if (events.isNotEmpty() || diaries.isNotEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                if (events.isNotEmpty()) {
                    repeat(minOf(events.size, 3)) {
                        val dotColor = listOf(Color(0xFFFF6B6B), Color(0xFF7C5CFF), Color(0xFF00B8D9))[it]
                        Box(modifier = Modifier.size(6.dp).clip(RoundedCornerShape(3.dp)).background(dotColor))
                    }
                }
                if (diaries.isNotEmpty()) {
                    Box(modifier = Modifier.size(7.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFF42A5F5)))
                }
            }
        }
    }
}

@Composable
private fun CalendarDayDetailDialog(
    date: Long,
    events: List<Event>,
    diaries: List<DiaryEntry>,
    diaryMediaMap: Map<String, List<DiaryMedia>>,
    onDismiss: () -> Unit,
    onAddEvent: () -> Unit,
    onEventClick: (Event) -> Unit,
    onWriteDiary: () -> Unit,
    onViewDiary: (DiaryEntry) -> Unit,
    onEditDiary: (DiaryEntry) -> Unit,
    onDeleteDiary: (DiaryEntry) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(SimpleDateFormat("yyyy年M月d日", Locale.getDefault()).format(Date(date))) },
        text = {
            Box(modifier = Modifier.fillMaxWidth().heightIn(min = 220.dp, max = 420.dp)) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = 72.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("今日日记", style = MaterialTheme.typography.titleSmall)
                    if (diaries.isEmpty()) {
                        Surface(
                            modifier = Modifier.fillMaxWidth().clickable(onClick = onWriteDiary),
                            shape = MaterialTheme.shapes.large,
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("写今天日记", style = MaterialTheme.typography.titleSmall)
                                    Text("记录今天发生了什么，可添加照片或视频背景", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    } else {
                        diaries.forEachIndexed { index, diary ->
                            val diaryMedia = diaryMediaMap[diary.id] ?: emptyList()
                            Surface(
                                modifier = Modifier.fillMaxWidth().clickable { onViewDiary(diary) },
                                shape = MaterialTheme.shapes.large,
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.84f)
                            ) {
                                Box(modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp)) {
                                    MediaBackgroundPlayer(
                                        mediaUri = diary.backgroundMediaUri,
                                        mediaType = diary.backgroundMediaType,
                                        modifier = Modifier.matchParentSize()
                                    )
                                    Box(modifier = Modifier.matchParentSize().background(Color.Black.copy(alpha = 0.45f)))
                                    Column(
                                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        if (diaries.size > 1) {
                                            Surface(
                                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                                shape = RoundedCornerShape(999.dp)
                                            ) {
                                                Text(
                                                    "第 ${index + 1} 篇",
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                                    color = Color.White,
                                                    style = MaterialTheme.typography.labelSmall
                                                )
                                            }
                                        }
                                        ScrollingTextAnimation(text = diary.content, charDelay = 45L)
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                            AssistChip(
                                                onClick = { onEditDiary(diary) },
                                                label = { Text("编辑") },
                                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                            )
                                            if (diaryMedia.isNotEmpty()) {
                                                AssistChip(
                                                    onClick = { onViewDiary(diary) },
                                                    label = { Text("${diaryMedia.size} 个媒体") },
                                                    leadingIcon = { Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        Surface(
                            modifier = Modifier.fillMaxWidth().clickable(onClick = onWriteDiary),
                            shape = MaterialTheme.shapes.large,
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.52f)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                Text("再写一篇", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }

                    Text("当天日子", style = MaterialTheme.typography.titleSmall)
                    if (events.isEmpty()) {
                        Text("这一天还没有日子，可以点击右下角添加。", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        events.forEach { event ->
                            Surface(
                                modifier = Modifier.fillMaxWidth().clickable { onEventClick(event) },
                                shape = MaterialTheme.shapes.large,
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier.size(10.dp).clip(RoundedCornerShape(5.dp)).background(ColorUtils.hexToColor(event.gradientStart))
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(event.name, style = MaterialTheme.typography.titleSmall, maxLines = 1)
                                        Text(eventTypeText(event.type), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Icon(Icons.Default.ChevronRight, contentDescription = null)
                                }
                            }
                        }
                    }
                }
                FloatingActionButton(
                    onClick = onAddEvent,
                    modifier = Modifier.align(Alignment.BottomEnd),
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "添加日子")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("关闭") }
        }
    )
}

@Composable
fun LogFilterBar(
    onFilterChange: (String) -> Unit,
    onDateRangeChange: (Pair<Long, Long>?) -> Unit
) {
    var selectedFilter by remember { mutableStateOf("全部") }
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        listOf("全部", "BOX", "EVENT", "BACKUP").forEach { filter ->
            FilterChip(
                selected = selectedFilter == filter,
                onClick = { selectedFilter = filter; onFilterChange(if (filter == "全部") "" else filter) },
                label = { Text(filter) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

fun calculateDays(event: Event): Long = calculateDays(event.date, event.type, event.lunar)

fun calculateDays(dateMillis: Long, type: EventType, lunar: String? = null): Long {
    val now = System.currentTimeMillis()
    return when (type) {
        EventType.COUNTDOWN -> TimeUnit.MILLISECONDS.toDays(dateMillis - now)
        EventType.ANNIVERSARY -> TimeUnit.MILLISECONDS.toDays(now - dateMillis)
        EventType.ELAPSED -> TimeUnit.MILLISECONDS.toDays(now - dateMillis)
        EventType.BIRTHDAY -> {
            lunar?.let { lunarValue ->
                LunarDateUtils.daysUntilNextOccurrence(lunarValue, now)?.let { return it }
            }
            val eventCal = Calendar.getInstance().apply { timeInMillis = dateMillis }
            val nowCal = Calendar.getInstance()
            val eventMonth = eventCal.get(Calendar.MONTH)
            val eventDay = eventCal.get(Calendar.DAY_OF_MONTH)
            nowCal.set(Calendar.MONTH, eventMonth)
            nowCal.set(Calendar.DAY_OF_MONTH, eventDay)
            if (nowCal.before(now)) {
                nowCal.add(Calendar.YEAR, 1)
            }
            TimeUnit.MILLISECONDS.toDays(nowCal.timeInMillis - now)
        }
        EventType.TODO -> 0
    }
}

fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

private fun startOfDayMillis(timestamp: Long): Long {
    return Calendar.getInstance().apply {
        timeInMillis = timestamp
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}

private fun occursOnDay(event: Event, dayCal: Calendar): Boolean {
    val eventCal = Calendar.getInstance().apply { timeInMillis = event.date }
    fun sameDay(left: Calendar, right: Calendar): Boolean {
        return left.get(Calendar.YEAR) == right.get(Calendar.YEAR) &&
            left.get(Calendar.MONTH) == right.get(Calendar.MONTH) &&
            left.get(Calendar.DAY_OF_MONTH) == right.get(Calendar.DAY_OF_MONTH)
    }
    if (sameDay(eventCal, dayCal)) return true
    if (dayCal.timeInMillis < event.date) return false
    val mode = when {
        event.repeatMode != RepeatMode.NONE -> event.repeatMode
        event.repeatYearly || event.type == EventType.BIRTHDAY -> RepeatMode.YEARLY
        else -> RepeatMode.NONE
    }
    return when (mode) {
        RepeatMode.YEARLY -> eventCal.get(Calendar.MONTH) == dayCal.get(Calendar.MONTH) && eventCal.get(Calendar.DAY_OF_MONTH) == dayCal.get(Calendar.DAY_OF_MONTH)
        RepeatMode.MONTHLY -> eventCal.get(Calendar.DAY_OF_MONTH) == dayCal.get(Calendar.DAY_OF_MONTH)
        RepeatMode.CUSTOM_DAYS -> ((dayCal.timeInMillis - event.date) / 86_400_000L) % event.repeatInterval.coerceAtLeast(1) == 0L
        RepeatMode.CUSTOM_WEEKS -> ((dayCal.timeInMillis - event.date) / 86_400_000L) % (7L * event.repeatInterval.coerceAtLeast(1)) == 0L
        RepeatMode.CUSTOM_MONTHS -> {
            val months = (dayCal.get(Calendar.YEAR) - eventCal.get(Calendar.YEAR)) * 12 + dayCal.get(Calendar.MONTH) - eventCal.get(Calendar.MONTH)
            eventCal.get(Calendar.DAY_OF_MONTH) == dayCal.get(Calendar.DAY_OF_MONTH) && months % event.repeatInterval.coerceAtLeast(1) == 0
        }
        RepeatMode.NONE -> false
    }
}

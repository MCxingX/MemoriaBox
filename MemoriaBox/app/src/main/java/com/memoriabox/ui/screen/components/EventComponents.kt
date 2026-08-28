package com.memoriabox.ui.screen.components

import android.net.Uri
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.memoriabox.data.model.*
import com.memoriabox.ui.theme.LocalMemoriaThemeTokens
import com.memoriabox.ui.utils.rememberAdaptiveUiSize
import com.memoriabox.utils.AppSettings
import com.memoriabox.utils.ColorUtils
import com.memoriabox.utils.ImageImportUtils
import com.memoriabox.utils.LunarDateUtils
import com.memoriabox.utils.HolidayUtils
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
    val tokens = LocalMemoriaThemeTokens.current
    val hasTodo = events.any { it.type == EventType.TODO }
    val hasBirthday = events.any { it.type == EventType.BIRTHDAY }
    val hasAnniversary = events.any { it.type == EventType.ANNIVERSARY || it.type == EventType.ELAPSED || it.type == EventType.COUNTDOWN }
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
    val style = when (event.cardTemplate) {
        "POSTER" -> CardVisualStyle.PosterTall
        "GLASS", "SOFT_GLASS" -> CardVisualStyle.GlassCompact
        "SPLIT" -> CardVisualStyle.SplitPanel
        "NEON" -> CardVisualStyle.NeonRail
        "MINIMAL" -> CardVisualStyle.MinimalBadge
        else -> CardVisualStyle.HeroWide
    }
    var styleIndex by remember(event.id, event.cardTemplate) {
        mutableIntStateOf(styleOptions.indexOf(style).coerceAtLeast(0))
    }
    var horizontalDrag by remember { mutableFloatStateOf(0f) }
    var horizontalDragActive by remember { mutableStateOf(false) }
    val hasImage = event.avatarUri != null
    val displayFields = event.displayFields.split(",").map { it.trim() }.toSet()
    val eventTextColor = ColorUtils.hexToColor(event.textColor)
    val context = LocalContext.current

    var imageRatio by remember(event.avatarUri) { mutableStateOf<Float?>(null) }
    LaunchedEffect(event.avatarUri) {
        if (event.avatarUri == null) return@LaunchedEffect
        imageRatio = runCatching {
            com.memoriabox.utils.ImageImportUtils.getImageAspectRatio(context, event.avatarUri)
        }.getOrNull()
    }

    val cardModifier = Modifier
        .fillMaxWidth()
        .then(
            if (imageRatio != null) {
                val adaptiveAspect = imageRatio!!.coerceIn(0.62f, 1.56f)
                Modifier.heightIn(min = 156.dp, max = 360.dp)
                    .aspectRatio(adaptiveAspect, matchHeightConstraintsFirst = false)
            } else {
                Modifier.height(156.dp)
            }
        )
        .shadow(elevation = 3.dp, shape = RoundedCornerShape(20.dp))
        .pointerInput(event.id) {
            detectDragGestures(
                onDragStart = {
                    horizontalDrag = 0f
                    horizontalDragActive = false
                },
                onDragCancel = {
                    horizontalDrag = 0f
                    horizontalDragActive = false
                },
                onDragEnd = {
                    if (horizontalDragActive && abs(horizontalDrag) >= 72f) {
                        val nextIndex = if (horizontalDrag < 0f) {
                            (styleIndex + 1) % styleOptions.size
                        } else {
                            (styleIndex - 1 + styleOptions.size) % styleOptions.size
                        }
                        styleIndex = nextIndex
                        onStyleChange(
                            when (styleOptions[nextIndex]) {
                                CardVisualStyle.PosterTall -> "POSTER"
                                CardVisualStyle.GlassCompact -> "GLASS"
                                CardVisualStyle.SplitPanel -> "SPLIT"
                                CardVisualStyle.NeonRail -> "NEON"
                                CardVisualStyle.MinimalBadge -> "MINIMAL"
                                CardVisualStyle.HeroWide -> "HERO"
                            }
                        )
                    }
                    horizontalDrag = 0f
                    horizontalDragActive = false
                },
                onDrag = { change, dragAmount ->
                    if (abs(dragAmount.x) > abs(dragAmount.y) * 1.2f || horizontalDragActive) {
                        horizontalDragActive = true
                        horizontalDrag += dragAmount.x
                        change.consume()
                    }
                }
            )
        }
        .combinedClickable(onClick = onClick, onLongClick = onLongPress)

    Card(
        modifier = cardModifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(0.8.dp, Color.White.copy(alpha = 0.30f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(20.dp))
        ) {
            if (hasImage) {
                if (style == CardVisualStyle.HeroWide) {
                    AsyncImage(
                        model = event.avatarUri,
                        contentDescription = event.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .matchParentSize()
                            .blur(22.dp)
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
                    .padding(10.dp),
                    color = Color.White.copy(alpha = 0.22f),
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
private fun EventReminderLeading(event: Event, accent: Color, accentEnd: Color) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Brush.linearGradient(listOf(accent.copy(alpha = 0.92f), accentEnd.copy(alpha = 0.78f)))),
        contentAlignment = Alignment.Center
    ) {
        if (event.avatarUri != null) {
            AsyncImage(
                model = event.avatarUri,
                contentDescription = event.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize()
            )
        } else {
            Text(
                text = eventTypeInitial(event.type),
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )
        }
    }
}

private fun eventTypeInitial(type: EventType): String = when (type) {
    EventType.COUNTDOWN -> "D"
    EventType.ANNIVERSARY -> "A"
    EventType.ELAPSED -> "E"
    EventType.BIRTHDAY -> "B"
    EventType.TODO -> "T"
}

private fun eventStatusText(event: Event, daysRemaining: Long): String = when (event.type) {
    EventType.COUNTDOWN -> if (daysRemaining >= 0) "还剩 $daysRemaining 天" else "已过 ${abs(daysRemaining)} 天"
    EventType.ANNIVERSARY -> "已走过 $daysRemaining 天"
    EventType.ELAPSED -> "已过去 $daysRemaining 天"
    EventType.BIRTHDAY -> if (daysRemaining == 0L) birthdayGreetingText(event.name) else "生日还有 $daysRemaining 天"
    EventType.TODO -> when (event.todoStatus) {
        TodoStatus.COMPLETED -> "已完成"
        TodoStatus.CANCELLED -> "已取消"
        TodoStatus.PENDING -> event.dueDate?.let { "待办到期 ${formatDate(it)}" } ?: "待办"
    }
}

private fun eventPrimaryCountText(event: Event, daysRemaining: Long): String {
    return if (event.type == EventType.BIRTHDAY && daysRemaining == 0L) "生日快乐" else "$daysRemaining 天"
}

private fun eventPrimaryNumberText(event: Event, daysRemaining: Long): String {
    return if (event.type == EventType.BIRTHDAY && daysRemaining == 0L) "生日" else daysRemaining.toString()
}

private fun eventPrimaryUnitText(event: Event, daysRemaining: Long): String {
    return if (event.type == EventType.BIRTHDAY && daysRemaining == 0L) "快乐" else "天"
}

private fun birthdayGreetingText(name: String): String {
    val displayName = name.trim().takeIf { it.isNotBlank() }
    return if (displayName != null) "今天生日，愿 $displayName 生日快乐" else "今天生日，愿你生日快乐"
}

private fun birthdayWishLine(event: Event, daysRemaining: Long): String? {
    if (event.type != EventType.BIRTHDAY || daysRemaining != 0L) return null
    return "平安喜乐，日日有光"
}

private fun eventReminderMeta(event: Event): String {
    val parts = mutableListOf<String>()
    if (event.reminderEnabled) parts += "提前 ${event.reminderDays} 天提醒"
    if (event.note.isNotBlank()) parts += event.note
    return parts.joinToString(" · ")
}

private fun eventProgress(event: Event, daysRemaining: Long): Float {
    val now = System.currentTimeMillis()
    return when (event.type) {
        EventType.TODO -> when (event.todoStatus) {
            TodoStatus.COMPLETED -> 1f
            TodoStatus.CANCELLED -> 0.08f
            TodoStatus.PENDING -> event.dueDate?.let { due ->
                val start = event.createdAt.coerceAtMost(due)
                ((now - start).toFloat() / (due - start).coerceAtLeast(1L).toFloat()).coerceIn(0f, 1f)
            } ?: 0.35f
        }
        EventType.COUNTDOWN -> {
            val start = event.createdAt.coerceAtMost(event.date)
            ((now - start).toFloat() / (event.date - start).coerceAtLeast(1L).toFloat()).coerceIn(0f, 1f)
        }
        EventType.BIRTHDAY -> ((365L - daysRemaining.coerceIn(0L, 365L)).toFloat() / 365f).coerceIn(0f, 1f)
        EventType.ANNIVERSARY, EventType.ELAPSED -> ((daysRemaining % 365L).toFloat() / 365f).coerceIn(0.08f, 1f)
    }
}

@Composable
private fun EventCardHeroContent(event: Event, daysRemaining: Long, displayFields: Set<String>, color: Color, insetForRail: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(if (insetForRail) 18.dp else 16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            EventTypePill(event.type, color)
            Text(formatDate(event.date), style = MaterialTheme.typography.labelSmall, color = color.copy(alpha = 0.82f), maxLines = 1)
        }
        Column(modifier = Modifier.fillMaxWidth(0.84f)) {
            Text(text = event.name, style = MaterialTheme.typography.titleLarge, color = color, maxLines = 2)
            Text(text = eventPrimaryCountText(event, daysRemaining), style = MaterialTheme.typography.headlineMedium, color = color)
            birthdayWishLine(event, daysRemaining)?.let { wish ->
                Text(text = wish, style = MaterialTheme.typography.bodySmall, color = color.copy(alpha = 0.88f), maxLines = 1)
            }
            EventMetaLines(event, displayFields - "date", color)
        }
    }
}

@Composable
private fun EventCardPosterContent(event: Event, daysRemaining: Long, displayFields: Set<String>, color: Color) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            EventTypePill(event.type, color)
            Text(formatDate(event.date), style = MaterialTheme.typography.labelSmall, color = color.copy(alpha = 0.86f), maxLines = 1)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Text(text = eventPrimaryNumberText(event, daysRemaining), style = MaterialTheme.typography.displayMedium, color = color)
            Text(text = eventPrimaryUnitText(event, daysRemaining), style = MaterialTheme.typography.titleMedium, color = color.copy(alpha = 0.88f))
        }
        Column {
            Text(text = event.name, style = MaterialTheme.typography.titleLarge, color = color, maxLines = 2)
            birthdayWishLine(event, daysRemaining)?.let { wish ->
                Text(text = wish, style = MaterialTheme.typography.bodySmall, color = color.copy(alpha = 0.86f), maxLines = 1)
            }
            EventMetaLines(event, displayFields - "date", color)
        }
    }
}

@Composable
private fun BoxScope.EventCardGlassContent(event: Event, daysRemaining: Long, displayFields: Set<String>, color: Color) {
    Surface(
        modifier = Modifier
            .align(Alignment.BottomStart)
            .padding(12.dp)
            .fillMaxWidth(0.88f),
        color = Color.White.copy(alpha = 0.18f),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = event.name, style = MaterialTheme.typography.titleMedium, color = color, maxLines = 1)
            Text(text = eventPrimaryCountText(event, daysRemaining), style = MaterialTheme.typography.headlineSmall, color = color)
            birthdayWishLine(event, daysRemaining)?.let { wish ->
                Text(text = wish, style = MaterialTheme.typography.labelSmall, color = color.copy(alpha = 0.86f), maxLines = 1)
            }
            EventMetaLines(event, displayFields, color)
        }
    }
}

@Composable
private fun EventCardSplitContent(event: Event, daysRemaining: Long, displayFields: Set<String>, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Surface(color = Color.White.copy(alpha = 0.22f), shape = RoundedCornerShape(18.dp), modifier = Modifier.size(70.dp)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center, modifier = Modifier.fillMaxSize()) {
                Text(text = eventPrimaryNumberText(event, daysRemaining), style = MaterialTheme.typography.headlineMedium, color = color, maxLines = 1)
                Text(text = eventPrimaryUnitText(event, daysRemaining), style = MaterialTheme.typography.labelSmall, color = color.copy(alpha = 0.82f))
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            EventTypePill(event.type, color)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = event.name, style = MaterialTheme.typography.titleMedium, color = color, maxLines = 2)
            birthdayWishLine(event, daysRemaining)?.let { wish ->
                Text(text = wish, style = MaterialTheme.typography.labelSmall, color = color.copy(alpha = 0.86f), maxLines = 1)
            }
            EventMetaLines(event, displayFields, color)
        }
    }
}

@Composable
private fun BoxScope.EventCardMinimalContent(event: Event, daysRemaining: Long, displayFields: Set<String>, color: Color) {
    Column(modifier = Modifier.align(Alignment.CenterStart).padding(18.dp).fillMaxWidth(0.74f)) {
        Text(text = eventPrimaryCountText(event, daysRemaining), style = MaterialTheme.typography.titleLarge, color = color)
        Text(text = event.name, style = MaterialTheme.typography.titleMedium, color = color, maxLines = 2)
        birthdayWishLine(event, daysRemaining)?.let { wish ->
            Text(text = wish, style = MaterialTheme.typography.labelSmall, color = color.copy(alpha = 0.86f), maxLines = 1)
        }
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
    CardVisualStyle.HeroWide -> "柔和玻璃 · 封面"
    CardVisualStyle.PosterTall -> "柔和玻璃 · 海报"
    CardVisualStyle.GlassCompact -> "柔和玻璃 · 紧凑"
    CardVisualStyle.SplitPanel -> "柔和玻璃 · 分栏"
    CardVisualStyle.NeonRail -> "柔和玻璃 · 光轨"
    CardVisualStyle.MinimalBadge -> "柔和玻璃 · 徽章"
}

private fun cardOverlayBrush(style: CardVisualStyle): Brush = when (style) {
    CardVisualStyle.GlassCompact -> Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.05f), Color.Black.copy(alpha = 0.46f)))
    CardVisualStyle.SplitPanel -> Brush.horizontalGradient(listOf(Color.Black.copy(alpha = 0.54f), Color.Black.copy(alpha = 0.12f)))
    CardVisualStyle.NeonRail -> Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.16f), Color.Black.copy(alpha = 0.54f)))
    CardVisualStyle.MinimalBadge -> Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.10f), Color.Black.copy(alpha = 0.42f)))
    else -> Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.06f), Color.Black.copy(alpha = 0.52f)))
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
    val themeTokens = LocalMemoriaThemeTokens.current
    val context = LocalContext.current
    val settingsVersion = AppSettings.settingsVersion
    var currentMonthYear by remember { mutableStateOf("${Calendar.getInstance().get(Calendar.YEAR)}-${Calendar.getInstance().get(Calendar.MONTH) + 1}") }
    var showMonthPicker by remember { mutableStateOf(false) }
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
    val currentYear = cal.get(Calendar.YEAR)
    val currentMonth = cal.get(Calendar.MONTH) + 1
    val nearestEvent = remember(monthEvents) {
        monthEvents.minByOrNull { kotlin.math.abs(it.date - System.currentTimeMillis()) }
    }
    var selectedDay by remember { mutableStateOf<Pair<Long, List<Event>>?>(null) }
    var selectedDiaryForView by remember { mutableStateOf<DiaryEntry?>(null) }
    var editingDiary by remember { mutableStateOf<DiaryEntry?>(null) }
    var editingDiaryDate by remember { mutableStateOf<Long?>(null) }
    var monthSwipeOffset by remember { mutableFloatStateOf(0f) }
    val animatedMonthSwipeOffset by animateFloatAsState(monthSwipeOffset, label = "calendarMonthSwipe")

    val diaryMap = remember(diaries) {
        diaries.groupBy { startOfDayMillis(it.dateStart) }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer { translationX = animatedMonthSwipeOffset }
            .pointerInput(currentMonthYear) {
                var dragDistance = 0f
                detectHorizontalDragGestures(
                    onHorizontalDrag = { change, amount ->
                        dragDistance += amount
                        monthSwipeOffset = dragDistance.coerceIn(-120f, 120f)
                        if (abs(dragDistance) > 12f) change.consume()
                    },
                    onDragEnd = {
                        if (abs(dragDistance) >= 36f) {
                            val updated = cal.clone() as Calendar
                            updated.add(Calendar.MONTH, if (dragDistance < 0f) 1 else -1)
                            currentMonthYear = "${updated.get(Calendar.YEAR)}-${updated.get(Calendar.MONTH) + 1}"
                        }
                        dragDistance = 0f
                        monthSwipeOffset = 0f
                    },
                    onDragCancel = {
                        dragDistance = 0f
                        monthSwipeOffset = 0f
                    }
                )
            }
    ) {
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
            colors = CardDefaults.cardColors(containerColor = themeTokens.calendarCard)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.linearGradient(listOf(themeTokens.calendarSelected, themeTokens.calendarToday, themeTokens.anniversaryMarker)))
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    val updated = cal.clone() as Calendar
                    updated.add(Calendar.MONTH, -1)
                    currentMonthYear = "${updated.get(Calendar.YEAR)}-${updated.get(Calendar.MONTH) + 1}"
                }) { Icon(Icons.Default.ChevronLeft, contentDescription = "上月", tint = Color.White) }
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { showMonthPicker = true }) {
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
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = adaptiveUi.screenPadding),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(Modifier.weight(1f))
            TextButton(onClick = {
                val now = Calendar.getInstance()
                currentMonthYear = "${now.get(Calendar.YEAR)}-${now.get(Calendar.MONTH) + 1}"
                selectedDay = startOfDayMillis(now.timeInMillis) to events.filter { occursOnDay(it, now) }
            }) { Text("今天") }
        }
        CalendarHeatStrip(events = monthEvents, diaries = diaries, month = cal)
        selectedDay?.let { (date, dayEvents) ->
            SelectedDaySummaryCard(
                date = date,
                events = dayEvents,
                diaries = diaryMap[startOfDayMillis(date)] ?: emptyList(),
                onAddEvent = onAddEvent,
                onWriteDiary = { editingDiaryDate = date }
            )
        }
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val horizontalPadding = if (adaptiveUi.compact) 8.dp else adaptiveUi.screenPadding
            val widthCell = (maxWidth - horizontalPadding * 2) / 7
            val fontScale = LocalDensity.current.fontScale
            val cellSize = (widthCell * (1f + (fontScale - 1f).coerceAtLeast(0f) * 0.35f))
                .coerceIn(52.dp, 78.dp)
            CalendarGrid(
                currentMonth = cal,
                events = events,
                cellSize = cellSize,
                horizontalPadding = horizontalPadding,
                diaryMap = diaryMap,
                onDayClick = { dayCal, dayEvents -> selectedDay = dayCal.timeInMillis to dayEvents }
            )
        }
    }

    if (showMonthPicker) {
        MonthJumpDialog(
            initialMonth = cal.timeInMillis,
            onDismiss = { showMonthPicker = false },
            onConfirm = { target ->
                val targetCal = Calendar.getInstance().apply { timeInMillis = target }
                currentMonthYear = "${targetCal.get(Calendar.YEAR)}-${targetCal.get(Calendar.MONTH) + 1}"
                showMonthPicker = false
            }
        )
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
private fun MonthlyMediaFloatingButton(
    hasMedia: Boolean,
    mediaCount: Int,
    month: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val gradient = if (hasMedia) {
        Brush.linearGradient(listOf(Color(0xFFFFB86B), Color(0xFFFF6B9A), Color(0xFF7C5CFF)))
    } else {
        Brush.linearGradient(listOf(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.outline.copy(alpha = 0.54f)))
    }
    Box(
        modifier = modifier
            .size(64.dp)
            .shadow(10.dp, RoundedCornerShape(22.dp))
            .clip(RoundedCornerShape(22.dp))
            .background(gradient)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(Color.White.copy(alpha = if (hasMedia) 0.12f else 0.28f))
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(
                if (hasMedia) Icons.Default.Collections else Icons.Default.Inventory2,
                contentDescription = "${month}月素材",
                tint = if (hasMedia) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(25.dp)
            )
            Text(
                "${month}月",
                style = MaterialTheme.typography.labelSmall,
                color = if (hasMedia) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (hasMedia) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(5.dp),
                shape = RoundedCornerShape(999.dp),
                color = Color(0xFFFFF4C2),
                shadowElevation = 2.dp
            ) {
                Text(
                    text = mediaCount.coerceAtMost(99).toString(),
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF7A3B00),
                    maxLines = 1
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(9.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(MaterialTheme.colorScheme.outline)
            )
        }
    }
}

@Composable
private fun MonthlyMediaPreviewDialog(
    month: Int,
    images: List<String>,
    videos: List<String>,
    onDismiss: () -> Unit
) {
    val media = remember(images, videos) {
        images.map { MonthlyMediaItem(it, DiaryMediaType.IMAGE) } + videos.map { MonthlyMediaItem(it, DiaryMediaType.VIDEO) }
    }
    var selectedIndex by remember(media) { mutableIntStateOf(0) }
    var fullscreenVideo by remember { mutableStateOf<String?>(null) }
    val selected = media.getOrNull(selectedIndex)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("${month} 月图片 / 视频总结")
                Text("图片总结 ${images.size} 张 · 视频总结 ${videos.size} 个", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 280.dp, max = 420.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    when (selected?.type) {
                        DiaryMediaType.IMAGE -> AsyncImage(
                            model = selected.uri,
                            contentDescription = "${month}月照片",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.matchParentSize()
                        )
                        DiaryMediaType.VIDEO -> MonthlyVideoTile(
                            uri = selected.uri,
                            modifier = Modifier.matchParentSize(),
                            autoPlay = false,
                            onPlay = { fullscreenVideo = selected.uri }
                        )
                        null -> Text("本月暂无素材", color = Color.White)
                    }
                    if (media.size > 1) {
                        IconButton(
                            onClick = { selectedIndex = (selectedIndex - 1 + media.size) % media.size },
                            modifier = Modifier.align(Alignment.CenterStart).padding(6.dp)
                        ) { Icon(Icons.Default.ChevronLeft, contentDescription = "上一张", tint = Color.White) }
                        IconButton(
                            onClick = { selectedIndex = (selectedIndex + 1) % media.size },
                            modifier = Modifier.align(Alignment.CenterEnd).padding(6.dp)
                        ) { Icon(Icons.Default.ChevronRight, contentDescription = "下一张", tint = Color.White) }
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    media.forEachIndexed { index, item ->
                        MonthlyMediaThumb(
                            item = item,
                            selected = index == selectedIndex,
                            onClick = { selectedIndex = index }
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("关闭") } }
    )

    fullscreenVideo?.let { uri ->
        Dialog(onDismissRequest = { fullscreenVideo = null }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 360.dp, max = 620.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                MonthlyVideoTile(uri = uri, modifier = Modifier.matchParentSize(), autoPlay = true, onPlay = {})
                IconButton(
                    onClick = { fullscreenVideo = null },
                    modifier = Modifier.align(Alignment.TopEnd).padding(10.dp)
                ) { Icon(Icons.Default.Close, contentDescription = "关闭视频", tint = Color.White) }
            }
        }
    }
}

@Composable
private fun MonthlyVideoTile(uri: String, modifier: Modifier = Modifier, autoPlay: Boolean = false, onPlay: () -> Unit) {
    Box(
        modifier = modifier
            .background(Brush.verticalGradient(listOf(Color(0xFF111827), Color(0xFF312E81))))
            .pointerInput(uri) { detectTapGestures { onPlay() } },
        contentAlignment = Alignment.Center
    ) {
        if (autoPlay) {
            AndroidView(
                modifier = Modifier.matchParentSize(),
                factory = { context ->
                    VideoView(context).apply {
                        setVideoURI(Uri.parse(uri))
                        setMediaController(MediaController(context).also { it.setAnchorView(this) })
                        setOnPreparedListener { player ->
                            player.isLooping = false
                            start()
                        }
                    }
                },
                update = { view ->
                    if (!view.isPlaying) view.start()
                }
            )
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Default.PlayCircle, contentDescription = "播放视频", tint = Color.White, modifier = Modifier.size(58.dp))
                Text("点击播放短视频", style = MaterialTheme.typography.titleSmall, color = Color.White)
                Text(uri.substringAfterLast('/').take(32), style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f), maxLines = 1)
            }
        }
    }
}

@Composable
private fun MonthlyMediaThumb(item: MonthlyMediaItem, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(width = 72.dp, height = 56.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .then(if (selected) Modifier.border(BorderStroke(2.dp, MaterialTheme.colorScheme.primary), RoundedCornerShape(16.dp)) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        if (item.type == DiaryMediaType.IMAGE) {
            AsyncImage(model = item.uri, contentDescription = "素材缩略图", contentScale = ContentScale.Crop, modifier = Modifier.matchParentSize())
        } else {
            Icon(Icons.Default.Videocam, contentDescription = "视频素材", tint = MaterialTheme.colorScheme.primary)
        }
    }
}

private data class MonthlyMediaItem(val uri: String, val type: DiaryMediaType)

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

private enum class CalendarDisplayMode(val label: String) {
    MONTH("月"), WEEK("周"), AGENDA("议程")
}

@Composable
private fun CalendarHeatStrip(events: List<Event>, diaries: List<DiaryEntry>, month: Calendar) {
    val tokens = LocalMemoriaThemeTokens.current
    val daysInMonth = month.getActualMaximum(Calendar.DAY_OF_MONTH)
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        (1..daysInMonth).forEach { day ->
            val dayCal = (month.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, day) }
            val count = events.count { occursOnDay(it, dayCal) } + diaries.count { startOfDayMillis(it.dateStart) == startOfDayMillis(dayCal.timeInMillis) }
            val color = when {
                count >= 3 -> tokens.heatHigh
                count > 0 -> tokens.heatLow
                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
            }
            Box(modifier = Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(99.dp)).background(color))
        }
    }
}

@Composable
private fun AnniversaryStoryStrip(events: List<Event>, onEventClick: (Event) -> Unit) {
    val storyEvents = remember(events) {
        events.filter { it.type == EventType.ANNIVERSARY || it.type == EventType.BIRTHDAY }.take(3)
    }
    if (storyEvents.isEmpty()) return
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        storyEvents.forEach { event ->
            Surface(
                modifier = Modifier.width(220.dp).clickable { onEventClick(event) },
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.72f)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(if (event.type == EventType.BIRTHDAY) "生日故事" else "纪念日故事", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    Text(event.name, style = MaterialTheme.typography.titleSmall, maxLines = 1)
                    Text("${kotlin.math.abs(calculateDays(event))} 天 · 点开查看详情", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun SelectedDaySummaryCard(
    date: Long,
    events: List<Event>,
    diaries: List<DiaryEntry>,
    onAddEvent: (Long) -> Unit,
    onWriteDiary: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val holidayLabel = HolidayUtils.holidayForDay(date)
            if (holidayLabel != null) {
                Text(
                    text = "今日是$holidayLabel",
                    style = MaterialTheme.typography.titleMedium,
                    color = LocalMemoriaThemeTokens.current.festivalMarker
                )
            } else {
                Text(formatDate(date), style = MaterialTheme.typography.titleMedium)
            }
            val firstEvent = events.firstOrNull()
            if (firstEvent != null) {
                Text("${firstEvent.name} · ${eventTypeText(firstEvent.type)} · ${kotlin.math.abs(calculateDays(firstEvent))} 天", style = MaterialTheme.typography.bodyMedium)
            }
            if (diaries.isNotEmpty()) {
                Text("今日日记 ${diaries.size} 篇：${diaries.first().content.take(28)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (events.isEmpty() && diaries.isEmpty()) {
                Text("这一天还没有内容，可以新增日子或写日记。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { onAddEvent(date) }) { Text("新增日子") }
                TextButton(onClick = onWriteDiary) { Text("写日记") }
            }
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
                        val lunarDayLabel = LunarDateUtils.dayLabelForGregorian(dayCal.timeInMillis)
                        val holidayLabel = HolidayUtils.holidayForDay(dayCal.timeInMillis)
                        CalendarDayCell(
                            day = dayNumber,
                            lunarDayLabel = lunarDayLabel,
                            holidayLabel = holidayLabel,
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
private fun CalendarWeekView(
    events: List<Event>,
    diaryMap: Map<Long, List<DiaryEntry>>,
    cellSize: androidx.compose.ui.unit.Dp,
    horizontalPadding: androidx.compose.ui.unit.Dp,
    onDayClick: (Calendar, List<Event>) -> Unit
) {
    val weekStart = remember { Calendar.getInstance() }.apply {
        val diff = (get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY + 7) % 7
        add(Calendar.DAY_OF_MONTH, -diff)
    }
    Column(modifier = Modifier.padding(horizontal = horizontalPadding).verticalScroll(rememberScrollState())) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            (0 until 7).forEach { offset ->
                val dayCal = (weekStart.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, offset) }
                val dayEvents = events.filter { occursOnDay(it, dayCal) }
                val dayDiaries = diaryMap[startOfDayMillis(dayCal.timeInMillis)] ?: emptyList()
                CalendarDayCell(
                    day = dayCal.get(Calendar.DAY_OF_MONTH),
                    lunarDayLabel = LunarDateUtils.dayLabelForGregorian(dayCal.timeInMillis),
                    holidayLabel = HolidayUtils.holidayForDay(dayCal.timeInMillis),
                    isToday = startOfDayMillis(dayCal.timeInMillis) == startOfDayMillis(System.currentTimeMillis()),
                    events = dayEvents,
                    diaries = dayDiaries,
                    modifier = Modifier.weight(1f).height(cellSize),
                    onClick = { onDayClick(dayCal, dayEvents) }
                )
            }
        }
    }
}

@Composable
private fun CalendarAgendaView(
    events: List<Event>,
    diaries: List<DiaryEntry>,
    horizontalPadding: androidx.compose.ui.unit.Dp,
    onEventClick: (Event) -> Unit,
    onDiaryClick: (DiaryEntry) -> Unit
) {
    val agendaEvents = remember(events) { events.sortedBy { nextEventDistanceMillis(it) }.take(80) }
    Column(modifier = Modifier.padding(horizontal = horizontalPadding).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (agendaEvents.isEmpty() && diaries.isEmpty()) {
            Text("暂无议程，点底部中间颜文字记录一个日子。", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        agendaEvents.forEach { event ->
            Surface(modifier = Modifier.fillMaxWidth().clickable { onEventClick(event) }, shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(if (event.type == EventType.TODO) Icons.Default.CheckCircle else Icons.Default.Event, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(event.name, style = MaterialTheme.typography.titleSmall, maxLines = 1)
                        Text("${eventTypeText(event.type)} · ${formatDate(event.date)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        diaries.take(20).forEach { diary ->
            Surface(modifier = Modifier.fillMaxWidth().clickable { onDiaryClick(diary) }, shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.38f)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("日记 · ${formatDate(diary.dateStart)}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Text(diary.content.ifBlank { "无文字内容" }, style = MaterialTheme.typography.bodySmall, maxLines = 2, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun MonthJumpDialog(initialMonth: Long, onDismiss: () -> Unit, onConfirm: (Long) -> Unit) {
    val initial = remember(initialMonth) { Calendar.getInstance().apply { timeInMillis = initialMonth } }
    var yearText by remember(initialMonth) { mutableStateOf(initial.get(Calendar.YEAR).toString()) }
    var monthText by remember(initialMonth) { mutableStateOf((initial.get(Calendar.MONTH) + 1).toString()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("跳转月份") },
        text = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = yearText, onValueChange = { yearText = it.filter(Char::isDigit).take(4) }, label = { Text("年份") }, modifier = Modifier.weight(1f))
                OutlinedTextField(value = monthText, onValueChange = { monthText = it.filter(Char::isDigit).take(2) }, label = { Text("月份") }, modifier = Modifier.weight(1f))
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val year = yearText.toIntOrNull()?.coerceIn(1900, 2100) ?: initial.get(Calendar.YEAR)
                val month = monthText.toIntOrNull()?.coerceIn(1, 12) ?: initial.get(Calendar.MONTH) + 1
                onConfirm(Calendar.getInstance().apply { set(year, month - 1, 1, 0, 0, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis)
            }) { Text("跳转") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
fun CalendarDayCell(
    day: Int,
    lunarDayLabel: String,
    holidayLabel: String? = null,
    isToday: Boolean,
    events: List<Event>,
    diaries: List<DiaryEntry> = emptyList(),
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    val tokens = LocalMemoriaThemeTokens.current
    val hasTodo = events.any { it.type == EventType.TODO }
    val hasBirthday = events.any { it.type == EventType.BIRTHDAY }
    val hasAnniversary = events.any { it.type == EventType.ANNIVERSARY || it.type == EventType.ELAPSED || it.type == EventType.COUNTDOWN }
    Box(
        modifier = modifier
            .padding(2.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .background(
                if (isToday) Brush.linearGradient(listOf(tokens.calendarToday.copy(alpha = 0.24f), tokens.calendarSelected.copy(alpha = 0.16f)))
                else Brush.linearGradient(listOf(tokens.calendarCard, MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.52f)))
            )
            .padding(horizontal = 2.dp, vertical = 4.dp),
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(top = 1.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            Text(
                text = day.toString(),
                style = MaterialTheme.typography.bodyMedium,
                color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                textAlign = TextAlign.Center
            )
            Text(
                text = holidayLabel ?: lunarDayLabel,
                style = MaterialTheme.typography.labelSmall,
                color = if (holidayLabel != null) {
                    tokens.festivalMarker
                } else if (isToday) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.78f)
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                maxLines = 1,
                textAlign = TextAlign.Center
            )
        }
        if (events.isNotEmpty() || diaries.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (diaries.isNotEmpty()) {
                    CalendarMarker(color = tokens.diaryMarker, wide = diaries.size > 1)
                }
                if (hasAnniversary) {
                    CalendarMarker(color = tokens.anniversaryMarker, wide = events.size > 1)
                }
                if (hasBirthday) {
                    CalendarMarker(color = tokens.festivalMarker)
                }
                if (hasTodo) {
                    CalendarMarker(color = tokens.todoMarker)
                }
            }
        }
    }
}

@Composable
private fun CalendarMarker(color: Color, wide: Boolean = false) {
    Box(
        modifier = Modifier
            .height(7.dp)
            .width(if (wide) 16.dp else 9.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(color)
    )
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
    onFilterChange: (String) -> Unit
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
            com.memoriabox.utils.AnnualDateUtils.daysUntil(dateMillis, now)
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

private fun nextEventDistanceMillis(event: Event): Long {
    val today = startOfDayMillis(System.currentTimeMillis())
    val target = startOfDayMillis(event.date)
    return if (target >= today) target - today else Long.MAX_VALUE / 2 + (today - target)
}

private fun occursOnDay(event: Event, dayCal: Calendar): Boolean {
    val eventCal = Calendar.getInstance().apply { timeInMillis = event.date }
    val lunarMonthDay = if (event.type == EventType.BIRTHDAY && !event.lunar.isNullOrBlank()) {
        LunarDateUtils.parseMonthDay(event.lunar)
    } else {
        null
    }
    if (lunarMonthDay != null) {
        return LunarDateUtils.isGregorianMatchingLunar(dayCal.timeInMillis, lunarMonthDay.first, lunarMonthDay.second)
    }
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

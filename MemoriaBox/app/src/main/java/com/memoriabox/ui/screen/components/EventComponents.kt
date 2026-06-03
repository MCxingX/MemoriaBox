package com.memoriabox.ui.screen.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.input.pointer.pointerInput
import coil.compose.AsyncImage
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.memoriabox.data.model.*
import com.memoriabox.utils.ColorUtils
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

@Composable
fun LayoutModeSelector(
    currentMode: CardLayoutMode,
    onModeChange: (CardLayoutMode) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        listOf(
            CardLayoutMode.SINGLE_COLUMN to "单列",
            CardLayoutMode.GRID_2X4 to "双列",
            CardLayoutMode.GRID_3X3 to "海报"
        ).forEach { (mode, label) ->
            FilterChip(
                selected = currentMode == mode,
                onClick = { onModeChange(mode) },
                label = { Text(label) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun EnhancedEventGrid(
    events: List<Event>,
    layoutMode: CardLayoutMode,
    onEventClick: (Event) -> Unit,
    onEventEdit: (Event) -> Unit,
    boxBgType: BgType = BgType.COLOR,
    boxBgValue: String = "#F5F5F5"
) {
    val columns = when (layoutMode) {
        CardLayoutMode.SINGLE_COLUMN -> 1
        CardLayoutMode.GRID_2X4 -> 2
        CardLayoutMode.GRID_3X3 -> 3
        CardLayoutMode.FLOW -> 2
    }

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
                        onLongPress = { onEventEdit(event) }
                    )
                }
            }
        }
    }
}

@Composable
fun EnhancedEventCard(event: Event, onClick: () -> Unit, onLongPress: () -> Unit) {
    val daysRemaining = calculateDays(event.date, event.type)
    val styleOptions = remember { listOf(CardVisualStyle.HeroWide, CardVisualStyle.PosterTall, CardVisualStyle.GlassCompact, CardVisualStyle.SplitPanel) }
    val initialStyle = when (event.cardTemplate) {
        "POSTER" -> CardVisualStyle.PosterTall
        "GLASS" -> CardVisualStyle.GlassCompact
        "SPLIT" -> CardVisualStyle.SplitPanel
        else -> CardVisualStyle.HeroWide
    }
    var styleIndex by remember(event.id, event.cardTemplate) { mutableIntStateOf(styleOptions.indexOf(initialStyle).coerceAtLeast(0)) }
    var isDragging by remember { mutableStateOf(false) }
    val style = styleOptions[styleIndex]
    val hasImage = event.avatarUri != null
    val displayFields = event.displayFields.split(",").map { it.trim() }.toSet()
    val eventTextColor = ColorUtils.hexToColor(event.textColor)
    val cardHeight = when (style) {
        CardVisualStyle.HeroWide -> 172.dp
        CardVisualStyle.PosterTall -> 236.dp
        CardVisualStyle.GlassCompact -> 138.dp
        CardVisualStyle.SplitPanel -> 196.dp
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(cardHeight)
            .shadow(elevation = if (isDragging) 12.dp else 4.dp, shape = RoundedCornerShape(22.dp))
            .pointerInput(event.id) {
                detectDragGestures(
                    onDragStart = { isDragging = true },
                    onDragCancel = { isDragging = false },
                    onDragEnd = {
                        isDragging = false
                        styleIndex = (styleIndex + 1) % styleOptions.size
                    },
                    onDrag = { change, _ -> change.consume() }
                )
            }
            .combinedClickable(onClick = onClick, onLongClick = onLongPress),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
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
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Black.copy(alpha = 0.06f), Color.Black.copy(alpha = 0.58f))
                        )
                    )
            )

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
                        "松手应用新排版",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
            ) {
                Surface(
                    color = Color.White.copy(alpha = 0.22f),
                    shape = RoundedCornerShape(999.dp)
                ) {
                    Text(
                        text = eventTypeText(event.type),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        color = eventTextColor,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = event.name, style = MaterialTheme.typography.titleLarge, color = eventTextColor, maxLines = 2)
                Text(
                    text = "${daysRemaining} 天",
                    style = if (style == CardVisualStyle.PosterTall) MaterialTheme.typography.displaySmall else MaterialTheme.typography.headlineMedium,
                    color = eventTextColor
                )
                if ("date" in displayFields) {
                    Text(text = formatDate(event.date), style = MaterialTheme.typography.bodySmall, color = eventTextColor.copy(alpha = 0.86f))
                }
                if ("lunar" in displayFields && event.lunar != null) {
                    Text(text = event.lunar, style = MaterialTheme.typography.labelSmall, color = eventTextColor.copy(alpha = 0.82f), maxLines = 1)
                }
                if ("note" in displayFields && event.note.isNotBlank()) {
                    Text(text = event.note, style = MaterialTheme.typography.labelSmall, color = eventTextColor.copy(alpha = 0.82f), maxLines = 1)
                }
                if ("reminder" in displayFields && event.reminderEnabled) {
                    Text(text = "提前 ${event.reminderDays} 天提醒", style = MaterialTheme.typography.labelSmall, color = eventTextColor.copy(alpha = 0.82f), maxLines = 1)
                }
            }
        }
    }
}

private enum class CardVisualStyle { HeroWide, PosterTall, GlassCompact, SplitPanel }

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
    modifier: Modifier = Modifier
) {
    var currentMonthYear by remember { mutableStateOf("${Calendar.getInstance().get(Calendar.YEAR)}-${Calendar.getInstance().get(Calendar.MONTH) + 1}") }
    val monthFormat = SimpleDateFormat("yyyy年MM月", Locale.getDefault())
    val cal = remember(currentMonthYear) {
        Calendar.getInstance().apply {
            val parts = currentMonthYear.split("-").map { it.toInt() }
            set(parts[0], parts[1] - 1, 1)
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(title = { Text("日历视图") })
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
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
                    Text("把重要的小日子圈起来", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.82f))
                }
                IconButton(onClick = {
                    val updated = cal.clone() as Calendar
                    updated.add(Calendar.MONTH, 1)
                    currentMonthYear = "${updated.get(Calendar.YEAR)}-${updated.get(Calendar.MONTH) + 1}"
                }) { Icon(Icons.Default.ChevronRight, contentDescription = "下月", tint = Color.White) }
            }
        }
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val horizontalPadding = 12.dp
            val widthCell = (maxWidth - horizontalPadding * 2) / 7
            val heightCell = ((maxHeight - 36.dp).coerceAtLeast(240.dp)) / 6
            val cellSize = minOf(widthCell, heightCell).coerceAtLeast(36.dp)
            CalendarGrid(currentMonth = cal, events = events, cellSize = cellSize, horizontalPadding = horizontalPadding)
        }
    }
}

@Composable
fun CalendarGrid(currentMonth: Calendar, events: List<Event>, cellSize: androidx.compose.ui.unit.Dp, horizontalPadding: androidx.compose.ui.unit.Dp) {
    val daysOfWeek = listOf("日", "一", "二", "三", "四", "五", "六")
    val calendar = currentMonth.clone() as Calendar
    calendar.set(Calendar.DAY_OF_MONTH, 1)
    val firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
    val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    val today = Calendar.getInstance()
    val todayDay = today.get(Calendar.DAY_OF_MONTH)
    val todayMonth = today.get(Calendar.MONTH)
    val todayYear = today.get(Calendar.YEAR)

    Column(modifier = Modifier.padding(horizontal = horizontalPadding)) {
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            daysOfWeek.forEach { day ->
                Surface(color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.56f), shape = MaterialTheme.shapes.large, modifier = Modifier.weight(1f).padding(horizontal = 2.dp)) {
                    Text(text = day, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(vertical = 5.dp), textAlign = TextAlign.Center)
                }
            }
        }

        val daysToFirst = (firstDayOfWeek - Calendar.SUNDAY + 7) % 7
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
                        val isToday = dayNumber == todayDay && currentMonth.get(Calendar.MONTH) == todayMonth && currentMonth.get(Calendar.YEAR) == todayYear

                        val dayEvents = events.filter { event -> occursOnDay(event, dayCal) }
                        CalendarDayCell(day = dayNumber, isToday = isToday, events = dayEvents, modifier = Modifier.weight(1f).height(cellSize))
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
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .padding(2.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(
                if (isToday) Brush.linearGradient(listOf(Color(0xFFFF6B6B).copy(alpha = 0.22f), Color(0xFF7C5CFF).copy(alpha = 0.18f)))
                else Brush.linearGradient(listOf(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.52f)))
            )
            .padding(horizontal = 2.dp, vertical = 3.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = day.toString(),
            style = MaterialTheme.typography.bodyMedium,
            color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
        if (events.isNotEmpty()) {
            Spacer(modifier = Modifier.height(2.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                repeat(minOf(events.size, 3)) {
                    val dotColor = listOf(Color(0xFFFF6B6B), Color(0xFF7C5CFF), Color(0xFF00B8D9))[it]
                    Box(modifier = Modifier.size(5.dp).clip(RoundedCornerShape(3.dp)).background(dotColor))
                }
            }
        }
    }
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

fun calculateDays(dateMillis: Long, type: EventType): Long {
    val now = System.currentTimeMillis()
    return when (type) {
        EventType.COUNTDOWN -> TimeUnit.MILLISECONDS.toDays(dateMillis - now)
        EventType.ANNIVERSARY -> TimeUnit.MILLISECONDS.toDays(now - dateMillis)
        EventType.ELAPSED -> TimeUnit.MILLISECONDS.toDays(now - dateMillis)
        EventType.BIRTHDAY -> {
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

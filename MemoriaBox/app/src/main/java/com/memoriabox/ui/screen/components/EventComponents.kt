package com.memoriabox.ui.screen.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
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
            CardLayoutMode.SINGLE_COLUMN to "1×1",
            CardLayoutMode.GRID_2X4 to "2×4",
            CardLayoutMode.GRID_3X3 to "3×3"
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
                columns = GridCells.Fixed(columns),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(events) { event -> EnhancedEventCard(event = event, onClick = { onEventClick(event) }, onLongPress = { onEventEdit(event) }) }
            }
        }
    }
}

@Composable
fun EnhancedEventCard(event: Event, onClick: () -> Unit, onLongPress: () -> Unit) {
    val daysRemaining = calculateDays(event.date, event.type)
    Card(
        modifier = Modifier.fillMaxWidth().shadow(elevation = 4.dp, shape = RoundedCornerShape(12.dp)).combinedClickable(onClick = onClick, onLongClick = onLongPress),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFFFF))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                Text(text = event.name, style = MaterialTheme.typography.titleLarge)
                IconButton(onClick = { }, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.MoreVert, contentDescription = null, tint = Color.Gray)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = when (event.type) {
                    EventType.COUNTDOWN -> "还剩"
                    EventType.ANNIVERSARY -> "已过"
                    EventType.ELAPSED -> "已过"
                    EventType.BIRTHDAY -> "生日"
                    EventType.TODO -> "待办"
                },
                style = MaterialTheme.typography.labelMedium,
                color = Color.Gray
            )
            Text(
                text = "${daysRemaining}天",
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.primary
            )
            Text(text = formatDate(event.date), style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
            if (event.lunar != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = event.lunar, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
            if (event.note.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = event.note, style = MaterialTheme.typography.bodySmall, color = Color.Gray, maxLines = 2)
            }
        }
    }
}

@Composable
fun CalendarViewScreen(
    events: List<Event>,
    modifier: Modifier = Modifier
) {
    var currentMonthYear by remember { mutableStateOf("${Calendar.getInstance().get(Calendar.YEAR)}-${Calendar.getInstance().get(Calendar.MONTH) + 1}") }
    val monthFormat = SimpleDateFormat("yyyy年MM月", Locale.getDefault())

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("日历视图") },
            actions = {
                val cal = Calendar.getInstance()
                val parts = currentMonthYear.split("-").map { it.toInt() }
                cal.set(parts[0], parts[1] - 1, 1)
                IconButton(onClick = {
                    cal.add(Calendar.MONTH, -1)
                    currentMonthYear = "${cal.get(Calendar.YEAR)}-${cal.get(Calendar.MONTH) + 1}"
                }) { Icon(Icons.Default.ChevronLeft, contentDescription = "上月") }
                Text(text = monthFormat.format(cal.time), style = MaterialTheme.typography.titleLarge)
                IconButton(onClick = {
                    cal.add(Calendar.MONTH, 1)
                    currentMonthYear = "${cal.get(Calendar.YEAR)}-${cal.get(Calendar.MONTH) + 1}"
                }) { Icon(Icons.Default.ChevronRight, contentDescription = "下月") }
            }
        )

        val parts = currentMonthYear.split("-").map { it.toInt() }
        val calendar = Calendar.getInstance()
        calendar.set(parts[0], parts[1] - 1, 1)
        CalendarGrid(currentMonth = calendar, events = events)
    }
}

@Composable
fun CalendarGrid(currentMonth: Calendar, events: List<Event>) {
    val daysOfWeek = listOf("日", "一", "二", "三", "四", "五", "六")
    val calendar = currentMonth.clone() as Calendar
    calendar.set(Calendar.DAY_OF_MONTH, 1)
    val firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
    val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    val today = Calendar.getInstance()
    val todayDay = today.get(Calendar.DAY_OF_MONTH)
    val todayMonth = today.get(Calendar.MONTH)
    val todayYear = today.get(Calendar.YEAR)

    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            daysOfWeek.forEach { day ->
                Text(text = day, style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
            }
        }

        val daysToFirst = (firstDayOfWeek - Calendar.SUNDAY + 7) % 7
        val totalCells = daysToFirst + daysInMonth
        val rows = (totalCells + 6) / 7

        for (row in 0 until rows) {
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                for (col in 0 until 7) {
                    val cellIndex = row * 7 + col
                    val dayNumber = cellIndex - daysToFirst + 1
                    if (dayNumber in 1..daysInMonth) {
                        val dayCal = calendar.clone() as Calendar
                        dayCal.set(Calendar.DAY_OF_MONTH, dayNumber)
                        val isToday = dayNumber == todayDay && currentMonth.get(Calendar.MONTH) == todayMonth && currentMonth.get(Calendar.YEAR) == todayYear

                        val dayEvents = events.filter { event ->
                            val eventCal = Calendar.getInstance().apply { timeInMillis = event.date }
                            eventCal.get(Calendar.YEAR) == dayCal.get(Calendar.YEAR) &&
                            eventCal.get(Calendar.MONTH) == dayCal.get(Calendar.MONTH) &&
                            eventCal.get(Calendar.DAY_OF_MONTH) == dayCal.get(Calendar.DAY_OF_MONTH)
                        }
                        CalendarDayCell(day = dayNumber, isToday = isToday, events = dayEvents, modifier = Modifier.weight(1f))
                    } else {
                        Box(modifier = Modifier.weight(1f))
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
        modifier = modifier.aspectRatio(1f).clip(RoundedCornerShape(8.dp))
            .then(if (isToday) Modifier.background(Color(0xFF7C4DFF).copy(0.2f)) else Modifier)
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = day.toString(),
            style = MaterialTheme.typography.bodyMedium,
            color = if (isToday) Color(0xFF7C4DFF) else Color.Black
        )
        if (events.isNotEmpty()) {
            Spacer(modifier = Modifier.height(2.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                repeat(minOf(events.size, 3)) {
                    Box(modifier = Modifier.size(4.dp).clip(RoundedCornerShape(2.dp)).background(Color(0xFF7C4DFF)))
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

package com.memoriabox.ui.screen

import android.app.Application
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.memoriabox.data.model.Event
import com.memoriabox.ui.screen.components.formatDate
import com.memoriabox.viewmodel.createCalendarViewModel
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.foundation.clickable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimelineScreen(application: Application) {
    val vm = remember { createCalendarViewModel(application) }
    val events by vm.allEvents.collectAsState(initial = emptyList())

    var selectedYear by remember { mutableStateOf<Int?>(null) }
    var selectedMonth by remember { mutableStateOf<Int?>(null) }

    val groupedEvents = remember(events, selectedYear, selectedMonth) {
        val filtered = when {
            selectedYear != null && selectedMonth != null -> {
                events.filter { event ->
                    val cal = Calendar.getInstance().apply { timeInMillis = event.date }
                    cal.get(Calendar.YEAR) == selectedYear && 
                    cal.get(Calendar.MONTH) == selectedMonth
                }
            }
            selectedYear != null -> {
                events.filter { event ->
                    Calendar.getInstance().apply { timeInMillis = event.date }
                        .get(Calendar.YEAR) == selectedYear
                }
            }
            else -> {
                events.sortedBy { it.date }
            }
        }
        
        filtered.groupBy { event ->
            val cal = Calendar.getInstance().apply { timeInMillis = event.date }
            "${cal.get(Calendar.YEAR)}年${cal.get(Calendar.MONTH) + 1}月"
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("时间轴") },
                actions = {
                    IconButton(onClick = { 
                        selectedYear = null
                        selectedMonth = null
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "重置")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            groupedEvents.forEach { (period, eventsInPeriod) ->
                item {
                    TimelineSection(
                        period = period,
                        events = eventsInPeriod,
                        selectedYear = selectedYear,
                        selectedMonth = selectedMonth,
                        onYearSelect = { selectedYear = it },
                        onMonthSelect = { selectedMonth = it }
                    )
                }
            }
        }
    }
}

@Composable
fun TimelineSection(
    period: String,
    events: List<Event>,
    selectedYear: Int?,
    selectedMonth: Int?,
    onYearSelect: (Int) -> Unit,
    onMonthSelect: (Int) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Period header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                period,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                "${events.size} 个事件",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.height(12.dp))

        // Timeline events
        events.sortedByDescending { it.date }.forEachIndexed { index, event ->
            TimelineEventItem(
                event = event,
                isLast = index == events.size - 1
            )
        }
    }
}

@Composable
fun TimelineEventItem(
    event: Event,
    isLast: Boolean
) {
    val lineColor = MaterialTheme.colorScheme.outlineVariant
    
    Row(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Timeline line
        Box(
            modifier = Modifier
                .width(32.dp)
                .fillMaxHeight(),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier
                    .width(2.dp)
                    .fillMaxHeight()
            ) {
                drawLine(
                    color = lineColor,
                    start = Offset(size.width / 2, 0f),
                    end = Offset(size.width / 2, size.height),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }

        Spacer(Modifier.width(8.dp))

        // Event card
        Card(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = if (isLast) 0.dp else 16.dp),
            colors = CardDefaults.cardColors(
                containerColor = when (event.type) {
                    com.memoriabox.data.model.EventType.BIRTHDAY -> MaterialTheme.colorScheme.secondaryContainer
                    com.memoriabox.data.model.EventType.TODO -> MaterialTheme.colorScheme.tertiaryContainer
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        event.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        com.memoriabox.ui.screen.components.formatDate(event.date),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(Modifier.height(8.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = when (event.type) {
                            com.memoriabox.data.model.EventType.BIRTHDAY -> Icons.Default.Cake
                            com.memoriabox.data.model.EventType.TODO -> Icons.Default.CheckCircle
                            else -> Icons.Default.Event
                        },
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        getEventTypeLabel(event.type),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (event.note.isNotEmpty()) {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            event.note,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

fun getEventTypeLabel(type: com.memoriabox.data.model.EventType): String = when (type) {
    com.memoriabox.data.model.EventType.COUNTDOWN -> "倒数日"
    com.memoriabox.data.model.EventType.ANNIVERSARY -> "纪念日"
    com.memoriabox.data.model.EventType.ELAPSED -> "正计时"
    com.memoriabox.data.model.EventType.BIRTHDAY -> "生日"
    com.memoriabox.data.model.EventType.TODO -> "待办"
}

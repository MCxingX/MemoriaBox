package com.memoriabox.ui.screen

import android.app.Application
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.memoriabox.data.model.Event
import com.memoriabox.data.model.EventType
import com.memoriabox.viewmodel.createCalendarViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun StatisticsScreen(application: Application) {
    val vm = remember { createCalendarViewModel(application) }
    val events by vm.allEvents.collectAsState(initial = emptyList())

    val stats = remember(events) { calculateStatistics(events) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("数据统计") }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Summary cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                StatCard(
                    title = "总事件数",
                    value = stats.totalEvents.toString(),
                    icon = Icons.Default.Event,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "分类数",
                    value = stats.totalBoxes.toString(),
                    icon = Icons.Default.Folder,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(24.dp))

            // Event type distribution
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "事件类型分布",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(16.dp))
                    
                    // Pie chart
                    Box(
                        modifier = Modifier
                            .size(200.dp)
                            .align(Alignment.CenterHorizontally),
                        contentAlignment = Alignment.Center
                    ) {
                        EventTypePieChart(stats = stats.typeDistribution)
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                stats.totalEvents.toString(),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text("总计", style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Legend
                    stats.typeDistribution.forEach { (type, count) ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .padding(2.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Canvas(modifier = Modifier.size(12.dp)) {
                                        drawCircle(
                                            color = getTypeColor(type),
                                            radius = size.minDimension / 2
                                        )
                                    }
                                }
                                Spacer(Modifier.width(8.dp))
                                Text(getTypeName(type))
                            }
                            Text(
                                "$count (${(count.toFloat() / stats.totalEvents.coerceAtLeast(1) * 100).toInt()}%)",
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Upcoming events
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "即将到来的事件",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(12.dp))

                    val upcoming = events
                        .filter { it.date > System.currentTimeMillis() }
                        .sortedBy { it.date }
                        .take(5)

                    if (upcoming.isEmpty()) {
                        Text("暂无即将到来的事件", style = MaterialTheme.typography.bodyMedium)
                    } else {
                        upcoming.forEach { event ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(event.name, fontWeight = FontWeight.Bold)
                                    Text(
                                        SimpleDateFormat("MM-dd", Locale.getDefault()).format(Date(event.date)),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                Text(
                                    "${calculateDays(event.date, event.type)}天",
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            if (event != upcoming.last()) {
                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Monthly trend
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "近 6 个月趋势",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(16.dp))
                    
                    // Simple bar chart
                    val monthlyData = getMonthlyData(events)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        monthlyData.forEach { (month, count) ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            ) {
                                Text(count.toString(), style = MaterialTheme.typography.bodySmall)
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(100.dp),
                                    contentAlignment = Alignment.BottomCenter
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height((count.toFloat() / monthlyData.maxOf { it.second }.coerceAtLeast(1) * 100).dp)
                                            .background(MaterialTheme.colorScheme.primary)
                                    )
                                }
                                Text(month, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(title, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(4.dp))
                Text(
                    value,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Icon(
                icon,
                null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun EventTypePieChart(stats: Map<EventType, Int>) {
    val total = stats.values.sum().coerceAtLeast(1)
    var startAngle = 0f
    
    val slices = stats.map { (type, count) ->
        val sweep = (count.toFloat() / total * 360)
        val slice = Triple(startAngle, sweep, getTypeColor(type))
        startAngle += sweep
        slice
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val diameter = size.minDimension
        slices.forEach { (startAngle, sweep, color) ->
            drawArc(
                color = color,
                startAngle = startAngle,
                sweepAngle = sweep,
                useCenter = true,
                size = Size(diameter, diameter)
            )
        }
    }
}

data class StatisticsData(
    val totalEvents: Int,
    val totalBoxes: Int,
    val typeDistribution: Map<EventType, Int>,
    val monthlyData: List<Pair<String, Int>>
)

fun calculateStatistics(events: List<Event>): StatisticsData {
    val typeDist = EventType.values().associateWith { type ->
        events.count { it.type == type }
    }
    
    return StatisticsData(
        totalEvents = events.size,
        totalBoxes = events.map { it.boxId }.distinct().size,
        typeDistribution = typeDist,
        monthlyData = getMonthlyData(events)
    )
}

fun getMonthlyData(events: List<Event>): List<Pair<String, Int>> {
    val calendar = Calendar.getInstance()
    return (0 until 6).map { monthsAgo ->
        calendar.add(Calendar.MONTH, -monthsAgo)
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val monthName = SimpleDateFormat("MM", Locale.getDefault()).format(calendar.time)
        val count = events.count { event ->
            val eventCal = Calendar.getInstance().apply { timeInMillis = event.date }
            eventCal.get(Calendar.YEAR) == year && eventCal.get(Calendar.MONTH) == month
        }
        calendar.add(Calendar.MONTH, monthsAgo) // Reset
        monthName to count
    }.reversed()
}

fun getTypeName(type: EventType): String = when (type) {
    EventType.COUNTDOWN -> "倒数日"
    EventType.ANNIVERSARY -> "纪念日"
    EventType.ELAPSED -> "正计时"
    EventType.BIRTHDAY -> "生日"
    EventType.TODO -> "待办"
}

fun getTypeColor(type: EventType): Color = when (type) {
    EventType.COUNTDOWN -> Color(0xFFFF6B6B)
    EventType.ANNIVERSARY -> Color(0xFFFFA07A)
    EventType.ELAPSED -> Color(0xFF98FB98)
    EventType.BIRTHDAY -> Color(0xFFFFD700)
    EventType.TODO -> Color(0xFF87CEEB)
}

fun calculateDays(dateMillis: Long, type: EventType): Long {
    val now = System.currentTimeMillis()
    return when (type) {
        EventType.COUNTDOWN -> (dateMillis - now) / (1000 * 60 * 60 * 24)
        EventType.ANNIVERSARY -> (now - dateMillis) / (1000 * 60 * 60 * 24)
        EventType.ELAPSED -> (now - dateMillis) / (1000 * 60 * 60 * 24)
        EventType.BIRTHDAY -> {
            val eventCal = Calendar.getInstance().apply { timeInMillis = dateMillis }
            val nowCal = Calendar.getInstance()
            nowCal.set(Calendar.MONTH, eventCal.get(Calendar.MONTH))
            nowCal.set(Calendar.DAY_OF_MONTH, eventCal.get(Calendar.DAY_OF_MONTH))
            if (nowCal.before(Calendar.getInstance())) {
                nowCal.add(Calendar.YEAR, 1)
            }
            (nowCal.timeInMillis - now) / (1000 * 60 * 60 * 24)
        }
        EventType.TODO -> 0
    }
}

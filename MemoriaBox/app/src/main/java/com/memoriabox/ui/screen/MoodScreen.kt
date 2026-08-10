package com.memoriabox.ui.screen

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.memoriabox.data.model.MoodEntry
import com.memoriabox.viewmodel.createMoodViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

val moodEmojis = listOf("😞", "😕", "😐", "🙂", "😄")
val moodLabels = listOf("很差", "低落", "一般", "不错", "超棒")
val moodColors = listOf(
    Color(0xFF90A4AE),
    Color(0xFF64B5F6),
    Color(0xFFFFCA28),
    Color(0xFFFFA726),
    Color(0xFFEC407A)
)

@Composable
fun MoodScreen(
    application: Application,
    onNavigateBack: () -> Unit
) {
    val viewModel = remember { createMoodViewModel(application) }
    val moods by viewModel.moods.collectAsState(initial = emptyList())
    val moodByDate = remember(moods) {
        moods.associateBy { startOfDay(it.date) }
    }
    var editingDate by remember { mutableStateOf<Long?>(null) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("心情打卡") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { MoodPixelMap(moods = moods) }
            item { MoodMonthTrend(moods = moods) }
            item {
                Text("本月打卡", style = MaterialTheme.typography.titleLarge)
                Text("点击日期记录或修改心情", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            val monthEntries = moodsForCurrentMonth(moods)
            if (monthEntries.isEmpty()) {
                item { MoodEmptyCard() }
            } else {
                items(monthEntries.sortedByDescending { it.date }) { mood ->
                    MoodEntryRow(
                        mood = mood,
                        onClick = { editingDate = startOfDay(mood.date) }
                    )
                }
            }
        }
    }

    editingDate?.let { date ->
        MoodEditDialog(
            date = date,
            existing = moodByDate[date],
            onDismiss = { editingDate = null },
            onSave = { level, activity, note ->
                viewModel.upsertMood(date, level, activity, note)
                editingDate = null
            },
            onDelete = {
                viewModel.deleteMood(date)
                editingDate = null
            }
        )
    }
}

@Composable
private fun MoodPixelMap(moods: List<MoodEntry>) {
    val year = Calendar.getInstance().get(Calendar.YEAR)
    val moodMap = remember(moods) {
        moods.associateBy { entry ->
            val cal = Calendar.getInstance().apply { timeInMillis = entry.date }
            val dayOfYear = cal.get(Calendar.DAY_OF_YEAR)
            "${cal.get(Calendar.YEAR)}-$dayOfYear"
        }
    }
    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("年度像素图 · $year", style = MaterialTheme.typography.titleMedium)
            val daysInYear = if (com.memoriabox.utils.NextFeaturesLogic.isLeapYear(year)) 366 else 365
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                var dayOfYear = 1
                var week = 0
                while (dayOfYear <= daysInYear) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        repeat(7) { col ->
                            if (dayOfYear <= daysInYear) {
                                val key = "$year-$dayOfYear"
                                val mood = moodMap[key]
                                val color = when {
                                    mood == null -> MaterialTheme.colorScheme.surfaceVariant
                                    else -> moodColors.getOrElse((mood.level - 1).coerceIn(0, 4)) { moodColors[2] }
                                }
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(color)
                                )
                                dayOfYear++
                            } else {
                                Spacer(Modifier.size(10.dp))
                            }
                        }
                    }
                    week++
                    if (week % 5 == 0 && week > 0) {
                        Spacer(Modifier.height(2.dp))
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                moodEmojis.forEachIndexed { index, emoji ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(emoji, style = MaterialTheme.typography.bodySmall)
                        Box(Modifier.size(8.dp).clip(CircleShape).background(moodColors[index]))
                    }
                }
            }
        }
    }
}

@Composable
private fun MoodMonthTrend(moods: List<MoodEntry>) {
    val months = remember(moods) {
        (0 until 6).map { offset ->
            val cal = Calendar.getInstance()
            cal.add(Calendar.MONTH, -offset)
            val key = "${cal.get(Calendar.YEAR)}-${cal.get(Calendar.MONTH)}"
            val monthMoods = moods.filter { entry ->
                val c = Calendar.getInstance().apply { timeInMillis = entry.date }
                "${c.get(Calendar.YEAR)}-${c.get(Calendar.MONTH)}" == key
            }
            Triple(cal.get(Calendar.MONTH), monthMoods.size, if (monthMoods.isEmpty()) 0.0 else monthMoods.map { it.level }.average())
        }.reversed()
    }
    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("月度趋势", style = MaterialTheme.typography.titleMedium)
            Row(
                modifier = Modifier.fillMaxWidth().height(120.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                months.forEach { (month, count, avg) ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(if (count > 0) "%.1f".format(avg) else "-", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        val barHeight = if (avg <= 0) 8.dp else (12 + avg * 20).dp
                        Box(
                            modifier = Modifier
                                .width(18.dp)
                                .height(barHeight)
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (avg <= 0) MaterialTheme.colorScheme.surfaceVariant else moodColors.getOrElse((avg.toInt() - 1).coerceIn(0, 4)) { moodColors[2] })
                        )
                        Text("${month + 1}月", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun MoodEntryRow(mood: MoodEntry, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(moodEmojis.getOrElse((mood.level - 1).coerceIn(0, 4)) { moodEmojis[2] }, style = MaterialTheme.typography.headlineSmall)
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(SimpleDateFormat("M月d日 EEEE", Locale.getDefault()).format(Date(mood.date)), style = MaterialTheme.typography.titleMedium)
                    Text(
                        buildString {
                            append(moodLabels.getOrElse((mood.level - 1).coerceIn(0, 4)) { moodLabels[2] })
                            if (mood.activity.isNotBlank()) append(" · ${mood.activity}")
                            if (mood.note.isNotBlank()) append(" · ${mood.note}")
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2
                    )
                }
            }
        }
    }
}

@Composable
private fun MoodEmptyCard() {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Text(
            "本月还没有打卡记录，点上方标题提示的日期开始记录吧。",
            modifier = Modifier.padding(20.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun MoodEditDialog(
    date: Long,
    existing: MoodEntry?,
    onDismiss: () -> Unit,
    onSave: (Int, String, String) -> Unit,
    onDelete: () -> Unit
) {
    var level by remember(existing?.id) { mutableStateOf(existing?.level ?: 3) }
    var activity by remember(existing?.id) { mutableStateOf(existing?.activity ?: "") }
    var note by remember(existing?.id) { mutableStateOf(existing?.note ?: "") }
    val dateText = SimpleDateFormat("yyyy年M月d日", Locale.getDefault()).format(Date(date))

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("心情打卡 · $dateText") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    moodEmojis.forEachIndexed { index, emoji ->
                        val selected = level == index + 1
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(emoji, style = MaterialTheme.typography.headlineMedium)
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .border(
                                        width = if (selected) 3.dp else 1.dp,
                                        color = if (selected) moodColors[index] else MaterialTheme.colorScheme.outline,
                                        shape = CircleShape
                                    )
                                    .clickable { level = index + 1 }
                                    .background(if (selected) moodColors[index].copy(alpha = 0.3f) else Color.Transparent)
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = activity,
                    onValueChange = { activity = it },
                    label = { Text("活动标签（可选）") },
                    placeholder = { Text("如：散步、聚会、工作") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("备注（可选）") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
        confirmButton = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (existing != null) {
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "删除", tint = MaterialTheme.colorScheme.error)
                    }
                }
                Button(onClick = { onSave(level, activity.trim(), note.trim()) }) { Text("保存") }
            }
        }
    )
}

private fun moodsForCurrentMonth(moods: List<MoodEntry>): List<MoodEntry> {
    val now = Calendar.getInstance()
    val month = now.get(Calendar.MONTH)
    val year = now.get(Calendar.YEAR)
    return moods.filter { entry ->
        val cal = Calendar.getInstance().apply { timeInMillis = entry.date }
        cal.get(Calendar.YEAR) == year && cal.get(Calendar.MONTH) == month
    }
}

private fun startOfDay(timestamp: Long): Long {
    val cal = Calendar.getInstance()
    cal.timeInMillis = timestamp
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}

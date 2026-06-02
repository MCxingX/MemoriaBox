package com.memoriabox.ui.screen.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.memoriabox.data.model.BgType
import com.memoriabox.data.model.Box
import com.memoriabox.data.model.Event
import com.memoriabox.data.model.EventType
import com.memoriabox.utils.ColorUtils
import java.text.SimpleDateFormat
import java.util.*

val COMMON_EMOJIS = listOf(
    "\uD83C\uDFB2", "\uD83C\uDF89", "\uD83D\uDC96", "\uD83C\uDF82", "\uD83C\uDF81", "\uD83C\uDF1F",
    "\uD83C\uDF38", "\u2600\uFE0F", "\uD83C\uDF08", "\uD83C\uDF40", "\uD83E\uDD8B", "\uD83D\uDC36",
    "\uD83D\uDC31", "\uD83D\uDC3B", "\uD83D\uDC3C", "\uD83E\uDD8A", "\uD83D\uDC2F", "\uD83E\uDD81",
    "\uD83C\uDFE0", "\uD83D\uDCBC", "\uD83C\uDF93", "\uD83D\uDC8E", "\uD83D\uDC8D", "\uD83D\uDC8C",
    "\uD83D\uDC95", "\uD83D\uDC90", "\uD83C\uDF39", "\uD83C\uDF37", "\uD83C\uDF3B", "\uD83C\uDF3A",
    "\uD83E\uDE77", "\uD83C\uDF4E", "\uD83C\uDF53", "\uD83C\uDF52", "\uD83C\uDF4C", "\uD83C\uDF49",
    "\uD83C\uDF51", "\uD83C\uDF47", "\uD83C\uDF50", "\uD83C\uDF4D", "\uD83E\uDD65", "\uD83E\uDD51"
)

@Composable
fun BoxDialog(
    existingBox: Box? = null,
    onDismiss: () -> Unit,
    onSave: (name: String, icon: String, bgType: BgType, bgValue: String) -> Unit
) {
    var name by remember { mutableStateOf(existingBox?.name ?: "") }
    var selectedIcon by remember { mutableStateOf(existingBox?.icon ?: "\uD83D\uDCE6") }
    var selectedBgType by remember { mutableStateOf(existingBox?.bgType ?: BgType.COLOR) }
    var selectedBgValue by remember { mutableStateOf(existingBox?.bgValue ?: "#7C4DFF") }
    var showEmojiPicker by remember { mutableStateOf(false) }
    var showColorPicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existingBox == null) "创建盒子" else "编辑盒子") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("盒子名称") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("图标", style = MaterialTheme.typography.labelLarge)
                        Spacer(modifier = Modifier.height(8.dp))
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(0.2f))
                                .padding(8.dp)
                                .clickable { showEmojiPicker = true },
                            tint = Color.Unspecified
                        )
                        Text(selectedIcon, style = MaterialTheme.typography.headlineMedium)
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("背景", style = MaterialTheme.typography.labelLarge)
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(ColorUtils.hexToColor(selectedBgValue))
                                .clickable { showColorPicker = true }
                        )
                        Text(
                            text = if (selectedBgType == BgType.COLOR) "颜色" else "图片",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { showEmojiPicker = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.EmojiEmotions, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("选择图标")
                    }
                    OutlinedButton(
                        onClick = { showColorPicker = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Palette, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("选择颜色")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(name, selectedIcon, selectedBgType, selectedBgValue) },
                enabled = name.isNotBlank()
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )

    if (showEmojiPicker) {
        EmojiPickerDialog(
            onDismiss = { showEmojiPicker = false },
            onSelected = { emoji ->
                selectedIcon = emoji
                showEmojiPicker = false
            }
        )
    }

    if (showColorPicker) {
        ColorPickerDialog(
            initialColor = selectedBgValue,
            onDismiss = { showColorPicker = false },
            onSelected = { color ->
                selectedBgValue = color
                selectedBgType = BgType.COLOR
                showColorPicker = false
            }
        )
    }
}

@Composable
fun EmojiPickerDialog(
    onDismiss: () -> Unit,
    onSelected: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择图标") },
        text = {
            LazyVerticalGrid(
                columns = GridCells.Fixed(6),
                contentPadding = PaddingValues(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.height(300.dp)
            ) {
                items(COMMON_EMOJIS) { emoji ->
                    Text(
                        text = emoji,
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onSelected(emoji) }
                            .padding(8.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )
}

@Composable
fun EventDialog(
    existingEvent: Event? = null,
    availableBoxes: List<Box>,
    onDismiss: () -> Unit,
    onSave: (Event) -> Unit
) {
    var name by remember { mutableStateOf(existingEvent?.name ?: "") }
    var selectedDate by remember { mutableStateOf<Long?>(existingEvent?.date) }
    var selectedType by remember { mutableStateOf(existingEvent?.type ?: EventType.COUNTDOWN) }
    var note by remember { mutableStateOf(existingEvent?.note ?: "") }
    var reminderEnabled by remember { mutableStateOf(existingEvent?.reminderEnabled ?: false) }
    var reminderDays by remember { mutableIntStateOf(existingEvent?.reminderDays ?: 1) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showLunarCalendar by remember { mutableStateOf(false) }
    var selectedLunar by remember { mutableStateOf(existingEvent?.lunar) }
    var selectedBoxId by remember { mutableStateOf(existingEvent?.boxId ?: (availableBoxes.firstOrNull()?.id ?: "")) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existingEvent == null) "添加日子" else "编辑日子") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("事件名称") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = selectedDate?.let { formatDate(it) } ?: "",
                    onValueChange = { },
                    label = { Text("日期") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDatePicker = true },
                    readOnly = true,
                    trailingIcon = {
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(Icons.Default.CalendarToday, contentDescription = "选择日期")
                        }
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { showLunarCalendar = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(selectedLunar ?: "选择农历")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text("事件类型", style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    EventType.entries.forEach { type ->
                        FilterChip(
                            selected = selectedType == type,
                            onClick = { selectedType = type },
                            label = {
                                Text(
                                    when (type) {
                                        EventType.COUNTDOWN -> "倒数日"
                                        EventType.ANNIVERSARY -> "纪念日"
                                        EventType.ELAPSED -> "正计时"
                                        EventType.BIRTHDAY -> "生日"
                                        EventType.TODO -> "待办"
                                    }
                                )
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("备注") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = reminderEnabled,
                        onCheckedChange = { reminderEnabled = it }
                    )
                    Text("开启提醒", modifier = Modifier.clickable { reminderEnabled = !reminderEnabled })

                    if (reminderEnabled) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("提前", style = MaterialTheme.typography.bodySmall)
                        OutlinedTextField(
                            value = reminderDays.toString(),
                            onValueChange = { 
                                reminderDays = it.toIntOrNull() ?: 1
                            },
                            modifier = Modifier.width(64.dp),
                            textStyle = MaterialTheme.typography.bodySmall
                        )
                        Text("天", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val event = Event(
                        id = existingEvent?.id ?: UUID.randomUUID().toString(),
                        boxId = selectedBoxId,
                        name = name,
                        date = selectedDate ?: System.currentTimeMillis(),
                        lunar = selectedLunar,
                        type = selectedType,
                        note = note,
                        reminderEnabled = reminderEnabled,
                        reminderDays = reminderDays,
                        createdAt = existingEvent?.createdAt ?: System.currentTimeMillis()
                    )
                    onSave(event)
                },
                enabled = name.isNotBlank() && selectedDate != null
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )

    if (showDatePicker) {
        DatePickerDialog(
            onDismiss = { showDatePicker = false },
            onDateSelected = { timestamp ->
                selectedDate = timestamp
                showDatePicker = false
            }
        )
    }

    if (showLunarCalendar) {
        LunarCalendarDialog(
            onDismiss = { showLunarCalendar = false },
            onSelected = { lunar ->
                selectedLunar = lunar
                showLunarCalendar = false
            }
        )
    }
}

@Composable
fun DatePickerDialog(
    onDismiss: () -> Unit,
    onDateSelected: (Long) -> Unit
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = System.currentTimeMillis()
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择日期") },
        text = {
            DatePicker(
                state = datePickerState,
                showModeToggle = true
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    datePickerState.selectedDateMillis?.let { onDateSelected(it) }
                },
                enabled = datePickerState.selectedDateMillis != null
            ) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
fun LunarCalendarDialog(
    onDismiss: () -> Unit,
    onSelected: (String) -> Unit
) {
    val currentYear = Calendar.getInstance().get(Calendar.YEAR)
    var selectedYear by remember { mutableIntStateOf(currentYear) }
    var selectedMonth by remember { mutableIntStateOf(1) }
    var selectedDay by remember { mutableIntStateOf(1) }

    val lunarMonths = listOf(
        "正月", "二月", "三月", "四月", "五月", "六月",
        "七月", "八月", "九月", "十月", "十一月", "腊月"
    )
    val lunarDays = (1..30).toList()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择农历") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                Text("年份", style = MaterialTheme.typography.labelLarge)
                OutlinedTextField(
                    value = selectedYear.toString(),
                    onValueChange = { selectedYear = it.toIntOrNull() ?: currentYear },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                Text("月份", style = MaterialTheme.typography.labelLarge)
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    modifier = Modifier.height(150.dp)
                ) {
                    items(lunarMonths) { month ->
                        FilterChip(
                            selected = selectedMonth == lunarMonths.indexOf(month) + 1,
                            onClick = { selectedMonth = lunarMonths.indexOf(month) + 1 },
                            label = { Text(month) },
                            modifier = Modifier.padding(4.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                Text("日期", style = MaterialTheme.typography.labelLarge)
                LazyVerticalGrid(
                    columns = GridCells.Fixed(6),
                    modifier = Modifier.height(150.dp)
                ) {
                    items(lunarDays) { day ->
                        FilterChip(
                            selected = selectedDay == day,
                            onClick = { selectedDay = day },
                            label = { Text(day.toString()) },
                            modifier = Modifier.padding(4.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val lunar = "${selectedYear}年农历${lunarMonths[selectedMonth - 1]}${selectedDay}日"
                    onSelected(lunar)
                }
            ) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

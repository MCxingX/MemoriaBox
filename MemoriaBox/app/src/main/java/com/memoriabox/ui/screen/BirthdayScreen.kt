package com.memoriabox.ui.screen

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.memoriabox.data.model.Event
import com.memoriabox.viewmodel.createCalendarViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BirthdayScreen(application: Application) {
    val vm = remember { createCalendarViewModel(application) }
    val events by vm.allEvents.collectAsState(initial = emptyList())

    val birthdayEvents = remember(events) {
        events.filter { it.type == com.memoriabox.data.model.EventType.BIRTHDAY }
    }

    val upcomingBirthdays = remember(birthdayEvents) {
        birthdayEvents
            .map { event ->
                val nextBirthday = getNextBirthday(event.date)
                val daysUntil = (nextBirthday - System.currentTimeMillis()) / (1000 * 60 * 60 * 24)
                val age = calculateAge(event.date, System.currentTimeMillis())
                BirthdayData(event, nextBirthday, daysUntil, age + 1)
            }
            .filter { it.daysUntil >= 0 }
            .sortedBy { it.daysUntil }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("生日管理") },
                actions = {
                    IconButton(onClick = { /* Add birthday */ }) {
                        Icon(Icons.Default.Cake, contentDescription = "添加生日")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            // Summary card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatItem(
                        value = birthdayEvents.size.toString(),
                        label = "总生日数"
                    )
                    Divider(
                        modifier = Modifier
                            .height(40.dp)
                            .width(1.dp)
                    )
                    val thisMonthBirthdays = birthdayEvents.count { 
                        Calendar.getInstance().apply { timeInMillis = it.date }
                            .get(Calendar.MONTH) == Calendar.getInstance().get(Calendar.MONTH)
                    }
                    StatItem(
                        value = thisMonthBirthdays.toString(),
                        label = "本月生日"
                    )
                    Divider(
                        modifier = Modifier
                            .height(40.dp)
                            .width(1.dp)
                    )
                    val upcomingCount = upcomingBirthdays.count { it.daysUntil <= 30 }
                    StatItem(
                        value = upcomingCount.toString(),
                        label = "近期生日"
                    )
                }
            }

            // Upcoming birthdays
            Text(
                "即将到来的生日",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(upcomingBirthdays) { birthday ->
                    BirthdayCard(
                        birthday = birthday,
                        onClick = { /* Show detail */ }
                    )
                }

                if (upcomingBirthdays.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.Cake,
                                    null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.outline
                                )
                                Text(
                                    "暂无生日记录",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

data class BirthdayData(
    val event: Event,
    val nextBirthday: Long,
    val daysUntil: Long,
    val age: Int
)

@Composable
fun BirthdayCard(
    birthday: BirthdayData,
    onClick: () -> Unit
) {
    val isUrgent = birthday.daysUntil <= 7

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = if (isUrgent) {
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            )
        } else {
            CardDefaults.cardColors()
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar or initial
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            if (isUrgent) 
                                MaterialTheme.colorScheme.onErrorContainer
                            else 
                                MaterialTheme.colorScheme.primaryContainer
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        birthday.event.name.firstOrNull()?.toString() ?: "🎂",
                        style = MaterialTheme.typography.titleLarge
                    )
                }

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            birthday.event.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (isUrgent) {
                            Spacer(Modifier.width(8.dp))
                            AssistChip(
                                onClick = { },
                                label = { Text("即将到期") },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Warning,
                                        null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "${birthday.event.note.ifEmpty { "生日" }} · ${SimpleDateFormat("MM-dd", Locale.getDefault()).format(Date(birthday.nextBirthday))}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    "还剩 ${birthday.daysUntil} 天",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isUrgent) 
                        MaterialTheme.colorScheme.onErrorContainer
                    else 
                        MaterialTheme.colorScheme.primary
                )
                Text(
                    "${birthday.age}岁",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun StatItem(
    value: String,
    label: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

fun getNextBirthday(birthTimestamp: Long): Long {
    val birthCal = Calendar.getInstance().apply { timeInMillis = birthTimestamp }
    val nowCal = Calendar.getInstance()
    
    nowCal.set(Calendar.MONTH, birthCal.get(Calendar.MONTH))
    nowCal.set(Calendar.DAY_OF_MONTH, birthCal.get(Calendar.DAY_OF_MONTH))
    
    if (nowCal.before(Calendar.getInstance())) {
        nowCal.add(Calendar.YEAR, 1)
    }
    
    return nowCal.timeInMillis
}

fun calculateAge(birthTimestamp: Long, currentTimestamp: Long): Int {
    val birthCal = Calendar.getInstance().apply { timeInMillis = birthTimestamp }
    val currentCal = Calendar.getInstance().apply { timeInMillis = currentTimestamp }
    
    var age = currentCal.get(Calendar.YEAR) - birthCal.get(Calendar.YEAR)
    
    if (currentCal.get(Calendar.DAY_OF_YEAR) < birthCal.get(Calendar.DAY_OF_YEAR)) {
        age--
    }
    
    return age
}

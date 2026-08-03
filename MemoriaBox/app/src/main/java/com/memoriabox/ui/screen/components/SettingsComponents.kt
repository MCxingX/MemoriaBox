package com.memoriabox.ui.screen.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.memoriabox.ui.utils.rememberAdaptiveUiSize
import com.memoriabox.utils.AppSettings
import com.memoriabox.utils.ImageImportUtils
import com.memoriabox.BuildConfig

@Composable
fun SettingsList(
    onBackupSettingsClick: () -> Unit = {},
    onWebDavSettingsClick: () -> Unit = {},
    onDiarySettingsClick: () -> Unit = {}
) {
    Column(modifier = Modifier.fillMaxSize()) {
        SettingsItem(
            icon = Icons.Default.Backup,
            title = "备份设置",
            description = "本地备份、导入导出、自动备份",
            onClick = onBackupSettingsClick
        )
        SettingsItem(
            icon = Icons.Default.Cloud,
            title = "WebDAV 同步",
            description = "配置云端同步服务",
            onClick = onWebDavSettingsClick
        )
        SettingsItem(
            icon = Icons.Default.Edit,
            title = "日记设置",
            description = "滚动动画速度、开关",
            onClick = onDiarySettingsClick
        )
        SettingsItem(
            icon = Icons.Default.NotificationImportant,
            title = "提醒设置",
            description = "PushPlus 推送、通知管理"
        )
        SettingsItem(
            icon = Icons.Default.Info,
            title = "关于",
            description = "版本 ${BuildConfig.VERSION_NAME}"
        )
    }
}
@Composable
fun SettingsItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit = {}
) {
    val adaptiveUi = rememberAdaptiveUiSize()
    val gradient = Brush.linearGradient(
        listOf(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.92f),
            MaterialTheme.colorScheme.secondary.copy(alpha = 0.88f),
            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.84f)
        )
    )
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = adaptiveUi.screenPadding, vertical = adaptiveUi.sectionSpacing / 2f)
            .clickable(onClick = onClick),
        shape = if (adaptiveUi.compact) MaterialTheme.shapes.medium else MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.84f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(if (adaptiveUi.compact) 12.dp else 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(if (adaptiveUi.compact) 42.dp else 48.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(gradient),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = title, modifier = Modifier.size(if (adaptiveUi.compact) 22.dp else 25.dp), tint = Color.White)
            }
            Spacer(Modifier.width(if (adaptiveUi.compact) 12.dp else 16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
        }
    }
}

@Composable
fun BackupSettingsContent(
    modifier: Modifier = Modifier,
    backupPassword: String = "",
    onBackupPasswordChange: (String) -> Unit = {},
    onSelectDir: () -> Unit = {},
    onManualBackup: () -> Unit = {},
    onImport: () -> Unit = {},
    isBusy: Boolean = false
) {
    Column(
        modifier = modifier
            .padding(16.dp)
            .fillMaxWidth()
    ) {
        Text("备份设置", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(16.dp))
        
        OutlinedCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("本地备份", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = backupPassword,
                    onValueChange = onBackupPasswordChange,
                    label = { Text("备份密码（可选）") },
                    supportingText = { Text("留空会使用默认保护；跨设备导入建议设置密码。") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isBusy
                )
                Spacer(Modifier.height(8.dp))
                Button(onClick = onSelectDir, enabled = !isBusy, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Folder, null)
                    Spacer(Modifier.width(8.dp))
                    Text("选择备份目录")
                }
                Spacer(Modifier.height(8.dp))
                Button(onClick = onManualBackup, enabled = !isBusy, modifier = Modifier.fillMaxWidth()) {
                    if (isBusy) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.Backup, null)
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(if (isBusy) "处理中" else "立即备份")
                }
                Spacer(Modifier.height(8.dp))
                Button(onClick = onImport, enabled = !isBusy, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Upload, null)
                    Spacer(Modifier.width(8.dp))
                    Text("导入备份")
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "备份文件可跨设备导入。导入会合并到当前数据，保留现有日子、日记和素材。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun WebDavSettingsContent(
    modifier: Modifier = Modifier
) {
    var serverUrl by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var statusText by remember { mutableStateOf<String?>(null) }
    
    Column(
        modifier = modifier
            .padding(16.dp)
            .fillMaxWidth()
    ) {
        Text("WebDAV 设置", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(16.dp))
        
        OutlinedTextField(
            value = serverUrl,
            onValueChange = { serverUrl = it },
            label = { Text("服务器地址") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("用户名") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("密码") },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = {
                statusText = if (serverUrl.isBlank()) {
                    "请先填写 WebDAV 服务器地址。"
                } else {
                    "配置已填写。请在同步页面执行同步检查。"
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("检查配置")
        }
        statusText?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun DiarySettingsDialog(
    onDismiss: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var scrollEnabled by remember { mutableStateOf(com.memoriabox.utils.AppSettings.getDiaryScrollEnabled(context)) }
    var scrollSpeed by remember { mutableIntStateOf(com.memoriabox.utils.AppSettings.getDiaryScrollSpeed(context)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("日记设置") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("滚动动画", style = MaterialTheme.typography.titleSmall)
                        Text("关闭后直接显示全部文字", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = scrollEnabled,
                        onCheckedChange = {
                            scrollEnabled = it
                            com.memoriabox.utils.AppSettings.setDiaryScrollEnabled(context, it)
                        }
                    )
                }

                if (scrollEnabled) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("滚动速度", style = MaterialTheme.typography.titleSmall)
                            Text("${scrollSpeed}ms/字", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Slider(
                            value = scrollSpeed.toFloat(),
                            onValueChange = {
                                scrollSpeed = it.toInt()
                                com.memoriabox.utils.AppSettings.setDiaryScrollSpeed(context, scrollSpeed)
                            },
                            valueRange = 10f..200f,
                            steps = 18,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("快", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("慢", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("完成") }
        }
    )
}

@Composable
fun MonthlySummarySettingsDialog(
    onDismiss: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var summaryEnabled by remember { mutableStateOf(com.memoriabox.utils.AppSettings.getMonthlySummaryEnabled(context)) }
    var autoPromptEnabled by remember { mutableStateOf(com.memoriabox.utils.AppSettings.getMonthlySummaryAutoPromptEnabled(context)) }
    var textEnabled by remember { mutableStateOf(com.memoriabox.utils.AppSettings.getMonthlySummaryTextEnabled(context)) }
    var playSpeed by remember { mutableFloatStateOf(com.memoriabox.utils.AppSettings.getMonthlySummaryPlaySpeedFactor(context)) }
    var selectedYear by remember { mutableIntStateOf(java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)) }
    var selectedMonth by remember { mutableIntStateOf(java.util.Calendar.getInstance().get(java.util.Calendar.MONTH) + 1) }
    val settingsVersion = AppSettings.settingsVersion
    var monthImages by remember(settingsVersion, selectedYear, selectedMonth) { mutableStateOf(AppSettings.getMonthlyMediaImages(context, selectedYear, selectedMonth)) }
    var monthVideos by remember(settingsVersion, selectedYear, selectedMonth) { mutableStateOf(AppSettings.getMonthlyMediaVideos(context, selectedYear, selectedMonth)) }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia()) { uris ->
        val copied = uris.mapNotNull { ImageImportUtils.copyImageToPrivateStorage(context, it, "monthly_media_images") }
        if (copied.isNotEmpty()) {
            monthImages = (monthImages + copied).distinct()
            AppSettings.setMonthlyMediaImages(context, selectedYear, selectedMonth, monthImages)
        }
    }
    val videoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia()) { uris ->
        val copied = uris.mapNotNull { ImageImportUtils.copyImageToPrivateStorage(context, it, "monthly_media_videos") }
        if (copied.isNotEmpty()) {
            monthVideos = (monthVideos + copied).distinct()
            AppSettings.setMonthlyMediaVideos(context, selectedYear, selectedMonth, monthVideos)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("月度总结设置") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("启用月度总结", style = MaterialTheme.typography.titleSmall)
                        Text("在日历视图中查看月度照片与日记总结", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = summaryEnabled,
                        onCheckedChange = {
                            summaryEnabled = it
                            com.memoriabox.utils.AppSettings.setMonthlySummaryEnabled(context, it)
                        }
                    )
                }
                HorizontalDivider()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("新月自动提醒", style = MaterialTheme.typography.titleSmall)
                        Text("进入新月份时弹出总结预览", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = autoPromptEnabled,
                        onCheckedChange = {
                            autoPromptEnabled = it
                            com.memoriabox.utils.AppSettings.setMonthlySummaryAutoPromptEnabled(context, it)
                        }
                    )
                }
                HorizontalDivider()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("显示文字摘要", style = MaterialTheme.typography.titleSmall)
                        Text("在月度总结中展示文字描述", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = textEnabled,
                        onCheckedChange = {
                            textEnabled = it
                            com.memoriabox.utils.AppSettings.setMonthlySummaryTextEnabled(context, it)
                        }
                    )
                }
                HorizontalDivider()
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("播放速度", style = MaterialTheme.typography.titleSmall)
                        Text("${"%.1f".format(playSpeed)}x", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Slider(
                        value = playSpeed,
                        onValueChange = {
                            playSpeed = it
                            com.memoriabox.utils.AppSettings.setMonthlySummaryPlaySpeedFactor(context, it)
                        },
                        valueRange = 0.5f..2.0f,
                        steps = 4,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("慢", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("快", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                HorizontalDivider()
                MonthlyMediaSettingsPanel(
                    selectedYear = selectedYear,
                    selectedMonth = selectedMonth,
                    onYearSelected = { selectedYear = it },
                    onMonthSelected = { selectedMonth = it },
                    images = monthImages,
                    videos = monthVideos,
                    onAddImages = { imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    onAddVideos = { videoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)) },
                    onRemoveImage = { uri ->
                        monthImages = monthImages.filterNot { it == uri }
                        AppSettings.setMonthlyMediaImages(context, selectedYear, selectedMonth, monthImages)
                    },
                    onRemoveVideo = { uri ->
                        monthVideos = monthVideos.filterNot { it == uri }
                        AppSettings.setMonthlyMediaVideos(context, selectedYear, selectedMonth, monthVideos)
                    },
                    onClearImages = {
                        monthImages = emptyList()
                        AppSettings.setMonthlyMediaImages(context, selectedYear, selectedMonth, monthImages)
                    },
                    onClearVideos = {
                        monthVideos = emptyList()
                        AppSettings.setMonthlyMediaVideos(context, selectedYear, selectedMonth, monthVideos)
                    }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("完成") }
        }
    )
}

@Composable
private fun MonthlyMediaSettingsPanel(
    selectedYear: Int,
    selectedMonth: Int,
    onYearSelected: (Int) -> Unit,
    onMonthSelected: (Int) -> Unit,
    images: List<String>,
    videos: List<String>,
    onAddImages: () -> Unit,
    onAddVideos: () -> Unit,
    onRemoveImage: (String) -> Unit,
    onRemoveVideo: (String) -> Unit,
    onClearImages: () -> Unit,
    onClearVideos: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text("当月相册 / 视频", style = MaterialTheme.typography.titleSmall)
            Text("当前配置 ${selectedYear} 年 ${selectedMonth} 月素材。支持一次选择多张照片或多个视频，下面可横向滑动预览和移除。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(onClick = { onYearSelected((selectedYear - 1).coerceIn(1900, 2100)) }) { Text("上一年") }
            Text("${selectedYear} 年", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            OutlinedButton(onClick = { onYearSelected((selectedYear + 1).coerceIn(1900, 2100)) }) { Text("下一年") }
        }
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            (1..12).forEach { month ->
                FilterChip(
                    selected = selectedMonth == month,
                    onClick = { onMonthSelected(month) },
                    label = { Text("${month}月") }
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = onAddImages, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.Image, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("批量加照片")
            }
            OutlinedButton(onClick = onAddVideos, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.Videocam, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("批量加视频")
            }
        }
        MonthlyMediaStrip(title = "照片 ${images.size}", items = images, isVideo = false, onRemove = onRemoveImage, onClear = onClearImages)
        MonthlyMediaStrip(title = "视频 ${videos.size}", items = videos, isVideo = true, onRemove = onRemoveVideo, onClear = onClearVideos)
    }
}

@Composable
private fun MonthlyMediaStrip(title: String, items: List<String>, isVideo: Boolean, onRemove: (String) -> Unit, onClear: () -> Unit) {
    var selectedIndex by remember(items) { mutableIntStateOf(0) }
    val selectedItem = items.getOrNull(selectedIndex.coerceIn(0, (items.size - 1).coerceAtLeast(0)))
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, style = MaterialTheme.typography.labelLarge)
            if (items.isNotEmpty()) {
                TextButton(onClick = onClear) { Text("清空") }
            }
        }
        if (items.isEmpty()) {
            Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.56f), modifier = Modifier.fillMaxWidth()) {
                Text(if (isVideo) "暂无视频，可一次选择多个视频" else "暂无照片，可一次选择多张照片", modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (isVideo) 96.dp else 148.dp)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surface),
                    contentAlignment = Alignment.Center
                ) {
                    if (selectedItem != null && !isVideo) {
                        AsyncImage(model = selectedItem, contentDescription = "当前照片", contentScale = ContentScale.Crop, modifier = Modifier.matchParentSize())
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.PlayCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                            Text(selectedItem?.substringAfterLast('/')?.take(36) ?: "视频素材", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                        }
                    }
                    IconButton(
                        onClick = { selectedItem?.let(onRemove) },
                        modifier = Modifier.align(Alignment.TopEnd).size(36.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "移除当前素材", tint = if (isVideo) MaterialTheme.colorScheme.primary else Color.White)
                    }
                }
            }
            Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items.forEachIndexed { index, uri ->
                    Box(
                        modifier = Modifier
                            .size(width = 84.dp, height = 68.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(if (index == selectedIndex) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { selectedIndex = index }
                    ) {
                        if (isVideo) {
                            Icon(Icons.Default.PlayCircle, contentDescription = "视频", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.align(Alignment.Center).size(28.dp))
                        } else {
                            AsyncImage(model = uri, contentDescription = "照片", contentScale = ContentScale.Crop, modifier = Modifier.matchParentSize())
                        }
                        IconButton(onClick = { onRemove(uri) }, modifier = Modifier.align(Alignment.TopEnd).size(28.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "移除", tint = if (isVideo) MaterialTheme.colorScheme.primary else Color.White, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UpcomingEventsSettingsDialog(
    onDismiss: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var enabled by remember { mutableStateOf(com.memoriabox.utils.AppSettings.getUpcomingEventsEnabled(context)) }
    var days by remember { mutableIntStateOf(com.memoriabox.utils.AppSettings.getUpcomingEventsDays(context)) }
    var urgentDays by remember { mutableIntStateOf(com.memoriabox.utils.AppSettings.getUpcomingEventsUrgentDays(context)) }
    var reminderEnabled by remember { mutableStateOf(com.memoriabox.utils.AppSettings.getUpcomingEventsReminderEnabled(context)) }
    var urgentColor by remember { mutableStateOf(com.memoriabox.utils.AppSettings.getUpcomingEventsUrgentColor(context)) }
    var normalColor by remember { mutableStateOf(com.memoriabox.utils.AppSettings.getUpcomingEventsNormalColor(context)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("即将到来") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("首页只看临近日子", style = MaterialTheme.typography.titleSmall)
                        Text("默认显示 30 天内事件，并按剩余天数排序", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = enabled,
                        onCheckedChange = {
                            enabled = it
                            com.memoriabox.utils.AppSettings.setUpcomingEventsEnabled(context, it)
                        }
                    )
                }

                HorizontalDivider()
                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("显示范围", style = MaterialTheme.typography.titleSmall)
                        Text("${days}天内", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Slider(
                        value = days.toFloat(),
                        onValueChange = {
                            days = it.toInt()
                            if (urgentDays > days) {
                                urgentDays = days
                                com.memoriabox.utils.AppSettings.setUpcomingEventsUrgentDays(context, urgentDays)
                            }
                            com.memoriabox.utils.AppSettings.setUpcomingEventsDays(context, days)
                        },
                        valueRange = 1f..90f,
                        steps = 88,
                        enabled = enabled
                    )
                }

                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("紧急阈值", style = MaterialTheme.typography.titleSmall)
                        Text("${urgentDays}天内", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Slider(
                        value = urgentDays.toFloat(),
                        onValueChange = {
                            urgentDays = it.toInt().coerceAtMost(days)
                            com.memoriabox.utils.AppSettings.setUpcomingEventsUrgentDays(context, urgentDays)
                        },
                        valueRange = 1f..days.coerceAtLeast(2).toFloat(),
                        steps = (days - 2).coerceAtLeast(0),
                        enabled = enabled
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("即将到来提醒", style = MaterialTheme.typography.titleSmall)
                        Text("作为首页提醒展示开关，默认开启", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = reminderEnabled,
                        onCheckedChange = {
                            reminderEnabled = it
                            com.memoriabox.utils.AppSettings.setUpcomingEventsReminderEnabled(context, it)
                        },
                        enabled = enabled
                    )
                }

                OutlinedTextField(
                    value = urgentColor,
                    onValueChange = {
                        urgentColor = it
                        com.memoriabox.utils.AppSettings.setUpcomingEventsUrgentColor(context, it)
                    },
                    label = { Text("紧急颜色") },
                    supportingText = { Text("默认 #F97316") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = enabled,
                    singleLine = true
                )
                OutlinedTextField(
                    value = normalColor,
                    onValueChange = {
                        normalColor = it
                        com.memoriabox.utils.AppSettings.setUpcomingEventsNormalColor(context, it)
                    },
                    label = { Text("正常颜色") },
                    supportingText = { Text("默认 #2563EB") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = enabled,
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("完成") }
        }
    )
}

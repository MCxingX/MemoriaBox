package com.memoriabox.ui.screen

import android.app.Application
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.*
import com.memoriabox.ui.navigation.Screen
import com.memoriabox.ui.screen.components.*
import com.memoriabox.ui.utils.AdaptiveUiSize
import com.memoriabox.ui.utils.rememberAdaptiveUiSize
import com.memoriabox.data.model.*
import com.memoriabox.ui.theme.AppThemeMode
import com.memoriabox.ui.theme.AppThemeGroup
import com.memoriabox.ui.theme.NianJiLogoMark
import com.memoriabox.ui.theme.MemoriaDesign
import com.memoriabox.ui.theme.group
import com.memoriabox.update.UpdateManager
import com.memoriabox.update.UpdateState
import com.memoriabox.utils.NotificationHelper
import com.memoriabox.utils.installedAppVersion
import com.memoriabox.viewmodel.*
import java.util.Date

@Composable
fun SettingsScreen(
    application: Application,
    currentThemeMode: AppThemeMode,
    onThemeModeChange: (AppThemeMode) -> Unit,
    onNavigateToCustomization: () -> Unit,
    onNavigateToFriends: () -> Unit,
    onBackupSettingsClick: () -> Unit,
    onWebDavSettingsClick: () -> Unit,
    onCheckUpdate: () -> Unit
) {
    val context = LocalContext.current
    val installedVersion = remember(context) { context.installedAppVersion() }
    val pushPlusHelper = remember { com.memoriabox.utils.NotificationHelper(application) }
    var pushPlusToken by rememberSaveable { mutableStateOf(pushPlusHelper.getPushPlusToken()) }
    var pushPlusEnabled by remember { mutableStateOf(pushPlusHelper.isPushPlusEnabled()) }
    var pushPlusChannel by rememberSaveable { mutableStateOf(pushPlusHelper.getPushPlusChannel()) }

    LaunchedEffect(pushPlusToken) {
        kotlinx.coroutines.delay(500)
        pushPlusHelper.setPushPlusToken(pushPlusToken)
    }
    LaunchedEffect(pushPlusChannel) {
        kotlinx.coroutines.delay(500)
        pushPlusHelper.setPushPlusChannel(pushPlusChannel)
    }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showDiarySettings by remember { mutableStateOf(false) }
    var showMonthlySummarySettings by remember { mutableStateOf(false) }
    var showUpcomingSettings by remember { mutableStateOf(false) }
    var showHolidaySettings by remember { mutableStateOf(false) }
    val updateState by UpdateManager.state.collectAsState()
    val adaptiveUi = rememberAdaptiveUiSize()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(top = adaptiveUi.sectionSpacing, bottom = adaptiveUi.screenPadding)
    ) {
        SettingsHeroCard()
        SettingsSectionTitle("轻松一点", "先选一个舒服的颜色，再整理常用入口")
        ThemeModeCard(currentThemeMode = currentThemeMode, onThemeModeChange = onThemeModeChange)
        SettingsSectionTitle("常用", "每天会用到的功能放在这里")
        SettingsItem(
            icon = Icons.Default.Palette,
            title = "个性化设置",
            description = "自定义页面背景、固定每日语录",
            onClick = onNavigateToCustomization
        )
        SettingsItem(
            icon = Icons.Default.Groups,
            title = "好友管理",
            description = "生日按一个月内临近优先排序，全部好友都会保留",
            onClick = onNavigateToFriends
        )

        SettingsSectionTitle("记录和数据", "备份、同步、日记和月度总结")
        
        SettingsItem(
            icon = Icons.Default.Backup,
            title = "备份设置",
            description = "本地备份、导入导出",
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
            onClick = { showDiarySettings = true }
        )
        SettingsItem(
            icon = Icons.Default.AutoStories,
            title = "月度总结",
            description = "开关、自动推送、播放速度",
            onClick = { showMonthlySummarySettings = true }
        )
        
        SettingsSectionTitle("提醒", "需要跨平台推送时再开启")

        SettingsItem(
            icon = Icons.Default.NotificationsActive,
            title = "即将到来",
            description = "首页显示、天数范围、颜色和提醒",
            onClick = { showUpcomingSettings = true }
        )

        SettingsItem(
            icon = Icons.Default.Celebration,
            title = "节假日提醒",
            description = "每天上午提醒春节、中秋、国庆等节假日",
            onClick = { showHolidaySettings = true }
        )

        // PushPlus settings inline
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = adaptiveUi.screenPadding, vertical = adaptiveUi.sectionSpacing),
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(if (adaptiveUi.compact) 12.dp else 16.dp)) {
                Text("PushPlus 推送", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("启用 PushPlus")
                    Switch(
                        checked = pushPlusEnabled,
                        onCheckedChange = { 
                            pushPlusEnabled = it
                            pushPlusHelper.setPushPlusEnabled(it)
                        }
                    )
                }
                if (pushPlusEnabled) {
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = pushPlusToken,
                        onValueChange = { pushPlusToken = it },
                        label = { Text("Token") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("wechat", "webhook", "mail", "sms").forEach { ch ->
                            FilterChip(
                                selected = pushPlusChannel == ch,
                                onClick = { pushPlusChannel = ch },
                                label = { Text(ch, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                            )
                        }
                    }
                }
            }
        }
        
        SettingsSectionTitle("关于", "版本和应用信息")
        
        val updateSummary = when (val state = updateState) {
            is UpdateState.Checking -> "正在检查 GitHub Release…"
            is UpdateState.Available -> "发现新版本 v${state.info.versionName}"
            is UpdateState.Downloading -> "正在下载 v${state.info.versionName}：${state.progress}%"
            is UpdateState.Ready -> "v${state.info.versionName} 已下载并完成校验"
            is UpdateState.UpToDate -> "当前已是最新版本"
            is UpdateState.Error -> state.message
            UpdateState.Idle -> "检查官方 GitHub Release 更新"
        }
        SettingsItem(
            icon = Icons.Default.SystemUpdate,
            title = "检查更新",
            description = updateSummary,
            onClick = onCheckUpdate
        )
        SettingsItem(
            icon = Icons.Default.Info,
            title = "关于",
            description = "版本 ${installedVersion.name} (${installedVersion.code}) · 念记",
            onClick = { showAboutDialog = true }
        )
    }

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text("关于 念记") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("版本：${installedVersion.name} (${installedVersion.code})", style = MaterialTheme.typography.bodyMedium)
                    Text("念记 是一个本地优先的日子、纪念日、待办和照片记录工具。", style = MaterialTheme.typography.bodyMedium)
                    Text("数据默认保存在本机，可通过备份和 WebDAV 功能进行迁移或同步。", style = MaterialTheme.typography.bodyMedium)
                    Text("著名木羽制作", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) { Text("知道了") }
            }
        )
    }

    if (showDiarySettings) {
        DiarySettingsDialog(onDismiss = { showDiarySettings = false })
    }
    if (showMonthlySummarySettings) {
        MonthlySummarySettingsDialog(onDismiss = { showMonthlySummarySettings = false })
    }
    if (showUpcomingSettings) {
        UpcomingEventsSettingsDialog(onDismiss = { showUpcomingSettings = false })
    }
    if (showHolidaySettings) {
        HolidaySettingsDialog(onDismiss = { showHolidaySettings = false })
    }
}

@Composable
fun SettingsHeroCard() {
    val adaptiveUi = rememberAdaptiveUiSize()
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = adaptiveUi.screenPadding, vertical = adaptiveUi.sectionSpacing),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.80f),
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            NianJiLogoMark(size = if (adaptiveUi.compact) 48.dp else 56.dp)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                Text("我的 念记", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.headlineSmall)
                Text("数据安全、外观、提醒和常用工具都放在这里。", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ThemeModeCard(currentThemeMode: AppThemeMode, onThemeModeChange: (AppThemeMode) -> Unit) {
    val adaptiveUi = rememberAdaptiveUiSize()
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = adaptiveUi.screenPadding, vertical = adaptiveUi.sectionSpacing),
        shape = RoundedCornerShape(MemoriaDesign.cardRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.84f)),
        elevation = CardDefaults.cardElevation(defaultElevation = MemoriaDesign.softShadow)
    ) {
        Column(modifier = Modifier.padding(adaptiveUi.cardPadding), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("当前主题", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                    Text(currentThemeMode.label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }
                Box(
                    modifier = Modifier
                        .width(54.dp)
                        .height(28.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(themePreviewBrush(currentThemeMode))
                )
            }
            Text(currentThemeMode.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
            AppThemeGroup.entries.forEach { group ->
                val modes = AppThemeMode.entries.filter { it.group == group }
                if (modes.isNotEmpty()) {
                    Text(group.label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        modes.forEach { mode ->
                            ThemePreviewCard(
                                mode = mode,
                                selected = currentThemeMode == mode,
                                onClick = { onThemeModeChange(mode) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemePreviewCard(mode: AppThemeMode, selected: Boolean, onClick: () -> Unit) {
    OutlinedCard(
        onClick = onClick,
        modifier = Modifier.width(118.dp),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(themePreviewBrush(mode))
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(mode.label, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(mode.description, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

private fun themePreviewBrush(mode: AppThemeMode): Brush {
    return when (mode) {
        AppThemeMode.BLUE_WHITE -> Brush.linearGradient(listOf(Color(0xFF1677FF), Color(0xFFFFFFFF)))
        AppThemeMode.DARK -> Brush.linearGradient(listOf(Color(0xFF17121A), Color(0xFFB8A6FF)))
        AppThemeMode.EYE_CARE -> Brush.linearGradient(listOf(Color(0xFF2E7D32), Color(0xFFFAFCF4)))
        AppThemeMode.LAVENDER -> Brush.linearGradient(listOf(Color(0xFF8B5CF6), Color(0xFFFCF8FF)))
    }
}

@Composable
fun SettingsSectionTitle(title: String, description: String) {
    val adaptiveUi = rememberAdaptiveUiSize()
    Column(
        modifier = Modifier.padding(
            start = adaptiveUi.screenPadding,
            end = adaptiveUi.screenPadding,
            top = 24.dp,
            bottom = 8.dp
        )
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

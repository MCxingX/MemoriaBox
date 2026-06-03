package com.memoriabox.ui.screen

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.memoriabox.utils.AppSettings

@Composable
fun CustomizationSettingsScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    var homeBg by remember { mutableStateOf(AppSettings.getHomeBgUri(context)) }
    var calendarBg by remember { mutableStateOf(AppSettings.getCalendarBgUri(context)) }
    var todoBg by remember { mutableStateOf(AppSettings.getTodoBgUri(context)) }
    var settingsBg by remember { mutableStateOf(AppSettings.getSettingsBgUri(context)) }
    var allBg by remember {
        mutableStateOf(if (!homeBg.isNullOrBlank() && homeBg == calendarBg && homeBg == todoBg && homeBg == settingsBg) homeBg else null)
    }
    var homeIcon by remember { mutableStateOf(AppSettings.getHomeIconUri(context)) }
    var calendarIcon by remember { mutableStateOf(AppSettings.getCalendarIconUri(context)) }
    var todoIcon by remember { mutableStateOf(AppSettings.getTodoIconUri(context)) }
    var settingsIcon by remember { mutableStateOf(AppSettings.getSettingsIconUri(context)) }

    var targetForPick by remember { mutableStateOf<String?>(null) }
    var customQuote by remember { mutableStateOf(AppSettings.getCustomDailyQuote(context).orEmpty()) }
    var useCustomQuote by remember { mutableStateOf(AppSettings.getUseCustomQuote(context)) }

    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        val target = targetForPick
        targetForPick = null
        uri ?: return@rememberLauncherForActivityResult
        persistReadPermission(context, uri)
        val value = uri.toString()
        when (target) {
            "ALL_BG" -> {
                allBg = value
                homeBg = value
                calendarBg = value
                todoBg = value
                settingsBg = value
                AppSettings.setHomeBgUri(context, value)
                AppSettings.setCalendarBgUri(context, value)
                AppSettings.setTodoBgUri(context, value)
                AppSettings.setSettingsBgUri(context, value)
            }
            "HOME_BG" -> { homeBg = value; AppSettings.setHomeBgUri(context, value) }
            "CALENDAR_BG" -> { calendarBg = value; AppSettings.setCalendarBgUri(context, value) }
            "TODO_BG" -> { todoBg = value; AppSettings.setTodoBgUri(context, value) }
            "SETTINGS_BG" -> { settingsBg = value; AppSettings.setSettingsBgUri(context, value) }
            "HOME_ICON" -> { homeIcon = value; AppSettings.setHomeIconUri(context, value) }
            "CALENDAR_ICON" -> { calendarIcon = value; AppSettings.setCalendarIconUri(context, value) }
            "TODO_ICON" -> { todoIcon = value; AppSettings.setTodoIconUri(context, value) }
            "SETTINGS_ICON" -> { settingsIcon = value; AppSettings.setSettingsIconUri(context, value) }
        }
    }

    fun chooseImage(target: String) {
        targetForPick = target
        pickImage.launch(arrayOf("image/*"))
    }

    Scaffold(topBar = { TopAppBar(title = { Text("个性化设置") }, navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") } }) }) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("页面背景", style = MaterialTheme.typography.titleLarge)
            CustomizationBgCard(title = "全部页面背景", uri = allBg, onClick = { chooseImage("ALL_BG") }, onClear = { allBg = null; homeBg = null; calendarBg = null; todoBg = null; settingsBg = null; AppSettings.setHomeBgUri(context, null); AppSettings.setCalendarBgUri(context, null); AppSettings.setTodoBgUri(context, null); AppSettings.setSettingsBgUri(context, null) })
            CustomizationBgCard(title = "日子首页背景", uri = homeBg, onClick = { chooseImage("HOME_BG") }, onClear = { homeBg = null; AppSettings.setHomeBgUri(context, null) })
            CustomizationBgCard(title = "日历页背景", uri = calendarBg, onClick = { chooseImage("CALENDAR_BG") }, onClear = { calendarBg = null; AppSettings.setCalendarBgUri(context, null) })
            CustomizationBgCard(title = "待办页背景", uri = todoBg, onClick = { chooseImage("TODO_BG") }, onClear = { todoBg = null; AppSettings.setTodoBgUri(context, null) })
            CustomizationBgCard(title = "我的页背景", uri = settingsBg, onClick = { chooseImage("SETTINGS_BG") }, onClear = { settingsBg = null; AppSettings.setSettingsBgUri(context, null) })
            Button(onClick = { AppSettings.setHomeBgUri(context, null); AppSettings.setCalendarBgUri(context, null); AppSettings.setTodoBgUri(context, null); AppSettings.setSettingsBgUri(context, null); allBg = null; homeBg = null; calendarBg = null; todoBg = null; settingsBg = null }, modifier = Modifier.fillMaxWidth()) { Text("全部清除") }

            Text("底部图标", style = MaterialTheme.typography.titleLarge)
            CustomizationBgCard(title = "日子图标", uri = homeIcon, onClick = { chooseImage("HOME_ICON") }, onClear = { homeIcon = null; AppSettings.setHomeIconUri(context, null) })
            CustomizationBgCard(title = "日历图标", uri = calendarIcon, onClick = { chooseImage("CALENDAR_ICON") }, onClear = { calendarIcon = null; AppSettings.setCalendarIconUri(context, null) })
            CustomizationBgCard(title = "待办图标", uri = todoIcon, onClick = { chooseImage("TODO_ICON") }, onClear = { todoIcon = null; AppSettings.setTodoIconUri(context, null) })
            CustomizationBgCard(title = "我的图标", uri = settingsIcon, onClick = { chooseImage("SETTINGS_ICON") }, onClear = { settingsIcon = null; AppSettings.setSettingsIconUri(context, null) })
            Button(onClick = { AppSettings.setHomeIconUri(context, null); AppSettings.setCalendarIconUri(context, null); AppSettings.setTodoIconUri(context, null); AppSettings.setSettingsIconUri(context, null); homeIcon = null; calendarIcon = null; todoIcon = null; settingsIcon = null }, modifier = Modifier.fillMaxWidth()) { Text("恢复默认图标") }

            Text("每日一言", style = MaterialTheme.typography.titleLarge)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("启用自定义固定语录")
                Switch(checked = useCustomQuote, onCheckedChange = { useCustomQuote = it; AppSettings.setUseCustomQuote(context, it) })
            }
            if (useCustomQuote) {
                OutlinedTextField(
                    value = customQuote,
                    onValueChange = { customQuote = it; AppSettings.setCustomDailyQuote(context, it) },
                    label = { Text("输入每日固定语录") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
                Button(onClick = { customQuote = ""; AppSettings.setCustomDailyQuote(context, null); useCustomQuote = false; AppSettings.setUseCustomQuote(context, false) }, modifier = Modifier.fillMaxWidth()) { Text("恢复随机轮换") }
            }
        }
    }
}

private fun persistReadPermission(context: Context, uri: Uri) {
    try {
        context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
    } catch (_: SecurityException) {
    }
}

@Composable
private fun CustomizationBgCard(title: String, uri: String?, onClick: () -> Unit, onClear: () -> Unit) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(title, style = MaterialTheme.typography.bodyLarge)
                if (uri != null) Text("已设置", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall) else Text("未设置", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (uri != null) {
                Box(modifier = Modifier.fillMaxWidth().height(100.dp)) {
                    AsyncImage(model = uri, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                }
                TextButton(onClick = onClear, modifier = Modifier.align(Alignment.End)) { Text("移除这张") }
            }
        }
    }
}

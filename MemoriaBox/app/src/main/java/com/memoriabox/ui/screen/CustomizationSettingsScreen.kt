package com.memoriabox.ui.screen

import android.app.Application
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
fun CustomizationSettingsScreen(application: Application, onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    var homeBg by remember { mutableStateOf(AppSettings.getHomeBgUri(context)) }
    var calendarBg by remember { mutableStateOf(AppSettings.getCalendarBgUri(context)) }
    var todoBg by remember { mutableStateOf(AppSettings.getTodoBgUri(context)) }
    var settingsBg by remember { mutableStateOf(AppSettings.getSettingsBgUri(context)) }

    var targetForPick by remember { mutableStateOf<String?>(null) }
    var customQuote by remember { mutableStateOf(AppSettings.getCustomDailyQuote(context).orEmpty()) }
    var useCustomQuote by remember { mutableStateOf(AppSettings.getUseCustomQuote(context)) }

    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        when (targetForPick) {
            "HOME" -> { homeBg = uri.toString(); AppSettings.setHomeBgUri(context, uri.toString()) }
            "CALENDAR" -> { calendarBg = uri.toString(); AppSettings.setCalendarBgUri(context, uri.toString()) }
            "TODO" -> { todoBg = uri.toString(); AppSettings.setTodoBgUri(context, uri.toString()) }
            "SETTINGS" -> { settingsBg = uri.toString(); AppSettings.setSettingsBgUri(context, uri.toString()) }
        }
    }

    LaunchedEffect(targetForPick) {
        if (targetForPick != null) {
            pickImage.launch("image/*")
        }
    }

    Scaffold(topBar = { TopAppBar(title = { Text("个性化设置") }, navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, "返回") } }) }) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("页面背景", style = MaterialTheme.typography.titleLarge)
            CustomizationBgCard(title = "日子首页背景", uri = homeBg, onClick = { targetForPick = "HOME" }, onClear = { homeBg = null; AppSettings.setHomeBgUri(context, null) })
            CustomizationBgCard(title = "日历页背景", uri = calendarBg, onClick = { targetForPick = "CALENDAR" }, onClear = { calendarBg = null; AppSettings.setCalendarBgUri(context, null) })
            CustomizationBgCard(title = "待办页背景", uri = todoBg, onClick = { targetForPick = "TODO" }, onClear = { todoBg = null; AppSettings.setTodoBgUri(context, null) })
            CustomizationBgCard(title = "我的页背景", uri = settingsBg, onClick = { targetForPick = "SETTINGS" }, onClear = { settingsBg = null; AppSettings.setSettingsBgUri(context, null) })
            Button(onClick = { AppSettings.setHomeBgUri(context, null); AppSettings.setCalendarBgUri(context, null); AppSettings.setTodoBgUri(context, null); AppSettings.setSettingsBgUri(context, null); homeBg = null; calendarBg = null; todoBg = null; settingsBg = null }, modifier = Modifier.fillMaxWidth()) { Text("全部清除") }

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
                Button(onClick = { customQuote = ""; AppSettings.setCustomDailyQuote(context, null); useCustomQuote = false }, modifier = Modifier.fillMaxWidth()) { Text("恢复随机轮换") }
            }
        }
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

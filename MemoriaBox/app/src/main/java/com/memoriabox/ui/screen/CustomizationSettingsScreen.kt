package com.memoriabox.ui.screen

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
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
import com.memoriabox.ui.utils.rememberAdaptiveUiSize
import com.memoriabox.ui.screen.dialogs.EventImageCropDialog
import com.memoriabox.utils.AppSettings
import com.memoriabox.utils.ImageImportUtils

@Composable
fun CustomizationSettingsScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val adaptiveUi = rememberAdaptiveUiSize()
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
    var pendingCropUri by remember { mutableStateOf<Uri?>(null) }
    var pendingEditState by remember { mutableStateOf<ImageImportUtils.EditState?>(null) }
    var customQuotes by remember { mutableStateOf(AppSettings.getCustomDailyQuotes(context)) }
    var useCustomQuote by remember { mutableStateOf(AppSettings.getUseCustomQuote(context)) }
    var quoteDraft by remember { mutableStateOf("") }
    var editingQuoteIndex by remember { mutableStateOf<Int?>(null) }
    var showQuoteEditor by remember { mutableStateOf(false) }
    var quoteToDelete by remember { mutableStateOf<Int?>(null) }
    var showClearAllBgConfirm by remember { mutableStateOf(false) }
    var showResetIconsConfirm by remember { mutableStateOf(false) }

    fun updateAllBgState() {
        allBg = if (!homeBg.isNullOrBlank() && homeBg == calendarBg && homeBg == todoBg && homeBg == settingsBg) {
            homeBg
        } else {
            null
        }
    }

    fun applyPickedImage(value: String, target: String?) {
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
        if (target?.endsWith("_BG") == true) updateAllBgState()
    }

    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri == null || targetForPick == null) {
            targetForPick = null
        } else {
            val sourceUri = ImageImportUtils.saveOriginalImage(context, uri)?.let(Uri::parse) ?: uri
            pendingCropUri = sourceUri
            pendingEditState = ImageImportUtils.EditState(sourceUri.toString())
        }
    }

    fun chooseImage(target: String) {
        targetForPick = target
        val current = when (target) {
            "ALL_BG" -> allBg
            "HOME_BG" -> homeBg
            "CALENDAR_BG" -> calendarBg
            "TODO_BG" -> todoBg
            "SETTINGS_BG" -> settingsBg
            "HOME_ICON" -> homeIcon
            "CALENDAR_ICON" -> calendarIcon
            "TODO_ICON" -> todoIcon
            "SETTINGS_ICON" -> settingsIcon
            else -> null
        }
        if (!current.isNullOrBlank()) {
            val editState = ImageImportUtils.getEditState(context, current)
            pendingEditState = editState
            pendingCropUri = Uri.parse(editState.sourceUri)
        } else {
            pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
    }

    Scaffold(topBar = { TopAppBar(modifier = Modifier.height(adaptiveUi.topBarHeight), windowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp), title = { Text("个性化设置") }, navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") } }) }) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues).fillMaxSize().verticalScroll(rememberScrollState()).padding(adaptiveUi.screenPadding), verticalArrangement = Arrangement.spacedBy(adaptiveUi.sectionSpacing + 4.dp)) {
            Text("页面背景", style = MaterialTheme.typography.titleLarge)
            CustomizationBgCard(title = "全部页面背景", uri = allBg, onClick = { chooseImage("ALL_BG") }, onClear = { allBg?.let { ImageImportUtils.removeEditState(context, it) }; allBg = null; homeBg = null; calendarBg = null; todoBg = null; settingsBg = null; AppSettings.setHomeBgUri(context, null); AppSettings.setCalendarBgUri(context, null); AppSettings.setTodoBgUri(context, null); AppSettings.setSettingsBgUri(context, null) })
            CustomizationBgCard(title = "日子首页背景", uri = homeBg, onClick = { chooseImage("HOME_BG") }, onClear = { ImageImportUtils.removeEditState(context, homeBg); homeBg = null; AppSettings.setHomeBgUri(context, null); updateAllBgState() })
            CustomizationBgCard(title = "日历页背景", uri = calendarBg, onClick = { chooseImage("CALENDAR_BG") }, onClear = { ImageImportUtils.removeEditState(context, calendarBg); calendarBg = null; AppSettings.setCalendarBgUri(context, null); updateAllBgState() })
            CustomizationBgCard(title = "待办页背景", uri = todoBg, onClick = { chooseImage("TODO_BG") }, onClear = { ImageImportUtils.removeEditState(context, todoBg); todoBg = null; AppSettings.setTodoBgUri(context, null); updateAllBgState() })
            CustomizationBgCard(title = "我的页背景", uri = settingsBg, onClick = { chooseImage("SETTINGS_BG") }, onClear = { ImageImportUtils.removeEditState(context, settingsBg); settingsBg = null; AppSettings.setSettingsBgUri(context, null); updateAllBgState() })
            Button(onClick = { showClearAllBgConfirm = true }, modifier = Modifier.fillMaxWidth()) { Text("全部清除") }

            Text("底部图标", style = MaterialTheme.typography.titleLarge)
            IconCustomizationGrid(
                items = listOf(
                    IconCustomizationItem("日子图标", homeIcon, { chooseImage("HOME_ICON") }, { ImageImportUtils.removeEditState(context, homeIcon); homeIcon = null; AppSettings.setHomeIconUri(context, null) }),
                    IconCustomizationItem("日历图标", calendarIcon, { chooseImage("CALENDAR_ICON") }, { ImageImportUtils.removeEditState(context, calendarIcon); calendarIcon = null; AppSettings.setCalendarIconUri(context, null) }),
                    IconCustomizationItem("待办图标", todoIcon, { chooseImage("TODO_ICON") }, { ImageImportUtils.removeEditState(context, todoIcon); todoIcon = null; AppSettings.setTodoIconUri(context, null) }),
                    IconCustomizationItem("我的图标", settingsIcon, { chooseImage("SETTINGS_ICON") }, { ImageImportUtils.removeEditState(context, settingsIcon); settingsIcon = null; AppSettings.setSettingsIconUri(context, null) })
                )
            )
            Button(onClick = { showResetIconsConfirm = true }, modifier = Modifier.fillMaxWidth()) { Text("恢复默认图标") }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("每日一言", style = MaterialTheme.typography.titleLarge)
                FilledTonalButton(
                    onClick = {
                        useCustomQuote = true
                        AppSettings.setUseCustomQuote(context, true)
                        editingQuoteIndex = null
                        quoteDraft = ""
                        showQuoteEditor = true
                    }
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("新增")
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("启用我的语录轮放")
                    Text("每次新增会单独保存为一条，首页按日期轮流展示", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = useCustomQuote, onCheckedChange = { useCustomQuote = it; AppSettings.setUseCustomQuote(context, it) })
            }
            if (useCustomQuote) {
                if (customQuotes.isEmpty()) {
                    Text("还没有自定义语录，添加后首页会优先轮放你的句子。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                customQuotes.forEachIndexed { index, quote ->
                    QuoteEditCard(
                        quote = quote,
                        onEdit = {
                            editingQuoteIndex = index
                            quoteDraft = quote
                            showQuoteEditor = true
                        },
                        onDelete = {
                            quoteToDelete = index
                        }
                    )
                }
                OutlinedButton(
                    onClick = {
                        useCustomQuote = false
                        AppSettings.setUseCustomQuote(context, false)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("使用内置随机轮换") }
            }
        }
    }

    if (showQuoteEditor) {
        AlertDialog(
            onDismissRequest = { showQuoteEditor = false },
            title = { Text(if (editingQuoteIndex == null) "新增每日一言" else "编辑每日一言") },
            text = {
                OutlinedTextField(
                    value = quoteDraft,
                    onValueChange = { quoteDraft = it },
                    label = { Text("单条句子内容") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 8
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val value = quoteDraft.trim()
                        if (value.isNotBlank()) {
                            customQuotes = customQuotes.toMutableList().apply {
                                val editIndex = editingQuoteIndex
                                if (editIndex == null) add(value) else set(editIndex, value)
                            }
                            AppSettings.setCustomDailyQuotes(context, customQuotes)
                            useCustomQuote = true
                            AppSettings.setUseCustomQuote(context, true)
                            showQuoteEditor = false
                            quoteDraft = ""
                            editingQuoteIndex = null
                        }
                    },
                    enabled = quoteDraft.isNotBlank()
                ) { Text("保存") }
            },
            dismissButton = {
                TextButton(onClick = { showQuoteEditor = false }) { Text("取消") }
            }
        )
    }

    quoteToDelete?.let { index ->
        AlertDialog(
            onDismissRequest = { quoteToDelete = null },
            title = { Text("删除语录") },
            text = { Text("确定要删除这条语录吗？此操作不可恢复。") },
            confirmButton = {
                TextButton(onClick = {
                    customQuotes = customQuotes.toMutableList().also { it.removeAt(index) }
                    AppSettings.setCustomDailyQuotes(context, customQuotes)
                    quoteToDelete = null
                }) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { quoteToDelete = null }) { Text("取消") }
            }
        )
    }

    if (showClearAllBgConfirm) {
        AlertDialog(
            onDismissRequest = { showClearAllBgConfirm = false },
            title = { Text("全部清除") },
            text = { Text("确定要清除所有页面的背景设置吗？此操作不可恢复。") },
            confirmButton = {
                TextButton(onClick = {
                    ImageImportUtils.removeEditState(context, homeBg)
                    ImageImportUtils.removeEditState(context, calendarBg)
                    ImageImportUtils.removeEditState(context, todoBg)
                    ImageImportUtils.removeEditState(context, settingsBg)
                    AppSettings.setHomeBgUri(context, null)
                    AppSettings.setCalendarBgUri(context, null)
                    AppSettings.setTodoBgUri(context, null)
                    AppSettings.setSettingsBgUri(context, null)
                    allBg = null
                    homeBg = null
                    calendarBg = null
                    todoBg = null
                    settingsBg = null
                    showClearAllBgConfirm = false
                }) { Text("清除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllBgConfirm = false }) { Text("取消") }
            }
        )
    }

    if (showResetIconsConfirm) {
        AlertDialog(
            onDismissRequest = { showResetIconsConfirm = false },
            title = { Text("恢复默认图标") },
            text = { Text("确定要恢复所有页面底部图标的默认设置吗？") },
            confirmButton = {
                TextButton(onClick = {
                    AppSettings.setHomeIconUri(context, null)
                    AppSettings.setCalendarIconUri(context, null)
                    AppSettings.setTodoIconUri(context, null)
                    AppSettings.setSettingsIconUri(context, null)
                    homeIcon = null
                    calendarIcon = null
                    todoIcon = null
                    settingsIcon = null
                    showResetIconsConfirm = false
                }) { Text("恢复", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showResetIconsConfirm = false }) { Text("取消") }
            }
        )
    }

    pendingCropUri?.let { uri ->
        val target = targetForPick
        if (target != null) {
            val isIcon = target.endsWith("_ICON")
            val cropRatio = if (isIcon) 1f else 9f / 16f
            EventImageCropDialog(
                sourceUri = uri,
                cropAspectRatio = cropRatio,
                displayLabel = if (isIcon) "底部图标" else "页面背景",
                initialState = pendingEditState,
                onDismiss = {
                    pendingCropUri = null
                    pendingEditState = null
                    targetForPick = null
                },
                onSave = { left, top, width, height ->
                    val value = ImageImportUtils.cropImageToPrivateStorage(
                        context, uri, "customization_images", left, top, width, height
                    ) ?: ImageImportUtils.copyImageToPrivateStorage(context, uri, "customization_images") ?: uri.toString()
                    applyPickedImage(value, target)
                    ImageImportUtils.saveEditState(context, value, ImageImportUtils.EditState(uri.toString(), left, top, width, height))
                    pendingCropUri = null
                    pendingEditState = null
                    targetForPick = null
                }
            )
        }
    }
}

private data class IconCustomizationItem(
    val title: String,
    val uri: String?,
    val onClick: () -> Unit,
    val onClear: () -> Unit
)

@Composable
private fun IconCustomizationGrid(items: List<IconCustomizationItem>) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items.chunked(2).forEach { rowItems ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                rowItems.forEach { item ->
                    IconCustomizationCard(
                        title = item.title,
                        uri = item.uri,
                        onClick = item.onClick,
                        onClear = item.onClear,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (rowItems.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun IconCustomizationCard(title: String, uri: String?, onClick: () -> Unit, onClear: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedCard(
        modifier = modifier.clickable(onClick = onClick),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.84f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.52f), MaterialTheme.shapes.large),
                contentAlignment = Alignment.Center
            ) {
                if (uri.isNullOrBlank()) {
                    Icon(Icons.Default.Image, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                } else {
                    AsyncImage(model = uri, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                }
            }
            Text(title, style = MaterialTheme.typography.bodyMedium)
            Text(if (uri.isNullOrBlank()) "点击上传" else "已设置", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (!uri.isNullOrBlank()) {
                TextButton(onClick = onClear) { Text("移除") }
            }
        }
    }
}

@Composable
private fun CustomizationBgCard(title: String, uri: String?, onClick: () -> Unit, onClear: () -> Unit) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.84f)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
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

@Composable
private fun QuoteEditCard(quote: String, onEdit: () -> Unit, onDelete: () -> Unit) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.84f))
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(quote, style = MaterialTheme.typography.bodyMedium)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
                TextButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("编辑")
                }
                TextButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("删除")
                }
            }
        }
    }
}

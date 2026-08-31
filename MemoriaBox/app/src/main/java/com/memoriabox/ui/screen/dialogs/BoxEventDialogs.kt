package com.memoriabox.ui.screen.dialogs

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.memoriabox.data.model.BgType
import com.memoriabox.data.model.Box
import com.memoriabox.data.model.Event
import com.memoriabox.data.model.EventType
import com.memoriabox.data.model.RepeatMode
import com.memoriabox.utils.ColorUtils
import com.memoriabox.utils.ImageImportUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.memoriabox.utils.LunarDateUtils
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

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
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf(existingBox?.name ?: "") }
    var selectedIcon by remember { mutableStateOf(existingBox?.icon ?: "\uD83D\uDCE6") }
    var selectedBgType by remember { mutableStateOf(existingBox?.bgType ?: BgType.COLOR) }
    var selectedBgValue by remember { mutableStateOf(existingBox?.bgValue ?: "#7C4DFF") }
    var showEmojiPicker by remember { mutableStateOf(false) }
    var showColorPicker by remember { mutableStateOf(false) }
    val iconCropLauncher = com.memoriabox.utils.UCropHelper.rememberCropLauncher("box_icons") { result ->
        selectedIcon = result ?: selectedIcon
    }
    val iconImagePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch(Dispatchers.IO) {
            val sourceUri = ImageImportUtils.saveOriginalImage(context, uri)?.let(Uri::parse) ?: uri
            iconCropLauncher(sourceUri, 1f)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existingBox == null) "创建分类" else "编辑分类") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("分类名称") },
                    modifier = Modifier.fillMaxWidth()
                )

                if (existingBox != null) {
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
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(0.2f))
                                .clickable { showEmojiPicker = true }
                                .semantics { contentDescription = "选择图标" },
                            contentAlignment = Alignment.Center
                        ) {
                            if (selectedIcon.isImageUri()) {
                                AsyncImage(
                                    model = selectedIcon,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Text(selectedIcon, style = MaterialTheme.typography.headlineMedium)
                            }
                        }
                        Text(if (selectedIcon.isImageUri()) "自定义图片" else selectedIcon, style = MaterialTheme.typography.bodySmall)
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
                                .semantics { contentDescription = "选择背景" }
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
                        onClick = {
                            if (selectedIcon.isImageUri()) {
                                val editState = ImageImportUtils.getEditState(context, selectedIcon)
                                iconCropLauncher(Uri.parse(editState.sourceUri), 1f)
                            } else {
                                iconImagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("上传图片")
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

private fun String.isImageUri(): Boolean = startsWith("content://") || startsWith("file://")

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
                        textAlign = TextAlign.Center
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
@OptIn(ExperimentalLayoutApi::class)
fun EventDialog(
    existingEvent: Event? = null,
    availableBoxes: List<Box>,
    defaultBoxId: String? = null,
    defaultType: EventType = EventType.COUNTDOWN,
    defaultDate: Long? = null,
    defaultReminderEnabled: Boolean = false,
    defaultPushPlusEnabled: Boolean = false,
    allowTypeChange: Boolean = true,
    showFixedTypeLabel: Boolean = true,
    onPushPlusEnabledChange: (Boolean) -> Unit = {},
    onDismiss: () -> Unit,
    onSave: (Event) -> Unit
) {
    var name by remember { mutableStateOf(existingEvent?.name ?: "") }
    var selectedDate by remember { mutableStateOf(existingEvent?.date ?: defaultDate) }
    var selectedType by remember { mutableStateOf(existingEvent?.type ?: defaultType) }
    var note by remember { mutableStateOf(existingEvent?.note ?: "") }
    var reminderEnabled by remember { mutableStateOf(existingEvent?.reminderEnabled ?: defaultReminderEnabled) }
    var pushPlusEnabled by remember { mutableStateOf(existingEvent?.pushPlusEnabled ?: defaultPushPlusEnabled) }
    var calendarSyncEnabled by remember { mutableStateOf(existingEvent?.calendarSyncEnabled ?: false) }
    var backgroundUri by remember { mutableStateOf(existingEvent?.avatarUri) }
    var repeatMode by remember {
        mutableStateOf(
            when {
                existingEvent?.repeatMode != null && existingEvent.repeatMode != RepeatMode.NONE -> existingEvent.repeatMode
                existingEvent?.repeatYearly == true -> RepeatMode.YEARLY
                existingEvent?.type == EventType.BIRTHDAY -> RepeatMode.YEARLY
                else -> RepeatMode.NONE
            }
        )
    }
    var repeatInterval by remember { mutableIntStateOf(existingEvent?.repeatInterval ?: 1) }
    var gradientStart by remember { mutableStateOf(existingEvent?.gradientStart ?: "#7C4DFF") }
    var gradientEnd by remember { mutableStateOf(existingEvent?.gradientEnd ?: "#FF8A80") }
    var textColor by remember { mutableStateOf(existingEvent?.textColor ?: "#FFFFFF") }
    var cardTemplate by remember { mutableStateOf(existingEvent?.cardTemplate ?: "HERO") }
    val displayFieldSet = remember(existingEvent?.id) {
        mutableStateMapOf(
            "date" to (existingEvent?.displayFields?.contains("date") ?: true),
            "note" to (existingEvent?.displayFields?.contains("note") ?: true),
            "lunar" to (existingEvent?.displayFields?.contains("lunar") ?: true),
            "reminder" to (existingEvent?.displayFields?.contains("reminder") ?: true)
        )
    }
    var reminderDays by remember { mutableIntStateOf(existingEvent?.reminderDays ?: 1) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showLunarCalendar by remember { mutableStateOf(false) }
    var selectedLunar by remember { mutableStateOf(existingEvent?.lunar) }
    val cardCropRatio = cardCropAspectRatio(cardTemplate)
    val eventCropLauncher = com.memoriabox.utils.UCropHelper.rememberCropLauncher("event_images") { result ->
        backgroundUri = result ?: backgroundUri
    }
    var selectedBoxId by remember { mutableStateOf(existingEvent?.boxId ?: defaultBoxId ?: (availableBoxes.firstOrNull()?.id ?: "")) }
    var showColorPickerFor by remember { mutableStateOf<String?>(null) }
    var reminderOffsetsText by remember { mutableStateOf(existingEvent?.reminderOffsets ?: (existingEvent?.reminderDays ?: 1).toString()) }
    var repeatCountText by remember { mutableStateOf(existingEvent?.repeatCount?.takeIf { it > 0 }?.toString() ?: "") }
    var repeatEndDate by remember { mutableStateOf(existingEvent?.repeatEndDate) }
    var showRepeatEndPicker by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            scope.launch(Dispatchers.IO) {
                val sourceUri = ImageImportUtils.saveOriginalImage(context, uri)?.let(Uri::parse) ?: uri
                eventCropLauncher(sourceUri, cardCropRatio)
            }
        }
    }

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
                        Text(
                            selectedLunar ?: "选择农历",
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (availableBoxes.isNotEmpty()) {
                    Text("所属分类", style = MaterialTheme.typography.labelLarge)
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        availableBoxes.forEach { box ->
                            FilterChip(
                                selected = selectedBoxId == box.id,
                                onClick = { selectedBoxId = box.id },
                                label = { Text(box.name) },
                                leadingIcon = {
                                    Text(box.icon.takeIf { it.isNotBlank() } ?: "•")
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                }

                if (allowTypeChange) {
                    Text("事件类型", style = MaterialTheme.typography.labelLarge)
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        EventType.entries.forEach { type ->
                            FilterChip(
                                selected = selectedType == type,
                                onClick = {
                                    selectedType = type
                                    if (type == EventType.BIRTHDAY) repeatMode = RepeatMode.YEARLY
                                },
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
                                }
                            )
                        }
                    }
                } else if (showFixedTypeLabel) {
                    AssistChip(
                        onClick = {},
                        label = {
                            Text(
                                when (selectedType) {
                                    EventType.COUNTDOWN -> "正在添加：倒数日"
                                    EventType.ANNIVERSARY -> "正在添加：纪念日"
                                    EventType.ELAPSED -> "正在添加：正计时"
                                    EventType.BIRTHDAY -> "正在添加：生日"
                                    EventType.TODO -> "正在添加：待办"
                                }
                            )
                        }
                    )
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

                Text("卡片背景", style = MaterialTheme.typography.labelLarge)
                Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .clickable {
                                if (!backgroundUri.isNullOrBlank()) {
                                    val displayUri = backgroundUri.orEmpty()
                                    val editState = runCatching {
                                        ImageImportUtils.getEditState(context, displayUri)
                                    }.getOrElse {
                                        ImageImportUtils.EditState(displayUri)
                                    }
                                    val sourceStr = editState.sourceUri.trim().ifBlank { displayUri }
                                    val sourceExists = runCatching {
                                        val u = Uri.parse(sourceStr)
                                        when (u.scheme) {
                                            "file" -> java.io.File(u.path.orEmpty()).exists()
                                            else -> context.contentResolver.openInputStream(u)?.use { true } ?: false
                                        }
                                    }.getOrDefault(false)
                                    if (sourceExists) {
                                        eventCropLauncher(Uri.parse(sourceStr), cardCropRatio)
                                    } else {
                                        val displayExists = runCatching {
                                            val u = Uri.parse(displayUri)
                                            when (u.scheme) {
                                                "file" -> java.io.File(u.path.orEmpty()).exists()
                                                else -> context.contentResolver.openInputStream(u)?.use { true } ?: false
                                            }
                                        }.getOrDefault(false)
                                        if (displayExists) {
                                            eventCropLauncher(Uri.parse(displayUri), cardCropRatio)
                                        } else {
                                            imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                        }
                                    }
                                } else {
                                    imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                }
                            },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        if (backgroundUri != null) {
                            AsyncImage(
                                model = backgroundUri,
                                contentDescription = "卡片背景",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            Surface(
                                modifier = Modifier.align(Alignment.BottomEnd).padding(6.dp),
                                color = Color.Black.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
                                    Text("点击编辑", color = Color.White, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Image, contentDescription = null)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("选择背景图", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("文字排版", style = MaterialTheme.typography.titleSmall)
                                Text(
                                    when (cardTemplate) {
                                        "POSTER" -> "海报"
                                        "GLASS" -> "紧凑"
                                        "SPLIT" -> "分栏"
                                        "NEON" -> "光轨"
                                        "MINIMAL" -> "徽章"
                                        else -> "封面"
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Text("6 种布局", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        CardTemplatePreview(
                            template = cardTemplate,
                            name = name.ifBlank { "纪念日预览" },
                            dateText = selectedDate?.let { formatDate(it) } ?: "选择日期",
                            backgroundUri = backgroundUri,
                            gradientStart = gradientStart,
                            gradientEnd = gradientEnd,
                            textColor = textColor
                        )
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                "HERO" to "封面",
                                "POSTER" to "海报",
                                "GLASS" to "紧凑",
                                "SPLIT" to "分栏",
                                "NEON" to "光轨",
                                "MINIMAL" to "徽章"
                            ).forEach { (template, label) ->
                                FilterChip(
                                    selected = cardTemplate == template,
                                    onClick = { cardTemplate = template },
                                    label = { Text(label) }
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    ColorSelectButton("起色", gradientStart, { showColorPickerFor = "start" }, Modifier.weight(1f))
                    ColorSelectButton("止色", gradientEnd, { showColorPickerFor = "end" }, Modifier.weight(1f))
                }
                Spacer(modifier = Modifier.height(8.dp))
                ColorSelectButton("字体颜色", textColor, { showColorPickerFor = "text" }, Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                Text("展示字段", style = MaterialTheme.typography.labelMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("date" to "日期", "note" to "备注", "lunar" to "农历", "reminder" to "提醒").forEach { (key, label) ->
                        FilterChip(
                            selected = displayFieldSet[key] == true,
                            onClick = { displayFieldSet[key] = displayFieldSet[key] != true },
                            label = { Text(label) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (selectedType != EventType.BIRTHDAY) {
                    Text("重复规则", style = MaterialTheme.typography.labelLarge)
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            RepeatMode.NONE to "不重复",
                            RepeatMode.CUSTOM_DAYS to "每日/按天",
                            RepeatMode.CUSTOM_WEEKS to "每周/按周",
                            RepeatMode.MONTHLY to "每月",
                            RepeatMode.YEARLY to "每年",
                            RepeatMode.CUSTOM_MONTHS to "按月数"
                        ).forEach { (mode, label) ->
                            FilterChip(
                                selected = repeatMode == mode,
                                onClick = {
                                    repeatMode = mode
                                    if (mode == RepeatMode.CUSTOM_DAYS || mode == RepeatMode.CUSTOM_WEEKS) repeatInterval = 1
                                },
                                label = { Text(label) }
                            )
                        }
                    }
                    if (repeatMode in listOf(RepeatMode.CUSTOM_DAYS, RepeatMode.CUSTOM_WEEKS, RepeatMode.CUSTOM_MONTHS)) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("每", style = MaterialTheme.typography.bodySmall)
                            OutlinedTextField(
                                value = repeatInterval.toString(),
                                onValueChange = { repeatInterval = it.toIntOrNull()?.coerceIn(1, 365) ?: 1 },
                                modifier = Modifier.width(88.dp),
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                when (repeatMode) {
                                    RepeatMode.CUSTOM_DAYS -> "天重复"
                                    RepeatMode.CUSTOM_WEEKS -> "周重复"
                                    else -> "个月重复"
                                },
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    if (repeatMode != RepeatMode.NONE) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = repeatCountText,
                                onValueChange = { repeatCountText = it.filter { char -> char.isDigit() } },
                                label = { Text("重复次数") },
                                placeholder = { Text("留空为不限") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedButton(
                                onClick = { showRepeatEndPicker = true },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(repeatEndDate?.let { formatDate(it) } ?: "结束日期")
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { reminderEnabled = !reminderEnabled }
                    ) {
                        Checkbox(
                            checked = reminderEnabled,
                            onCheckedChange = { reminderEnabled = it }
                        )
                        Text("开启提醒")
                    }

                    if (reminderEnabled) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("提前", style = MaterialTheme.typography.bodySmall)
                            OutlinedTextField(
                                value = reminderDays.toString(),
                                onValueChange = {
                                    reminderDays = it.toIntOrNull()?.coerceIn(0, 365) ?: 1
                                },
                                modifier = Modifier.width(88.dp),
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodySmall
                            )
                            Text("天提醒", style = MaterialTheme.typography.bodySmall)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = reminderOffsetsText,
                            onValueChange = { reminderOffsetsText = it.filter { char -> char.isDigit() || char == ',' } },
                            label = { Text("多提醒点") },
                            placeholder = { Text("例如 0,1,3,7") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(
                                "当天" to 0,
                                "提前1天" to 1,
                                "提前3天" to 3,
                                "提前7天" to 7,
                                "提前30天" to 30
                            ).forEach { (label, offset) ->
                                val offsets = reminderOffsetsText.split(",").mapNotNull { it.trim().toIntOrNull() }.toSet()
                                FilterChip(
                                    selected = offset in offsets,
                                    onClick = {
                                        val updated = if (offset in offsets) offsets - offset else offsets + offset
                                        reminderOffsetsText = updated.sorted().joinToString(",")
                                    },
                                    label = { Text(label) }
                                )
                            }
                        }
                        Text(
                            "0 表示当天提醒，多个提醒点用英文逗号分隔",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { pushPlusEnabled = !pushPlusEnabled }
                        ) {
                            Checkbox(
                                checked = pushPlusEnabled,
                                onCheckedChange = { pushPlusEnabled = it }
                            )
                            Column {
                                Text("同步 PushPlus 推送")
                                Text(
                                    "需要在我的页面填写 PushPlus Token",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { calendarSyncEnabled = !calendarSyncEnabled }
                        ) {
                            Checkbox(
                                checked = calendarSyncEnabled,
                                onCheckedChange = { calendarSyncEnabled = it }
                            )
                            Column {
                                Text("写入系统日历")
                                Text(
                                    "开启后保存时尝试同步到系统日历，软件通知仍会兜底提醒",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
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
                        reminderOffsets = reminderOffsetsText.split(",").mapNotNull { it.trim().toIntOrNull() }.filter { it in 0..365 }.distinct().joinToString(",").ifBlank { reminderDays.toString() },
                        avatarUri = backgroundUri,
                        isPinned = existingEvent?.isPinned ?: false,
                        pushPlusEnabled = pushPlusEnabled && reminderEnabled,
                        calendarSyncEnabled = calendarSyncEnabled && reminderEnabled,
                        repeatMode = if (selectedType == EventType.BIRTHDAY) RepeatMode.YEARLY else repeatMode,
                        repeatInterval = repeatInterval.coerceAtLeast(1),
                        repeatEndDate = repeatEndDate,
                        repeatCount = repeatCountText.toIntOrNull()?.coerceAtLeast(0) ?: 0,
                        gradientStart = gradientStart,
                        gradientEnd = gradientEnd,
                        textColor = textColor,
                        cardTemplate = cardTemplate,
                        displayFields = displayFieldSet.filterValues { it }.keys.joinToString(","),
                        isBirthday = selectedType == EventType.BIRTHDAY,
                        repeatYearly = selectedType == EventType.BIRTHDAY || repeatMode == RepeatMode.YEARLY,
                        createdAt = existingEvent?.createdAt ?: System.currentTimeMillis()
                    )
                    if (pushPlusEnabled && reminderEnabled) {
                        onPushPlusEnabledChange(true)
                    }
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
            },
            initialDateMillis = selectedDate ?: System.currentTimeMillis()
        )
    }

    if (showRepeatEndPicker) {
        DatePickerDialog(
            onDismiss = { showRepeatEndPicker = false },
            onDateSelected = {
                repeatEndDate = it
                showRepeatEndPicker = false
            },
            initialDateMillis = repeatEndDate ?: selectedDate ?: System.currentTimeMillis()
        )
    }

    if (showLunarCalendar) {
        LunarCalendarDialog(
            onDismiss = { showLunarCalendar = false },
            initialLunar = selectedLunar,
            initialDateMillis = selectedDate,
            onSelected = { lunar, gregorianDate ->
                selectedLunar = lunar
                selectedDate = gregorianDate
                if (selectedType == EventType.BIRTHDAY) repeatMode = RepeatMode.YEARLY
                showLunarCalendar = false
            }
        )
    }

    showColorPickerFor?.let { target ->
        ColorPickerDialog(
            initialColor = when (target) {
                "start" -> gradientStart
                "end" -> gradientEnd
                else -> textColor
            },
            onDismiss = { showColorPickerFor = null },
            onSelected = { color ->
                when (target) {
                    "start" -> gradientStart = color
                    "end" -> gradientEnd = color
                    else -> textColor = color
                }
                showColorPickerFor = null
            }
        )
    }
}

private fun cardCropAspectRatio(template: String): Float = when (template) {
    "SOFT_GLASS" -> 1.02f
    "POSTER" -> 0.78f
    "GLASS" -> 1.27f
    "SPLIT" -> 0.95f
    "NEON" -> 0.98f
    "MINIMAL" -> 1.16f
    else -> 1.02f
}


@Composable
fun ColorSelectButton(label: String, color: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(onClick = onClick, modifier = modifier.height(56.dp)) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(safeDialogColor(color))
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text("$label $color", maxLines = 1)
    }
}

@Composable
private fun CardTemplatePreview(
    template: String,
    name: String,
    dateText: String,
    backgroundUri: String?,
    gradientStart: String,
    gradientEnd: String,
    textColor: String
) {
    val foreground = safeDialogColor(textColor)
    val glassPanel = Color.White.copy(alpha = 0.18f)
    val glassShape = RoundedCornerShape(14.dp)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(156.dp),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(0.8.dp, Color.White.copy(alpha = 0.30f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(20.dp))
        ) {
            if (!backgroundUri.isNullOrBlank()) {
                AsyncImage(
                    model = backgroundUri,
                    contentDescription = "样式预览",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize()
                )
                Box(modifier = Modifier.matchParentSize().background(Color.Black.copy(alpha = 0.28f)))
            } else {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(Brush.linearGradient(listOf(safeDialogColor(gradientStart), safeDialogColor(gradientEnd))))
                )
            }
            when (template) {
                "SPLIT" -> Row(
                    modifier = Modifier.fillMaxSize().padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(shape = RoundedCornerShape(14.dp), color = glassPanel, modifier = Modifier.size(52.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("99", color = foreground, style = MaterialTheme.typography.titleLarge)
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(name, color = foreground, style = MaterialTheme.typography.titleSmall, maxLines = 1)
                        Text(dateText, color = foreground.copy(alpha = 0.80f), style = MaterialTheme.typography.labelSmall, maxLines = 1)
                    }
                }
                "NEON" -> {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .fillMaxHeight()
                            .width(6.dp)
                            .background(Brush.verticalGradient(listOf(safeDialogColor(gradientStart), safeDialogColor(gradientEnd))))
                    )
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(start = 18.dp, bottom = 12.dp, end = 12.dp)
                    ) {
                        Text("99 天", color = foreground, style = MaterialTheme.typography.titleLarge)
                        Text(name, color = foreground, style = MaterialTheme.typography.labelLarge, maxLines = 1)
                    }
                }
                "MINIMAL" -> Column(
                    modifier = Modifier.align(Alignment.CenterStart).padding(14.dp)
                ) {
                    Surface(color = glassPanel, shape = RoundedCornerShape(999.dp)) {
                        Text("99 天", color = foreground, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(name, color = foreground, style = MaterialTheme.typography.titleSmall, maxLines = 2)
                }
                "POSTER" -> Column(
                    modifier = Modifier.fillMaxSize().padding(12.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(dateText, color = foreground.copy(alpha = 0.80f), style = MaterialTheme.typography.labelSmall, maxLines = 1)
                    Text("99", color = foreground, style = MaterialTheme.typography.displaySmall)
                    Text(name, color = foreground, style = MaterialTheme.typography.titleSmall, maxLines = 1)
                }
                "HERO" -> Column(
                    modifier = Modifier.fillMaxSize().padding(12.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Surface(color = glassPanel, shape = RoundedCornerShape(999.dp)) {
                            Text("倒数日", color = foreground, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                        }
                        Text(dateText, color = foreground.copy(alpha = 0.82f), style = MaterialTheme.typography.labelSmall, maxLines = 1)
                    }
                    Column(modifier = Modifier.fillMaxWidth(0.84f)) {
                        Text("99 天", color = foreground, style = MaterialTheme.typography.headlineMedium)
                        Text(name, color = foreground, style = MaterialTheme.typography.titleSmall, maxLines = 1)
                    }
                }
                else -> Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(10.dp)
                        .fillMaxWidth(0.84f)
                        .background(glassPanel, glassShape)
                        .padding(10.dp)
                ) {
                    Text("99 天", color = foreground, style = MaterialTheme.typography.titleMedium)
                    Text(name, color = foreground, style = MaterialTheme.typography.labelMedium, maxLines = 1)
                    Text(dateText, color = foreground.copy(alpha = 0.80f), style = MaterialTheme.typography.labelSmall, maxLines = 1)
                }
            }
        }
    }
}

private fun safeDialogColor(value: String): Color = runCatching {
    ColorUtils.hexToColor(value)
}.getOrElse { Color(0xFF7C4DFF) }

@Composable
@OptIn(ExperimentalLayoutApi::class)
fun DatePickerDialog(
    onDismiss: () -> Unit,
    onDateSelected: (Long) -> Unit,
    initialDateMillis: Long = System.currentTimeMillis()
) {
    var visibleMonth by remember(initialDateMillis) {
        mutableStateOf(Calendar.getInstance().apply {
            timeInMillis = initialDateMillis
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        })
    }
    var selectedDate by remember(initialDateMillis) { mutableLongStateOf(toLocalStartOfDay(initialDateMillis)) }
    var yearText by remember(initialDateMillis) { mutableStateOf(visibleMonth.get(Calendar.YEAR).toString()) }
    val monthTitle = remember(visibleMonth.timeInMillis) {
        SimpleDateFormat("yyyy年M月", Locale.getDefault()).format(visibleMonth.time)
    }
    val selectedCal = remember(selectedDate) { Calendar.getInstance().apply { timeInMillis = selectedDate } }
    val daysInMonth = visibleMonth.getActualMaximum(Calendar.DAY_OF_MONTH)
    val firstDayOffset = (visibleMonth.get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY + 7) % 7
    val weekLabels = listOf("一", "二", "三", "四", "五", "六", "日")
    val monthLabels = (1..12).map { "${it}月" }

    fun updateVisibleMonth(year: Int, month: Int) {
        val selectedDay = Calendar.getInstance().apply { timeInMillis = selectedDate }.get(Calendar.DAY_OF_MONTH)
        visibleMonth = Calendar.getInstance().apply {
            clear()
            set(year.coerceIn(1900, 2100), month.coerceIn(0, 11), 1, 0, 0, 0)
        }
        selectedDate = Calendar.getInstance().apply {
            clear()
            set(visibleMonth.get(Calendar.YEAR), visibleMonth.get(Calendar.MONTH), 1, 0, 0, 0)
            set(Calendar.DAY_OF_MONTH, selectedDay.coerceIn(1, getActualMaximum(Calendar.DAY_OF_MONTH)))
        }.timeInMillis
        yearText = visibleMonth.get(Calendar.YEAR).toString()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择日期") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(min = 320.dp, max = 380.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        visibleMonth = (visibleMonth.clone() as Calendar).apply { add(Calendar.MONTH, -1) }
                        yearText = visibleMonth.get(Calendar.YEAR).toString()
                    }) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "上个月")
                    }
                    Text(monthTitle, style = MaterialTheme.typography.titleMedium)
                    IconButton(onClick = {
                        visibleMonth = (visibleMonth.clone() as Calendar).apply { add(Calendar.MONTH, 1) }
                        yearText = visibleMonth.get(Calendar.YEAR).toString()
                    }) {
                        Icon(Icons.Default.ChevronRight, contentDescription = "下个月")
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(onClick = { updateVisibleMonth(visibleMonth.get(Calendar.YEAR) - 1, visibleMonth.get(Calendar.MONTH)) }) {
                        Text("上一年")
                    }
                    OutlinedTextField(
                        value = yearText,
                        onValueChange = { value ->
                            yearText = value.filter { it.isDigit() }.take(4)
                            if (yearText.length == 4) {
                                yearText.toIntOrNull()?.let { updateVisibleMonth(it, visibleMonth.get(Calendar.MONTH)) }
                            }
                        },
                        label = { Text("年份") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedButton(onClick = { updateVisibleMonth(visibleMonth.get(Calendar.YEAR) + 1, visibleMonth.get(Calendar.MONTH)) }) {
                        Text("下一年")
                    }
                }

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    monthLabels.forEachIndexed { index, label ->
                        FilterChip(
                            selected = visibleMonth.get(Calendar.MONTH) == index,
                            onClick = { updateVisibleMonth(visibleMonth.get(Calendar.YEAR), index) },
                            label = { Text(label) }
                        )
                    }
                }

                Row(modifier = Modifier.fillMaxWidth()) {
                    weekLabels.forEach { label ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(28.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    for (row in 0 until 6) {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            for (col in 0 until 7) {
                                val index = row * 7 + col
                                val day = index - firstDayOffset + 1
                                if (day in 1..daysInMonth) {
                                    val dayMillis = Calendar.getInstance().apply {
                                        clear()
                                        set(
                                            visibleMonth.get(Calendar.YEAR),
                                            visibleMonth.get(Calendar.MONTH),
                                            day,
                                            0,
                                            0,
                                            0
                                        )
                                    }.timeInMillis
                                    val isSelected = selectedCal.get(Calendar.YEAR) == visibleMonth.get(Calendar.YEAR) &&
                                        selectedCal.get(Calendar.MONTH) == visibleMonth.get(Calendar.MONTH) &&
                                        selectedCal.get(Calendar.DAY_OF_MONTH) == day
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(44.dp)
                                            .padding(2.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                                            .clickable { selectedDate = dayMillis },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = day.toString(),
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                } else {
                                    Spacer(modifier = Modifier.weight(1f).height(44.dp))
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onDateSelected(selectedDate) }) { Text("确定") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
fun LunarCalendarDialog(
    onDismiss: () -> Unit,
    initialLunar: String? = null,
    initialDateMillis: Long? = null,
    onSelected: (String, Long) -> Unit
) {
    val lunarMonths = listOf(
        "正月", "二月", "三月", "四月", "五月", "六月",
        "七月", "八月", "九月", "十月", "十一月", "腊月"
    )
    val lunarDays = (1..30).map { it to LunarDateUtils.dayLabel(it) }
    val initialSelection = remember(initialLunar, initialDateMillis) {
        parseLunarSelection(initialLunar, lunarMonths)
            ?: LunarDateUtils.selectionForGregorian(initialDateMillis ?: System.currentTimeMillis())
    }
    var selectedYear by remember(initialSelection) { mutableIntStateOf(initialSelection.first) }
    var selectedMonth by remember(initialSelection) { mutableIntStateOf(initialSelection.second) }
    var selectedDay by remember(initialSelection) { mutableIntStateOf(initialSelection.third) }
    var yearText by remember(initialSelection) { mutableStateOf(initialSelection.first.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择农历") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(min = 320.dp, max = 380.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val selectedGregorianDate = remember(selectedYear, selectedMonth, selectedDay) {
                    LunarDateUtils.monthDayToGregorian(selectedYear, selectedMonth, selectedDay)
                }
                Text(
                    selectedGregorianDate?.let { "对应公历：${formatDate(it)}" } ?: "当前农历日期在所选年份中无法换算",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(onClick = {
                        selectedYear = (selectedYear - 1).coerceIn(1900, 2100)
                        yearText = selectedYear.toString()
                    }) { Text("上一年") }
                    OutlinedTextField(
                        value = yearText,
                        onValueChange = { value ->
                            yearText = value.filter { it.isDigit() }.take(4)
                            if (yearText.length == 4) {
                                selectedYear = yearText.toIntOrNull()?.coerceIn(1900, 2100) ?: selectedYear
                            }
                        },
                        label = { Text("年份") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedButton(onClick = {
                        selectedYear = (selectedYear + 1).coerceIn(1900, 2100)
                        yearText = selectedYear.toString()
                    }) { Text("下一年") }
                }

                Text("月份", style = MaterialTheme.typography.labelLarge)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    lunarMonths.forEachIndexed { index, month ->
                        FilterChip(
                            selected = selectedMonth == index + 1,
                            onClick = { selectedMonth = index + 1 },
                            label = { Text(month) },
                        )
                    }
                }

                Text("日期", style = MaterialTheme.typography.labelLarge)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    lunarDays.forEach { (day, label) ->
                        FilterChip(
                            selected = selectedDay == day,
                            onClick = { selectedDay = day },
                            label = { Text(label) },
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val lunar = "${selectedYear}年农历${lunarMonths[selectedMonth - 1]}${LunarDateUtils.dayLabel(selectedDay)}"
                    onSelected(lunar, approximateLunarSelectionDate(selectedYear, selectedMonth, selectedDay))
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

private fun approximateLunarSelectionDate(year: Int, month: Int, day: Int): Long {
    val today = toLocalStartOfDay(System.currentTimeMillis())
    LunarDateUtils.monthDayToGregorian(year, month, day)?.takeIf { it >= today }?.let { return it }
    return LunarDateUtils.monthDayToGregorian(year + 1, month, day) ?: today
}

private fun parseLunarSelection(lunar: String?, lunarMonths: List<String>): Triple<Int, Int, Int>? {
    if (lunar.isNullOrBlank()) return null
    val year = Regex("(\\d{4})年").find(lunar)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: return null
    val month = lunarMonths.indexOfFirst { lunar.contains(it) }.takeIf { it >= 0 }?.plus(1) ?: return null
    val day = LunarDateUtils.parseDay(lunar) ?: return null
    return Triple(year.coerceIn(1900, 2100), month, day)
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

private fun toLocalStartOfDay(timestamp: Long): Long {
    return Calendar.getInstance().apply {
        timeInMillis = timestamp
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}

private fun localDateMillis(year: Int, month: Int, day: Int): Long {
    return Calendar.getInstance().apply {
        clear()
        set(year, month, day, 0, 0, 0)
    }.timeInMillis
}

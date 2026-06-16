package com.memoriabox.ui.screen

import android.app.Application
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.memoriabox.data.model.Event
import com.memoriabox.ui.utils.rememberAdaptiveUiSize
import com.memoriabox.viewmodel.createCalendarViewModel
import androidx.compose.ui.graphics.Color as ComposeColor
import java.text.SimpleDateFormat
import java.util.*
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.Typeface
import androidx.core.graphics.createBitmap
import java.io.File
import java.io.FileOutputStream

@Composable
fun PhotoWallScreen(application: Application) {
    val vm = remember { createCalendarViewModel(application) }
    val events by vm.allEvents.collectAsState(initial = emptyList())
    val adaptiveUi = rememberAdaptiveUiSize()

    // Filter events with images
    val eventsWithImages = remember(events) {
        events.filter { it.avatarUri != null || it.cardStyleJson?.contains("image") == true }
    }

    var selectedEvent by remember { mutableStateOf<Event?>(null) }
    var showShareDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.height(adaptiveUi.topBarHeight),
                windowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp),
                title = { Text("照片墙") },
                actions = {
                    IconButton(onClick = { showShareDialog = true }) {
                        Icon(Icons.Default.Share, contentDescription = "分享")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (eventsWithImages.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.PhotoLibrary,
                        null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Spacer(Modifier.height(16.dp))
                    Text("暂无照片，为事件添加头像后在这里展示")
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = if (adaptiveUi.compact) 104.dp else 132.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(adaptiveUi.sectionSpacing),
                contentPadding = PaddingValues(adaptiveUi.sectionSpacing),
                horizontalArrangement = Arrangement.spacedBy(adaptiveUi.sectionSpacing),
                verticalArrangement = Arrangement.spacedBy(adaptiveUi.sectionSpacing)
            ) {
                items(eventsWithImages) { event ->
                    Card(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(12.dp)),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            if (event.avatarUri != null) {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(event.avatarUri)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = event.name,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth()
                                    .background(ComposeColor.Black.copy(0.6f))
                                    .padding(8.dp)
                            ) {
                                Text(
                                    event.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = ComposeColor.White,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showShareDialog) {
        ShareOptionsDialog(
            events = eventsWithImages,
            onDismiss = { showShareDialog = false },
            onShare = { showShareDialog = false }
        )
    }
}

@Composable
fun ShareOptionsDialog(
    events: List<Event>,
    onDismiss: () -> Unit,
    onShare: () -> Unit
) {
    val context = LocalContext.current
    var isGenerating by remember { mutableStateOf(false) }
    var shareResult by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("分享选项") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text("请选择分享类型", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(16.dp))

                // Share single event card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val event = events.firstOrNull()
                            if (event != null) {
                                val daysLeft = ((event.date - System.currentTimeMillis()) / 86_400_000L).coerceAtLeast(0)
                                shareBitmap(context, generateEventCardBitmap(context, event, daysLeft), buildShareCaption(event, daysLeft))
                                shareResult = "已生成第一张事件卡片，请在系统分享面板中选择目标应用。"
                            } else {
                                shareResult = "当前没有可分享的带图事件。"
                            }
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Event, null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text("分享单个事件")
                                Text(
                                    "生成精美卡片图片",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                        Icon(Icons.Default.ChevronRight, null)
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Share photo wall
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (events.isNotEmpty()) {
                                shareBitmap(context, generateStatisticsBitmap(events), "我用 念记 记录了 ${events.size} 个重要日子。")
                                shareResult = "已生成照片墙概览，请在系统分享面板中选择目标应用。"
                            } else {
                                shareResult = "当前没有可分享的照片墙内容。"
                            }
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.PhotoLibrary, null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text("分享照片墙")
                                Text(
                                    "生成所有带图片的事件拼图",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                        Icon(Icons.Default.ChevronRight, null)
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Share statistics
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            shareBitmap(context, generateStatisticsBitmap(events), "我的 念记 重要日子统计。")
                            shareResult = "已生成统计图片，请在系统分享面板中选择目标应用。"
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.BarChart, null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text("分享统计")
                                Text(
                                    "生成数据统计图表",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                        Icon(Icons.Default.ChevronRight, null)
                    }
                }

                if (isGenerating) {
                    Spacer(Modifier.height(16.dp))
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Text(
                        "正在生成分享图片...",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
                shareResult?.let { result ->
                    Spacer(Modifier.height(12.dp))
                    Text(result, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text("关闭") }
        }
    )
}

@Composable
fun ExportScreen(
    application: Application,
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val vm = remember { createCalendarViewModel(application) }
    val events by vm.allEvents.collectAsState(initial = emptyList())
    
    var showExportOptions by remember { mutableStateOf(false) }
    var isExporting by remember { mutableStateOf(false) }
    var exportResult by remember { mutableStateOf<String?>(null) }
    val exportPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            exportResult = if (writeTextToUri(context, it, buildJsonExport(events))) "JSON 导出成功" else "JSON 导出失败"
        }
    }

    val icalPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/calendar")
    ) { uri ->
        uri?.let {
            exportResult = if (writeTextToUri(context, it, buildIcalExport(events))) "iCal 导出成功" else "iCal 导出失败"
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("导出分享") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Text(
                "分享或迁移到其他应用",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Text(
                "这里导出的是公开分享格式。完整数据备份请在设置里的备份与恢复中操作。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { exportPicker.launch("NianJi_Export_${System.currentTimeMillis()}.json") }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Description,
                            null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text("导出为 JSON", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "用于分享日子清单，不包含完整备份数据",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, null)
                }
            }

            Spacer(Modifier.height(12.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { icalPicker.launch("NianJi_${System.currentTimeMillis()}.ics") }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.CalendarToday,
                            null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text("导出为 iCal", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "可导入系统日历、Google Calendar 等应用",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, null)
                }
            }

            Spacer(Modifier.height(12.dp))

            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("分享模板", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "选择不同模板生成事件卡片或统计图片。",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(Modifier.height(12.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = {
                                    val event = events.firstOrNull()
                                    if (event != null) {
                                        val daysLeft = ((event.date - System.currentTimeMillis()) / 86_400_000L).coerceAtLeast(0)
                                        shareBitmap(context, generateEventCardBitmap(context, event, daysLeft, "CLASSIC"), buildShareCaption(event, daysLeft))
                                        exportResult = "经典卡片已生成"
                                    } else {
                                        exportResult = "暂无可分享事件"
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("经典")
                            }
                            OutlinedButton(
                                onClick = {
                                    val event = events.firstOrNull()
                                    if (event != null) {
                                        val daysLeft = ((event.date - System.currentTimeMillis()) / 86_400_000L).coerceAtLeast(0)
                                        shareBitmap(context, generateEventCardBitmap(context, event, daysLeft, "POSTER"), buildShareCaption(event, daysLeft))
                                        exportResult = "海报卡片已生成"
                                    } else {
                                        exportResult = "暂无可分享事件"
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("海报")
                            }
                            OutlinedButton(
                                onClick = {
                                    val event = events.firstOrNull()
                                    if (event != null) {
                                        val daysLeft = ((event.date - System.currentTimeMillis()) / 86_400_000L).coerceAtLeast(0)
                                        shareBitmap(context, generateEventCardBitmap(context, event, daysLeft, "MINIMAL"), buildShareCaption(event, daysLeft))
                                        exportResult = "极简卡片已生成"
                                    } else {
                                        exportResult = "暂无可分享事件"
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("极简")
                            }
                        }
                        OutlinedButton(
                            onClick = {
                                shareBitmap(context, generateStatisticsBitmap(events), "我的 念记 重要日子统计。")
                                exportResult = "统计图表已生成"
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.BarChart, null)
                            Spacer(Modifier.width(4.dp))
                            Text("统计图表")
                        }
                    }
                }
            }

            if (isExporting) {
                Spacer(Modifier.height(24.dp))
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Text("正在生成图片...")
            }

            exportResult?.let { result ->
                Spacer(Modifier.height(24.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(result, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        IconButton(onClick = {
                            // Share the result
                        }) {
                            Icon(Icons.Default.Share, null)
                        }
                    }
                }
            }
        }
    }
}

private fun writeTextToUri(context: android.content.Context, uri: Uri, content: String): Boolean {
    return try {
        context.contentResolver.openOutputStream(uri)?.use { output ->
            output.write(content.toByteArray(Charsets.UTF_8))
        }
        true
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}

private fun buildJsonExport(events: List<Event>): String {
    return buildString {
        append("{\n  \"version\": \"1.0\",\n  \"events\": [\n")
        events.forEachIndexed { index, event ->
            append("    {")
            append("\"id\": \"${event.id.escapeJson()}\", ")
            append("\"boxId\": \"${event.boxId.escapeJson()}\", ")
            append("\"name\": \"${event.name.escapeJson()}\", ")
            append("\"date\": ${event.date}, ")
            append("\"type\": \"${event.type.name}\", ")
            append("\"note\": \"${event.note.escapeJson()}\"")
            append("}")
            if (index != events.lastIndex) append(",")
            append("\n")
        }
        append("  ]\n}")
    }
}

private fun buildIcalExport(events: List<Event>): String {
    val formatter = SimpleDateFormat("yyyyMMdd", Locale.US)
    return buildString {
        appendLine("BEGIN:VCALENDAR")
        appendLine("VERSION:2.0")
        appendLine("PRODID:-//NianJi//CN")
        events.forEach { event ->
            appendLine("BEGIN:VEVENT")
            appendLine("UID:${event.id}@memoriabox")
            appendLine("SUMMARY:${event.name.escapeIcal()}")
            appendLine("DTSTART;VALUE=DATE:${formatter.format(Date(event.date))}")
            appendLine("DESCRIPTION:${event.note.escapeIcal()}")
            appendLine("END:VEVENT")
        }
        appendLine("END:VCALENDAR")
    }
}

private fun String.escapeJson(): String = replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")

private fun String.escapeIcal(): String = replace("\\", "\\\\").replace(";", "\\;").replace(",", "\\,").replace("\n", "\\n")

// Generate shareable image from event
fun generateEventCardBitmap(
    context: android.content.Context,
    event: Event,
    daysLeft: Long,
    template: String = "CLASSIC"
): Bitmap {
    val width = 1080
    val height = 1920
    val bitmap = createBitmap(width, height)
    val canvas = Canvas(bitmap)

    val isMinimal = template == "MINIMAL"
    val isPoster = template == "POSTER"

    val bgPaint = Paint().apply {
        isAntiAlias = true
        if (isMinimal) {
            color = android.graphics.Color.WHITE
        } else {
            shader = LinearGradient(
                0f,
                0f,
                width.toFloat(),
                height.toFloat(),
                intArrayOf(
                    android.graphics.Color.parseColor(if (isPoster) "#FF7A00" else "#1677FF"),
                    android.graphics.Color.parseColor(if (isPoster) "#FFB020" else "#13C2C2")
                ),
                null,
                Shader.TileMode.CLAMP
            )
        }
    }
    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

    val mainColor = if (isMinimal) android.graphics.Color.parseColor("#111827") else android.graphics.Color.WHITE
    val subColor = if (isMinimal) android.graphics.Color.parseColor("#52677D") else android.graphics.Color.argb(220, 255, 255, 255)

    if (isPoster) {
        val decorPaint = Paint().apply {
            color = android.graphics.Color.argb(42, 255, 255, 255)
            isAntiAlias = true
        }
        canvas.drawCircle(920f, 260f, 260f, decorPaint)
        canvas.drawCircle(110f, 1660f, 220f, decorPaint)
    }

    if (isMinimal) {
        val linePaint = Paint().apply {
            color = android.graphics.Color.parseColor("#1677FF")
            strokeWidth = 12f
            isAntiAlias = true
        }
        canvas.drawLine(120f, 180f, 980f, 180f, linePaint)
    }

    // Title
    val titlePaint = Paint().apply {
        color = mainColor
        textSize = if (isPoster) 88f else 76f
        isAntiAlias = true
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }
    canvas.drawText(event.name.take(18), width / 2f, if (isPoster) 360f else 420f, titlePaint)

    // Days count
    val daysPaint = Paint().apply {
        color = mainColor
        textSize = if (isPoster) 260f else 220f
        isAntiAlias = true
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }
    canvas.drawText(if (daysLeft == 0L) "今天" else "$daysLeft", width / 2f, if (isPoster) 900f else 880f, daysPaint)

    // Label
    val labelPaint = Paint().apply {
        color = subColor
        textSize = 60f
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
    }
    canvas.drawText(if (daysLeft == 0L) "就是今天" else "天", width / 2f + if (isPoster) 180f else 130f, if (isPoster) 900f else 880f, labelPaint)

    // Date
    val datePaint = Paint().apply {
        color = subColor
        textSize = 50f
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
    }
    val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(event.date))
    canvas.drawText(dateStr, width / 2f, 1180f, datePaint)
    if (event.note.isNotBlank()) {
        canvas.drawText(event.note.take(24), width / 2f, 1280f, datePaint)
    }

    // App signature
    val sigPaint = Paint().apply {
        color = if (isMinimal) android.graphics.Color.parseColor("#52677D") else android.graphics.Color.argb(180, 255, 255, 255)
        textSize = 40f
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
    }
    canvas.drawText("念记", width / 2f, height - 100f, sigPaint)

    return bitmap
}

fun shareBitmap(context: android.content.Context, bitmap: Bitmap, caption: String = "") {
    try {
        val file = File(context.cacheDir, "share_image.png")
        FileOutputStream(file).use { fos ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
        }

        val uri = androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            if (caption.isNotBlank()) putExtra(Intent.EXTRA_TEXT, caption)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(shareIntent, "分享到"))
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

private fun buildShareCaption(event: Event, daysLeft: Long): String {
    val dateText = SimpleDateFormat("yyyy年M月d日", Locale.getDefault()).format(Date(event.date))
    val dayText = if (daysLeft == 0L) "就是今天" else "还有 ${daysLeft} 天"
    return "${event.name}，$dayText。\n日期：$dateText\n来自 念记"
}

fun generateStatisticsBitmap(events: List<Event>): Bitmap {
    val width = 1080
    val height = 1600
    val bitmap = createBitmap(width, height)
    val canvas = Canvas(bitmap)
    val background = Paint().apply {
        color = android.graphics.Color.parseColor("#1E1B4B")
    }
    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), background)

    val titlePaint = Paint().apply {
        color = android.graphics.Color.WHITE
        textSize = 72f
        isAntiAlias = true
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }
    canvas.drawText("念记 数据统计", width / 2f, 180f, titlePaint)

    val metricPaint = Paint().apply {
        color = android.graphics.Color.WHITE
        textSize = 56f
        isAntiAlias = true
        typeface = Typeface.DEFAULT_BOLD
    }
    val labelPaint = Paint().apply {
        color = android.graphics.Color.argb(210, 255, 255, 255)
        textSize = 42f
        isAntiAlias = true
    }
    val accentPaint = Paint().apply {
        color = android.graphics.Color.parseColor("#FF4081")
        isAntiAlias = true
    }

    val birthdays = events.count { it.type == com.memoriabox.data.model.EventType.BIRTHDAY }
    val todos = events.count { it.type == com.memoriabox.data.model.EventType.TODO }
    val reminders = events.count { it.reminderEnabled }
    val rows = listOf(
        "总事件" to events.size,
        "生日" to birthdays,
        "待办" to todos,
        "已开提醒" to reminders
    )

    rows.forEachIndexed { index, (label, value) ->
        val top = 320f + index * 220f
        canvas.drawRoundRect(100f, top, 980f, top + 150f, 36f, 36f, accentPaint)
        canvas.drawText(label, 150f, top + 92f, labelPaint)
        canvas.drawText(value.toString(), 820f, top + 96f, metricPaint)
    }

    val footerPaint = Paint().apply {
        color = android.graphics.Color.argb(180, 255, 255, 255)
        textSize = 36f
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
    }
    canvas.drawText("Generated by 念记", width / 2f, height - 120f, footerPaint)
    return bitmap
}

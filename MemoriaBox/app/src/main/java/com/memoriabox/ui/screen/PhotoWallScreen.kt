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
import com.memoriabox.viewmodel.createCalendarViewModel
import androidx.compose.ui.graphics.Color as ComposeColor
import java.text.SimpleDateFormat
import java.util.*
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import androidx.core.graphics.createBitmap
import java.io.File
import java.io.FileOutputStream

@Composable
fun PhotoWallScreen(application: Application) {
    val vm = remember { createCalendarViewModel(application) }
    val events by vm.allEvents.collectAsState(initial = emptyList())

    // Filter events with images
    val eventsWithImages = remember(events) {
        events.filter { it.avatarUri != null || it.cardStyleJson?.contains("image") == true }
    }

    var selectedEvent by remember { mutableStateOf<Event?>(null) }
    var showShareDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
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
                columns = GridCells.Adaptive(minSize = 120.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(8.dp),
                contentPadding = PaddingValues(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
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
            onShare = { /* TODO: implement sharing */ }
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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("分享选项") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text("选择分享类型:", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(16.dp))

                // Share single event card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { /* TODO */ }
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
                        .clickable { /* TODO */ }
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
                        .clickable { /* TODO */ }
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
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text("关闭") }
        }
    )
}

@Composable
fun ExportScreen(application: Application) {
    val context = LocalContext.current
    
    var showExportOptions by remember { mutableStateOf(false) }
    var isExporting by remember { mutableStateOf(false) }
    var exportResult by remember { mutableStateOf<String?>(null) }

    val exportPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            // TODO: Export data to file
            exportResult = "导出成功"
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("导出分享") }
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
                "选择导出格式",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { exportPicker.launch("MemoriaBox_Export_${System.currentTimeMillis()}.json") }
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
                                "包含所有事件和设置数据",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    Icon(Icons.Default.ArrowForward, null)
                }
            }

            Spacer(Modifier.height(12.dp))

            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("分享图片", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "生成精美的事件卡片图片，可分享到社交媒体",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { isExporting = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Event, null)
                            Spacer(Modifier.width(4.dp))
                            Text("事件卡片")
                        }
                        OutlinedButton(
                            onClick = { isExporting = true },
                            modifier = Modifier.weight(1f)
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

// Generate shareable image from event
fun generateEventCardBitmap(
    context: android.content.Context,
    event: Event,
    daysLeft: Long
): Bitmap {
    val width = 1080
    val height = 1920
    val bitmap = createBitmap(width, height)
    val canvas = Canvas(bitmap)

    // Background gradient
    val bgPaint = Paint().apply {
        color = android.graphics.Color.parseColor("#7C4DFF")
    }
    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

    // Title
    val titlePaint = Paint().apply {
        color = android.graphics.Color.WHITE
        textSize = 80f
        isAntiAlias = true
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }
    canvas.drawText(event.name, width / 2f, 400f, titlePaint)

    // Days count
    val daysPaint = Paint().apply {
        color = android.graphics.Color.WHITE
        textSize = 200f
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
    }
    canvas.drawText("$daysLeft", width / 2f, 900f, daysPaint)

    // Label
    val labelPaint = Paint().apply {
        color = android.graphics.Color.WHITE
        textSize = 60f
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
    }
    canvas.drawText("天", width / 2f + 100f, 900f, labelPaint)

    // Date
    val datePaint = Paint().apply {
        color = android.graphics.Color.WHITE
        textSize = 50f
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
    }
    val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(event.date))
    canvas.drawText(dateStr, width / 2f, 1200f, datePaint)

    // App signature
    val sigPaint = Paint().apply {
        color = android.graphics.Color.argb(180, 255, 255, 255)
        textSize = 40f
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
    }
    canvas.drawText("MemoriaBox", width / 2f, height - 100f, sigPaint)

    return bitmap
}

fun shareBitmap(context: android.content.Context, bitmap: Bitmap) {
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
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(shareIntent, "分享到"))
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

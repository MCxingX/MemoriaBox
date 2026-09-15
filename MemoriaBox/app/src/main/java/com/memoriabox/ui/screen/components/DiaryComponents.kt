package com.memoriabox.ui.screen.components

import android.content.Context
import android.net.Uri
import android.widget.MediaController
import android.widget.VideoView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.LifecycleOwner
import coil.compose.AsyncImage
import com.memoriabox.data.model.DiaryEntry
import com.memoriabox.data.model.DiaryMedia
import com.memoriabox.data.model.DiaryMediaType
import com.memoriabox.utils.ColorUtils
import com.memoriabox.utils.ImageImportUtils
import com.memoriabox.ui.screen.dialogs.DatePickerDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun ScrollingTextAnimation(
    text: String,
    charDelay: Long = 60L,
    onComplete: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val settingsVersion = com.memoriabox.utils.AppSettings.settingsVersion
    val scrollEnabled = remember(settingsVersion) { com.memoriabox.utils.AppSettings.getDiaryScrollEnabled(context) }
    val scrollSpeed = remember(settingsVersion) { com.memoriabox.utils.AppSettings.getDiaryScrollSpeed(context) }

    if (!scrollEnabled) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White,
            modifier = modifier.fillMaxWidth()
        )
        LaunchedEffect(text) { onComplete() }
        return
    }

    var visibleChars by remember { mutableIntStateOf(0) }
    var isComplete by remember { mutableStateOf(false) }

    LaunchedEffect(text, scrollSpeed) {
        visibleChars = 0
        isComplete = false
        for (i in text.indices) {
            if (!isActive) break
            visibleChars = i + 1
            delay(scrollSpeed.toLong())
        }
        isComplete = true
        onComplete()
    }

    Box(modifier = modifier) {
        Text(
            text = text.take(visibleChars),
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White,
            modifier = Modifier.fillMaxWidth()
        )
        if (!isComplete && visibleChars < text.length) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .size(2.dp)
                    .background(Color.White)
            )
        }
    }
}

@Composable
fun MediaBackgroundPlayer(
    mediaUri: String?,
    mediaType: DiaryMediaType?,
    modifier: Modifier = Modifier
) {
    if (mediaUri.isNullOrEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.4f)
                        )
                    )
                )
        )
        return
    }

    when (mediaType) {
        DiaryMediaType.VIDEO -> {
            DiaryVideoPlayer(
                uri = mediaUri,
                modifier = modifier.fillMaxSize(),
                showControls = false,
                autoPlay = true,
                loop = true,
                muted = true
            )
        }
        else -> {
            AsyncImage(
                model = mediaUri,
                contentDescription = "日记背景",
                contentScale = ContentScale.Crop,
                modifier = modifier.fillMaxSize(),
                onError = {
                    // fallback handled by parent
                }
            )
        }
    }
}

@Composable
fun DiaryVideoPlayer(
    uri: String,
    modifier: Modifier = Modifier,
    showControls: Boolean = true,
    autoPlay: Boolean = false,
    loop: Boolean = false,
    muted: Boolean = false,
    onCompletion: (() -> Unit)? = null,
    onFullscreen: (() -> Unit)? = null
) {
    val context = LocalContext.current
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black)
    ) {
        AndroidView(
            modifier = Modifier.matchParentSize(),
            factory = {
                VideoView(context).apply {
                    setVideoURI(Uri.parse(uri))
                    if (showControls) {
                        setMediaController(MediaController(context).also { controller -> controller.setAnchorView(this) })
                    }
                    setOnPreparedListener { player ->
                        player.isLooping = loop
                        if (muted) player.setVolume(0f, 0f)
                        if (autoPlay) start()
                    }
                    setOnCompletionListener {
                        if (!loop) onCompletion?.invoke()
                    }
                }
            },
            update = { view ->
                val currentUri = Uri.parse(uri)
                if (view.tag != uri) {
                    view.tag = uri
                    view.setVideoURI(currentUri)
                }
                if (autoPlay && !view.isPlaying) view.start()
            },
            onRelease = { view ->
                view.stopPlayback()
            }
        )
        if (onFullscreen != null) {
            SmallIconButton(
                onClick = onFullscreen,
                modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp),
                contentDescription = "全屏播放"
            ) {
                Icon(Icons.Default.Fullscreen, contentDescription = "全屏播放", tint = Color.White)
            }
        }
    }
}

@Composable
private fun SmallIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .size(36.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color.Black.copy(alpha = 0.55f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { content() }
}

@Composable
fun FullscreenVideoDialog(
    uri: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val view = androidx.compose.ui.platform.LocalView.current
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = {
                    VideoView(context).apply {
                        setVideoURI(Uri.parse(uri))
                        setMediaController(MediaController(context).also { controller -> controller.setAnchorView(this) })
                        setOnPreparedListener { player ->
                            player.isLooping = false
                            player.setVolume(1f, 1f)
                            start()
                        }
                    }
                },
                onRelease = { view -> view.stopPlayback() }
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.TopEnd).padding(12.dp)
            ) {
                Icon(Icons.Default.Close, contentDescription = "关闭", tint = Color.White)
            }
        }
    }
    androidx.compose.runtime.DisposableEffect(uri) {
        val window = (context as? android.app.Activity)?.window
            ?: (view.parent as? android.view.View)?.let { (it.context as? android.app.Activity)?.window }
        val originalFlags = window?.decorView?.systemUiVisibility ?: 0
        window?.decorView?.systemUiVisibility = (
            android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or android.view.View.SYSTEM_UI_FLAG_FULLSCREEN
                or android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        )
        onDispose {
            window?.decorView?.systemUiVisibility = originalFlags
        }
    }
}

@Composable
fun DiaryDetailDialog(
    diary: DiaryEntry,
    mediaList: List<DiaryMedia>,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var fullscreenVideoUri by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        title = {
            Text(
                text = java.text.SimpleDateFormat("yyyy年M月d日", java.util.Locale.getDefault())
                    .format(java.util.Date(diary.dateStart))
            )
        },
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 300.dp, max = 500.dp)
                    .clip(RoundedCornerShape(16.dp))
            ) {
                MediaBackgroundPlayer(
                    mediaUri = diary.backgroundMediaUri,
                    mediaType = diary.backgroundMediaType,
                    modifier = Modifier.matchParentSize()
                )

                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(Color.Black.copy(alpha = 0.45f))
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (diary.content.isNotEmpty()) {
                        Surface(
                            color = Color.Black.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            ScrollingTextAnimation(
                                text = diary.content,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    if (mediaList.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            mediaList.forEach { media ->
                                when (media.mediaType) {
                                    DiaryMediaType.IMAGE -> {
                                        AsyncImage(
                                            model = media.mediaUri,
                                            contentDescription = "日记图片",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .aspectRatio(media.aspectRatio.toFloatRatio())
                                                .clip(RoundedCornerShape(12.dp))
                                        )
                                    }
                                    DiaryMediaType.VIDEO -> {
                                        DiaryVideoPlayer(
                                            uri = media.mediaUri,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .aspectRatio(media.aspectRatio.toFloatRatio()),
                                            showControls = true,
                                            autoPlay = false,
                                            onFullscreen = { fullscreenVideoUri = media.mediaUri }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onEdit) {
                Text("编辑")
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = { showDeleteConfirm = true }) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
                TextButton(onClick = onDismiss) {
                    Text("关闭")
                }
            }
        }
    )

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("删除日记") },
            text = { Text("确定要删除这篇日记吗？删除后无法恢复。") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    onDelete()
                }) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("取消") }
            }
        )
    }

    fullscreenVideoUri?.let { videoUri ->
        FullscreenVideoDialog(uri = videoUri, onDismiss = { fullscreenVideoUri = null })
    }
}

@Composable
fun DiaryEditorDialog(
    existingDiary: DiaryEntry? = null,
    existingMedia: List<DiaryMedia> = emptyList(),
    allDiaries: List<DiaryEntry> = emptyList(),
    dateStart: Long,
    onDismiss: () -> Unit,
    onSave: (dateStart: Long, content: String, media: List<DiaryMedia>, backgroundUri: String?) -> Unit,
    onDelete: () -> Unit = {},
    onOpenExistingDiary: (DiaryEntry) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dateFormatter = remember { SimpleDateFormat("yyyy年M月d日", Locale.getDefault()) }
    var selectedDateStart by remember(existingDiary?.id, dateStart) { mutableLongStateOf(existingDiary?.dateStart ?: startOfDayForEditor(dateStart)) }
    var content by remember { mutableStateOf(existingDiary?.content ?: "") }
    var backgroundUri by remember { mutableStateOf(existingDiary?.backgroundMediaUri) }
    val mediaItems = remember(existingDiary?.id, existingMedia) { mutableStateListOf<DiaryMedia>().apply { addAll(existingMedia) } }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var replacingMediaIndex by remember { mutableStateOf<Int?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    val sameDateDiaries = remember(allDiaries, selectedDateStart, existingDiary?.id) {
        allDiaries
            .filter { startOfDayForEditor(it.dateStart) == selectedDateStart && it.id != existingDiary?.id }
            .sortedBy { it.createdAt }
    }

    // Image picker
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        if (uris.isNotEmpty()) {
            scope.launch(Dispatchers.IO) {
                val copied = uris.mapNotNull { uri ->
                    ImageImportUtils.copyImageToPrivateStorage(context, uri, "diary_images")
                }
                mediaItems.addAll(copied.mapIndexed { index, uri ->
                    DiaryMedia(
                        diaryId = existingDiary?.id ?: "",
                        mediaUri = uri,
                        mediaType = DiaryMediaType.IMAGE,
                        sortOrder = mediaItems.size + index
                    )
                })
            }
        }
    }

    // Video picker
    val videoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let {
            scope.launch(Dispatchers.IO) {
                val copied = ImageImportUtils.copyMediaToPrivateStorage(context, it, "diary_videos", "mp4")
                copied?.let { uri ->
                    mediaItems.add(
                        DiaryMedia(
                            diaryId = existingDiary?.id ?: "",
                            mediaUri = uri,
                            mediaType = DiaryMediaType.VIDEO,
                            sortOrder = mediaItems.size
                        )
                    )
                }
            }
        }
    }

    val replacePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        val index = replacingMediaIndex
        replacingMediaIndex = null
        if (uri != null && index != null && index in mediaItems.indices) {
            scope.launch(Dispatchers.IO) {
                val copied = ImageImportUtils.copyMediaToPrivateStorage(context, uri, "diary_media", "jpg") ?: return@launch
                mediaItems[index] = mediaItems[index].copy(
                    mediaUri = copied,
                    mediaType = inferDiaryMediaTypeForEditor(copied)
                )
            }
        }
    }

    // Background media picker
    val bgPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let {
            scope.launch(Dispatchers.IO) {
                val copied = ImageImportUtils.copyMediaToPrivateStorage(context, it, "diary_backgrounds", "jpg")
                if (copied != null) {
                    backgroundUri = copied
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(if (existingDiary == null) "写日记" else "编辑日记")
                    Text(dateFormatter.format(selectedDateStart), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = { showDatePicker = true }) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = "选择日记日期")
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (sameDateDiaries.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("这一天已有 ${sameDateDiaries.size} 篇日记", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Text("可以继续新建，也可以切换编辑已有日记。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                            sameDateDiaries.forEachIndexed { index, diary ->
                                Surface(
                                    modifier = Modifier.fillMaxWidth().clickable { onOpenExistingDiary(diary) },
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.82f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("已有日记 ${index + 1}", style = MaterialTheme.typography.labelLarge)
                                            Text(diary.content.ifBlank { "无文字内容" }.take(42), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                                        }
                                        Icon(Icons.Default.ChevronRight, contentDescription = null, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("今天发生了什么...") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 4,
                    maxLines = 12
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text("日记内容", style = MaterialTheme.typography.labelLarge)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Image, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("添加图片")
                    }

                    OutlinedButton(
                        onClick = { videoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Videocam, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("添加视频")
                    }
                }

                OutlinedButton(
                    onClick = { bgPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Wallpaper, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("选择背景（照片/视频）")
                }

                // Preview selected background
                if (backgroundUri != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        AsyncImage(
                            model = backgroundUri,
                            contentDescription = "背景预览",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                // Preview media attachments
                if (mediaItems.isNotEmpty()) {
                    Text("已添加 ${mediaItems.size} 个媒体文件", style = MaterialTheme.typography.bodySmall)
                    mediaItems.forEachIndexed { index, media ->
                        DiaryMediaEditorRow(
                            media = media,
                            onDelete = { mediaItems.removeAt(index) },
                            onReplace = {
                                replacingMediaIndex = index
                                replacePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
                            },
                            onAspectRatioChange = { ratio ->
                                mediaItems[index] = media.copy(aspectRatio = ratio)
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        selectedDateStart,
                        content,
                        mediaItems.mapIndexed { index, media -> media.copy(sortOrder = index) },
                        backgroundUri
                    )
                    onDismiss()
                },
                enabled = content.trim().isNotEmpty() || mediaItems.isNotEmpty() || backgroundUri != null
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (existingDiary != null) {
                    TextButton(onClick = { showDeleteConfirm = true }) {
                        Text("删除", color = MaterialTheme.colorScheme.error)
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("取消")
                }
            }
        }
    )

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("删除日记") },
            text = { Text("确定要删除这篇日记吗？删除后无法恢复。") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    onDelete()
                    onDismiss()
                }) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("取消") }
            }
        )
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismiss = { showDatePicker = false },
            onDateSelected = { selected ->
                selectedDateStart = startOfDayForEditor(selected)
                showDatePicker = false
            },
            initialDateMillis = selectedDateStart
        )
    }
}

@Composable
fun DiaryIndicator(
    hasDiary: Boolean,
    hasImage: Boolean,
    hasVideo: Boolean,
    count: Int = 1,
    modifier: Modifier = Modifier
) {
    if (!hasDiary) return

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (hasImage) {
            Icon(
                Icons.Default.Image,
                contentDescription = "图片日记",
                tint = Color(0xFFFF6B6B),
                modifier = Modifier.size(10.dp)
            )
        }
        if (hasVideo) {
            Icon(
                Icons.Default.Videocam,
                contentDescription = "视频日记",
                tint = Color(0xFF7C5CFF),
                modifier = Modifier.size(10.dp)
            )
        }
        if (!hasImage && !hasVideo) {
            Box(
                modifier = Modifier
                    .size(5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color(0xFF00B8D9))
            )
        }
        if (count > 1) {
            Text(
                text = "$count",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF00B8D9),
                modifier = Modifier.padding(start = 1.dp)
            )
        }
    }
}

@Composable
private fun DiaryMediaEditorRow(
    media: DiaryMedia,
    onDelete: () -> Unit,
    onReplace: () -> Unit,
    onAspectRatioChange: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                if (media.mediaType == DiaryMediaType.IMAGE) {
                    AsyncImage(
                        model = media.mediaUri,
                        contentDescription = "媒体预览",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(64.dp).clip(RoundedCornerShape(10.dp))
                    )
                } else {
                    Box(
                        modifier = Modifier.size(64.dp).clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.secondaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Videocam, contentDescription = null)
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(if (media.mediaType == DiaryMediaType.IMAGE) "图片" else "视频", style = MaterialTheme.typography.titleSmall)
                    Text("比例：${media.aspectRatio}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                TextButton(onClick = onReplace) { Text("替换") }
                TextButton(onClick = onDelete) { Text("删除", color = MaterialTheme.colorScheme.error) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                listOf("1:1", "4:3", "16:9", "3:4", "9:16").forEach { ratio ->
                    FilterChip(
                        selected = media.aspectRatio == ratio,
                        onClick = { onAspectRatioChange(ratio) },
                        label = { Text(ratio) }
                    )
                }
            }
        }
    }
}

private fun inferDiaryMediaTypeForEditor(uri: String): DiaryMediaType {
    val videoExtensions = listOf(".mp4", ".mkv", ".webm", ".3gp", ".mov")
    return if (videoExtensions.any { uri.endsWith(it, ignoreCase = true) }) DiaryMediaType.VIDEO else DiaryMediaType.IMAGE
}

private fun String.toFloatRatio(): Float {
    val parts = split(":")
    val width = parts.getOrNull(0)?.toFloatOrNull()
    val height = parts.getOrNull(1)?.toFloatOrNull()
    return if (width != null && height != null && height > 0f) width / height else 16f / 9f
}

private fun startOfDayForEditor(timestamp: Long): Long {
    val cal = java.util.Calendar.getInstance()
    cal.timeInMillis = timestamp
    cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
    cal.set(java.util.Calendar.MINUTE, 0)
    cal.set(java.util.Calendar.SECOND, 0)
    cal.set(java.util.Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}

package com.memoriabox.ui.screen.components

import android.content.Context
import android.net.Uri
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
import androidx.lifecycle.LifecycleOwner
import coil.compose.AsyncImage
import com.memoriabox.data.model.DiaryEntry
import com.memoriabox.data.model.DiaryMedia
import com.memoriabox.data.model.DiaryMediaType
import com.memoriabox.utils.ColorUtils
import com.memoriabox.utils.ImageImportUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
fun ScrollingTextAnimation(
    text: String,
    charDelay: Long = 60L,
    onComplete: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var visibleChars by remember { mutableIntStateOf(0) }
    var isComplete by remember { mutableStateOf(false) }

    LaunchedEffect(text) {
        visibleChars = 0
        isComplete = false
        for (i in text.indices) {
            if (!isActive) break
            visibleChars = i + 1
            delay(charDelay)
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
            Box(modifier = modifier.fillMaxSize()) {
                Text(
                    text = "[视频背景：$mediaUri]",
                    color = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.align(Alignment.Center)
                )
            }
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
fun DiaryDetailDialog(
    diary: DiaryEntry,
    mediaList: List<DiaryMedia>,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        title = {
            Text(
                text = java.text.SimpleDateFormat("yyyy年M月d日", java.util.Locale.getDefault())
                    .format(java.util.Date(diary.dateStart)),
                color = Color.White
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
                                                .height(200.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                        )
                                    }
                                    DiaryMediaType.VIDEO -> {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(200.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(Color.Black.copy(alpha = 0.6f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.Default.PlayCircle,
                                                contentDescription = "播放视频",
                                                tint = Color.White,
                                                modifier = Modifier.size(48.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row {
                TextButton(onClick = onEdit) {
                    Text("编辑", color = Color.White)
                }
                TextButton(onClick = { showDeleteConfirm = true }) {
                    Text("删除", color = Color(0xFFFF6B6B))
                }
                TextButton(onClick = onDismiss) {
                    Text("关闭", color = Color.White)
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
                }) { Text("删除", color = Color(0xFFFF6B6B)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("取消") }
            }
        )
    }
}

@Composable
fun DiaryEditorDialog(
    existingDiary: DiaryEntry? = null,
    dateStart: Long,
    onDismiss: () -> Unit,
    onSave: (content: String, mediaUris: List<String>, backgroundUri: String?) -> Unit,
    onDelete: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var content by remember { mutableStateOf(existingDiary?.content ?: "") }
    var backgroundUri by remember { mutableStateOf(existingDiary?.backgroundMediaUri) }
    var mediaUris by remember { mutableStateOf(mutableListOf<String>()) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    // Image picker
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        if (uris.isNotEmpty()) {
            val copied = uris.mapNotNull { uri ->
                ImageImportUtils.copyImageToPrivateStorage(context, uri, "diary_images")
            }
            mediaUris.addAll(copied)
        }
    }

    // Video picker
    val videoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let {
            val copied = ImageImportUtils.copyImageToPrivateStorage(context, it, "diary_videos")
            copied?.let { mediaUris.add(it) }
        }
    }

    // Background media picker
    val bgPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        backgroundUri = uri?.let {
            ImageImportUtils.copyImageToPrivateStorage(context, it, "diary_backgrounds")
        } ?: backgroundUri
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existingDiary == null) "写日记" else "编辑日记") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
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
                if (mediaUris.isNotEmpty()) {
                    Text("已添加 ${mediaUris.size} 个媒体文件", style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Row {
                if (existingDiary != null) {
                    TextButton(onClick = { showDeleteConfirm = true }) {
                        Text("删除", color = Color(0xFFFF6B6B))
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("取消")
                }
                Button(
                    onClick = {
                        onSave(content, mediaUris.toList(), backgroundUri)
                        onDismiss()
                    },
                    enabled = content.trim().isNotEmpty()
                ) {
                    Text("保存")
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
                }) { Text("删除", color = Color(0xFFFF6B6B)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("取消") }
            }
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



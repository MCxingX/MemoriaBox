package com.memoriabox.ui.screen.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.memoriabox.data.model.DiaryMediaType
import com.memoriabox.utils.MonthlyPhotoItem
import com.memoriabox.utils.MonthlySummaryHelper
import com.memoriabox.utils.MonthlySummaryStatus
import com.memoriabox.utils.MonthlySummaryUiState
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun MonthlySummaryPanel(
    state: MonthlySummaryUiState,
    onDismiss: () -> Unit,
    onMonthChange: (Long) -> Unit,
    onPlayModeChange: (Boolean) -> Unit,
    onSpeedChange: (Float) -> Unit,
    onTextEnabledChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedIndex by remember(state.monthStart, state.slides) { mutableIntStateOf(0) }
    var selectedMediaIndex by remember(state.monthStart, selectedIndex) { mutableIntStateOf(0) }
    var playing by remember(state.isPlayMode) { mutableStateOf(state.isPlayMode) }
    val monthFormat = remember { SimpleDateFormat("yyyy年M月", Locale.getDefault()) }
    val dayFormat = remember { SimpleDateFormat("M月d日", Locale.getDefault()) }
    val currentSlide = state.slides.getOrNull(selectedIndex)
    val currentMedia = currentSlide?.photos?.getOrNull(selectedMediaIndex)

    fun advancePlayback() {
        val slide = state.slides.getOrNull(selectedIndex)
        if (slide != null && selectedMediaIndex < slide.photos.lastIndex) {
            selectedMediaIndex++
            return
        }
        if (selectedIndex < state.slides.lastIndex) {
            selectedIndex++
            selectedMediaIndex = 0
        } else {
            playing = false
        }
    }

    LaunchedEffect(playing, selectedIndex, selectedMediaIndex, currentMedia?.mediaUri, state.playSpeedFactor, state.slides.size) {
        if (playing && state.slides.isNotEmpty() && currentMedia?.mediaType != DiaryMediaType.VIDEO) {
            delay(MonthlySummaryHelper.slideDelayMillis(state.playSpeedFactor))
            advancePlayback()
        }
    }

    ImmersiveSummaryDialog(onDismiss = onDismiss, modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color(0xAA101828), Color(0xDD111827))))
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.AutoStories, contentDescription = null, tint = Color.White)
                    Column(modifier = Modifier.weight(1f)) {
                        Text("月度总结", color = Color.White, style = MaterialTheme.typography.titleLarge)
                        Text(monthFormat.format(state.monthStart), color = Color.White.copy(alpha = 0.78f), style = MaterialTheme.typography.bodySmall)
                    }
                    IconButton(onClick = { onMonthChange(addMonths(state.monthStart, -1)) }) { Icon(Icons.Default.ChevronLeft, contentDescription = "上月", tint = Color.White) }
                    IconButton(onClick = { onMonthChange(addMonths(state.monthStart, 1)) }) { Icon(Icons.Default.ChevronRight, contentDescription = "下月", tint = Color.White) }
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, contentDescription = "关闭", tint = Color.White) }
                }

                MonthlySummaryControls(
                    playing = playing,
                    textEnabled = state.isSummaryEnabled,
                    speed = state.playSpeedFactor,
                    canMovePrev = selectedIndex > 0,
                    canMoveNext = selectedIndex < state.slides.lastIndex,
                    onPlayPause = {
                        playing = !playing
                        onPlayModeChange(playing)
                    },
                    onStop = {
                        playing = false
                        selectedIndex = 0
                        onPlayModeChange(false)
                    },
                    onPrev = { if (selectedIndex > 0) selectedIndex-- },
                    onNext = { advancePlayback() },
                    onSpeedChange = onSpeedChange,
                    onTextEnabledChange = onTextEnabledChange
                )

                when (state.summaryStatus) {
                    MonthlySummaryStatus.LOADING -> LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    MonthlySummaryStatus.ERROR -> Text("月度总结加载失败，请稍后重试。", color = Color.White)
                    MonthlySummaryStatus.EMPTY -> MonthlySummaryEmpty(monthFormat.format(state.monthStart), modifier = Modifier.weight(1f))
                    MonthlySummaryStatus.READY -> {
                        if (playing && state.slides.isNotEmpty()) {
                            MonthlySummarySlideCard(
                                slide = state.slides[selectedIndex.coerceIn(0, state.slides.lastIndex)],
                                dayText = dayFormat.format(state.slides[selectedIndex.coerceIn(0, state.slides.lastIndex)].dateStart),
                                textEnabled = state.isSummaryEnabled,
                                focusMediaIndex = selectedMediaIndex,
                                autoPlayVideo = true,
                                onVideoComplete = { advancePlayback() },
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                if (state.isSummaryEnabled) {
                                    item { SummaryTextCard(state.summaryText) }
                                }
                                items(state.slides) { slide ->
                                    MonthlySummarySlideCard(slide = slide, dayText = dayFormat.format(slide.dateStart), textEnabled = state.isSummaryEnabled)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DailySummaryPanel(
    state: MonthlySummaryUiState,
    onDismiss: () -> Unit,
    onPlayModeChange: (Boolean) -> Unit,
    onSpeedChange: (Float) -> Unit,
    onTextEnabledChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedIndex by remember(state.monthStart, state.slides) { mutableIntStateOf(0) }
    var selectedMediaIndex by remember(state.monthStart, selectedIndex) { mutableIntStateOf(0) }
    var playing by remember(state.isPlayMode) { mutableStateOf(state.isPlayMode) }
    val dayFormat = remember { SimpleDateFormat("yyyy年M月d日 EEEE", Locale.getDefault()) }
    val slideDayFormat = remember { SimpleDateFormat("M月d日", Locale.getDefault()) }
    val currentSlide = state.slides.getOrNull(selectedIndex)
    val currentMedia = currentSlide?.photos?.getOrNull(selectedMediaIndex)

    fun advancePlayback() {
        val slide = state.slides.getOrNull(selectedIndex)
        if (slide != null && selectedMediaIndex < slide.photos.lastIndex) {
            selectedMediaIndex++
            return
        }
        if (selectedIndex < state.slides.lastIndex) {
            selectedIndex++
            selectedMediaIndex = 0
        } else {
            playing = false
        }
    }

    LaunchedEffect(playing, selectedIndex, selectedMediaIndex, currentMedia?.mediaUri, state.playSpeedFactor, state.slides.size) {
        if (playing && state.slides.isNotEmpty() && currentMedia?.mediaType != DiaryMediaType.VIDEO) {
            delay(MonthlySummaryHelper.slideDelayMillis(state.playSpeedFactor))
            advancePlayback()
        }
    }

    ImmersiveSummaryDialog(onDismiss = onDismiss, modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color(0xAA101828), Color(0xDD111827))))
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.AutoStories, contentDescription = null, tint = Color.White)
                    Column(modifier = Modifier.weight(1f)) {
                        Text("今日总结", color = Color.White, style = MaterialTheme.typography.titleLarge)
                        Text(dayFormat.format(state.monthStart), color = Color.White.copy(alpha = 0.78f), style = MaterialTheme.typography.bodySmall)
                    }
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, contentDescription = "关闭", tint = Color.White) }
                }

                MonthlySummaryControls(
                    playing = playing,
                    textEnabled = state.isSummaryEnabled,
                    speed = state.playSpeedFactor,
                    canMovePrev = selectedIndex > 0,
                    canMoveNext = selectedIndex < state.slides.lastIndex,
                    onPlayPause = {
                        playing = !playing
                        onPlayModeChange(playing)
                    },
                    onStop = {
                        playing = false
                        selectedIndex = 0
                        onPlayModeChange(false)
                    },
                    onPrev = { if (selectedIndex > 0) selectedIndex-- },
                    onNext = { advancePlayback() },
                    onSpeedChange = onSpeedChange,
                    onTextEnabledChange = onTextEnabledChange
                )

                when (state.summaryStatus) {
                    MonthlySummaryStatus.LOADING -> LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    MonthlySummaryStatus.ERROR -> Text("今日总结加载失败，请稍后重试。", color = Color.White)
                    MonthlySummaryStatus.EMPTY -> MonthlySummaryEmpty(slideDayFormat.format(state.monthStart), modifier = Modifier.weight(1f))
                    MonthlySummaryStatus.READY -> {
                        if (playing && state.slides.isNotEmpty()) {
                            MonthlySummarySlideCard(
                                slide = state.slides[selectedIndex.coerceIn(0, state.slides.lastIndex)],
                                dayText = slideDayFormat.format(state.slides[selectedIndex.coerceIn(0, state.slides.lastIndex)].dateStart),
                                textEnabled = state.isSummaryEnabled,
                                focusMediaIndex = selectedMediaIndex,
                                autoPlayVideo = true,
                                onVideoComplete = { advancePlayback() },
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                if (state.isSummaryEnabled) {
                                    item { SummaryTextCard(state.summaryText) }
                                }
                                items(state.slides) { slide ->
                                    MonthlySummarySlideCard(slide = slide, dayText = slideDayFormat.format(slide.dateStart), textEnabled = state.isSummaryEnabled)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ImmersiveSummaryDialog(onDismiss: () -> Unit, modifier: Modifier, content: @Composable () -> Unit) {
    val configuration = LocalConfiguration.current
    val compact = configuration.screenWidthDp < 600
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        val shape = if (compact) RoundedCornerShape(0.dp) else RoundedCornerShape(28.dp)
        val sizeModifier = if (compact) {
            Modifier.fillMaxSize()
        } else {
            Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.92f)
                .widthIn(max = 760.dp)
        }
        Surface(shape = shape, color = Color.Transparent, modifier = modifier.then(sizeModifier)) {
            content()
        }
    }
}

@Composable
private fun MonthlySummaryControls(
    playing: Boolean,
    textEnabled: Boolean,
    speed: Float,
    canMovePrev: Boolean,
    canMoveNext: Boolean,
    onPlayPause: () -> Unit,
    onStop: () -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onSpeedChange: (Float) -> Unit,
    onTextEnabledChange: (Boolean) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Color.White.copy(alpha = 0.14f)).padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IconButton(onClick = onPlayPause) { Icon(if (playing) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = "播放暂停", tint = Color.White) }
            IconButton(onClick = onStop) { Icon(Icons.Default.Stop, contentDescription = "停止", tint = Color.White) }
            IconButton(onClick = onPrev, enabled = canMovePrev) { Icon(Icons.Default.SkipPrevious, contentDescription = "上一项", tint = Color.White) }
            IconButton(onClick = onNext, enabled = canMoveNext) { Icon(Icons.Default.SkipNext, contentDescription = "下一项", tint = Color.White) }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = textEnabled, onCheckedChange = onTextEnabledChange)
                Text("文字", color = Color.White, style = MaterialTheme.typography.labelMedium)
            }
        }
        Text("播放速度 ${"%.1f".format(speed)}x", color = Color.White.copy(alpha = 0.84f), style = MaterialTheme.typography.labelMedium)
        Slider(value = speed, onValueChange = onSpeedChange, valueRange = 0.5f..2.0f, steps = 4)
    }
}

@Composable
private fun SummaryTextCard(text: String) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.16f)), shape = RoundedCornerShape(18.dp)) {
        Text(text, modifier = Modifier.padding(14.dp), color = Color.White, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun MonthlySummarySlideCard(
    slide: com.memoriabox.utils.MonthlySummarySlide,
    dayText: String,
    textEnabled: Boolean,
    focusMediaIndex: Int? = null,
    autoPlayVideo: Boolean = false,
    onVideoComplete: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.16f)), shape = RoundedCornerShape(20.dp)) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(dayText, color = Color.White, style = MaterialTheme.typography.titleMedium)
            if (slide.photos.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(16.dp)).background(Color.White.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = Color.White.copy(alpha = 0.85f), modifier = Modifier.size(42.dp))
                        Text("文字日记", color = Color.White.copy(alpha = 0.85f))
                    }
                }
            } else {
                val focused = focusMediaIndex?.let { index -> slide.photos.getOrNull(index) }
                if (focused != null) {
                    MonthlySummaryMediaItem(focused, autoPlayVideo = autoPlayVideo, onVideoComplete = onVideoComplete)
                    Text(
                        "${focusMediaIndex + 1} / ${slide.photos.size}",
                        color = Color.White.copy(alpha = 0.78f),
                        style = MaterialTheme.typography.labelSmall
                    )
                } else {
                    slide.photos.take(6).forEach { photo ->
                        MonthlySummaryMediaItem(photo)
                    }
                }
            }
            if (textEnabled) Text(slide.text, color = Color.White, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun MonthlySummaryMediaItem(
    photo: MonthlyPhotoItem,
    autoPlayVideo: Boolean = false,
    onVideoComplete: (() -> Unit)? = null
) {
    if (photo.mediaType == DiaryMediaType.VIDEO) {
        DiaryVideoPlayer(
            uri = photo.mediaUri,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(photo.aspectRatio.toRatio())
                .clip(RoundedCornerShape(16.dp)),
            showControls = true,
            autoPlay = autoPlayVideo,
            onCompletion = onVideoComplete
        )
    } else {
        AsyncImage(
            model = photo.mediaUri,
            contentDescription = "月度照片",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxWidth().aspectRatio(photo.aspectRatio.toRatio()).clip(RoundedCornerShape(16.dp)),
            error = androidx.compose.ui.res.painterResource(android.R.drawable.ic_menu_report_image),
            placeholder = androidx.compose.ui.res.painterResource(android.R.drawable.ic_menu_gallery)
        )
    }
}

@Composable
private fun MonthlySummaryEmpty(monthText: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Color.White.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = Color.White.copy(alpha = 0.82f), modifier = Modifier.size(48.dp))
            Text("$monthText 暂无日记和照片记录", color = Color.White)
        }
    }
}

private fun String.toRatio(): Float {
    val parts = split(":")
    val width = parts.getOrNull(0)?.toFloatOrNull()
    val height = parts.getOrNull(1)?.toFloatOrNull()
    return if (width != null && height != null && height > 0f) width / height else 16f / 9f
}

private fun addMonths(timestamp: Long, offset: Int): Long {
    return java.util.Calendar.getInstance().apply {
        timeInMillis = timestamp
        add(java.util.Calendar.MONTH, offset)
    }.timeInMillis
}

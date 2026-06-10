package com.memoriabox.utils

import com.memoriabox.data.model.DiaryEntry
import com.memoriabox.data.model.DiaryMedia
import com.memoriabox.data.model.DiaryMediaType
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class MonthlyPhotoItem(
    val diaryId: String,
    val dateStart: Long,
    val mediaUri: String,
    val mediaType: DiaryMediaType,
    val aspectRatio: String,
    val caption: String
)

data class MonthlySummarySlide(
    val dateStart: Long,
    val diaryIds: List<String>,
    val photos: List<MonthlyPhotoItem>,
    val text: String,
    val diaryCount: Int
)

enum class MonthlySummaryStatus {
    READY,
    EMPTY,
    LOADING,
    ERROR
}

data class MonthlySummaryUiState(
    val monthStart: Long = startOfMonth(System.currentTimeMillis()),
    val slides: List<MonthlySummarySlide> = emptyList(),
    val summaryText: String = "",
    val summaryStatus: MonthlySummaryStatus = MonthlySummaryStatus.EMPTY,
    val selectedIndex: Int = 0,
    val isSummaryEnabled: Boolean = true,
    val isPlayMode: Boolean = false,
    val playSpeedFactor: Float = 1.0f,
    val isLoading: Boolean = false
)

object MonthlySummaryHelper {
    private val monthFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())
    private val dayFormat = SimpleDateFormat("M月d日", Locale.getDefault())

    fun buildSummary(
        monthStart: Long,
        diaries: List<DiaryEntry>,
        media: List<DiaryMedia>,
        summaryEnabled: Boolean,
        playMode: Boolean,
        playSpeedFactor: Float
    ): MonthlySummaryUiState {
        val sortedDiaries = diaries.sortedWith(compareBy<DiaryEntry> { startOfDay(it.dateStart) }.thenBy { it.createdAt })
        val mediaByDiary = media.groupBy { it.diaryId }
        val slides = sortedDiaries.groupBy { startOfDay(it.dateStart) }.toSortedMap().map { (dateStart, dayDiaries) ->
            val photos = dayDiaries.flatMap { diary ->
                val backgroundItem = diary.backgroundMediaUri?.let { uri ->
                    MonthlyPhotoItem(
                        diaryId = diary.id,
                        dateStart = dateStart,
                        mediaUri = uri,
                        mediaType = diary.backgroundMediaType ?: DiaryMediaType.IMAGE,
                        aspectRatio = "16:9",
                        caption = diary.content.take(40)
                    )
                }
                val attachedItems = mediaByDiary[diary.id].orEmpty().sortedBy { it.sortOrder }.map { item ->
                    MonthlyPhotoItem(
                        diaryId = diary.id,
                        dateStart = dateStart,
                        mediaUri = item.mediaUri,
                        mediaType = item.mediaType,
                        aspectRatio = item.aspectRatio,
                        caption = diary.content.take(40)
                    )
                }
                listOfNotNull(backgroundItem) + attachedItems
            }
            MonthlySummarySlide(
                dateStart = dateStart,
                diaryIds = dayDiaries.map { it.id },
                photos = photos,
                text = buildDayText(dateStart, dayDiaries, photos.size),
                diaryCount = dayDiaries.size
            )
        }

        val mediaCount = slides.sumOf { it.photos.size }
        val summaryText = buildMonthText(monthStart, sortedDiaries, slides, mediaCount)
        val status = if (sortedDiaries.isEmpty() && mediaCount == 0) MonthlySummaryStatus.EMPTY else MonthlySummaryStatus.READY
        return MonthlySummaryUiState(
            monthStart = monthStart,
            slides = slides,
            summaryText = summaryText,
            summaryStatus = status,
            selectedIndex = 0,
            isSummaryEnabled = summaryEnabled,
            isPlayMode = playMode,
            playSpeedFactor = playSpeedFactor.coerceIn(0.5f, 2.0f),
            isLoading = false
        )
    }

    fun monthKey(timestamp: Long): String = monthFormat.format(timestamp)

    fun previousMonthStart(now: Long = System.currentTimeMillis()): Long {
        return Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.MONTH, -1)
        }.timeInMillis
    }

    fun isNewMonthFirstOpenCandidate(now: Long = System.currentTimeMillis()): Boolean {
        return Calendar.getInstance().apply { timeInMillis = now }.get(Calendar.DAY_OF_MONTH) == 1
    }

    fun monthRange(monthStart: Long): Pair<Long, Long> {
        val start = startOfMonth(monthStart)
        val end = Calendar.getInstance().apply {
            timeInMillis = start
            add(Calendar.MONTH, 1)
            add(Calendar.MILLISECOND, -1)
        }.timeInMillis
        return start to end
    }

    fun slideDelayMillis(speedFactor: Float): Long {
        return (3000L / speedFactor.coerceIn(0.5f, 2.0f)).toLong().coerceIn(1500L, 6000L)
    }

    private fun buildMonthText(
        monthStart: Long,
        diaries: List<DiaryEntry>,
        slides: List<MonthlySummarySlide>,
        mediaCount: Int
    ): String {
        if (diaries.isEmpty() && mediaCount == 0) {
            return "${monthFormat.format(monthStart)} 暂无日记和媒体记录。"
        }
        val lines = mutableListOf("${monthFormat.format(monthStart)} 记录了 ${diaries.size} 篇日记，留下了 $mediaCount 个媒体。")
        slides.take(8).forEach { slide -> lines.add("${dayFormat.format(slide.dateStart)}：${slide.text}") }
        if (slides.size > 8) lines.add("还有 ${slides.size - 8} 天的记录，打开月度总结继续查看。")
        return lines.joinToString("\n")
    }

    private fun buildDayText(dateStart: Long, diaries: List<DiaryEntry>, mediaCount: Int): String {
        val content = diaries.map { it.content.trim() }.filter { it.isNotBlank() }.joinToString("；") { it.take(40) }
        return when {
            content.isNotBlank() && mediaCount > 0 -> "$content，留下了 $mediaCount 个媒体。"
            content.isNotBlank() -> content
            mediaCount > 0 -> "这一天留下了 $mediaCount 个媒体。"
            else -> "这一天留下了一篇日记。"
        }
    }
}

private fun startOfDay(timestamp: Long): Long {
    return Calendar.getInstance().apply {
        timeInMillis = timestamp
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}

fun startOfMonth(timestamp: Long): Long {
    return Calendar.getInstance().apply {
        timeInMillis = timestamp
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}

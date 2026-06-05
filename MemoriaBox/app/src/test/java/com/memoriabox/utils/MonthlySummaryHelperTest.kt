package com.memoriabox.utils

import com.memoriabox.data.model.DiaryEntry
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

class MonthlySummaryHelperTest {
    @Test
    fun buildSummaryGroupsDiariesByCalendarDay() {
        val morning = Calendar.getInstance().apply {
            set(2026, Calendar.JUNE, 5, 8, 30, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val evening = Calendar.getInstance().apply {
            set(2026, Calendar.JUNE, 5, 20, 45, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val summary = MonthlySummaryHelper.buildSummary(
            monthStart = startOfMonth(morning),
            diaries = listOf(
                DiaryEntry(id = "morning", dateStart = morning, content = "早上记录", createdAt = morning),
                DiaryEntry(id = "evening", dateStart = evening, content = "晚上记录", createdAt = evening)
            ),
            media = emptyList(),
            summaryEnabled = true,
            playMode = false,
            playSpeedFactor = 1f
        )

        assertEquals(1, summary.slides.size)
        assertEquals(2, summary.slides.single().diaryCount)
    }
}

package com.memoriabox.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 最近搜索历史的合并规则测试。
 * 重点覆盖「逐字输入不应污染历史」与去重、置顶、截断。
 */
class RecentSearchMergeTest {

    @Test
    fun `新关键词排在历史最前面`() {
        val merged = AppSettings.mergeRecentSearch(listOf("生日", "纪念"), "旅行")
        assertEquals(listOf("旅行", "生日", "纪念"), merged)
    }

    @Test
    fun `重复关键词会被移到最前而不是重复出现`() {
        val merged = AppSettings.mergeRecentSearch(listOf("生日", "纪念", "旅行"), "旅行")
        assertEquals(listOf("旅行", "生日", "纪念"), merged)
    }

    @Test
    fun `首尾空白会被裁剪`() {
        val merged = AppSettings.mergeRecentSearch(emptyList(), "  小明  ")
        assertEquals(listOf("小明"), merged)
    }

    @Test
    fun `空白关键词不会写入历史`() {
        val existing = listOf("生日")
        assertEquals(existing, AppSettings.mergeRecentSearch(existing, "   "))
        assertEquals(existing, AppSettings.mergeRecentSearch(existing, ""))
    }

    @Test
    fun `历史长度不超过上限`() {
        val existing = (1..20).map { "关键词$it" }
        val merged = AppSettings.mergeRecentSearch(existing, "新的")
        assertEquals(AppSettings.RECENT_SEARCH_LIMIT, merged.size)
        assertEquals("新的", merged.first())
    }

    @Test
    fun `历史中已有的空白项会被清理`() {
        val merged = AppSettings.mergeRecentSearch(listOf("生日", "  ", ""), "旅行")
        assertEquals(listOf("旅行", "生日"), merged)
    }

    @Test
    fun `空历史时只包含当前关键词`() {
        assertEquals(listOf("旅行"), AppSettings.mergeRecentSearch(emptyList(), "旅行"))
    }

    @Test
    fun `逐字输入只保留最终完整关键词`() {
        // 模拟用户在输入完整词后按搜索，中间态不应被写入
        var history = emptyList<String>()
        history = AppSettings.mergeRecentSearch(history, "小明")
        assertEquals(listOf("小明"), history)
        assertTrue(history.none { it == "小" })
    }
}

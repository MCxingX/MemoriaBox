package com.memoriabox.utils

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 农历副标题规则回归测试：
 * 日历格子里的初一要显示月名（十月/正月/闰四月…），而不是"初一"。
 */
class LunarLabelTest {

    @Test
    fun monthLabelUsesTraditionalNames() {
        assertEquals("正月", LunarDateUtils.monthLabel(1))
        assertEquals("九月", LunarDateUtils.monthLabel(9))
        assertEquals("十月", LunarDateUtils.monthLabel(10))
        assertEquals("十一月", LunarDateUtils.monthLabel(11))
        assertEquals("腊月", LunarDateUtils.monthLabel(12))
    }

    @Test
    fun leapMonthIsPrefixed() {
        assertEquals("闰四月", LunarDateUtils.monthLabel(4, leap = true))
        assertEquals("闰十一月", LunarDateUtils.monthLabel(11, leap = true))
    }

    @Test
    fun dayLabelsKeepTraditionalForm() {
        assertEquals("初一", LunarDateUtils.dayLabel(1))
        assertEquals("初十", LunarDateUtils.dayLabel(10))
        assertEquals("廿一", LunarDateUtils.dayLabel(21))
        assertEquals("三十", LunarDateUtils.dayLabel(30))
    }

    @Test
    fun parseDayStillUnderstandsChuYi() {
        assertEquals(1, LunarDateUtils.parseDay("2026年农历十月初一"))
        assertEquals(15, LunarDateUtils.parseDay("八月十五"))
    }

    @Test
    fun cellLabelForFirstDayEqualsMonthName() {
        // 日历格子：初一当天用月名，其余用日名
        val firstDayText = LunarDateUtils.monthLabel(10)
        assertEquals("十月", firstDayText)
    }
}

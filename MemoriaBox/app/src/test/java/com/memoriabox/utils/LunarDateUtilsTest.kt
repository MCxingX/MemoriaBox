package com.memoriabox.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 覆盖农历字符串解析的回归用例。
 * 重点防止"十一月"被"十月"抢先命中、"二十一"被"二十"抢先命中这类前缀误判。
 */
class LunarDateUtilsTest {

    @Test
    fun `十一月不会被解析成十月`() {
        val parsed = LunarDateUtils.parseMonthDayWithLeap("农历十一月初五")
        assertNotNull(parsed)
        assertEquals(11, parsed!!.first)
        assertEquals(5, parsed.second)
    }

    @Test
    fun `腊月不会被解析成十月`() {
        val parsed = LunarDateUtils.parseMonthDayWithLeap("腊月廿九")
        assertNotNull(parsed)
        assertEquals(12, parsed!!.first)
        assertEquals(29, parsed.second)
    }

    @Test
    fun `二十一不会被解析成二十`() {
        assertEquals(21, LunarDateUtils.parseDay("正月二十一"))
    }

    @Test
    fun `初十不会被解析成初一`() {
        assertEquals(10, LunarDateUtils.parseDay("三月初十"))
    }

    @Test
    fun `三十正确解析`() {
        assertEquals(30, LunarDateUtils.parseDay("腊月三十"))
    }

    @Test
    fun `闰月会被标记`() {
        val parsed = LunarDateUtils.parseMonthDayWithLeap("闰四月初八")
        assertNotNull(parsed)
        assertEquals(4, parsed!!.first)
        assertEquals(8, parsed.second)
        assertTrue(parsed.third)
    }

    @Test
    fun `非闰月不会被误标为闰月`() {
        val parsed = LunarDateUtils.parseMonthDayWithLeap("四月初八")
        assertNotNull(parsed)
        assertFalse(parsed!!.third)
    }

    @Test
    fun `月名标签保留闰月前缀`() {
        assertEquals("闰四月", LunarDateUtils.monthLabel(4, leap = true))
        assertEquals("正月", LunarDateUtils.monthLabel(1, leap = false))
        assertEquals("腊月", LunarDateUtils.monthLabel(12, leap = false))
    }
}

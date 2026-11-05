package com.memoriabox.ui.screen.components

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 日历视图切换的契约测试。
 * 月视图是默认入口，顺序固定为 月 → 周 → 议程，
 * 因为界面用序号持久化选择，顺序变化会导致旧状态错位。
 */
class CalendarDisplayModeTest {

    @Test
    fun `默认视图为月`() {
        val defaultIndex = 0
        assertEquals("月", calendarDisplayModeLabel(defaultIndex))
    }

    @Test
    fun `三种视图顺序固定`() {
        assertEquals(listOf("月", "周", "议程"), calendarDisplayModeLabels())
    }

    @Test
    fun `序号越界时夹到有效范围`() {
        assertEquals("月", calendarDisplayModeLabel(-1))
        assertEquals("议程", calendarDisplayModeLabel(99))
    }

    @Test
    fun `序号可以正确映射到对应视图`() {
        assertEquals("周", calendarDisplayModeLabel(1))
        assertEquals("议程", calendarDisplayModeLabel(2))
    }
}

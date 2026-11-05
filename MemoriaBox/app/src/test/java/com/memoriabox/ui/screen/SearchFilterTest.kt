package com.memoriabox.ui.screen

import com.memoriabox.data.model.Event
import com.memoriabox.data.model.EventType
import com.memoriabox.data.model.RepeatMode
import com.memoriabox.data.model.TodoStatus
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 搜索页的筛选逻辑测试。
 * 只覆盖纯判断分支，不依赖 Compose 或 Android 运行时。
 */
class SearchFilterTest {

    private fun event(
        name: String = "纪念日",
        note: String = "",
        type: EventType = EventType.COUNTDOWN,
        pinned: Boolean = false,
        repeat: RepeatMode = RepeatMode.NONE,
        repeatYearly: Boolean = false,
        avatar: String? = null,
        reminder: Boolean = false
    ) = Event(
        boxId = "box-1",
        name = name,
        date = 1_700_000_000_000L,
        type = type,
        note = note,
        isPinned = pinned,
        repeatMode = repeat,
        repeatYearly = repeatYearly,
        avatarUri = avatar,
        reminderEnabled = reminder,
        todoStatus = TodoStatus.PENDING
    )

    @Test
    fun `全部筛选返回真`() {
        assertTrue(matchesEventFilter(event(), EventFilter.ALL))
    }

    @Test
    fun `置顶筛选只匹配置顶`() {
        assertTrue(matchesEventFilter(event(pinned = true), EventFilter.PINNED))
        assertFalse(matchesEventFilter(event(pinned = false), EventFilter.PINNED))
    }

    @Test
    fun `重复筛选识别重复模式与周年`() {
        assertTrue(matchesEventFilter(event(repeat = RepeatMode.MONTHLY), EventFilter.REPEATING))
        assertTrue(matchesEventFilter(event(repeatYearly = true), EventFilter.REPEATING))
        assertTrue(matchesEventFilter(event(type = EventType.BIRTHDAY), EventFilter.REPEATING))
        assertFalse(matchesEventFilter(event(), EventFilter.REPEATING))
    }

    @Test
    fun `有图筛选依据头像地址`() {
        assertTrue(matchesEventFilter(event(avatar = "content://media/1"), EventFilter.WITH_IMAGE))
        assertFalse(matchesEventFilter(event(avatar = null), EventFilter.WITH_IMAGE))
    }

    @Test
    fun `提醒筛选依据提醒开关`() {
        assertTrue(matchesEventFilter(event(reminder = true), EventFilter.REMINDER))
        assertFalse(matchesEventFilter(event(reminder = false), EventFilter.REMINDER))
    }

    @Test
    fun `生日与待办筛选按类型匹配`() {
        assertTrue(matchesEventFilter(event(type = EventType.BIRTHDAY), EventFilter.BIRTHDAY))
        assertFalse(matchesEventFilter(event(type = EventType.COUNTDOWN), EventFilter.BIRTHDAY))
        assertTrue(matchesEventFilter(event(type = EventType.TODO), EventFilter.TODO))
        assertFalse(matchesEventFilter(event(type = EventType.COUNTDOWN), EventFilter.TODO))
    }

    @Test
    fun `按名称与备注可以命中关键字`() {
        val target = event(name = "小明生日", note = "准备蛋糕")
        assertTrue(target.name.contains("生日", ignoreCase = true))
        assertTrue(target.note.contains("蛋糕", ignoreCase = true))
        assertFalse(target.name.contains("毕业", ignoreCase = true))
    }
}

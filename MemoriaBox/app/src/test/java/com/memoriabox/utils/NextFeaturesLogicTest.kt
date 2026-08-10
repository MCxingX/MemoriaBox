package com.memoriabox.utils

import com.memoriabox.data.model.TodoStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NextFeaturesLogicTest {

    @Test
    fun moodLevelIsClampedToLowerBound() {
        assertEquals(1, NextFeaturesLogic.coerceMoodLevel(0))
        assertEquals(1, NextFeaturesLogic.coerceMoodLevel(-5))
        assertEquals(1, NextFeaturesLogic.coerceMoodLevel(1))
    }

    @Test
    fun moodLevelIsClampedToUpperBound() {
        assertEquals(5, NextFeaturesLogic.coerceMoodLevel(6))
        assertEquals(5, NextFeaturesLogic.coerceMoodLevel(99))
        assertEquals(5, NextFeaturesLogic.coerceMoodLevel(5))
    }

    @Test
    fun moodLevelKeepsValidMiddleValues() {
        assertEquals(3, NextFeaturesLogic.coerceMoodLevel(3))
        assertEquals(2, NextFeaturesLogic.coerceMoodLevel(2))
        assertEquals(4, NextFeaturesLogic.coerceMoodLevel(4))
    }

    @Test
    fun pendingTodoWithPastDueIsOverdue() {
        val now = 1_000_000L
        assertTrue(NextFeaturesLogic.isTodoOverdue(TodoStatus.PENDING, dueDate = 999_999L, now = now))
    }

    @Test
    fun completedTodoIsNeverOverdue() {
        val now = 1_000_000L
        assertFalse(NextFeaturesLogic.isTodoOverdue(TodoStatus.COMPLETED, dueDate = 999_999L, now = now))
    }

    @Test
    fun todoWithoutDueDateIsNeverOverdue() {
        val now = 1_000_000L
        assertFalse(NextFeaturesLogic.isTodoOverdue(TodoStatus.PENDING, dueDate = null, now = now))
    }

    @Test
    fun futureDueTodoIsNotOverdue() {
        val now = 1_000_000L
        assertFalse(NextFeaturesLogic.isTodoOverdue(TodoStatus.PENDING, dueDate = 1_000_001L, now = now))
    }

    @Test
    fun leapYearDetection() {
        assertTrue(NextFeaturesLogic.isLeapYear(2024))
        assertTrue(NextFeaturesLogic.isLeapYear(2000))
        assertFalse(NextFeaturesLogic.isLeapYear(2025))
        assertFalse(NextFeaturesLogic.isLeapYear(1900))
    }
}

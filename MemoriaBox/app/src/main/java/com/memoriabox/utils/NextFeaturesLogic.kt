package com.memoriabox.utils

import com.memoriabox.data.model.TodoStatus

object NextFeaturesLogic {
    fun coerceMoodLevel(level: Int): Int = level.coerceIn(1, 5)

    fun isTodoOverdue(status: TodoStatus, dueDate: Long?, now: Long): Boolean =
        status == TodoStatus.PENDING && dueDate != null && dueDate < now

    fun isLeapYear(year: Int): Boolean =
        (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)
}

package com.memoriabox.utils

import java.util.Calendar

object AnnualDateUtils {
    fun nextOccurrenceMillis(dateMillis: Long, nowMillis: Long = System.currentTimeMillis()): Long {
        val today = startOfDay(nowMillis)
        val source = Calendar.getInstance().apply { timeInMillis = dateMillis }
        val target = Calendar.getInstance().apply {
            timeInMillis = today
            set(Calendar.MONTH, source.get(Calendar.MONTH))
            set(Calendar.DAY_OF_MONTH, source.get(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (target.before(today)) target.add(Calendar.YEAR, 1)
        return target.timeInMillis
    }

    fun daysUntil(dateMillis: Long, nowMillis: Long = System.currentTimeMillis()): Long {
        val today = startOfDay(nowMillis)
        val target = nextOccurrenceMillis(dateMillis, nowMillis)
        val cursor = Calendar.getInstance().apply { timeInMillis = today }
        var days = 0L
        while (cursor.timeInMillis < target) {
            cursor.add(Calendar.DAY_OF_MONTH, 1)
            days++
        }
        return days
    }

    private fun startOfDay(timeMillis: Long): Long = Calendar.getInstance().apply {
        timeInMillis = timeMillis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}

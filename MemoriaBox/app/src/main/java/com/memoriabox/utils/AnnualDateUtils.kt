package com.memoriabox.utils

import java.util.Calendar
import java.util.TimeZone
import java.util.concurrent.TimeUnit

object AnnualDateUtils {
    private val BEIJING_TZ = TimeZone.getTimeZone("Asia/Shanghai")

    fun nextOccurrenceMillis(dateMillis: Long, nowMillis: Long = System.currentTimeMillis()): Long {
        val today = startOfDay(nowMillis)
        val source = Calendar.getInstance(BEIJING_TZ).apply { timeInMillis = dateMillis }
        val target = Calendar.getInstance(BEIJING_TZ).apply {
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
        return TimeUnit.MILLISECONDS.toDays(target - today).coerceAtLeast(0)
    }

    private fun startOfDay(timeMillis: Long): Long = Calendar.getInstance(BEIJING_TZ).apply {
        timeInMillis = timeMillis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}

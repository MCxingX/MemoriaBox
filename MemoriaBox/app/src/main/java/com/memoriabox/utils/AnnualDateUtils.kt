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
            // 先把日号归到 1 再改月份：否则今天若是 31 号，
            // 直接 set(MONTH) 会得到「4 月 31 日」并被 Calendar 顺延成 5 月 1 日，
            // 之后的 set(DAY_OF_MONTH) 就落到了错误的月份上。
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.MONTH, source.get(Calendar.MONTH))
            // 目标日在目标月不存在时（如 2 月 30 日）夹到该月最后一天
            val maxDay = getActualMaximum(Calendar.DAY_OF_MONTH)
            set(Calendar.DAY_OF_MONTH, source.get(Calendar.DAY_OF_MONTH).coerceAtMost(maxDay))
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (target.timeInMillis < today) target.add(Calendar.YEAR, 1)
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

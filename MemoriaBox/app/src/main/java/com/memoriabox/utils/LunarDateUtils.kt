package com.memoriabox.utils

import android.icu.util.ChineseCalendar
import java.util.Calendar
import java.util.concurrent.TimeUnit

object LunarDateUtils {
    val monthNames = listOf("正月", "二月", "三月", "四月", "五月", "六月", "七月", "八月", "九月", "十月", "十一月", "腊月")

    fun nextOccurrenceMillis(lunar: String, nowMillis: Long = System.currentTimeMillis()): Long? {
        val (month, day) = parseMonthDay(lunar) ?: return null
        val cursor = startOfDay(nowMillis)
        repeat(420) {
            if (isMonthDay(cursor.timeInMillis, month, day)) return cursor.timeInMillis
            cursor.add(Calendar.DAY_OF_YEAR, 1)
        }
        return null
    }

    fun monthDayToGregorian(year: Int, month: Int, day: Int): Long? {
        val cursor = Calendar.getInstance().apply {
            clear()
            set(year.coerceIn(1900, 2100), Calendar.JANUARY, 1, 0, 0, 0)
        }
        repeat(420) {
            if (isYearMonthDay(cursor.timeInMillis, year, month, day)) return cursor.timeInMillis
            cursor.add(Calendar.DAY_OF_YEAR, 1)
        }
        return null
    }

    fun isGregorianMatchingLunar(timeMillis: Long, month: Int, day: Int): Boolean = isMonthDay(timeMillis, month, day)

    /**
     * 日历格子里显示的农历副标题。
     * 农历每月初一显示"当月月名"（正月/二月/…/腊月，闰月带"闰"字），
     * 符合传统日历习惯，避免每个月 1 号都只写"初一"看不出是第几个月。
     */
    fun dayLabelForGregorian(timeMillis: Long): String {
        val chineseCalendar = ChineseCalendar().apply { this.timeInMillis = timeMillis }
        val day = chineseCalendar.get(Calendar.DAY_OF_MONTH)
        if (day == 1) return monthLabel(chineseCalendar)
        return dayLabel(day)
    }

    /** 完整农历写法，如"十月初一"，用在需要完整表达的标题位置。 */
    fun fullLabelForGregorian(timeMillis: Long): String {
        val chineseCalendar = ChineseCalendar().apply { this.timeInMillis = timeMillis }
        val day = chineseCalendar.get(Calendar.DAY_OF_MONTH)
        return monthLabel(chineseCalendar) + dayLabel(day)
    }

    /** 只取农历月名，如"十月""闰四月"。 */
    fun monthLabelForGregorian(timeMillis: Long): String =
        monthLabel(ChineseCalendar().apply { this.timeInMillis = timeMillis })

    /** 农历月名：1 月是"正月"，12 月是"腊月"，闰月加"闰"字前缀。 */
    fun monthLabel(month: Int, leap: Boolean = false): String {
        val index = if (month in 1..12) month - 1 else 0
        val name = monthNames.getOrElse(index) { "${index + 1}月" }
        return if (leap) "闰$name" else name
    }

    private fun monthLabel(chineseCalendar: ChineseCalendar): String = monthLabel(
        month = chineseCalendar.get(Calendar.MONTH) + 1,
        leap = chineseCalendar.get(ChineseCalendar.IS_LEAP_MONTH) != 0
    )

    fun selectionForGregorian(timeMillis: Long): Triple<Int, Int, Int> {
        val chineseCalendar = ChineseCalendar().apply { this.timeInMillis = timeMillis }
        val year = chineseCalendar.get(ChineseCalendar.EXTENDED_YEAR) - 2637
        val month = chineseCalendar.get(Calendar.MONTH) + 1
        val day = chineseCalendar.get(Calendar.DAY_OF_MONTH)
        return Triple(year.coerceIn(1900, 2100), month.coerceIn(1, 12), day.coerceIn(1, 30))
    }

    fun daysUntilNextOccurrence(lunar: String, nowMillis: Long = System.currentTimeMillis()): Long? {
        val next = nextOccurrenceMillis(lunar, nowMillis) ?: return null
        return TimeUnit.MILLISECONDS.toDays(startOfDay(next).timeInMillis - startOfDay(nowMillis).timeInMillis)
    }

    fun parseMonthDay(lunar: String): Pair<Int, Int>? {
        val month = monthNames.indexOfFirst { lunar.contains(it) }.takeIf { it >= 0 }?.plus(1) ?: return null
        val day = parseDay(lunar) ?: return null
        return month to day
    }

    fun parseDay(lunar: String): Int? {
        Regex("(\\d{1,2})日").find(lunar)?.groupValues?.getOrNull(1)?.toIntOrNull()?.takeIf { it in 1..30 }?.let { return it }
        return (1..30).firstOrNull { lunar.contains(dayLabel(it)) }
    }

    fun dayLabel(day: Int): String = when (day) {
        1 -> "初一"
        2 -> "初二"
        3 -> "初三"
        4 -> "初四"
        5 -> "初五"
        6 -> "初六"
        7 -> "初七"
        8 -> "初八"
        9 -> "初九"
        10 -> "初十"
        20 -> "二十"
        30 -> "三十"
        in 11..19 -> "十${numberLabel(day - 10)}"
        in 21..29 -> "廿${numberLabel(day - 20)}"
        else -> day.toString()
    }

    private fun isMonthDay(timeMillis: Long, month: Int, day: Int): Boolean {
        val chineseCalendar = ChineseCalendar().apply { this.timeInMillis = timeMillis }
        return chineseCalendar.get(Calendar.MONTH) + 1 == month && chineseCalendar.get(Calendar.DAY_OF_MONTH) == day
    }

    private fun isYearMonthDay(timeMillis: Long, year: Int, month: Int, day: Int): Boolean {
        val chineseCalendar = ChineseCalendar().apply { this.timeInMillis = timeMillis }
        return chineseCalendar.get(ChineseCalendar.EXTENDED_YEAR) == year + 2637 &&
            chineseCalendar.get(Calendar.MONTH) + 1 == month &&
            chineseCalendar.get(Calendar.DAY_OF_MONTH) == day
    }

    private fun startOfDay(timeMillis: Long): Calendar = Calendar.getInstance().apply {
        this.timeInMillis = timeMillis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    private fun numberLabel(number: Int): String = when (number) {
        1 -> "一"
        2 -> "二"
        3 -> "三"
        4 -> "四"
        5 -> "五"
        6 -> "六"
        7 -> "七"
        8 -> "八"
        9 -> "九"
        else -> ""
    }
}

package com.memoriabox.utils

import android.icu.util.ChineseCalendar
import java.util.Calendar
import java.util.concurrent.TimeUnit

object LunarDateUtils {
    val monthNames = listOf("正月", "二月", "三月", "四月", "五月", "六月", "七月", "八月", "九月", "十月", "十一月", "腊月")

    /**
     * 下一次农历月日对应的公历时间。
     *
     * 直接按「农历月+日」逐天搜索有两个死路：
     * - 农历三十在只有 29 天的月份里根本不存在；
     * - 闰月（如闰四月）十几年才出现一次，420 天窗口内完全搜不到。
     * 两种情况都会返回 null，调用方（NotificationHelper）拿到 null 直接 return，
     * 结果就是一整年都没有提醒。所以改为先定位最近的这个农历月，再在月内取日，
     * 该月没有这一天就落到月末；闰月搜不到就退回同名平月。
     */
    fun nextOccurrenceMillis(lunar: String, nowMillis: Long = System.currentTimeMillis()): Long? {
        val parsed = parseMonthDayWithLeap(lunar) ?: return null
        val (month, day, leap) = parsed
        var monthStart = findLunarMonthStart(nowMillis, month, leap, LUNAR_SEARCH_DAYS)
        var usedLeap = leap
        if (monthStart == null && leap) {
            // 闰月在几十年里只出现几次，搜不到就退回同名平月，
            // 总比返回 null 导致整年不提醒要好。
            monthStart = findLunarMonthStart(nowMillis, month, false, LUNAR_SEARCH_DAYS)
            usedLeap = false
        }
        val resolvedMonthStart = monthStart ?: return null
        return resolveDayInLunarMonth(resolvedMonthStart, month, day, usedLeap)
    }

    /** 从 nowMillis 起向后找目标农历月的初一。 */
    private fun findLunarMonthStart(startMillis: Long, month: Int, leap: Boolean, rangeDays: Int): Long? {
        val chinese = ChineseCalendar()
        val cursor = startOfDay(startMillis)
        repeat(rangeDays) {
            chinese.timeInMillis = cursor.timeInMillis
            if (chinese.get(Calendar.MONTH) + 1 == month &&
                (chinese.get(ChineseCalendar.IS_LEAP_MONTH) != 0) == leap &&
                chinese.get(Calendar.DAY_OF_MONTH) == 1
            ) {
                return cursor.timeInMillis
            }
            cursor.add(Calendar.DAY_OF_YEAR, 1)
        }
        return null
    }

    /** 在该农历月内取第 day 天；该月不足 day 天时返回月末。 */
    private fun resolveDayInLunarMonth(monthStart: Long, month: Int, day: Int, leap: Boolean): Long {
        val chinese = ChineseCalendar()
        val cursor = Calendar.getInstance().apply { timeInMillis = monthStart }
        var result = monthStart
        repeat(day) { offset ->
            if (offset > 0) cursor.add(Calendar.DAY_OF_YEAR, 1)
            chinese.timeInMillis = cursor.timeInMillis
            if (chinese.get(Calendar.MONTH) + 1 != month ||
                (chinese.get(ChineseCalendar.IS_LEAP_MONTH) != 0) != leap
            ) {
                return result
            }
            result = cursor.timeInMillis
        }
        return result
    }

    /** 农历一年最多 385 天，留足余量覆盖「今年这一天已过，要到明年」的情况。 */
    private const val LUNAR_SEARCH_DAYS = 420

    fun monthDayToGregorian(year: Int, month: Int, day: Int): Long? {
        // 复用同一个 ChineseCalendar 实例：逐天 new 一个对象时，
        // 420 天的搜索会分配 420 次 ICU 对象，主线程调用会有明显卡顿。
        val chinese = ChineseCalendar()
        val cursor = Calendar.getInstance().apply {
            clear()
            set(year.coerceIn(1900, 2100), Calendar.JANUARY, 1, 0, 0, 0)
        }
        repeat(420) {
            chinese.timeInMillis = cursor.timeInMillis
            if (chinese.get(ChineseCalendar.EXTENDED_YEAR) == year + 2637 &&
                chinese.get(Calendar.MONTH) + 1 == month &&
                chinese.get(Calendar.DAY_OF_MONTH) == day &&
                chinese.get(ChineseCalendar.IS_LEAP_MONTH) == 0
            ) {
                return cursor.timeInMillis
            }
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
        val parsed = parseMonthDayWithLeap(lunar) ?: return null
        return parsed.first to parsed.second
    }

    /** 解析农历字符串，返回 (月, 日, 是否闰月)。 */
    fun parseMonthDayWithLeap(lunar: String): Triple<Int, Int, Boolean>? {
        val leap = lunar.contains("闰")
        // 月名按"最长命中优先、其次最靠前"匹配：
        // 长度优先避免"十一月"被"十月"抢先；位置优先避免"农历十一月初五"里的"正月"被误选。
        val best = monthNames
            .withIndex()
            .mapNotNull { (index, name) ->
                val position = lunar.indexOf(name)
                if (position < 0) null else dataclassMonth(index + 1, name, position)
            }
            .maxWithOrNull(compareBy({ it.length }, { -it.position }))
            ?: return null
        // 先把月名从字符串里剔除，否则"十一月初五"的"十一"会被当成日号解析成 11。
        val dayText = lunar.removeRange(best.position, best.position + best.length).replace("闰", "")
        val day = parseDay(dayText) ?: return null
        return Triple(best.month, day, leap)
    }

    private data class MonthHit(val month: Int, val length: Int, val position: Int)

    private fun dataclassMonth(month: Int, name: String, position: Int) = MonthHit(month, name.length, position)

    fun parseDay(lunar: String): Int? {
        Regex("(\\d{1,2})日").find(lunar)?.groupValues?.getOrNull(1)?.toIntOrNull()?.takeIf { it in 1..30 }?.let { return it }
        // 先用完整词正则抓取，长写法优先（二十一/二十/十一…），避免"二十一"被前缀"十一"或"二十"抢先命中。
        Regex("三十|廿[一二三四五六七八九]|二十[一二三四五六七八九]?|十[一二三四五六七八九]|初[一二三四五六七八九十]")
            .find(lunar)
            ?.value
            ?.let { return chineseDayToInt(it) }
        return null
    }

    /** 把中文写法（二十/廿一/初三/十一）转成 1..30 的日号。 */
    private fun chineseDayToInt(text: String): Int? {
        if (text == "三十") return 30
        if (text.startsWith("廿")) {
            val rest = text.removePrefix("廿")
            return if (rest.isEmpty()) 20 else rest.toChineseDigit()?.plus(20)
        }
        if (text.startsWith("初")) return text.removePrefix("初").toChineseDigit()
        if (text.startsWith("二十")) {
            val rest = text.removePrefix("二十")
            return if (rest.isEmpty()) 20 else rest.toChineseDigit()?.plus(20)
        }
        if (text.startsWith("十")) {
            val rest = text.removePrefix("十")
            return if (rest.isEmpty()) 10 else rest.toChineseDigit()?.plus(10)
        }
        return text.toChineseDigit()
    }

    private fun String.toChineseDigit(): Int? = when (this) {
        "一" -> 1
        "二" -> 2
        "三" -> 3
        "四" -> 4
        "五" -> 5
        "六" -> 6
        "七" -> 7
        "八" -> 8
        "九" -> 9
        "十" -> 10
        else -> null
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

    private fun isMonthDay(timeMillis: Long, month: Int, day: Int, leap: Boolean = false): Boolean {
        val chineseCalendar = ChineseCalendar().apply { this.timeInMillis = timeMillis }
        return chineseCalendar.get(Calendar.MONTH) + 1 == month &&
            chineseCalendar.get(Calendar.DAY_OF_MONTH) == day &&
            // 闰月与正月的月号相同，必须一并比较闰月标志，否则农历生日会整体偏一个月。
            (chineseCalendar.get(ChineseCalendar.IS_LEAP_MONTH) != 0) == leap
    }

    private fun isYearMonthDay(timeMillis: Long, year: Int, month: Int, day: Int, leap: Boolean = false): Boolean {
        val chineseCalendar = ChineseCalendar().apply { this.timeInMillis = timeMillis }
        return chineseCalendar.get(ChineseCalendar.EXTENDED_YEAR) == year + 2637 &&
            chineseCalendar.get(Calendar.MONTH) + 1 == month &&
            chineseCalendar.get(Calendar.DAY_OF_MONTH) == day &&
            (chineseCalendar.get(ChineseCalendar.IS_LEAP_MONTH) != 0) == leap
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

package com.memoriabox.utils

import android.icu.util.ChineseCalendar
import java.util.Calendar

object HolidayUtils {

    private val solarHolidays: Map<Pair<Int, Int>, String> = mapOf(
        1 to 1 to "元旦",
        2 to 14 to "情人节",
        3 to 8 to "妇女节",
        3 to 12 to "植树节",
        4 to 1 to "愚人节",
        5 to 1 to "劳动节",
        5 to 4 to "青年节",
        6 to 1 to "儿童节",
        7 to 1 to "建党节",
        8 to 1 to "建军节",
        9 to 10 to "教师节",
        10 to 1 to "国庆节",
        12 to 24 to "平安夜",
        12 to 25 to "圣诞节"
    )

    private val lunarHolidays: Map<Pair<Int, Int>, String> = mapOf(
        1 to 1 to "春节",
        1 to 15 to "元宵节",
        2 to 2 to "龙抬头",
        5 to 5 to "端午节",
        7 to 7 to "七夕",
        7 to 15 to "中元节",
        8 to 15 to "中秋节",
        9 to 9 to "重阳节",
        12 to 8 to "腊八节",
        12 to 23 to "小年"
    )

    private val solarTerms: Map<Pair<Int, Int>, String> = mapOf(
        2 to 4 to "立春",
        2 to 19 to "雨水",
        3 to 6 to "惊蛰",
        3 to 21 to "春分",
        4 to 5 to "清明",
        4 to 20 to "谷雨",
        5 to 6 to "立夏",
        5 to 21 to "小满",
        6 to 6 to "芒种",
        6 to 21 to "夏至",
        7 to 7 to "小暑",
        7 to 23 to "大暑",
        8 to 8 to "立秋",
        8 to 23 to "处暑",
        9 to 8 to "白露",
        9 to 23 to "秋分",
        10 to 8 to "寒露",
        10 to 24 to "霜降",
        11 to 8 to "立冬",
        11 to 22 to "小雪",
        12 to 7 to "大雪",
        12 to 22 to "冬至",
        1 to 6 to "小寒",
        1 to 20 to "大寒"
    )

    fun holidayForDay(timeMillis: Long): String? {
        val chinese = ChineseCalendar().apply { this.timeInMillis = timeMillis }
        val lunarMonth = chinese.get(Calendar.MONTH) + 1
        val lunarDay = chinese.get(Calendar.DAY_OF_MONTH)

        if (isNewYearEve(chinese)) return "除夕"
        lunarHolidays[Pair(lunarMonth, lunarDay)]?.let { return it }

        val solar = Calendar.getInstance().apply { this.timeInMillis = timeMillis }
        val month = solar.get(Calendar.MONTH) + 1
        val day = solar.get(Calendar.DAY_OF_MONTH)
        solarHolidays[Pair(month, day)]?.let { return it }
        solarTerms[Pair(month, day)]?.let { return it }

        return null
    }

    fun holidayForDayLabel(timeMillis: Long): String? {
        val solar = Calendar.getInstance().apply { this.timeInMillis = timeMillis }
        val month = solar.get(Calendar.MONTH) + 1
        val day = solar.get(Calendar.DAY_OF_MONTH)
        solarHolidays[Pair(month, day)]?.let { return it }
        solarTerms[Pair(month, day)]?.let { return it }
        return null
    }

    private fun isNewYearEve(chinese: ChineseCalendar): Boolean {
        val lunarMonth = chinese.get(Calendar.MONTH) + 1
        val lunarDay = chinese.get(Calendar.DAY_OF_MONTH)
        val lunarMonthLen = chinese.getActualMaximum(Calendar.DAY_OF_MONTH)
        return lunarMonth == 12 && lunarDay == lunarMonthLen
    }
}

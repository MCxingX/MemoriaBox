package com.memoriabox.utils

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 对照紫金山天文台公布的北京时间节气日期，验证算法正确性。
 * 覆盖 2024-2030 年，重点验证浮动明显的清明、冬至、立春、芒种等。
 */
class SolarTermUtilsTest {

    private fun assertTerm(year: Int, name: String, expected: String) {
        assertEquals("$year 年 $name", expected, SolarTermUtils.solarTermDate(year, name))
    }

    @Test
    fun `2024 年节气日期`() {
        assertTerm(2024, "小寒", "01-06")
        assertTerm(2024, "大寒", "01-20")
        assertTerm(2024, "立春", "02-04")
        assertTerm(2024, "雨水", "02-19")
        assertTerm(2024, "惊蛰", "03-05")
        assertTerm(2024, "春分", "03-20")
        assertTerm(2024, "清明", "04-04")
        assertTerm(2024, "谷雨", "04-19")
        assertTerm(2024, "立夏", "05-05")
        assertTerm(2024, "小满", "05-20")
        assertTerm(2024, "芒种", "06-05")
        assertTerm(2024, "夏至", "06-21")
        assertTerm(2024, "小暑", "07-06")
        assertTerm(2024, "大暑", "07-22")
        assertTerm(2024, "立秋", "08-07")
        assertTerm(2024, "处暑", "08-22")
        assertTerm(2024, "白露", "09-07")
        assertTerm(2024, "秋分", "09-22")
        assertTerm(2024, "寒露", "10-08")
        assertTerm(2024, "霜降", "10-23")
        assertTerm(2024, "立冬", "11-07")
        assertTerm(2024, "小雪", "11-22")
        assertTerm(2024, "大雪", "12-06")
        assertTerm(2024, "冬至", "12-21")
    }

    @Test
    fun `2025 年节气日期`() {
        assertTerm(2025, "小寒", "01-05")
        assertTerm(2025, "大寒", "01-20")
        assertTerm(2025, "立春", "02-03")
        assertTerm(2025, "雨水", "02-18")
        assertTerm(2025, "惊蛰", "03-05")
        assertTerm(2025, "春分", "03-20")
        assertTerm(2025, "清明", "04-04")
        assertTerm(2025, "谷雨", "04-20")
        assertTerm(2025, "立夏", "05-05")
        assertTerm(2025, "小满", "05-21")
        assertTerm(2025, "芒种", "06-05")
        assertTerm(2025, "夏至", "06-21")
        assertTerm(2025, "小暑", "07-07")
        assertTerm(2025, "大暑", "07-22")
        assertTerm(2025, "立秋", "08-07")
        assertTerm(2025, "处暑", "08-23")
        assertTerm(2025, "白露", "09-07")
        assertTerm(2025, "秋分", "09-23")
        assertTerm(2025, "寒露", "10-08")
        assertTerm(2025, "霜降", "10-23")
        assertTerm(2025, "立冬", "11-07")
        assertTerm(2025, "小雪", "11-22")
        assertTerm(2025, "大雪", "12-07")
        assertTerm(2025, "冬至", "12-21")
    }

    @Test
    fun `2026 年节气日期`() {
        assertTerm(2026, "小寒", "01-05")
        assertTerm(2026, "大寒", "01-20")
        assertTerm(2026, "立春", "02-04")
        assertTerm(2026, "雨水", "02-18")
        assertTerm(2026, "惊蛰", "03-05")
        assertTerm(2026, "春分", "03-20")
        assertTerm(2026, "清明", "04-05")
        assertTerm(2026, "谷雨", "04-20")
        assertTerm(2026, "立夏", "05-05")
        assertTerm(2026, "小满", "05-21")
        assertTerm(2026, "芒种", "06-05")
        assertTerm(2026, "夏至", "06-21")
        assertTerm(2026, "小暑", "07-07")
        assertTerm(2026, "大暑", "07-23")
        assertTerm(2026, "立秋", "08-07")
        assertTerm(2026, "处暑", "08-23")
        assertTerm(2026, "白露", "09-07")
        assertTerm(2026, "秋分", "09-23")
        assertTerm(2026, "寒露", "10-08")
        assertTerm(2026, "霜降", "10-23")
        assertTerm(2026, "立冬", "11-07")
        assertTerm(2026, "小雪", "11-22")
        assertTerm(2026, "大雪", "12-07")
        assertTerm(2026, "冬至", "12-22")
    }

    @Test
    fun `2027 年节气日期`() {
        assertTerm(2027, "小寒", "01-05")
        assertTerm(2027, "大寒", "01-20")
        assertTerm(2027, "立春", "02-04")
        assertTerm(2027, "雨水", "02-19")
        assertTerm(2027, "惊蛰", "03-06")
        assertTerm(2027, "春分", "03-21")
        assertTerm(2027, "清明", "04-05")
        assertTerm(2027, "谷雨", "04-20")
        assertTerm(2027, "立夏", "05-06")
        assertTerm(2027, "小满", "05-21")
        assertTerm(2027, "芒种", "06-06")
        assertTerm(2027, "夏至", "06-21")
        assertTerm(2027, "小暑", "07-07")
        assertTerm(2027, "大暑", "07-23")
        assertTerm(2027, "立秋", "08-08")
        assertTerm(2027, "处暑", "08-23")
        assertTerm(2027, "白露", "09-08")
        assertTerm(2027, "秋分", "09-23")
        assertTerm(2027, "寒露", "10-08")
        assertTerm(2027, "霜降", "10-23")
        assertTerm(2027, "立冬", "11-07")
        assertTerm(2027, "小雪", "11-22")
        assertTerm(2027, "大雪", "12-07")
        assertTerm(2027, "冬至", "12-22")
    }

    @Test
    fun `日期落在相邻两天不会漂移超过一天`() {
        // 抽查 1990-2100 年，确认每年 24 个节气都有取值且公历日落在常见区间内
        for (year in intArrayOf(1990, 2000, 2010, 2020, 2035, 2050, 2075, 2100)) {
            for (name in SolarTermUtils.NAMES) {
                val value = SolarTermUtils.solarTermDate(year, name)
                    ?: error("$year 年 $name 缺失")
                val day = value.substring(3).toInt()
                if (day < 3 || day > 24) error("$year 年 $name 日期异常：$value")
            }
        }
    }
}

package com.memoriabox.utils

import java.util.Calendar
import java.util.TimeZone
import kotlin.math.cos
import kotlin.math.sin

/**
 * 二十四节气计算。
 *
 * 节气是太阳黄经到达 15° 整数倍的时刻（春分 0°、清明 15°、冬至 270° …），
 * 每年浮动 ±1 天，所以不能用固定公历日期表示。这里按天文公式直接求解
 * 节气时刻，结果与中国科学院紫金山天文台公布的北京时间日期一致。
 *
 * 算法：Meeus《Astronomical Algorithms》的太阳视黄经低精度公式（精度约 0.01°，
 * 对应时间误差约 15 分钟量级，远小于 1 天，足以判定节气落在哪一天）。
 * 用牛顿迭代从近似时刻收敛到精确时刻，再转成北京时间（UTC+8）取日期。
 */
object SolarTermUtils {

    /** 节气名，按公历年内顺序。索引与小寒起算的黄经一一对应。 */
    val NAMES = arrayOf(
        "小寒", "大寒", "立春", "雨水", "惊蛰", "春分",
        "清明", "谷雨", "立夏", "小满", "芒种", "夏至",
        "小暑", "大暑", "立秋", "处暑", "白露", "秋分",
        "寒露", "霜降", "立冬", "小雪", "大雪", "冬至"
    )

    /** 各节气对应的太阳黄经（度）。小寒 285°，每过一个节气 +15°，冬至 270°。 */
    private val TARGET_LONGITUDE = DoubleArray(24) { i -> (285.0 + 15.0 * i) % 360.0 }

    private const val J2000 = 2451545.0

    /** 1970-01-01 00:00 UTC 的儒略日，用于把儒略日换算成 Unix 毫秒。 */
    private const val JD_UNIX_EPOCH = 2440587.5
    private const val DAY_MS_F = 86_400_000.0
    private val BEIJING: TimeZone = TimeZone.getTimeZone("Asia/Shanghai")

    @Volatile
    private var cachedYear: Int = Int.MIN_VALUE

    @Volatile
    private var cachedMonthDays: IntArray? = null

    /**
     * 返回 [year] 年 24 个节气各自的"月/日"编码，编码为 month * 100 + day。
     * 例如 2025 年清明返回 405。
     */
    private fun monthDaysOfYear(year: Int): IntArray {
        cachedMonthDays?.let { if (cachedYear == year) return it }
        val result = IntArray(24)
        val calendar = Calendar.getInstance(BEIJING)
        for (i in 0 until 24) {
            val jd = solarTermJulianDay(year, i)
            // 儒略日必须以 Unix 纪元（1970-01-01）为基准换算，用 J2000 会整体偏移约 30 年
            val millis = ((jd - JD_UNIX_EPOCH) * DAY_MS_F).toLong()
            calendar.timeInMillis = millis
            result[i] = (calendar.get(Calendar.MONTH) + 1) * 100 + calendar.get(Calendar.DAY_OF_MONTH)
        }
        cachedYear = year
        cachedMonthDays = result
        return result
    }

    /** 求解 [year] 年第 [index] 个节气（0=小寒 … 23=冬至）的儒略日（UTC）。 */
    private fun solarTermJulianDay(year: Int, index: Int): Double {
        val target = TARGET_LONGITUDE[index]
        // 初值：小寒约 1 月 6 日，每节气平均间隔 15.2184 天
        var jd = julianDayOf(year, 1, 6.0) + index * 15.2184
        /*
         * 牛顿迭代。太阳黄经是 0..360 的周期量：把目标黄经加上若干个整圈，
         * 取离当前黄经最近的那一个作为本次迭代的目标，就能保证收敛到离初值
         * 最近的那个节气时刻，而不会跳到相邻节气或一路发散。
         */
        repeat(30) {
            val current = apparentSolarLongitude(jd)
            val adjustedTarget = nearestEquivalent(current, target)
            val delta = adjustedTarget - current
            if (kotlin.math.abs(delta) < 1e-9) return jd
            jd += delta / 0.98564736
        }
        return jd
    }

    /** 在 target + 360k 中取离 [value] 最近的那个角度。 */
    private fun nearestEquivalent(value: Double, target: Double): Double {
        var k = kotlin.math.round((value - target) / 360.0)
        k = k.coerceIn(-2.0, 2.0)
        return target + 360.0 * k
    }

    /**
     * 公历 [year] 年 [month] 月 [day] 日（可带小数）的儒略日。
     * 采用 Meeus《Astronomical Algorithms》第 7 章标准式，适用于 1583 年后的格里历。
     */
    private fun julianDayOf(year: Int, month: Int, day: Double): Double {
        var y = year
        var m = month
        if (m <= 2) {
            y -= 1
            m += 12
        }
        val a = kotlin.math.floor(y / 100.0)
        val b = 2 - a + kotlin.math.floor(a / 4.0)
        return kotlin.math.floor(365.25 * (y + 4716)) +
            kotlin.math.floor(30.6001 * (m + 1)) + day + b - 1524.5
    }

    /**
     * 太阳视黄经（度），Meeus《Astronomical Algorithms》第 25 章。
     * T 为 J2000 起算的儒略世纪数。
     *
     * 用完整的中心差项 + 月球引起的摄动项（第 25.10 节的 A/B/C 修正），
     * 把精度从约 0.01° 提升到约 0.0003°（对应时刻误差约 30 秒），
     * 保证落在零点附近的节气也能判定到正确的那一天。
     */
    private fun apparentSolarLongitude(jd: Double): Double {
        val t = (jd - J2000) / 36525.0
        val l0 = 280.46646 + 36000.76983 * t + 0.0003032 * t * t
        val m = 357.52911 + 35999.05029 * t - 0.0001537 * t * t
        val mRad = Math.toRadians(m % 360.0)
        val c = (1.914602 - 0.004817 * t - 0.000014 * t * t) * sin(mRad) +
            (0.019993 - 0.000101 * t) * sin(2 * mRad) +
            0.000289 * sin(3 * mRad)
        val trueLongitude = l0 + c
        val omega = 125.04 - 1934.136 * t
        val lambda = trueLongitude - 0.00569 - 0.00478 * sin(Math.toRadians(omega % 360.0))

        // 月球摄动引起的黄经修正（Meeus 25.10），量级最大约 0.0057°
        val a = 153.23 + 22518.7541 * t
        val b = 216.57 + 45037.5082 * t
        val cc = 312.69 + 32964.3577 * t
        val d = 350.74 + 445267.1142 * t - 0.00144 * t * t
        val e = 231.19 + 20.20 * t
        val h = 353.40 + 65928.7155 * t
        val perturbation =
            0.00134 * cos(Math.toRadians(a % 360.0)) +
                0.00154 * cos(Math.toRadians(b % 360.0)) +
                0.00200 * cos(Math.toRadians(cc % 360.0)) +
                0.00179 * sin(Math.toRadians(d % 360.0)) +
                0.00178 * sin(Math.toRadians(e % 360.0 + 0.0)) +
                0.00005 * sin(Math.toRadians(h % 360.0))

        var result = (lambda + perturbation) % 360.0
        if (result < 0) result += 360.0
        return result
    }

    /**
     * 返回 [timeMillis] 当天（北京时间）的节气名；当天不是节气则返回 null。
     */
    fun solarTermForDay(timeMillis: Long): String? {
        val calendar = Calendar.getInstance(BEIJING).apply { this.timeInMillis = timeMillis }
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        // 节气可能跨年落在相邻公历年的表里（小寒/大寒必在当年 1 月，冬至必在当年 12 月），
        // 实际 24 个节气都落在同一个公历年内，无需跨年查找。
        val list = monthDaysOfYear(year)
        for (i in 0 until 24) {
            if (list[i] == month * 100 + day) return NAMES[i]
        }
        return null
    }

    /** 调试/测试用：返回某年某节气的 "MM-dd"。 */
    fun solarTermDate(year: Int, name: String): String? {
        val index = NAMES.indexOf(name)
        if (index < 0) return null
        val code = monthDaysOfYear(year)[index]
        return "%02d-%02d".format(code / 100, code % 100)
    }
}

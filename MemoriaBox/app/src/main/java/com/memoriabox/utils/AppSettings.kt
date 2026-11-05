package com.memoriabox.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import java.util.Calendar

object AppSettings {
    var settingsVersion by mutableIntStateOf(0)
        private set

    private fun getPrefs(context: Context): SharedPreferences =
        context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

    private fun saveString(context: Context, key: String, value: String?) {
        val editor = getPrefs(context).edit()
        if (value.isNullOrBlank()) {
            editor.remove(key)
        } else {
            editor.putString(key, value)
        }
        editor.apply()
        settingsVersion++
    }

    private fun saveBoolean(context: Context, key: String, value: Boolean) {
        getPrefs(context).edit().putBoolean(key, value).apply()
        settingsVersion++
    }

    private fun saveFloat(context: Context, key: String, value: Float) {
        getPrefs(context).edit().putFloat(key, value).apply()
        settingsVersion++
    }

    private fun saveInt(context: Context, key: String, value: Int) {
        getPrefs(context).edit().putInt(key, value).apply()
        settingsVersion++
    }

    private fun saveLong(context: Context, key: String, value: Long) {
        getPrefs(context).edit().putLong(key, value).apply()
        settingsVersion++
    }

    private fun monthlyMediaKey(year: Int, month: Int): String = "%04d-%02d".format(year.coerceIn(1900, 2100), month.coerceIn(1, 12))

    const val HOME_BG_URI = "home_bg_uri"
    const val CALENDAR_BG_URI = "calendar_bg_uri"
    const val TODO_BG_URI = "todo_bg_uri"
    const val SETTINGS_BG_URI = "settings_bg_uri"
    const val HOME_ICON_URI = "home_icon_uri"
    const val CALENDAR_ICON_URI = "calendar_icon_uri"
    const val TODO_ICON_URI = "todo_icon_uri"
    const val SETTINGS_ICON_URI = "settings_icon_uri"
    const val CUSTOM_DAILY_QUOTE = "custom_daily_quote"
    const val CUSTOM_DAILY_QUOTES = "custom_daily_quotes"
    const val USE_CUSTOM_QUOTE = "use_custom_daily_quote"
    const val DIARY_SCROLL_ENABLED = "diary_scroll_enabled"
    const val DIARY_SCROLL_SPEED = "diary_scroll_speed"
    const val MONTHLY_SUMMARY_ENABLED = "monthly_summary_enabled"
    const val MONTHLY_SUMMARY_AUTO_PROMPT_ENABLED = "monthly_summary_auto_prompt_enabled"
    const val MONTHLY_SUMMARY_TEXT_ENABLED = "monthly_summary_text_enabled"
    const val MONTHLY_SUMMARY_PLAY_MODE = "monthly_summary_play_mode"
    const val MONTHLY_SUMMARY_PLAY_SPEED_FACTOR = "monthly_summary_play_speed_factor"
    const val MONTHLY_SUMMARY_LAST_PROMPT_MONTH = "monthly_summary_last_prompt_month"
    const val MONTHLY_MEDIA_IMAGES_PREFIX = "monthly_media_images_"
    const val MONTHLY_MEDIA_VIDEOS_PREFIX = "monthly_media_videos_"
    const val UPCOMING_EVENTS_ENABLED = "upcoming_events_enabled"
    const val UPCOMING_EVENTS_DAYS = "upcoming_events_days"
    const val UPCOMING_EVENTS_URGENT_DAYS = "upcoming_events_urgent_days"
    const val UPCOMING_EVENTS_URGENT_COLOR = "upcoming_events_urgent_color"
    const val UPCOMING_EVENTS_NORMAL_COLOR = "upcoming_events_normal_color"
    const val UPCOMING_EVENTS_REMINDER_ENABLED = "upcoming_events_reminder_enabled"
    const val HOLIDAY_REMINDER_ENABLED = "holiday_reminder_enabled"
    const val UPDATE_SKIPPED_VERSION = "update_skipped_version"
    const val UPDATE_NEVER_REMIND = "update_never_remind"
    const val RECENT_SEARCHES = "recent_searches"
    const val RECENT_SEARCH_LIMIT = 8
    const val LAST_BACKUP_AT = "last_backup_at"
    const val LAST_BACKUP_NOTICE_AT = "last_backup_notice_at"

    /** 超过这个天数没有备份就提醒用户 */
    const val BACKUP_REMIND_DAYS = 30

    /** 提醒最多每 7 天出现一次，避免频繁打扰 */
    private const val BACKUP_NOTICE_INTERVAL_MS = 7L * 24 * 60 * 60 * 1000

    fun getHomeBgUri(context: Context) = getPrefs(context).getString(HOME_BG_URI, null)
    fun setHomeBgUri(context: Context, uri: String?) = saveString(context, HOME_BG_URI, uri)

    fun getCalendarBgUri(context: Context) = getPrefs(context).getString(CALENDAR_BG_URI, null)
    fun setCalendarBgUri(context: Context, uri: String?) = saveString(context, CALENDAR_BG_URI, uri)

    fun getTodoBgUri(context: Context) = getPrefs(context).getString(TODO_BG_URI, null)
    fun setTodoBgUri(context: Context, uri: String?) = saveString(context, TODO_BG_URI, uri)

    fun getSettingsBgUri(context: Context) = getPrefs(context).getString(SETTINGS_BG_URI, null)
    fun setSettingsBgUri(context: Context, uri: String?) = saveString(context, SETTINGS_BG_URI, uri)

    fun getHomeIconUri(context: Context) = getPrefs(context).getString(HOME_ICON_URI, null)
    fun setHomeIconUri(context: Context, uri: String?) = saveString(context, HOME_ICON_URI, uri)

    fun getCalendarIconUri(context: Context) = getPrefs(context).getString(CALENDAR_ICON_URI, null)
    fun setCalendarIconUri(context: Context, uri: String?) = saveString(context, CALENDAR_ICON_URI, uri)

    fun getTodoIconUri(context: Context) = getPrefs(context).getString(TODO_ICON_URI, null)
    fun setTodoIconUri(context: Context, uri: String?) = saveString(context, TODO_ICON_URI, uri)

    fun getSettingsIconUri(context: Context) = getPrefs(context).getString(SETTINGS_ICON_URI, null)
    fun setSettingsIconUri(context: Context, uri: String?) = saveString(context, SETTINGS_ICON_URI, uri)

    fun getCustomDailyQuote(context: Context) = getPrefs(context).getString(CUSTOM_DAILY_QUOTE, null)
    fun setCustomDailyQuote(context: Context, quote: String?) = saveString(context, CUSTOM_DAILY_QUOTE, quote)

    fun getCustomDailyQuotes(context: Context): List<String> {
        val prefs = getPrefs(context)
        val stored = prefs.getString(CUSTOM_DAILY_QUOTES, null)
        if (!stored.isNullOrBlank()) {
            return runCatching {
                val array = JSONArray(stored)
                buildList {
                    for (index in 0 until array.length()) {
                        val quote = array.optString(index).trim()
                        if (quote.isNotBlank()) add(quote)
                    }
                }
            }.getOrDefault(emptyList())
        }
        return prefs.getString(CUSTOM_DAILY_QUOTE, null)?.takeIf { it.isNotBlank() }?.let { listOf(it) }.orEmpty()
    }

    fun setCustomDailyQuotes(context: Context, quotes: List<String>) {
        val cleaned = quotes.map { it.trim() }.filter { it.isNotBlank() }
        val editor = getPrefs(context).edit()
        if (cleaned.isEmpty()) {
            editor.remove(CUSTOM_DAILY_QUOTES)
            editor.remove(CUSTOM_DAILY_QUOTE)
        } else {
            val array = JSONArray()
            cleaned.forEach { array.put(it) }
            editor.putString(CUSTOM_DAILY_QUOTES, array.toString())
            editor.putString(CUSTOM_DAILY_QUOTE, cleaned.first())
        }
        editor.apply()
        settingsVersion++
    }

    fun getUseCustomQuote(context: Context) = getPrefs(context).getBoolean(USE_CUSTOM_QUOTE, false)
    fun setUseCustomQuote(context: Context, use: Boolean) = saveBoolean(context, USE_CUSTOM_QUOTE, use)

    fun getDiaryScrollEnabled(context: Context) = getPrefs(context).getBoolean(DIARY_SCROLL_ENABLED, true)
    fun setDiaryScrollEnabled(context: Context, enabled: Boolean) = saveBoolean(context, DIARY_SCROLL_ENABLED, enabled)

    fun getDiaryScrollSpeed(context: Context) = getPrefs(context).getInt(DIARY_SCROLL_SPEED, 60)
    fun setDiaryScrollSpeed(context: Context, speed: Int) {
        getPrefs(context).edit().putInt(DIARY_SCROLL_SPEED, speed.coerceIn(10, 200)).apply()
        settingsVersion++
    }

    fun getMonthlySummaryEnabled(context: Context) = getPrefs(context).getBoolean(MONTHLY_SUMMARY_ENABLED, true)
    fun setMonthlySummaryEnabled(context: Context, enabled: Boolean) = saveBoolean(context, MONTHLY_SUMMARY_ENABLED, enabled)

    fun getMonthlySummaryAutoPromptEnabled(context: Context) = getPrefs(context).getBoolean(MONTHLY_SUMMARY_AUTO_PROMPT_ENABLED, true)
    fun setMonthlySummaryAutoPromptEnabled(context: Context, enabled: Boolean) = saveBoolean(context, MONTHLY_SUMMARY_AUTO_PROMPT_ENABLED, enabled)

    fun getMonthlySummaryTextEnabled(context: Context) = getPrefs(context).getBoolean(MONTHLY_SUMMARY_TEXT_ENABLED, true)
    fun setMonthlySummaryTextEnabled(context: Context, enabled: Boolean) = saveBoolean(context, MONTHLY_SUMMARY_TEXT_ENABLED, enabled)

    fun getMonthlySummaryPlayMode(context: Context) = getPrefs(context).getBoolean(MONTHLY_SUMMARY_PLAY_MODE, false)
    fun setMonthlySummaryPlayMode(context: Context, enabled: Boolean) = saveBoolean(context, MONTHLY_SUMMARY_PLAY_MODE, enabled)

    fun getMonthlySummaryPlaySpeedFactor(context: Context) = getPrefs(context).getFloat(MONTHLY_SUMMARY_PLAY_SPEED_FACTOR, 1.0f)
    fun setMonthlySummaryPlaySpeedFactor(context: Context, factor: Float) = saveFloat(context, MONTHLY_SUMMARY_PLAY_SPEED_FACTOR, factor.coerceIn(0.5f, 2.0f))

    fun getMonthlySummaryLastPromptMonth(context: Context) = getPrefs(context).getString(MONTHLY_SUMMARY_LAST_PROMPT_MONTH, null)
    fun setMonthlySummaryLastPromptMonth(context: Context, month: String) = saveString(context, MONTHLY_SUMMARY_LAST_PROMPT_MONTH, month)

    fun getMonthlyMediaImages(context: Context, year: Int, month: Int): List<String> {
        val monthKey = monthlyMediaKey(year, month)
        val current = getStringList(context, "$MONTHLY_MEDIA_IMAGES_PREFIX$monthKey")
        return current.ifEmpty { getStringList(context, "$MONTHLY_MEDIA_IMAGES_PREFIX${month.coerceIn(1, 12)}") }
    }

    fun setMonthlyMediaImages(context: Context, year: Int, month: Int, uris: List<String>) = saveStringList(context, "$MONTHLY_MEDIA_IMAGES_PREFIX${monthlyMediaKey(year, month)}", uris)

    fun getMonthlyMediaImages(context: Context, month: Int): List<String> = getMonthlyMediaImages(context, Calendar.getInstance().get(Calendar.YEAR), month)
    fun setMonthlyMediaImages(context: Context, month: Int, uris: List<String>) = setMonthlyMediaImages(context, Calendar.getInstance().get(Calendar.YEAR), month, uris)

    fun getMonthlyMediaVideos(context: Context, year: Int, month: Int): List<String> {
        val monthKey = monthlyMediaKey(year, month)
        val current = getStringList(context, "$MONTHLY_MEDIA_VIDEOS_PREFIX$monthKey")
        return current.ifEmpty { getStringList(context, "$MONTHLY_MEDIA_VIDEOS_PREFIX${month.coerceIn(1, 12)}") }
    }

    fun setMonthlyMediaVideos(context: Context, year: Int, month: Int, uris: List<String>) = saveStringList(context, "$MONTHLY_MEDIA_VIDEOS_PREFIX${monthlyMediaKey(year, month)}", uris)

    fun getMonthlyMediaVideos(context: Context, month: Int): List<String> = getMonthlyMediaVideos(context, Calendar.getInstance().get(Calendar.YEAR), month)
    fun setMonthlyMediaVideos(context: Context, month: Int, uris: List<String>) = setMonthlyMediaVideos(context, Calendar.getInstance().get(Calendar.YEAR), month, uris)

    fun getUpcomingEventsEnabled(context: Context) = getPrefs(context).getBoolean(UPCOMING_EVENTS_ENABLED, true)
    fun setUpcomingEventsEnabled(context: Context, enabled: Boolean) = saveBoolean(context, UPCOMING_EVENTS_ENABLED, enabled)

    fun getUpcomingEventsDays(context: Context) = getPrefs(context).getInt(UPCOMING_EVENTS_DAYS, 30)
    fun setUpcomingEventsDays(context: Context, days: Int) = saveInt(context, UPCOMING_EVENTS_DAYS, days.coerceIn(1, 365))

    fun getUpcomingEventsUrgentDays(context: Context) = getPrefs(context).getInt(UPCOMING_EVENTS_URGENT_DAYS, 7)
    fun setUpcomingEventsUrgentDays(context: Context, days: Int) = saveInt(context, UPCOMING_EVENTS_URGENT_DAYS, days.coerceIn(1, 30))

    fun getUpcomingEventsUrgentColor(context: Context) = getPrefs(context).getString(UPCOMING_EVENTS_URGENT_COLOR, "#F97316") ?: "#F97316"
    fun setUpcomingEventsUrgentColor(context: Context, color: String) = saveString(context, UPCOMING_EVENTS_URGENT_COLOR, color)

    fun getUpcomingEventsNormalColor(context: Context) = getPrefs(context).getString(UPCOMING_EVENTS_NORMAL_COLOR, "#2563EB") ?: "#2563EB"
    fun setUpcomingEventsNormalColor(context: Context, color: String) = saveString(context, UPCOMING_EVENTS_NORMAL_COLOR, color)

    fun getUpcomingEventsReminderEnabled(context: Context) = getPrefs(context).getBoolean(UPCOMING_EVENTS_REMINDER_ENABLED, true)
    fun setUpcomingEventsReminderEnabled(context: Context, enabled: Boolean) = saveBoolean(context, UPCOMING_EVENTS_REMINDER_ENABLED, enabled)

    fun getHolidayReminderEnabled(context: Context) = getPrefs(context).getBoolean(HOLIDAY_REMINDER_ENABLED, true)
    fun setHolidayReminderEnabled(context: Context, enabled: Boolean) = saveBoolean(context, HOLIDAY_REMINDER_ENABLED, enabled)

    /**
     * 被用户跳过的版本号。只对这一个版本生效：
     * 出现更高版本时仍会正常弹出更新提示。
     */
    fun getUpdateSkippedVersion(context: Context): String? =
        getPrefs(context).getString(UPDATE_SKIPPED_VERSION, null)?.takeIf { it.isNotBlank() }

    fun setUpdateSkippedVersion(context: Context, versionName: String?) =
        saveString(context, UPDATE_SKIPPED_VERSION, versionName)

    fun clearUpdateSkippedVersion(context: Context) = setUpdateSkippedVersion(context, null)

    /**
     * 永久屏蔽自动更新提示。开启后除非用户手动点「检查更新」，否则不再自动弹窗。
     * 手动检查更新成功后会由界面重置该项。
     */
    fun getUpdateNeverRemind(context: Context) = getPrefs(context).getBoolean(UPDATE_NEVER_REMIND, false)
    fun setUpdateNeverRemind(context: Context, enabled: Boolean) = saveBoolean(context, UPDATE_NEVER_REMIND, enabled)

    /**
     * 是否应弹出更新提示。抽成纯函数便于测试，界面只负责传入当前偏好。
     *
     * @param targetVersion 远端发现的新版本号
     * @param skippedVersion 用户跳过的版本号，只对同一版本生效
     * @param neverRemind 是否永久屏蔽自动提示
     * @param manual 是否由用户手动检查触发
     */
    fun shouldPromptUpdate(
        targetVersion: String,
        skippedVersion: String?,
        neverRemind: Boolean,
        manual: Boolean
    ): Boolean {
        // 手动检查更新始终弹窗，用户可以随时主动查看新版本
        if (manual) return true
        if (neverRemind) return false
        val skipped = skippedVersion?.takeIf { it.isNotBlank() } ?: return true
        return skipped != targetVersion
    }

    /**
     * 上次成功备份的时间戳，0 表示从未备份。
     */
    fun getLastBackupAt(context: Context): Long = getPrefs(context).getLong(LAST_BACKUP_AT, 0L)

    fun markBackupCompleted(context: Context, timestamp: Long = System.currentTimeMillis()) {
        saveLong(context, LAST_BACKUP_AT, timestamp)
    }

    fun getLastBackupNoticeAt(context: Context): Long = getPrefs(context).getLong(LAST_BACKUP_NOTICE_AT, 0L)

    private fun markBackupNoticeShown(context: Context, timestamp: Long) {
        saveLong(context, LAST_BACKUP_NOTICE_AT, timestamp)
    }

    /**
     * 是否需要提醒备份。抽成纯函数便于测试。
     *
     * @param lastBackupAt 上次备份时间戳，0 表示从未备份
     * @param now 当前时间戳
     * @param remindDays 超过多少天算过期
     * @return 过期的天数；不需要提醒时返回 null
     */
    fun backupOverdueDays(
        lastBackupAt: Long,
        now: Long,
        remindDays: Int = BACKUP_REMIND_DAYS
    ): Int? {
        val thresholdMs = remindDays.toLong() * 24 * 60 * 60 * 1000
        // 从未备份过视为过期，返回完整周期天数
        if (lastBackupAt <= 0L) return remindDays
        val elapsed = now - lastBackupAt
        if (elapsed < thresholdMs) return null
        return (elapsed / (24 * 60 * 60 * 1000)).toInt()
    }

    /**
     * 是否现在应该弹出/展示备份提醒：数据过期，且距上次提醒已超过间隔。
     * 提醒节流只影响展示频率，不影响「从未备份」的首次提醒。
     */
    fun shouldShowBackupNotice(
        lastBackupAt: Long,
        lastNoticeAt: Long,
        now: Long,
        remindDays: Int = BACKUP_REMIND_DAYS
    ): Boolean {
        if (backupOverdueDays(lastBackupAt, now, remindDays) == null) return false
        if (lastNoticeAt <= 0L) return true
        return now - lastNoticeAt >= BACKUP_NOTICE_INTERVAL_MS
    }

    /** 记录提醒已展示，用于节流 */
    fun markBackupNoticeShown(context: Context) = markBackupNoticeShown(context, System.currentTimeMillis())
    fun getRecentSearches(context: Context): List<String> =
        getStringList(context, RECENT_SEARCHES).take(RECENT_SEARCH_LIMIT)

    fun addRecentSearch(context: Context, query: String) {
        val merged = mergeRecentSearch(getRecentSearches(context), query, RECENT_SEARCH_LIMIT)
        saveStringList(context, RECENT_SEARCHES, merged)
    }

    fun clearRecentSearches(context: Context) {
        saveStringList(context, RECENT_SEARCHES, emptyList())
    }

    /**
     * 把新关键词并入历史：去空白、去重、置顶、截断。
     * 抽成纯函数便于测试，避免把键盘每次输入都写进历史。
     */
    fun mergeRecentSearch(
        existing: List<String>,
        query: String,
        limit: Int = RECENT_SEARCH_LIMIT
    ): List<String> {
        val keyword = query.trim()
        if (keyword.isBlank()) return existing.take(limit)
        return (listOf(keyword) + existing.map { it.trim() }.filter { it.isNotBlank() })
            .distinct()
            .take(limit)
    }

    private fun getStringList(context: Context, key: String): List<String> {
        val stored = getPrefs(context).getString(key, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(stored)
            buildList {
                for (index in 0 until array.length()) {
                    val value = array.optString(index).trim()
                    if (value.isNotBlank()) add(value)
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun saveStringList(context: Context, key: String, values: List<String>) {
        val cleaned = values.map { it.trim() }.filter { it.isNotBlank() }.distinct()
        val editor = getPrefs(context).edit()
        if (cleaned.isEmpty()) {
            editor.remove(key)
        } else {
            val array = JSONArray()
            cleaned.forEach { array.put(it) }
            editor.putString(key, array.toString())
        }
        editor.apply()
        settingsVersion++
    }
}

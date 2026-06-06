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

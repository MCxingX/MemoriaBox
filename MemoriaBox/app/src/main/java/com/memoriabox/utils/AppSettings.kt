package com.memoriabox.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray

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
}

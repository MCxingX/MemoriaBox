package com.memoriabox.utils

import android.content.Context
import android.content.SharedPreferences

object AppSettings {
    private fun getPrefs(context: Context): SharedPreferences =
        context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

    const val HOME_BG_URI = "home_bg_uri"
    const val CALENDAR_BG_URI = "calendar_bg_uri"
    const val TODO_BG_URI = "todo_bg_uri"
    const val SETTINGS_BG_URI = "settings_bg_uri"
    const val CUSTOM_DAILY_QUOTE = "custom_daily_quote"
    const val USE_CUSTOM_QUOTE = "use_custom_daily_quote"

    fun getHomeBgUri(context: Context) = getPrefs(context).getString(HOME_BG_URI, null)
    fun setHomeBgUri(context: Context, uri: String?) = getPrefs(context).edit().putString(HOME_BG_URI, uri).apply()

    fun getCalendarBgUri(context: Context) = getPrefs(context).getString(CALENDAR_BG_URI, null)
    fun setCalendarBgUri(context: Context, uri: String?) = getPrefs(context).edit().putString(CALENDAR_BG_URI, uri).apply()

    fun getTodoBgUri(context: Context) = getPrefs(context).getString(TODO_BG_URI, null)
    fun setTodoBgUri(context: Context, uri: String?) = getPrefs(context).edit().putString(TODO_BG_URI, uri).apply()

    fun getSettingsBgUri(context: Context) = getPrefs(context).getString(SETTINGS_BG_URI, null)
    fun setSettingsBgUri(context: Context, uri: String?) = getPrefs(context).edit().putString(SETTINGS_BG_URI, uri).apply()

    fun getCustomDailyQuote(context: Context) = getPrefs(context).getString(CUSTOM_DAILY_QUOTE, null)
    fun setCustomDailyQuote(context: Context, quote: String?) = getPrefs(context).edit().putString(CUSTOM_DAILY_QUOTE, quote).apply()

    fun getUseCustomQuote(context: Context) = getPrefs(context).getBoolean(USE_CUSTOM_QUOTE, false)
    fun setUseCustomQuote(context: Context, use: Boolean) = getPrefs(context).edit().putBoolean(USE_CUSTOM_QUOTE, use).apply()
}

package com.memoriabox.utils

import android.content.Context
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

object DailyQuoteRepository {
    private const val PREFS = "daily_quote_cache"
    private const val KEY_DATE = "date"
    private const val KEY_TEXT = "text"
    private const val API_KEY = "7e379d7f3330"
    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .callTimeout(8, TimeUnit.SECONDS)
        .build()

    fun today(context: Context): String? {
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return prefs.getString(KEY_TEXT, null)?.takeIf { prefs.getString(KEY_DATE, null) == date }
    }

    fun refresh(context: Context): String? = runCatching {
        val request = Request.Builder()
            .url("https://whyta.cn/api/yiyan?key=$API_KEY")
            .header("Accept", "application/json")
            .header("User-Agent", "MemoriaBox/${android.os.Build.VERSION.SDK_INT}")
            .build()
        val text = client.newCall(request).execute().use { response ->
            check(response.isSuccessful) { "Daily quote HTTP ${response.code}" }
            val body = response.body?.string().orEmpty()
            JSONObject(body).optString("hitokoto").trim()
        }.takeIf(String::isNotBlank) ?: return null
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_DATE, date)
            .putString(KEY_TEXT, text)
            .apply()
        text
    }.getOrNull()
}

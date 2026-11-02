package com.memoriabox.utils

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
        .writeTimeout(5, TimeUnit.SECONDS)
        .callTimeout(8, TimeUnit.SECONDS)
        .build()

    fun dayKey(now: Long = System.currentTimeMillis()): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(now))

    fun today(context: Context, now: Long = System.currentTimeMillis()): String? {
        val date = dayKey(now)
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return prefs.getString(KEY_TEXT, null)?.takeIf { prefs.getString(KEY_DATE, null) == date }
    }

    /** 网络请求固定切到 IO 线程，避免在 Compose 组合线程上阻塞。 */
    suspend fun refresh(context: Context): String? = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder()
                .url("https://whyta.cn/api/yiyan?key=$API_KEY")
                .header("Accept", "application/json")
                .header("User-Agent", "MemoriaBox/${android.os.Build.VERSION.SDK_INT}")
                .build()
            val text = client.newCall(request).execute().use { response ->
                check(response.isSuccessful) { "Daily quote HTTP ${response.code}" }
                val body = response.body?.string().orEmpty()
                JSONObject(body).optString("hitokoto").trim()
            }.takeIf(String::isNotBlank) ?: return@runCatching null
            val date = dayKey()
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putString(KEY_DATE, date)
                .putString(KEY_TEXT, text)
                .apply()
            text
        }.onFailure { error ->
            android.util.Log.w("DailyQuoteRepository", "刷新每日一言失败，继续使用本地缓存或内置文案", error)
        }.getOrNull()
    }
}

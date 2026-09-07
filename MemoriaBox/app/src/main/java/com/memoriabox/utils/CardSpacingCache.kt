package com.memoriabox.utils

import android.content.Context
import org.json.JSONObject

object CardSpacingCache {
    private const val PREFS = "card_spacing_cache"
    private const val KEY_VERSION = "app_version"
    private const val KEY_GAPS = "gaps"

    @Volatile
    private var memory: MutableMap<String, Float>? = null

    @Volatile
    private var loadedVersion: String? = null

    fun onAppStart(context: Context) {
        val version = context.installedAppVersion().name
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (prefs.getString(KEY_VERSION, null) != version) {
            prefs.edit()
                .clear()
                .putString(KEY_VERSION, version)
                .apply()
            memory = mutableMapOf()
            loadedVersion = version
        } else {
            ensureLoaded(context, version)
        }
    }

    fun gapDp(context: Context, key: String, compute: () -> Float): Float {
        val version = context.installedAppVersion().name
        ensureLoaded(context, version)
        memory?.get(key)?.let { return it }
        val value = compute()
        val store = memory ?: mutableMapOf<String, Float>().also { memory = it }
        store[key] = value
        persist(context, version, store)
        return value
    }

    fun listGapDp(screenWidthDp: Float, screenPaddingDp: Float, imageRatio: Float?): Float {
        val cardWidth = (screenWidthDp - screenPaddingDp * 2f).coerceAtLeast(1f)
        return gapFromWidth(cardWidth, imageRatio)
    }

    fun gridGapDp(screenWidthDp: Float, imageRatio: Float?): Float {
        val padding = 16f
        val gutter = 12f
        val cardWidth = ((screenWidthDp - padding * 2f - gutter) / 2f).coerceAtLeast(1f)
        return gapFromWidth(cardWidth, imageRatio)
    }

    private fun gapFromWidth(cardWidthDp: Float, imageRatio: Float?): Float {
        val ratio = imageRatio
            ?.let { ImageImportUtils.snapToCommonAspectRatio(it).coerceIn(0.5f, 2.0f) }
            ?: 1f
        val height = (cardWidthDp / ratio).coerceAtMost(240f)
        return (height * 0.04f).coerceIn(6f, 16f)
    }

    private fun ensureLoaded(context: Context, version: String) {
        if (memory != null && loadedVersion == version) return
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (prefs.getString(KEY_VERSION, null) != version) {
            memory = mutableMapOf()
            loadedVersion = version
            prefs.edit().clear().putString(KEY_VERSION, version).apply()
            return
        }
        val parsed = mutableMapOf<String, Float>()
        runCatching {
            val json = JSONObject(prefs.getString(KEY_GAPS, "{}") ?: "{}")
            json.keys().forEach { key ->
                parsed[key] = json.optDouble(key, Double.NaN).toFloat()
            }
        }
        parsed.entries.removeAll { !it.value.isFinite() }
        memory = parsed
        loadedVersion = version
    }

    private fun persist(context: Context, version: String, store: Map<String, Float>) {
        val json = JSONObject()
        store.forEach { (key, value) -> json.put(key, value.toDouble()) }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_VERSION, version)
            .putString(KEY_GAPS, json.toString())
            .apply()
    }
}

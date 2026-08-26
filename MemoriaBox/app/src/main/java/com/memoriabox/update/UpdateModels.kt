package com.memoriabox.update

import org.json.JSONObject
import java.util.Locale

data class UpdateInfo(
    val versionName: String,
    val releaseName: String,
    val releaseNotes: String,
    val publishedAt: String,
    val apkName: String,
    val apkUrl: String,
    val apkSize: Long,
    val sha256: String
)

sealed interface UpdateState {
    data object Idle : UpdateState
    data class Checking(val manual: Boolean) : UpdateState
    data class Available(val info: UpdateInfo) : UpdateState
    data class Downloading(val info: UpdateInfo, val progress: Int) : UpdateState
    data class Ready(val info: UpdateInfo, val apkUri: String) : UpdateState
    data class UpToDate(val manual: Boolean) : UpdateState
    data class Error(val message: String, val info: UpdateInfo? = null) : UpdateState
}

object UpdateFormat {
    fun normalizeVersion(value: String): String = value.trim().removePrefix("v").removePrefix("V")

    fun isNewer(candidate: String, current: String): Boolean {
        val candidateParts = versionParts(candidate)
        val currentParts = versionParts(current)
        val size = maxOf(candidateParts.size, currentParts.size)
        for (index in 0 until size) {
            val candidatePart = candidateParts.getOrElse(index) { 0 }
            val currentPart = currentParts.getOrElse(index) { 0 }
            if (candidatePart != currentPart) return candidatePart > currentPart
        }
        return false
    }

    fun parseSha256(text: String): String? =
        Regex("(?i)(?<![0-9a-f])[0-9a-f]{64}(?![0-9a-f])")
            .find(text)
            ?.value
            ?.lowercase(Locale.US)

    /** 基于 GitHub Release 资产直链构造可参与测速的国内常用代理地址。 */
    fun mirrorUrls(originalUrl: String): List<String> {
        val mirrors = mutableListOf(originalUrl)
        val match = Regex("""https?://github\.com/([^/]+)/([^/]+)/releases/download/([^/]+)/(.+)""").matchEntire(originalUrl)
        if (match != null) {
            val canonicalUrl = "https://github.com/${match.groupValues[1]}/${match.groupValues[2]}/releases/download/${match.groupValues[3]}/${match.groupValues[4]}"
            val proxyPrefixes = listOf(
                "https://gh-proxy.com/",
                "https://mirror.ghproxy.com/",
                "https://ghproxy.net/",
                "https://ghfast.top/",
                "https://ghproxy.cc/",
                "https://github.moeyy.xyz/",
                "https://gh-proxy.llyke.com/"
            )
            mirrors += proxyPrefixes.map { prefix -> prefix + canonicalUrl }
        }
        return mirrors.distinct()
    }

    private fun versionParts(value: String): List<Int> = normalizeVersion(value)
        .substringBefore('-')
        .split('.')
        .map { part -> part.takeWhile(Char::isDigit).toIntOrNull() ?: 0 }
}

object UpdateInfoJson {
    fun toJson(info: UpdateInfo): String = JSONObject().apply {
        put("versionName", info.versionName)
        put("releaseName", info.releaseName)
        put("releaseNotes", info.releaseNotes)
        put("publishedAt", info.publishedAt)
        put("apkName", info.apkName)
        put("apkUrl", info.apkUrl)
        put("apkSize", info.apkSize)
        put("sha256", info.sha256)
    }.toString()

    fun fromJson(json: String): UpdateInfo? = runCatching {
        val o = JSONObject(json)
        UpdateInfo(
            versionName = o.getString("versionName"),
            releaseName = o.getString("releaseName"),
            releaseNotes = o.getString("releaseNotes"),
            publishedAt = o.getString("publishedAt"),
            apkName = o.getString("apkName"),
            apkUrl = o.getString("apkUrl"),
            apkSize = o.getLong("apkSize"),
            sha256 = o.getString("sha256")
        )
    }.getOrNull()
}

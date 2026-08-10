package com.memoriabox.update

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
    data class Ready(val info: UpdateInfo, val apkPath: String) : UpdateState
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

    fun mirrorUrls(originalUrl: String): List<String> = listOf(
        "https://ghfast.top/$originalUrl",
        "https://gh-proxy.com/$originalUrl",
        "https://github.moeyy.xyz/$originalUrl"
    )

    private fun versionParts(value: String): List<Int> = normalizeVersion(value)
        .substringBefore('-')
        .split('.')
        .map { part -> part.takeWhile(Char::isDigit).toIntOrNull() ?: 0 }
}

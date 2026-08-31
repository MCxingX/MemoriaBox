package com.memoriabox.update

import android.content.Context
import android.content.ContentValues
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.memoriabox.BuildConfig
import com.memoriabox.utils.installedAppVersion
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

object UpdateManager {
    private const val RELEASE_API = "https://api.github.com/repos/MCxingX/MemoriaBox/releases/latest"
    private const val PREFS_NAME = "update_settings"
    private const val LAST_CHECK_KEY = "last_auto_check"
    private const val AUTO_CHECK_INTERVAL_MS = 6 * 60 * 60 * 1000L

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .callTimeout(20, TimeUnit.MINUTES)
        .followRedirects(true)
        .build()
    private val speedClient = client.newBuilder()
        .readTimeout(8, TimeUnit.SECONDS)
        .callTimeout(10, TimeUnit.SECONDS)
        .build()
    private val releaseClient = client.newBuilder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .callTimeout(12, TimeUnit.SECONDS)
        .build()

    @Volatile
    private var activeCall: okhttp3.Call? = null

    @Volatile
    private var downloadCancelled = false

    private val mutableState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val state: StateFlow<UpdateState> = mutableState.asStateFlow()

    fun check(context: Context, manual: Boolean = false) {
        if (mutableState.value is UpdateState.Checking || mutableState.value is UpdateState.Downloading) return
        if (!manual && !autoCheckDue(context)) return
        scope.launch {
            mutableState.value = UpdateState.Checking(manual)
            if (!manual) markAutoCheck(context)
            runCatching { fetchLatestRelease() }
                .onSuccess { info ->
                    if (UpdateFormat.isNewer(info.versionName, context.installedAppVersion().name)) {
                        mutableState.value = UpdateState.Available(info)
                    } else {
                        mutableState.value = UpdateState.UpToDate(manual)
                    }
                }
                .onFailure { error ->
                    mutableState.value = UpdateState.Error(error.message ?: "版本检测失败")
                }
        }
    }

    fun retry(context: Context, info: UpdateInfo?) {
        if (info == null) {
            check(context, manual = true)
            return
        }
        if (mutableState.value is UpdateState.Downloading) return
        UpdateDownloadService.start(context.applicationContext, info)
    }

    fun download(context: Context, info: UpdateInfo) {
        if (mutableState.value is UpdateState.Downloading) return
        UpdateDownloadService.start(context.applicationContext, info)
    }

    fun cancelActiveDownload() {
        downloadCancelled = true
        activeCall?.cancel()
    }

    fun resetTransientState() {
        if (mutableState.value is UpdateState.UpToDate || mutableState.value is UpdateState.Error) {
            mutableState.value = UpdateState.Idle
        }
    }

    private suspend fun fetchLatestRelease(): UpdateInfo {
        val releaseJson = fetchTextTrusted(RELEASE_API)
        val release = JSONObject(releaseJson)
        require(!release.optBoolean("draft") && !release.optBoolean("prerelease")) { "最新版本尚未正式发布" }
        val versionName = UpdateFormat.normalizeVersion(release.getString("tag_name"))
        val assets = release.getJSONArray("assets")
        var apkName = ""
        var apkUrl = ""
        var apkSize = 0L
        var checksumUrl = ""
        for (index in 0 until assets.length()) {
            val asset = assets.getJSONObject(index)
            val name = asset.getString("name")
            when {
                name.equals(RELEASE_APK_NAME, ignoreCase = true) && apkUrl.isEmpty() -> {
                    apkName = name
                    apkUrl = asset.getString("browser_download_url")
                    apkSize = asset.optLong("size")
                }
                name.endsWith(".apk.sha256", ignoreCase = true) -> {
                    checksumUrl = asset.getString("browser_download_url")
                }
            }
        }
        require(apkUrl.isNotEmpty()) { "GitHub 正式 Release 缺少 $RELEASE_APK_NAME 资产" }
        val releaseBody = release.optString("body", "")
        val sha256 = UpdateFormat.parseSha256(releaseBody)
            ?: if (checksumUrl.isNotEmpty()) {
                UpdateFormat.parseSha256(fetchTextTrusted(checksumUrl))
            } else null
        require(!sha256.isNullOrEmpty()) { "GitHub Release 缺少 SHA-256 校验信息" }
        return UpdateInfo(
            versionName = versionName,
            releaseName = release.optString("name", "v$versionName"),
            releaseNotes = releaseBody.ifBlank { "本次更新未提供说明。" },
            publishedAt = release.optString("published_at"),
            apkName = apkName,
            apkUrl = apkUrl,
            apkSize = apkSize,
            sha256 = sha256.orEmpty()
        )
    }

    internal suspend fun executeDownload(context: Context, info: UpdateInfo) {
        downloadCancelled = false
        val updateDir = File(context.filesDir, "updates").apply { mkdirs() }
        val destination = File(updateDir, "MemoriaBox-${info.versionName}.apk")
        if (destination.isFile && UpdateVerifier.verify(context, destination, info).isSuccess) {
            mutableState.value = readyOrError(context, info, destination)
            return
        }
        val temporary = File(destination.parentFile, "${destination.name}.part")
        if (temporary.isFile && temporary.length() > 0) {
            // 完整大小但校验失败的 .part 直接删除，避免 resume 触发 416 死循环
            if (info.apkSize > 0 && temporary.length() >= info.apkSize &&
                UpdateVerifier.verify(context, temporary, info).isFailure
            ) {
                temporary.delete()
            } else if (UpdateVerifier.verify(context, temporary, info).isSuccess) {
                destination.delete()
                if (temporary.renameTo(destination)) {
                    mutableState.value = readyOrError(context, info, destination)
                    return
                }
            }
        }
        mutableState.value = UpdateState.Downloading(info, progressFromPart(temporary, info.apkSize))
        val mirror = fastestMirror(info.apkUrl) ?: run {
            mutableState.value = UpdateState.Error("更新镜像测速失败，请检查网络后重试", info)
            return
        }
        runCatching { download(mirror, destination, info) }
            .onFailure { error ->
                if (downloadCancelled) {
                    mutableState.value = UpdateState.Available(info)
                } else {
                    // 满大小但损坏的 .part 会导致 resume 死循环（HTTP 416），此处清理
                    if (info.apkSize > 0 && temporary.length() >= info.apkSize) temporary.delete()
                    mutableState.value = UpdateState.Error(error.message ?: "镜像下载失败", info)
                }
                return
            }
        if (downloadCancelled) {
            mutableState.value = UpdateState.Available(info)
            return
        }
        UpdateVerifier.verify(context, destination, info)
            .onSuccess {
                mutableState.value = readyOrError(context, info, destination)
            }
            .onFailure { error ->
                temporary.delete()
                destination.delete()
                mutableState.value = UpdateState.Error(error.message ?: "更新包校验失败", info)
            }
    }

    private fun download(url: String, destination: File, info: UpdateInfo) {
        val temporary = File(destination.parentFile, "${destination.name}.part")
        val resumeFrom = temporary.length().takeIf { it > 0 } ?: 0L
        val requestBuilder = Request.Builder()
            .url(url)
            .header("Accept", "application/octet-stream")
            .header("User-Agent", "MemoriaBox/${BuildConfig.VERSION_NAME}")
        if (resumeFrom > 0) {
            requestBuilder.header("Range", "bytes=$resumeFrom-")
        }
        val request = requestBuilder.build()
        val call = client.newCall(request)
        activeCall = call
        try {
            call.execute().use { response ->
                require(response.isSuccessful || response.code == 206) { "下载失败：HTTP ${response.code}" }
                val body = response.body ?: error("下载内容为空")
                val resumed = resumeFrom > 0 && response.code == 206
                val total = when {
                    resumed -> (resumeFrom + body.contentLength()).takeIf { it > resumeFrom } ?: info.apkSize
                    else -> body.contentLength().takeIf { it > 0 } ?: info.apkSize
                }
                body.byteStream().use { input ->
                    FileOutputStream(temporary, resumed).use { output ->
                        val buffer = ByteArray(64 * 1024)
                        var downloaded = resumeFrom
                        var lastProgress = -1
                        while (true) {
                            val count = input.read(buffer)
                            if (count < 0) break
                            if (count == 0) continue
                            output.write(buffer, 0, count)
                            downloaded += count
                            val progress = if (total > 0) ((downloaded * 100) / total).toInt().coerceIn(0, 99) else 0
                            if (progress != lastProgress) {
                                lastProgress = progress
                                mutableState.value = UpdateState.Downloading(info, progress)
                            }
                        }
                    }
                }
            }
        } finally {
            activeCall = null
        }
        require(temporary.length() > 0) { "下载内容为空" }
        destination.delete()
        require(temporary.renameTo(destination)) { "无法保存更新包" }
    }

    private fun progressFromPart(temporary: File, total: Long): Int =
        if (total > 0 && temporary.isFile) ((temporary.length() * 100) / total).toInt().coerceIn(0, 99) else 0

    /** 保存到系统下载目录，失败时返回 Error 而不是让异常漏出后状态永久卡在 Downloading */
    private fun readyOrError(context: Context, info: UpdateInfo, destination: File): UpdateState {
        return runCatching { existingOrSaveToDownloads(context, info.versionName, destination) }
            .fold({ UpdateState.Ready(info, it.toString()) }) { e ->
                UpdateState.Error(e.message ?: "保存更新包失败，请检查存储空间", info)
            }
    }

    private fun saveToDownloads(context: Context, versionName: String, source: File): Uri {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, "MemoriaBox-${System.currentTimeMillis()}.apk")
            put(MediaStore.Downloads.MIME_TYPE, "application/vnd.android.package-archive")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Downloads.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/MemoriaBox")
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
        }
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: error("无法保存更新包到下载目录")
        runCatching {
            resolver.openOutputStream(uri)?.use { output -> source.inputStream().use { it.copyTo(output) } }
                ?: error("无法写入下载目录")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                resolver.update(uri, ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) }, null, null)
            }
        }.getOrElse { error ->
            resolver.delete(uri, null, null)
            throw error
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putString(DOWNLOAD_URI_KEY, uri.toString())
            .putString(DOWNLOAD_VERSION_KEY, versionName)
            .apply()
        return uri
    }

    private fun existingOrSaveToDownloads(context: Context, versionName: String, source: File): Uri {
        val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedUri = preferences.getString(DOWNLOAD_URI_KEY, null)?.let(Uri::parse)
        val savedVersion = preferences.getString(DOWNLOAD_VERSION_KEY, null)
        if (savedUri != null && savedVersion == versionName && runCatching {
                context.contentResolver.openInputStream(savedUri)?.close() ?: error("更新包已清理")
            }.isSuccess
        ) {
            return savedUri
        }
        return saveToDownloads(context, versionName, source)
    }

    private suspend fun fastestMirror(originalUrl: String): String? =
        UpdateFormat.mirrorUrls(originalUrl)
            .map { url -> scope.async { probe(url) } }
            .awaitAll()
            .filterNotNull()
            .minByOrNull { it.second }
            ?.first

    private fun probe(url: String): Pair<String, Long>? = runCatching {
        if (downloadCancelled) return null
        val started = System.nanoTime()
        val request = Request.Builder()
            .url(url)
            .header("Range", "bytes=0-65535")
            .header("User-Agent", "MemoriaBox/${BuildConfig.VERSION_NAME}")
            .build()
        val call = speedClient.newCall(request)
        call.execute().use { response ->
            require(response.isSuccessful || response.code == 206) { "HTTP ${response.code}" }
            val input = response.body?.byteStream() ?: error("空响应")
            val buffer = ByteArray(8 * 1024)
            var received = 0
            while (received < 64 * 1024) {
                if (downloadCancelled) return null
                val count = input.read(buffer)
                if (count < 0) break
                received += count
            }
            require(received > 0) { "空响应" }
        }
        url to TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started)
    }.getOrNull()

    private fun fetchTextWithFallback(url: String, unavailableMessage: String): String {
        for (candidate in UpdateFormat.githubUrls(url)) {
            val response = runCatching {
                val request = Request.Builder()
                    .url(candidate)
                    .header("Accept", "application/vnd.github+json")
                    .header("X-GitHub-Api-Version", "2022-11-28")
                    .header("User-Agent", "MemoriaBox/${BuildConfig.VERSION_NAME}")
                    .build()
                executeText(request)
            }
            response.getOrNull()?.let { return it }
        }
        error("$unavailableMessage，请检查网络后重试")
    }

    private fun fetchTextTrusted(url: String): String {
        val directRequest = Request.Builder()
            .url(url)
            .header("Accept", "application/vnd.github+json")
            .header("X-GitHub-Api-Version", "2022-11-28")
            .header("User-Agent", "MemoriaBox/${BuildConfig.VERSION_NAME}")
            .build()
        runCatching { executeText(directRequest) }.getOrNull()?.let { return it }
        return fetchTextWithFallback(url, "更新服务器连接失败")
    }

    private fun executeText(request: Request): String = releaseClient.newCall(request).execute().use { response ->
        require(response.isSuccessful) { "更新请求失败：HTTP ${response.code}" }
        response.body?.string() ?: error("GitHub 返回空内容")
    }

    private fun autoCheckDue(context: Context): Boolean {
        val lastCheck = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getLong(LAST_CHECK_KEY, 0L)
        return System.currentTimeMillis() - lastCheck >= AUTO_CHECK_INTERVAL_MS
    }

    private fun markAutoCheck(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putLong(LAST_CHECK_KEY, System.currentTimeMillis())
            .apply()
    }

    fun removeInstalledApk(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(DOWNLOAD_URI_KEY, null)
            ?.let(Uri::parse)
            ?.let { uri -> runCatching { context.contentResolver.delete(uri, null, null) } }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .remove(DOWNLOAD_URI_KEY)
            .remove(DOWNLOAD_VERSION_KEY)
            .apply()
        File(context.filesDir, "updates").listFiles()
            ?.filter { it.extension.equals("apk", ignoreCase = true) }
            ?.forEach(File::delete)
    }

    private const val DOWNLOAD_URI_KEY = "download_apk_uri"
    private const val DOWNLOAD_VERSION_KEY = "download_apk_version"
    private const val RELEASE_APK_NAME = "MemoriaBox.apk"
}

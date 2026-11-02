package com.memoriabox.utils

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Credentials
import java.util.concurrent.TimeUnit

class WebDavClient {

    // 用 @Volatile 保证多线程（上传/下载/测速）间的可见性，避免重复初始化竞态。
    @Volatile
    private var client: OkHttpClient? = null

    data class WebDavConfig(
        val serverUrl: String,
        val username: String,
        val password: String,
        val path: String = "/NianJi/"
    )

    /** 拼接远端地址，处理 serverUrl 结尾斜杠与 path 开头斜杠的重复或缺失。 */
    private fun buildUrl(config: WebDavConfig, fileName: String = ""): String {
        val base = config.serverUrl.trimEnd('/')
        val path = config.path.let { if (it.startsWith("/")) it else "/$it" }
        val suffix = fileName.removePrefix("/")
        return base + path.trimEnd('/') + "/" + suffix
    }

    suspend fun testConnection(config: WebDavConfig): Result<Boolean> {
        return try {
            val credential = Credentials.basic(config.username, config.password)
            val url = buildUrl(config)

            val request = Request.Builder()
                .url(url)
                .method("PROPFIND", "".toRequestBody("application/xml".toMediaType()))
                .header("Depth", "0")
                .header("Authorization", credential)
                .build()

            getOrCreateClient().newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Result.success(true)
                } else {
                    Result.failure(Exception("Connection failed: ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun uploadFile(config: WebDavConfig, localFilePath: String, remoteFileName: String): Result<Boolean> {
        return try {
            val credential = Credentials.basic(config.username, config.password)
            val url = buildUrl(config, remoteFileName)

            val source = java.io.File(localFilePath)
            // 流式上传，避免大备份文件一次性读入内存导致 OOM。
            val requestBody = source.asRequestBody("application/octet-stream".toMediaType())

            val request = Request.Builder()
                .url(url)
                .put(requestBody)
                .header("Authorization", credential)
                .build()

            getOrCreateClient().newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Result.success(true)
                } else {
                    Result.failure(Exception("Upload failed: ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun listFiles(config: WebDavConfig): Result<List<WebDavFileInfo>> {
        return try {
            val credential = Credentials.basic(config.username, config.password)
            val url = buildUrl(config)

            val request = Request.Builder()
                .url(url)
                .method("PROPFIND", "".toRequestBody("application/xml".toMediaType()))
                .header("Depth", "1")
                .header("Authorization", credential)
                .build()

            getOrCreateClient().newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: ""
                    Result.success(parsePropFindResponse(body))
                } else {
                    Result.failure(Exception("Failed to list files: ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun downloadFile(config: WebDavConfig, remoteFileName: String, localFilePath: String): Result<Boolean> {
        return try {
            val credential = Credentials.basic(config.username, config.password)
            val url = buildUrl(config, remoteFileName)

            val request = Request.Builder()
                .url(url)
                .header("Authorization", credential)
                .build()

            getOrCreateClient().newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return Result.failure(Exception("Download failed: ${response.code}"))
                }
                val body = response.body ?: return Result.failure(Exception("Empty response"))
                // 流式落盘，避免大文件整包驻留内存。
                val target = java.io.File(localFilePath)
                target.parentFile?.mkdirs()
                body.byteStream().use { input ->
                    target.outputStream().use { output -> input.copyTo(output) }
                }
                Result.success(true)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun getOrCreateClient(): OkHttpClient {
        client?.let { return it }
        return synchronized(this) {
            client ?: OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build()
                .also { client = it }
        }
    }

    private fun parsePropFindResponse(xml: String): List<WebDavFileInfo> {
        val files = mutableListOf<WebDavFileInfo>()
        val responsePattern = """<D:href>(.*?)</D:href>.*?<D:getlastmodified>(.*?)</D:getlastmodified>.*?<D:getcontentlength>(\d+)</D:getcontentlength>""".toRegex(RegexOption.DOT_MATCHES_ALL)

        for (match in responsePattern.findAll(xml)) {
            val href = match.groupValues[1]
            val modified = match.groupValues[2]
            val size = match.groupValues[3].toLong()

            val name = href.substringAfterLast("/").trimEnd('/')
            if (name.isNotEmpty()) {
                files.add(WebDavFileInfo(name, href, modified, size))
            }
        }

        return files
    }

    data class WebDavFileInfo(
        val name: String,
        val href: String,
        val lastModified: String,
        val size: Long
    )
}

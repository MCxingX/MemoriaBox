package com.memoriabox.utils

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Credentials
import java.util.concurrent.TimeUnit

class WebDavClient {

    private lateinit var client: OkHttpClient

    data class WebDavConfig(
        val serverUrl: String,
        val username: String,
        val password: String,
        val path: String = "/MemoriaBox/"
    )

    suspend fun testConnection(config: WebDavConfig): Result<Boolean> {
        return try {
            client = OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build()

            val credential = Credentials.basic(config.username, config.password)
            val url = "${config.serverUrl}${config.path}"

            val request = Request.Builder()
                .url(url)
                .method("PROPFIND", "".toRequestBody("application/xml".toMediaType()))
                .header("Depth", "0")
                .header("Authorization", credential)
                .build()

            client.newCall(request).execute().use { response ->
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
            val client = getOrCreateClient()
            val credential = Credentials.basic(config.username, config.password)
            val url = "${config.serverUrl}${config.path}$remoteFileName"

            val fileBytes = java.io.File(localFilePath).readBytes()
            val requestBody = fileBytes.toRequestBody("application/octet-stream".toMediaType())

            val request = Request.Builder()
                .url(url)
                .put(requestBody)
                .header("Authorization", credential)
                .build()

            client.newCall(request).execute().use { response ->
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
            val client = getOrCreateClient()
            val credential = Credentials.basic(config.username, config.password)
            val url = "${config.serverUrl}${config.path}"

            val request = Request.Builder()
                .url(url)
                .method("PROPFIND", "".toRequestBody("application/xml".toMediaType()))
                .header("Depth", "1")
                .header("Authorization", credential)
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: ""
                    val files = parsePropFindResponse(body)
                    Result.success(files)
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
            val client = getOrCreateClient()
            val credential = Credentials.basic(config.username, config.password)
            val url = "${config.serverUrl}${config.path}$remoteFileName"

            val request = Request.Builder()
                .url(url)
                .header("Authorization", credential)
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.bytes()
                    if (body != null) {
                        java.io.File(localFilePath).writeBytes(body)
                        Result.success(true)
                    } else {
                        Result.failure(Exception("Empty response"))
                    }
                } else {
                    Result.failure(Exception("Download failed: ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun getOrCreateClient(): OkHttpClient {
        if (!::client.isInitialized) {
            client = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build()
        }
        return client
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

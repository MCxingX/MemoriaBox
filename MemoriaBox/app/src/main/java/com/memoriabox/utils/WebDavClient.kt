package com.memoriabox.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Credentials
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.util.concurrent.TimeUnit

class WebDavClient {

    private lateinit var client: OkHttpClient

    data class WebDavConfig(
        val serverUrl: String,
        val username: String,
        val password: String,
        val path: String = "/NianJi/"
    )

    suspend fun testConnection(config: WebDavConfig): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
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

    suspend fun uploadFile(config: WebDavConfig, localFilePath: String, remoteFileName: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val client = getOrCreateClient()
            val credential = Credentials.basic(config.username, config.password)
            val url = "${config.serverUrl}${config.path}$remoteFileName"

            val file = java.io.File(localFilePath)
            val requestBody = file.asRequestBody("application/octet-stream".toMediaType())

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

    suspend fun listFiles(config: WebDavConfig): Result<List<WebDavFileInfo>> = withContext(Dispatchers.IO) {
        try {
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

    suspend fun downloadFile(config: WebDavConfig, remoteFileName: String, localFilePath: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val client = getOrCreateClient()
            val credential = Credentials.basic(config.username, config.password)
            val url = "${config.serverUrl}${config.path}$remoteFileName"

            val request = Request.Builder()
                .url(url)
                .header("Authorization", credential)
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body
                    if (body != null) {
                        java.io.File(localFilePath).outputStream().use { output ->
                            body.byteStream().copyTo(output)
                        }
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
        return runCatching {
            val parser = XmlPullParserFactory.newInstance().apply { isNamespaceAware = true }.newPullParser()
            parser.setInput(xml.reader())
            var href = ""
            var lastModified = ""
            var contentLength = 0L
            var inResponse = false
            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        when (parser.name) {
                            "response" -> inResponse = true
                            "href" -> if (inResponse) href = parser.nextText()
                            "getlastmodified" -> if (inResponse) lastModified = parser.nextText()
                            "getcontentlength" -> if (inResponse) {
                                contentLength = parser.nextText().trim().toLongOrNull() ?: 0L
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (parser.name == "response") {
                            val name = href.substringAfterLast("/").trimEnd('/')
                            if (name.isNotEmpty()) {
                                files.add(WebDavFileInfo(name, href, lastModified, contentLength))
                            }
                            href = ""
                            lastModified = ""
                            contentLength = 0L
                            inResponse = false
                        }
                    }
                }
                eventType = parser.next()
            }
            files
        }.getOrDefault(emptyList())
    }

    data class WebDavFileInfo(
        val name: String,
        val href: String,
        val lastModified: String,
        val size: Long
    )
}

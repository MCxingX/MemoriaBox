package com.memoriabox.utils

import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

internal object BackupPackage {
    private const val MANIFEST_ENTRY = "manifest.txt"

    data class ManifestEntry(val name: String, val size: Long, val sha256: String)

    data class Manifest(
        val format: Int,
        val createdAt: Long,
        val appVersion: String,
        val entries: List<ManifestEntry>
    )

    fun build(
        archiveFile: File,
        createdAt: Long,
        appVersion: String,
        files: List<Pair<String, File>>
    ): Manifest {
        val entries = mutableListOf<ManifestEntry>()
        ZipOutputStream(BufferedOutputStream(FileOutputStream(archiveFile))).use { zip ->
            val manifestBuilder = StringBuilder()
            for ((name, file) in files) {
                val sha = sha256(file)
                entries += ManifestEntry(name, file.length(), sha)
                zip.putNextEntry(ZipEntry(name))
                file.inputStream().buffered().use { it.copyTo(zip) }
                zip.closeEntry()
            }
            manifestBuilder.appendLine("format=2")
            manifestBuilder.appendLine("created=$createdAt")
            manifestBuilder.appendLine("app=$appVersion")
            manifestBuilder.appendLine("count=${entries.size}")
            for (entry in entries) {
                manifestBuilder.appendLine("${entry.name}\t${entry.size}\t${entry.sha256}")
            }
            zip.putNextEntry(ZipEntry(MANIFEST_ENTRY))
            zip.write(manifestBuilder.toString().toByteArray(Charsets.UTF_8))
            zip.closeEntry()
        }
        return Manifest(
            format = 2,
            createdAt = createdAt,
            appVersion = appVersion,
            entries = entries
        )
    }

    fun readManifest(archiveFile: File): Manifest {
        ZipInputStream(BufferedInputStream(FileInputStream(archiveFile))).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                if (entry.name == MANIFEST_ENTRY) {
                    val text = zip.readBytes().toString(Charsets.UTF_8)
                    return parseManifest(text)
                }
            }
        }
        throw IllegalStateException("备份清单缺失，文件可能已损坏")
    }

    private fun parseManifest(text: String): Manifest {
        val lines = text.lineSequence().filter { it.isNotBlank() }.toList()
        var format = 0
        var created = 0L
        var app = ""
        val entries = mutableListOf<ManifestEntry>()
        var count = 0
        var index = 0
        while (index < lines.size) {
            val line = lines[index]
            if (line.startsWith("format=")) format = line.removePrefix("format=").toIntOrNull() ?: 0
            else if (line.startsWith("created=")) created = line.removePrefix("created=").toLongOrNull() ?: 0L
            else if (line.startsWith("app=")) app = line.removePrefix("app=")
            else if (line.startsWith("count=")) count = line.removePrefix("count=").toIntOrNull() ?: 0
            else break
            index++
        }
        for (i in 0 until count) {
            if (index + i >= lines.size) break
            val parts = lines[index + i].split("\t")
            if (parts.size == 3) {
                entries += ManifestEntry(parts[0], parts[1].toLongOrNull() ?: 0L, parts[2])
            }
        }
        return Manifest(format = format, createdAt = created, appVersion = app, entries = entries)
    }

    fun extractAll(
        archiveFile: File,
        destDir: File,
        expectedEntries: Map<String, ManifestEntry>
    ) {
        destDir.mkdirs()
        ZipInputStream(BufferedInputStream(FileInputStream(archiveFile))).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                if (entry.name == MANIFEST_ENTRY || entry.isDirectory) continue
                val expected = expectedEntries[entry.name] ?: continue
                if (entry.size >= 0 && expected.size > 0 && entry.size != expected.size) {
                    throw IllegalStateException("备份内容校验失败：${entry.name}")
                }
                val target = File(destDir, entry.name).canonicalFile
                val destRoot = destDir.canonicalFile
                require(target.path.startsWith(destRoot.path + File.separator) || target == destRoot) {
                    "非法压缩条目：${entry.name}"
                }
                target.parentFile?.mkdirs()
                target.outputStream().buffered().use { out -> zip.copyTo(out) }
                val actualSha = sha256(target)
                if (!actualSha.equals(expected.sha256, ignoreCase = true)) {
                    throw IllegalStateException("备份完整性校验失败：${entry.name}")
                }
            }
        }
    }

    fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        BufferedInputStream(FileInputStream(file)).use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            var count: Int
            while (input.read(buffer).also { count = it } >= 0) {
                if (count > 0) digest.update(buffer, 0, count)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    fun sha256(input: InputStream): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var count: Int
        while (input.read(buffer).also { count = it } >= 0) {
            if (count > 0) digest.update(buffer, 0, count)
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}

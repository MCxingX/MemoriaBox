package com.memoriabox.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Files

class BackupArchiveTest {

    private fun tempDir(): File = Files.createTempDirectory("backup_test").toFile()

    @Test
    fun plainBackupRoundTripPreservesBytes() {
        val payload = "明文备份内容测试-数据库字节".repeat(100).toByteArray(Charsets.UTF_8)
        val out = ByteArrayOutputStream()
        BackupArchive.openOutput(out, "").use { it.write(payload) }
        val archived = out.toByteArray()

        val decrypted = ByteArrayOutputStream()
        BackupArchive.openInput(ByteArrayInputStream(archived), "").use {
            it.copyTo(decrypted)
        }
        assertEquals(payload.toList(), decrypted.toByteArray().toList())
    }

    @Test
    fun plainBackupHeaderIsRecognizableAndUnencrypted() {
        val payload = byteArrayOf(1, 2, 3)
        val out = ByteArrayOutputStream()
        BackupArchive.openOutput(out, "").use { it.write(payload) }
        val archived = out.toByteArray()

        val header = BackupArchive.inspect(ByteArrayInputStream(archived))
        assertFalse(header.encrypted)
        assertFalse(header.legacy)
    }

    @Test
    fun encryptedBackupRoundTripPreservesBytes() {
        val payload = "加密备份内容测试-PBKDF2-AESGCM".repeat(200).toByteArray(Charsets.UTF_8)
        val out = ByteArrayOutputStream()
        BackupArchive.openOutput(out, "s3cret").use { it.write(payload) }
        val archived = out.toByteArray()

        val decrypted = ByteArrayOutputStream()
        BackupArchive.openInput(ByteArrayInputStream(archived), "s3cret").use {
            it.copyTo(decrypted)
        }
        assertEquals(payload.toList(), decrypted.toByteArray().toList())
    }

    @Test
    fun encryptedBackupHeaderIsRecognizable() {
        val payload = byteArrayOf(1, 2, 3)
        val out = ByteArrayOutputStream()
        BackupArchive.openOutput(out, "pw").use { it.write(payload) }
        val archived = out.toByteArray()

        val header = BackupArchive.inspect(ByteArrayInputStream(archived))
        assertTrue(header.encrypted)
        assertFalse(header.legacy)
    }

    @Test
    fun randomBytesAreTreatedAsLegacy() {
        val random = ByteArray(64) { it.toByte() }
        val header = BackupArchive.inspect(ByteArrayInputStream(random))
        assertTrue(header.legacy)
    }

    @Test
    fun wrongPasswordFailsDecryption() {
        val payload = "需要密码".repeat(50).toByteArray(Charsets.UTF_8)
        val out = ByteArrayOutputStream()
        BackupArchive.openOutput(out, "right").use { it.write(payload) }
        val archived = out.toByteArray()

        var failed = false
        try {
            BackupArchive.openInput(ByteArrayInputStream(archived), "wrong").use { input ->
                val buffer = ByteArray(4096)
                while (input.read(buffer) != -1) { /* drain to trigger GCM tag check */ }
            }
        } catch (e: Exception) {
            failed = true
        }
        assertTrue(failed)
    }
}

class BackupPackageTest {

    private fun tempDir(): File = Files.createTempDirectory("backup_pkg_test").toFile()

    private fun makeFile(dir: File, name: String, content: String): File {
        val file = File(dir, name).also { it.parentFile?.mkdirs() }
        file.writeText(content)
        return file
    }

    @Test
    fun buildAndExtractVerifyIntegrity() {
        val src = tempDir()
        val dest = tempDir()
        val archive = File(src, "payload.zip")

        val db = makeFile(src, "database.db", "SQLITE-BYTES-PLACEHOLDER")
        val media = makeFile(src, "media/diary_images/photo1.jpg", "JPEG-FAKE-BYTES")
        val settings = makeFile(src, "settings/app_settings.xml", "<map><string name=\"x\" value=\"1\"/></map>")

        val manifest = BackupPackage.build(
            archiveFile = archive,
            createdAt = 123456789L,
            appVersion = "3.4.0",
            files = listOf(
                "database.db" to db,
                "media/diary_images/photo1.jpg" to media,
                "settings/app_settings.xml" to settings
            )
        )

        assertEquals(3, manifest.entries.size)
        assertEquals("3.4.0", manifest.appVersion)

        val expected = manifest.entries.associateBy { it.name }
        BackupPackage.extractAll(archive, dest, expected)

        assertEquals("SQLITE-BYTES-PLACEHOLDER", File(dest, "database.db").readText())
        assertEquals("JPEG-FAKE-BYTES", File(dest, "media/diary_images/photo1.jpg").readText())
        assertTrue(File(dest, "settings/app_settings.xml").exists())
    }

    @Test
    fun tamperedFileFailsVerification() {
        val src = tempDir()
        val dest = tempDir()
        val archive = File(src, "payload.zip")

        val db = makeFile(src, "database.db", "ORIGINAL-CONTENT")

        val manifest = BackupPackage.build(
            archiveFile = archive,
            createdAt = 0L,
            appVersion = "3.4.0",
            files = listOf("database.db" to db)
        )

        val expected = manifest.entries.associateBy { it.name }

        // 篡改清单哈希，模拟文件被改动
        val tampered = expected.toMutableMap()
        tampered["database.db"] = tampered.getValue("database.db").copy(sha256 = "0".repeat(64))

        var failed = false
        try {
            BackupPackage.extractAll(archive, dest, tampered)
        } catch (e: Exception) {
            failed = true
        }
        assertTrue(failed)
    }

    @Test
    fun missingManifestFails() {
        val src = tempDir()
        val archive = File(src, "payload.zip")
        makeFile(src, "database.db", "no-manifest")

        var failed = false
        try {
            BackupPackage.readManifest(archive)
        } catch (e: Exception) {
            failed = true
        }
        assertTrue(failed)
    }

    @Test
    fun manifestRoundTripParsesAllFields() {
        val src = tempDir()
        val archive = File(src, "payload.zip")
        val db = makeFile(src, "database.db", "DATA")

        val manifest = BackupPackage.build(archive, 987654321L, "3.4.0", listOf("database.db" to db))
        val parsed = BackupPackage.readManifest(archive)

        assertEquals(2, parsed.format)
        assertEquals(987654321L, parsed.createdAt)
        assertEquals("3.4.0", parsed.appVersion)
        assertNotNull(parsed.entries.firstOrNull { it.name == "database.db" })
        assertEquals(manifest.entries.first().sha256, parsed.entries.first().sha256)
    }
}

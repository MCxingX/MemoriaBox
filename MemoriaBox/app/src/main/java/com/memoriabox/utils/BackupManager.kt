package com.memoriabox.utils

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import androidx.room.Room
import androidx.room.withTransaction
import com.memoriabox.BuildConfig
import com.memoriabox.database.AppDatabase
import com.memoriabox.database.MIGRATION_1_2
import com.memoriabox.database.MIGRATION_2_3
import com.memoriabox.database.MIGRATION_3_4
import com.memoriabox.database.MIGRATION_4_5
import com.memoriabox.database.MIGRATION_5_6
import com.memoriabox.database.MIGRATION_6_7
import com.memoriabox.data.model.BackupConfig
import kotlinx.coroutines.*
import java.io.*
import java.text.SimpleDateFormat
import java.util.*
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.crypto.KeyGenerator
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

class BackupManager(
    private val context: Context,
    private val database: AppDatabase
) {
    data class ImportResult(
        val boxes: Int,
        val labels: Int,
        val friends: Int,
        val friendRelations: Int,
        val events: Int,
        val eventLabels: Int,
        val diaries: Int,
        val media: Int,
        val logs: Int,
        val restoredMediaFiles: Int = 0
    ) {
        val userContentCount: Int
            get() = events + diaries + media + friends + boxes

        fun toSummary(): String = "导入方式：合并导入\n" +
            "分组：${boxes} 个\n" +
            "日子：${events} 个\n" +
            "日记：${diaries} 篇\n" +
            "素材：${media} 个\n" +
            "好友：${friends} 个\n" +
            "标签：${labels} 个\n" +
            "恢复媒体文件：${restoredMediaFiles} 个\n" +
            "当前数据：已保留\n" +
            "如存在同名或重复内容，会按备份数据合并更新。"
    }

    private var debounceJob: Job? = null
    private var config = BackupConfig()
    private var backupDirUri: Uri? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    companion object {
        private const val TAG = "BackupManager"
        private const val AUTO_BACKUP_DIR = "auto_backup"
        private const val BACKUP_EXTENSION = ".mbox"
        private const val BACKUP_KEY_ALIAS = "memoriabox-portable-backup"
        private const val DB_ENTRY = "database.db"
        private const val MEDIA_PREFIX = "media/"
        private const val SETTINGS_PREFIX = "settings/"
        private const val URI_MAP_ENTRY = "uris.txt"
        private val SETTINGS_FILES = listOf("app_settings", "ui_settings", "pushplus_config")
    }

    fun inspectBackup(uri: Uri): Header? {
        return runCatching {
            context.contentResolver.openInputStream(uri)?.use { input ->
                BackupArchive.inspect(input)
            }
        }.getOrNull()
    }

    fun initialize() {
        loadPreferences()
    }

    private fun loadPreferences() {
        val prefs = context.getSharedPreferences("backup_config", Context.MODE_PRIVATE)
        config = BackupConfig(
            autoBackupDelay = prefs.getLong("auto_backup_delay", 20000L),
            maxAutoBackups = prefs.getInt("max_auto_backups", 5),
            maxLogEntries = prefs.getInt("max_log_entries", 2000),
            backupPassword = prefs.getString("backup_password", "") ?: ""
        )
        val uriString = prefs.getString("backup_dir_uri", "")
        if (!uriString.isNullOrEmpty()) {
            backupDirUri = Uri.parse(uriString)
        }
    }

    private fun savePreferences() {
        val prefs = context.getSharedPreferences("backup_config", Context.MODE_PRIVATE)
        prefs.edit()
            .putLong("auto_backup_delay", config.autoBackupDelay)
            .putInt("max_auto_backups", config.maxAutoBackups)
            .putString("backup_dir_uri", backupDirUri?.toString() ?: "")
            .putString("backup_password", config.backupPassword)
            .apply()
    }

    fun updateConfig(newConfig: BackupConfig) {
        config = newConfig
        savePreferences()
    }

    fun saveBackupDirUri(uri: Uri) {
        backupDirUri = uri
        context.contentResolver.takePersistableUriPermission(
            uri,
            android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
            android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        )
        savePreferences()
    }

    fun onDataChanged() {
        debounceJob?.cancel()
        debounceJob = scope.launch {
            delay(config.autoBackupDelay)
            performAutoBackup()
        }
    }

    private suspend fun performAutoBackup() {
        try {
            val dir = getOrCreateAutoBackupDir() ?: return
            val timestamp = System.currentTimeMillis()
            val fileName = "auto_${formatTimestamp(timestamp)}$BACKUP_EXTENSION"
            val dbPath = context.getDatabasePath("memoriabox.db")

            if (!dbPath.exists()) {
                Log.w(TAG, "Database file not found")
                return
            }

            checkpointDatabase()
            validateDatabaseForBackup(dbPath)
            val archiveFile = buildArchive(dbPath, config.backupPassword)
            val resultUri = copyFileToDir(archiveFile, dir, fileName)
            if (resultUri != null) {
                rotateBackups(dir)
            }
            Log.d(TAG, "Auto backup completed: $fileName")
        } catch (e: Exception) {
            Log.e(TAG, "Auto backup failed", e)
        }
    }

    suspend fun performManualBackup(
        outputDir: Uri,
        password: String = "",
        onProgress: (Int) -> Unit = {}
    ): Result<Uri?> {
        return try {
            val timestamp = System.currentTimeMillis()
            val fileName = "manual_${formatTimestamp(timestamp)}$BACKUP_EXTENSION"
            val dbPath = context.getDatabasePath("memoriabox.db")

            if (!dbPath.exists()) {
                return Result.failure(IllegalStateException("Database file not found"))
            }

            onProgress(20)
            checkpointDatabase()
            validateDatabaseForBackup(dbPath)
            val archiveFile = buildArchive(dbPath, password)
            onProgress(60)

            val resultUri = copyFileToDir(archiveFile, outputDir, fileName)
            onProgress(100)

            Result.success(resultUri)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun importBackup(
        backupUri: Uri,
        password: String = "",
        onProgress: (Int) -> Unit = {}
    ): Result<ImportResult> {
        return try {
            onProgress(20)
            val header = inspectBackup(backupUri)
            if (header == null) {
                return Result.failure(IllegalStateException("无法识别的备份文件"))
            }

            val workDir = File(context.cacheDir, "import_${System.currentTimeMillis()}").apply { mkdirs() }
            try {
                if (header.legacy) {
                    val dbFile = File(workDir, "legacy.db")
                    if (!decryptLegacyDatabase(backupUri, password, dbFile)) {
                        return Result.failure(IllegalStateException("备份解密失败，请检查密码是否正确"))
                    }
                    onProgress(60)
                    val result = mergeDatabase(dbFile)
                    if (result.userContentCount == 0) {
                        return Result.failure(IllegalStateException("备份文件中没有可导入的日子、日记、素材、好友或分组"))
                    }
                    onProgress(100)
                    return Result.success(result)
                }

                if (header.encrypted && password.isNullOrEmpty()) {
                    return Result.failure(IllegalStateException("此备份已加密，请输入备份密码"))
                }

                val payload = File(workDir, "payload.zip")
                decryptToFile(backupUri, password ?: "", payload)

                val manifest = BackupPackage.readManifest(payload)
                if (manifest.format != 2) {
                    return Result.failure(IllegalStateException("备份格式版本 ${manifest.format} 暂不受支持"))
                }
                BackupPackage.extractAll(
                    payload,
                    workDir,
                    manifest.entries.associateBy { it.name }
                )

                val dbFile = File(workDir, DB_ENTRY)
                if (!dbFile.exists()) {
                    return Result.failure(IllegalStateException("备份中缺少数据库文件"))
                }

                onProgress(60)
                val restoredMedia = restoreMedia(workDir, dbFile)
                restoreSettings(workDir)
                val result = mergeDatabase(dbFile)
                if (result.userContentCount == 0) {
                    return Result.failure(IllegalStateException("备份文件中没有可导入的日子、日记、素材、好友或分组"))
                }
                onProgress(100)
                Result.success(result.copy(restoredMediaFiles = restoredMedia))
            } finally {
                workDir.deleteRecursively()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Import backup failed", e)
            Result.failure(e)
        }
    }

    private suspend fun buildArchive(dbFile: File, password: String): File {
        val workDir = File(context.cacheDir, "archive_${System.currentTimeMillis()}").apply { mkdirs() }
        try {
            val dbCopy = File(workDir, DB_ENTRY)
            dbFile.copyTo(dbCopy, overwrite = true)

            val settingsDir = File(workDir, SETTINGS_PREFIX).apply { mkdirs() }
            val settingsFiles = SETTINGS_FILES.mapNotNull { name ->
                val prefsFile = File(context.applicationInfo.dataDir, "shared_prefs/$name.xml")
                if (prefsFile.exists()) {
                    val target = File(settingsDir, "$name.xml")
                    prefsFile.copyTo(target, overwrite = true)
                    SETTINGS_PREFIX + name + ".xml" to target
                } else {
                    null
                }
            }

            val mediaDir = File(workDir, MEDIA_PREFIX).apply { mkdirs() }
            val mediaEntries = collectMediaEntries(mediaDir)

            val uriMapFile = File(workDir, URI_MAP_ENTRY)
            val uriLines = mediaEntries.map { (zipName, source, originalUri) ->
                "$originalUri\t$zipName"
            }
            uriMapFile.writeText(uriLines.joinToString("\n"))

            val allFiles = listOf(DB_ENTRY to dbCopy) +
                settingsFiles +
                mediaEntries.map { (zipName, source, _) -> zipName to source } +
                (URI_MAP_ENTRY to uriMapFile)

            val payload = File(workDir, "payload.zip")
            BackupPackage.build(
                archiveFile = payload,
                createdAt = System.currentTimeMillis(),
                appVersion = BuildConfig.VERSION_NAME,
                files = allFiles
            )

            val outFile = File(context.cacheDir, "temp_encrypt_${System.currentTimeMillis()}.mbox")
            if (outFile.exists()) outFile.delete()
            FileOutputStream(outFile).use { output ->
                BackupArchive.openOutput(output, password).use { archive ->
                    payload.inputStream().use { it.copyTo(archive) }
                }
            }
            return outFile
        } finally {
            workDir.deleteRecursively()
        }
    }

    private suspend fun collectMediaEntries(mediaDir: File): List<Triple<String, File, String>> {
        val result = mutableListOf<Triple<String, File, String>>()
        val referencedUris = mutableSetOf<String>()
        runCatching {
            database.diaryDao().getAllMediaOnce().forEach { referencedUris += it.mediaUri }
            database.diaryDao().getAllDiariesOnce().forEach { referencedUris += it.backgroundMediaUri.orEmpty() }
            database.eventDao().getAllEventsOnce().forEach { referencedUris += it.avatarUri.orEmpty() }
            database.friendDao().getAllFriendsOnce().forEach { referencedUris += it.avatarUri.orEmpty() }
        }
        val seenFiles = mutableSetOf<File>()
        referencedUris.filter { it.startsWith("file://") }.forEach { uriString ->
            runCatching {
                val file = File(Uri.parse(uriString).path ?: return@runCatching)
                if (file.exists() && file.isFile && file.length() > 0 && seenFiles.add(file)) {
                    val folder = file.parentFile?.name ?: "misc"
                    val target = File(mediaDir, "$folder/${file.name}")
                    target.parentFile?.mkdirs()
                    file.copyTo(target, overwrite = true)
                    result += Triple(MEDIA_PREFIX + folder + "/" + file.name, target, uriString)
                }
            }
        }
        return result
    }

    private fun restoreMedia(workDir: File, dbFile: File): Int {
        val mediaRoot = File(workDir, MEDIA_PREFIX)
        if (!mediaRoot.exists()) return 0

        val zipToOriginal = mutableMapOf<String, String>()
        runCatching {
            val uriMapFile = File(workDir, URI_MAP_ENTRY)
            if (uriMapFile.exists()) {
                uriMapFile.readLines().forEach { line ->
                    val parts = line.split("\t")
                    if (parts.size == 2) zipToOriginal[parts[1]] = parts[0]
                }
            }
        }

        val restoreRoot = File(context.filesDir, "restored_${System.currentTimeMillis()}")
        val uriMap = mutableMapOf<String, String>()
        var count = 0

        mediaRoot.walkTopDown().filter { it.isFile }.forEach { file ->
            val relative = file.relativeTo(mediaRoot).path
            val newFile = File(restoreRoot, relative)
            newFile.parentFile?.mkdirs()
            file.copyTo(newFile, overwrite = true)
            val originalUri = zipToOriginal[MEDIA_PREFIX + relative] ?: Uri.fromFile(file).toString()
            uriMap[originalUri] = Uri.fromFile(newFile).toString()
            count++
        }
        if (uriMap.isEmpty()) return 0

        rewriteUris(dbFile, uriMap)
        return count
    }

    private fun rewriteUris(dbFile: File, uriMap: Map<String, String>) {
        if (uriMap.isEmpty()) return
        val importDbName = "import_rewrite_${dbFile.name}"
        val importDbPath = context.getDatabasePath(importDbName)
        clearDatabaseFiles(importDbPath)
        dbFile.copyTo(importDbPath, overwrite = true)

        val importDb = Room.databaseBuilder(context, AppDatabase::class.java, importDbName).build()
        try {
            val db = importDb.openHelper.writableDatabase
            val updates = listOf(
                "UPDATE diary_media SET media_uri = ? WHERE media_uri = ?",
                "UPDATE diary_entries SET background_media_uri = ? WHERE background_media_uri = ?",
                "UPDATE events SET avatar_uri = ? WHERE avatar_uri = ?",
                "UPDATE friends SET avatar_uri = ? WHERE avatar_uri = ?"
            )
            for ((oldUri, newUri) in uriMap) {
                for (sql in updates) {
                    runCatching { db.execSQL(sql, arrayOf(newUri, oldUri)) }
                }
            }
            importDb.close()
            importDbPath.copyTo(dbFile, overwrite = true)
        } finally {
            runCatching { importDb.close() }
            clearDatabaseFiles(importDbPath)
        }
    }

    private fun restoreSettings(workDir: File) {
        val settingsDir = File(workDir, SETTINGS_PREFIX)
        if (!settingsDir.exists()) return
        for (name in SETTINGS_FILES) {
            val xml = File(settingsDir, "$name.xml")
            if (!xml.exists()) continue
            runCatching { applyPreferencesXml(name, xml) }
        }
    }

    private fun applyPreferencesXml(name: String, xmlFile: File) {
        val doc = javax.xml.parsers.DocumentBuilderFactory.newInstance().newDocumentBuilder()
            .parse(xmlFile)
        val root = doc.documentElement
        val mapTag = root.getElementsByTagName("map")
        if (mapTag.length == 0) return
        val prefs = context.getSharedPreferences(name, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        val nodes = mapTag.item(0).childNodes
        for (i in 0 until nodes.length) {
            val node = nodes.item(i)
            if (node.nodeType != org.w3c.dom.Node.ELEMENT_NODE) continue
            val key = node.attributes.getNamedItem("name")?.nodeValue ?: continue
            when (node.nodeName) {
                "string" -> editor.putString(key, node.attributes.getNamedItem("value")?.nodeValue ?: "")
                "int" -> editor.putInt(key, (node.attributes.getNamedItem("value")?.nodeValue ?: "0").toIntOrNull() ?: 0)
                "long" -> editor.putLong(key, (node.attributes.getNamedItem("value")?.nodeValue ?: "0").toLongOrNull() ?: 0L)
                "boolean" -> editor.putBoolean(key, (node.attributes.getNamedItem("value")?.nodeValue ?: "false").toBooleanStrictOrNull() ?: false)
                "float" -> editor.putFloat(key, (node.attributes.getNamedItem("value")?.nodeValue ?: "0").toFloatOrNull() ?: 0f)
            }
        }
        editor.apply()
    }

    private fun decryptToFile(backupUri: Uri, password: String, output: File) {
        context.contentResolver.openInputStream(backupUri)?.use { input ->
            BackupArchive.openInput(input, password).use { archive ->
                output.outputStream().use { archive.copyTo(it) }
            }
        } ?: throw IOException("Cannot open backup file")
    }

    private fun getOrCreateAutoBackupDir(): Uri? {
        return getOrCreateSubDir(backupDirUri, AUTO_BACKUP_DIR)
    }

    private fun getOrCreateSubDir(parentUri: Uri?, subDirName: String): Uri? {
        if (parentUri == null) return null

        return try {
            val pickedDir = DocumentFile.fromTreeUri(context, parentUri) ?: return null
            var subDir = pickedDir.findFile(subDirName)

            if (subDir == null) {
                subDir = pickedDir.createDirectory(subDirName)
            }
            subDir?.uri
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create/locate subdirectory", e)
            null
        }
    }

    private fun InputStream.readFully(buffer: ByteArray) {
        var offset = 0
        while (offset < buffer.size) {
            val bytesRead = read(buffer, offset, buffer.size - offset)
            if (bytesRead == -1) throw EOFException("Unexpected end of stream")
            offset += bytesRead
        }
    }

    private suspend fun decryptLegacyDatabase(encryptedUri: Uri, password: String?, output: File): Boolean {
        return try {
            context.contentResolver.openInputStream(encryptedUri)?.use { input ->
                val salt = ByteArray(16).also { input.readFully(it) }
                val iv = ByteArray(12).also { input.readFully(it) }

                val remainingBytes = input.readBytes()
                val decrypted = decryptBytes(remainingBytes, salt, iv, password)

                FileOutputStream(output).use { it.write(decrypted) }
            } ?: throw IOException("Cannot open backup file")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Legacy decryption failed", e)
            false
        }
    }

    private suspend fun mergeDatabase(tempFile: File): ImportResult {
        val importDbName = "import_${tempFile.name}"
        val importDbPath = context.getDatabasePath(importDbName)
        clearDatabaseFiles(importDbPath)
        tempFile.copyTo(importDbPath, overwrite = true)

        val importDb = Room.databaseBuilder(context, AppDatabase::class.java, importDbName)
            .addMigrations(
                MIGRATION_1_2,
                MIGRATION_2_3,
                MIGRATION_3_4,
                MIGRATION_4_5,
                MIGRATION_5_6,
                MIGRATION_6_7
            )
            .addCallback(object : androidx.room.RoomDatabase.Callback() {
                override fun onOpen(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                    super.onOpen(db)
                    AppDatabase.repairLegacyData(db)
                }
            })
            .build()

        try {
            val boxes = importDb.boxDao().getAllBoxesOnce()
            val labels = importDb.labelDao().getAllLabelsOnce()
            val friends = importDb.friendDao().getAllFriendsOnce()
            val friendRelations = importDb.friendDao().getAllFriendRelationsOnce()
            val events = importDb.eventDao().getAllEventsOnce()
            val eventLabels = importDb.labelDao().getAllEventLabelsOnce()
            val diaries = importDb.diaryDao().getAllDiariesOnce()
            val media = importDb.diaryDao().getAllMediaOnce()
            val logs = importDb.logDao().getAllLogsOnce()

            database.withTransaction {
                boxes.takeIf { it.isNotEmpty() }?.let { database.boxDao().insertBoxes(it) }
                labels.takeIf { it.isNotEmpty() }?.let { database.labelDao().insertLabels(it) }
                friends.takeIf { it.isNotEmpty() }?.let { database.friendDao().upsertFriends(it) }
                friendRelations.takeIf { it.isNotEmpty() }?.let { database.friendDao().upsertFriendRelations(it) }
                events.takeIf { it.isNotEmpty() }?.let { database.eventDao().insertEvents(it) }
                eventLabels.takeIf { it.isNotEmpty() }?.let { database.labelDao().addEventLabels(it) }
                diaries.takeIf { it.isNotEmpty() }?.let { database.diaryDao().upsertDiaries(it) }
                media.takeIf { it.isNotEmpty() }?.let { database.diaryDao().upsertMedia(it) }
                logs.takeIf { it.isNotEmpty() }?.let { database.logDao().insertLogs(it.map { log -> log.copy(id = 0) }) }
            }

            return ImportResult(
                boxes = boxes.count { it.id != "default_1" },
                labels = labels.size,
                friends = friends.size,
                friendRelations = friendRelations.size,
                events = events.size,
                eventLabels = eventLabels.size,
                diaries = diaries.size,
                media = media.size,
                logs = logs.size
            )
        } finally {
            importDb.close()
            clearDatabaseFiles(importDbPath)
        }
    }

    private fun checkpointDatabase() {
        database.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(TRUNCATE)").use { cursor ->
            if (cursor.moveToFirst()) {
                val busy = cursor.getInt(0)
                if (busy != 0) {
                    throw IOException("Database checkpoint is busy")
                }
            }
        }
    }

    private fun validateDatabaseForBackup(dbFile: File) {
        if (!dbFile.exists() || dbFile.length() <= 4096L) {
            throw IOException("Database file is empty after checkpoint")
        }

        database.openHelper.readableDatabase.query(
            "SELECT COUNT(*) FROM sqlite_master WHERE type='table' AND name IN ('boxes', 'events')"
        ).use { cursor ->
            if (!cursor.moveToFirst() || cursor.getInt(0) < 2) {
                throw IOException("Database schema is incomplete")
            }
        }
    }

    private fun clearDatabaseFiles(dbPath: File) {
        listOf(
            dbPath,
            File(dbPath.parentFile, "${dbPath.name}-wal"),
            File(dbPath.parentFile, "${dbPath.name}-shm")
        ).forEach { file ->
            if (file.exists()) file.delete()
        }
    }

    private fun decryptBytes(encryptedBytes: ByteArray, salt: ByteArray, iv: ByteArray, password: String?): ByteArray {
        val keys = if (!password.isNullOrEmpty()) {
            listOf(deriveKeyFromPassword(password, salt))
        } else {
            listOf(deriveKeyFromPassword(BACKUP_KEY_ALIAS, salt), getDeviceKey())
        }
        var lastError: Exception? = null
        keys.forEach { key ->
            try {
                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                val spec = GCMParameterSpec(128, iv)
                cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), spec)
                return cipher.update(encryptedBytes) + cipher.doFinal()
            } catch (e: Exception) {
                lastError = e
            }
        }
        throw lastError ?: IllegalStateException("Unable to decrypt backup")
    }

    private fun copyFileToDir(sourceFile: File, targetDir: Uri, fileName: String): Uri? {
        val pickedDir = DocumentFile.fromTreeUri(context, targetDir)
            ?: throw IllegalStateException("Invalid directory")
        val newFile = pickedDir.createFile("application/octet-stream", fileName)
            ?: throw IllegalStateException("Failed to create file")

        context.contentResolver.openOutputStream(newFile.uri)?.use { output ->
            FileInputStream(sourceFile).use { input ->
                input.copyTo(output)
            }
        }

        sourceFile.delete()
        return newFile.uri
    }

    private fun rotateBackups(dirUri: Uri) {
        try {
            val pickedDir = DocumentFile.fromTreeUri(context, dirUri)
            if (pickedDir != null && pickedDir.exists()) {
                val files = pickedDir.listFiles()
                    .filter { it.name?.endsWith(BACKUP_EXTENSION) == true }
                    .sortedBy { it.lastModified() }

                val mutableFiles = files.toMutableList()
                while (mutableFiles.size > config.maxAutoBackups) {
                    mutableFiles.first().delete()
                    mutableFiles.removeAt(0)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to rotate backups", e)
        }
    }

    private fun deriveKeyFromPassword(password: String, salt: ByteArray): ByteArray {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(password.toCharArray(), salt, 100000, 256)
        return factory.generateSecret(spec).encoded
    }

    private fun getDeviceKey(): ByteArray {
        val prefs = context.getSharedPreferences("device_key", Context.MODE_PRIVATE)
        var keyBase64 = prefs.getString("device_key", null)
        if (keyBase64 == null) {
            val keyGenerator = KeyGenerator.getInstance("AES")
            keyGenerator.init(256)
            val key = keyGenerator.generateKey()
            keyBase64 = android.util.Base64.encodeToString(key.encoded, android.util.Base64.NO_WRAP)
            prefs.edit().putString("device_key", keyBase64).apply()
        }
        return android.util.Base64.decode(keyBase64, android.util.Base64.NO_WRAP)
    }

    private fun formatTimestamp(timestamp: Long): String {
        val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}

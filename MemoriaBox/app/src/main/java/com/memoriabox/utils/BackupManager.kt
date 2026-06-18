package com.memoriabox.utils

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import androidx.room.Room
import androidx.room.withTransaction
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
import java.security.SecureRandom
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
        val logs: Int
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
            val encryptedFile = encryptDatabase(dbPath, config.backupPassword)
            val resultUri = copyFileToDir(encryptedFile, dir, fileName)
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
            val encryptedFile = encryptDatabase(dbPath, password)
            onProgress(60)
            
            val resultUri = copyFileToDir(encryptedFile, outputDir, fileName)
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
        val tempFile = File(context.cacheDir, "temp_import_backup_${System.currentTimeMillis()}.db")
        tempFile.delete()
        return try {
            onProgress(20)
            
            val decrypted = decryptDatabase(backupUri, password, tempFile)
            if (!decrypted) {
                return Result.failure(IllegalStateException("Decryption failed or wrong password"))
            }
            
            onProgress(60)
            val result = mergeDatabase(tempFile)
            if (result.userContentCount == 0) {
                return Result.failure(IllegalStateException("备份文件中没有可导入的日子、日记、素材、好友或分组"))
            }
            onProgress(100)
            
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            if (tempFile.exists()) {
                tempFile.delete()
            }
        }
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

    private suspend fun encryptDatabase(dbFile: File, password: String?): File {
        val encryptedFile = File(context.cacheDir, "temp_encrypt_${System.currentTimeMillis()}.mbox")
        if (encryptedFile.exists()) encryptedFile.delete()
        
        val salt = ByteArray(16)
        SecureRandom().nextBytes(salt)
        val iv = ByteArray(12)
        SecureRandom().nextBytes(iv)

        val key = if (!password.isNullOrEmpty()) {
            deriveKeyFromPassword(password, salt)
        } else {
            deriveKeyFromPassword(BACKUP_KEY_ALIAS, salt)
        }

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), spec)

        FileInputStream(dbFile).use { input ->
            FileOutputStream(encryptedFile).use { output ->
                output.write(salt)
                output.write(iv)
                
                val buffer = ByteArray(4096)
                var bytesRead: Int
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    val encrypted = cipher.update(buffer, 0, bytesRead)
                    if (encrypted != null) output.write(encrypted)
                }
                val finalBlock = cipher.doFinal()
                output.write(finalBlock)
            }
        }

        return encryptedFile
    }

    private suspend fun decryptDatabase(encryptedUri: Uri, password: String?, output: File): Boolean {
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
            Log.e(TAG, "Decryption failed", e)
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
        database.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)").use { }
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

package com.memoriabox.utils

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import com.memoriabox.database.AppDatabase
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
    private var debounceJob: Job? = null
    private var config = BackupConfig()
    private var backupDirUri: Uri? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    companion object {
        private const val TAG = "BackupManager"
        private const val AUTO_BACKUP_DIR = "auto_backup"
        private const val BACKUP_EXTENSION = ".mbox"
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
            val encryptedFile = encryptDatabase(dbPath, password.ifEmpty { null })
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
    ): Result<Unit> {
        val tempFile = File(context.cacheDir, "temp_import_backup.db")
        tempFile.delete()
        return try {
            onProgress(20)
            
            val decrypted = decryptDatabase(backupUri, password.ifEmpty { null }, tempFile)
            if (!decrypted) {
                return Result.failure(IllegalStateException("Decryption failed or wrong password"))
            }
            
            onProgress(60)
            restoreDatabase(tempFile)
            onProgress(100)
            
            Result.success(Unit)
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
            getDeviceKey()
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
                val salt = ByteArray(16).also { input.read(it) }
                val iv = ByteArray(12).also { input.read(it) }

                val key = if (!password.isNullOrEmpty()) {
                    deriveKeyFromPassword(password, salt)
                } else {
                    getDeviceKey()
                }

                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                val spec = GCMParameterSpec(128, iv)
                cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), spec)

                val remainingBytes = input.readBytes()
                val decrypted = cipher.update(remainingBytes) + cipher.doFinal()

                FileOutputStream(output).use { it.write(decrypted) }
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Decryption failed", e)
            false
        }
    }

    private suspend fun restoreDatabase(tempFile: File) {
        val dbPath = context.getDatabasePath("memoriabox.db")
        if (dbPath.exists()) dbPath.delete()
        tempFile.copyTo(dbPath, overwrite = true)
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

package com.memoriabox.update

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.pm.PackageInfoCompat
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest
import java.util.Locale

object UpdateVerifier {
    fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                if (count > 0) digest.update(buffer, 0, count)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    fun verify(context: Context, file: File, info: UpdateInfo): Result<Unit> = runCatching {
        require(file.isFile && file.length() > 0) { "更新包为空" }
        if (info.sha256.isNotBlank()) {
            require(sha256(file).equals(info.sha256, ignoreCase = true)) { "更新包 SHA-256 校验失败" }
        }

        val packageInfo = archivePackageInfo(context.packageManager, file)
            ?: error("无法读取更新包信息")
        require(packageInfo.packageName == context.packageName) { "更新包应用标识不匹配" }

        val archiveVersionName = packageInfo.versionName.orEmpty()
        require(
            UpdateFormat.normalizeVersion(archiveVersionName) == UpdateFormat.normalizeVersion(info.versionName)
        ) { "更新包版本与 GitHub Release 不匹配" }
        val currentInfo = currentPackageInfo(context.packageManager, context.packageName)
        require(PackageInfoCompat.getLongVersionCode(packageInfo) > PackageInfoCompat.getLongVersionCode(currentInfo)) {
            "更新包版本码未高于当前版本"
        }

        require(signatureDigests(packageInfo) == signatureDigests(currentInfo)) { "更新包签名与当前应用不一致" }
    }

    private fun archivePackageInfo(packageManager: PackageManager, file: File): PackageInfo? {
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            PackageManager.GET_SIGNING_CERTIFICATES
        } else {
            @Suppress("DEPRECATION") PackageManager.GET_SIGNATURES
        }
        return packageManager.getPackageArchiveInfo(file.absolutePath, flags)
    }

    private fun currentPackageInfo(packageManager: PackageManager, packageName: String): PackageInfo {
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            PackageManager.GET_SIGNING_CERTIFICATES
        } else {
            @Suppress("DEPRECATION") PackageManager.GET_SIGNATURES
        }
        @Suppress("DEPRECATION")
        return packageManager.getPackageInfo(packageName, flags)
    }

    private fun signatureDigests(packageInfo: PackageInfo): Set<String> {
        val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo.signingInfo?.let { signingInfo ->
                if (signingInfo.hasMultipleSigners()) {
                    signingInfo.apkContentsSigners
                } else {
                    signingInfo.signingCertificateHistory
                }
            }.orEmpty()
        } else {
            @Suppress("DEPRECATION") packageInfo.signatures.orEmpty()
        }
        return signatures.map { signature ->
            MessageDigest.getInstance("SHA-256")
                .digest(signature.toByteArray())
                .joinToString("") { "%02x".format(Locale.US, it) }
        }.toSet()
    }
}

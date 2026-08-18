package com.memoriabox.utils

import android.content.Context
import android.os.Build
import com.memoriabox.BuildConfig

data class InstalledAppVersion(
    val name: String,
    val code: Long
)

fun Context.installedAppVersion(): InstalledAppVersion = runCatching {
    val info = packageManager.getPackageInfo(packageName, 0)
    val code = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        info.longVersionCode
    } else {
        @Suppress("DEPRECATION")
        info.versionCode.toLong()
    }
    InstalledAppVersion(info.versionName.orEmpty(), code)
}.getOrElse {
    InstalledAppVersion(BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE.toLong())
}

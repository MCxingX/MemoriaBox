package com.memoriabox.utils

import android.content.Context
import android.net.Uri
import java.io.File

object ImageImportUtils {
    fun copyImageToPrivateStorage(context: Context, uri: Uri, folder: String = "picked_images"): String? {
        return runCatching {
            val dir = File(context.filesDir, folder).apply { mkdirs() }
            val target = File(dir, "image_${System.currentTimeMillis()}.jpg")
            context.contentResolver.openInputStream(uri)?.use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            } ?: return null
            Uri.fromFile(target).toString()
        }.getOrNull()
    }
}

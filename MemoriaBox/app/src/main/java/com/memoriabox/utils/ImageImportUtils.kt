package com.memoriabox.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.webkit.MimeTypeMap
import java.io.File
import org.json.JSONObject

object ImageImportUtils {
    data class EditState(
        val sourceUri: String,
        val cropLeft: Float = 0f,
        val cropTop: Float = 0f,
        val cropWidth: Float = 1f,
        val cropHeight: Float = 1f
    )

    private const val EDIT_STATE_PREFS = "image_edit_states"

    fun copyImageToPrivateStorage(context: Context, uri: Uri, folder: String = "picked_images"): String? {
        return copyMediaToPrivateStorage(context, uri, folder, "jpg")
    }

    fun saveOriginalImage(context: Context, uri: Uri): String? =
        copyImageToPrivateStorage(context, uri, "original_images")

    fun getEditState(context: Context, displayUri: String, fallbackSourceUri: String = displayUri): EditState {
        val stored = context.getSharedPreferences(EDIT_STATE_PREFS, Context.MODE_PRIVATE)
            .getString(displayUri, null)
        return runCatching {
            val json = JSONObject(stored ?: return@runCatching EditState(fallbackSourceUri))
            EditState(
                sourceUri = json.optString("sourceUri", fallbackSourceUri),
                cropLeft = json.optDouble("cropLeft", 0.0).toFloat().coerceIn(0f, 1f),
                cropTop = json.optDouble("cropTop", 0.0).toFloat().coerceIn(0f, 1f),
                cropWidth = json.optDouble("cropWidth", 1.0).toFloat().coerceIn(0.05f, 1f),
                cropHeight = json.optDouble("cropHeight", 1.0).toFloat().coerceIn(0.05f, 1f)
            )
        }.getOrDefault(EditState(fallbackSourceUri))
    }

    fun saveEditState(context: Context, displayUri: String, state: EditState) {
        val json = JSONObject()
            .put("sourceUri", state.sourceUri)
            .put("cropLeft", state.cropLeft)
            .put("cropTop", state.cropTop)
            .put("cropWidth", state.cropWidth)
            .put("cropHeight", state.cropHeight)
        context.getSharedPreferences(EDIT_STATE_PREFS, Context.MODE_PRIVATE)
            .edit().putString(displayUri, json.toString()).apply()
    }

    fun removeEditState(context: Context, displayUri: String?) {
        if (!displayUri.isNullOrBlank()) {
            context.getSharedPreferences(EDIT_STATE_PREFS, Context.MODE_PRIVATE)
                .edit().remove(displayUri).apply()
        }
    }

    fun copyMediaToPrivateStorage(context: Context, uri: Uri, folder: String = "picked_media", fallbackExtension: String = "bin"): String? {
        return runCatching {
            val dir = File(context.filesDir, folder).apply { mkdirs() }
            val extension = resolveExtension(context, uri, fallbackExtension)
            val target = File(dir, "media_${System.currentTimeMillis()}.$extension")
            context.contentResolver.openInputStream(uri)?.use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            } ?: return null
            Uri.fromFile(target).toString()
        }.getOrNull()
    }

    fun cropImageToPrivateStorage(
        context: Context,
        sourceUri: Uri,
        folder: String = "picked_images",
        sourceLeft: Float,
        sourceTop: Float,
        sourceWidth: Float,
        sourceHeight: Float
    ): String? {
        return runCatching {
            val source = decodeWithOrientation(context, sourceUri) ?: return null
            val left = (sourceLeft * source.width).toInt().coerceIn(0, (source.width - 1).coerceAtLeast(0))
            val top = (sourceTop * source.height).toInt().coerceIn(0, (source.height - 1).coerceAtLeast(0))
            val right = ((sourceLeft + sourceWidth) * source.width).toInt().coerceIn(left + 1, source.width)
            val bottom = ((sourceTop + sourceHeight) * source.height).toInt().coerceIn(top + 1, source.height)
            val cropped = Bitmap.createBitmap(source, left, top, right - left, bottom - top)
            val dir = File(context.filesDir, folder).apply { mkdirs() }
            val target = File(dir, "image_${System.currentTimeMillis()}.jpg")
            target.outputStream().use { output ->
                cropped.compress(Bitmap.CompressFormat.JPEG, 92, output)
            }
            if (source !== cropped) source.recycle()
            cropped.recycle()
            Uri.fromFile(target).toString()
        }.getOrNull()
    }

    private fun decodeWithOrientation(context: Context, uri: Uri): Bitmap? {
        val source = when (uri.scheme) {
            "file" -> BitmapFactory.decodeFile(uri.path)
            else -> context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
        } ?: return null
        val rotation = runCatching {
            val exif = if (uri.scheme == "file") {
                ExifInterface(uri.path.orEmpty())
            } else {
                val stream = context.contentResolver.openInputStream(uri) ?: return@runCatching 0f
                stream.use { ExifInterface(it) }
            }
            when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
        }.getOrDefault(0f)
        return rotateBitmap(source, rotation)
    }

    private fun rotateBitmap(source: Bitmap, degrees: Float): Bitmap {
        if (degrees == 0f) return source
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }

    private fun resolveExtension(context: Context, uri: Uri, fallbackExtension: String): String {
        val mime = context.contentResolver.getType(uri)
        val fromMime = mime?.let { MimeTypeMap.getSingleton().getExtensionFromMimeType(it) }
        val fromPath = uri.lastPathSegment?.substringAfterLast('.', missingDelimiterValue = "")?.takeIf { it.length in 2..5 }
        return (fromMime ?: fromPath ?: fallbackExtension).lowercase().trimStart('.')
    }
}

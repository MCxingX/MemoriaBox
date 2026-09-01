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

    private val COMMON_ASPECT_RATIOS = listOf(
        0.5625f,  // 9:16
        0.75f,    // 3:4
        1f,       // 1:1
        1.5f,     // 3:2
        4f / 3f,  // 4:3
        16f / 9f  // 16:9
    )

    fun snapToCommonAspectRatio(ratio: Float): Float {
        if (!ratio.isFinite() || ratio <= 0f) return 1f
        val nearest = COMMON_ASPECT_RATIOS.minByOrNull { kotlin.math.abs(it - ratio) } ?: return ratio.coerceIn(0.5f, 2f)
        return if (kotlin.math.abs(nearest - ratio) / nearest <= 0.06f) ratio else nearest
    }

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
            val left = json.optDouble("cropLeft", 0.0).toFloat().finiteOrDefault(0f).coerceIn(0f, 1f)
            val top = json.optDouble("cropTop", 0.0).toFloat().finiteOrDefault(0f).coerceIn(0f, 1f)
            val width = json.optDouble("cropWidth", 1.0).toFloat().finiteOrDefault(1f).coerceIn(0.05f, 1f)
            val height = json.optDouble("cropHeight", 1.0).toFloat().finiteOrDefault(1f).coerceIn(0.05f, 1f)
            val safeLeft = left.coerceIn(0f, (1f - width).coerceAtLeast(0f))
            val safeTop = top.coerceIn(0f, (1f - height).coerceAtLeast(0f))
            EditState(
                sourceUri = json.optString("sourceUri", fallbackSourceUri).ifBlank { fallbackSourceUri },
                cropLeft = safeLeft,
                cropTop = safeTop,
                cropWidth = width.coerceAtMost(1f - safeLeft),
                cropHeight = height.coerceAtMost(1f - safeTop)
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
            if (source.width <= 0 || source.height <= 0) {
                source.recycle()
                return null
            }
            val leftRatio = sourceLeft.finiteOrDefault(0f).coerceIn(0f, 1f)
            val topRatio = sourceTop.finiteOrDefault(0f).coerceIn(0f, 1f)
            val widthUpper = (1f - leftRatio).coerceAtLeast(0.01f)
            val heightUpper = (1f - topRatio).coerceAtLeast(0.01f)
            val widthRatio = sourceWidth.finiteOrDefault(1f).coerceIn(0.01f, widthUpper)
            val heightRatio = sourceHeight.finiteOrDefault(1f).coerceIn(0.01f, heightUpper)
            val left = (leftRatio * source.width).toInt().coerceIn(0, source.width - 1)
            val top = (topRatio * source.height).toInt().coerceIn(0, source.height - 1)
            val right = ((leftRatio + widthRatio) * source.width).toInt().coerceIn(left + 1, source.width)
            val bottom = ((topRatio + heightRatio) * source.height).toInt().coerceIn(top + 1, source.height)
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
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        when (uri.scheme) {
            "file" -> BitmapFactory.decodeFile(uri.path, bounds)
            else -> context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, bounds)
            }
        }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        val maxDimension = maxOf(bounds.outWidth, bounds.outHeight)
        val sample = generateSequence(1) { it * 2 }
            .takeWhile { it <= 8 }
            .lastOrNull { maxDimension / it >= 1600 }
            ?: 1
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        val source = when (uri.scheme) {
            "file" -> BitmapFactory.decodeFile(uri.path, options)
            else -> context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, options)
            }
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

    private fun Float.finiteOrDefault(default: Float): Float = if (isFinite()) this else default

    fun getImageAspectRatio(context: Context, uriString: String): Float? {
        return runCatching {
            val uri = Uri.parse(uriString)
            val opts = BitmapFactory.Options()
            opts.inJustDecodeBounds = true
            when (uri.scheme) {
                "file" -> BitmapFactory.decodeFile(uri.path, opts)
                else -> context.contentResolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it, null, opts)
                }
            }
            if (opts.outWidth <= 0 || opts.outHeight <= 0) return null

            val rotation = getExifRotation(context, uri)
            if (rotation == 90 || rotation == 270) {
                opts.outHeight.toFloat() / opts.outWidth.toFloat()
            } else {
                opts.outWidth.toFloat() / opts.outHeight.toFloat()
            }
        }.getOrNull()
    }

    private fun getExifRotation(context: Context, uri: Uri): Int {
        return runCatching {
            val exif = if (uri.scheme == "file") {
                ExifInterface(uri.path.orEmpty())
            } else {
                context.contentResolver.openInputStream(uri)?.use { ExifInterface(it) }
                    ?: return 0
            }
            when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90
                ExifInterface.ORIENTATION_ROTATE_270 -> 270
                else -> 0
            }
        }.getOrDefault(0)
    }

    private fun resolveExtension(context: Context, uri: Uri, fallbackExtension: String): String {
        val mime = context.contentResolver.getType(uri)
        val fromMime = mime?.let { MimeTypeMap.getSingleton().getExtensionFromMimeType(it) }
        val fromPath = uri.lastPathSegment?.substringAfterLast('.', missingDelimiterValue = "")?.takeIf { it.length in 2..5 }
        return (fromMime ?: fromPath ?: fallbackExtension).lowercase().trimStart('.')
    }
}

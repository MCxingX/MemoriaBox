package com.memoriabox.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
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

    fun cropImageToPrivateStorage(
        context: Context,
        sourceUri: Uri,
        folder: String = "picked_images",
        cropAspectRatio: Float,
        scale: Float,
        offsetX: Float,
        offsetY: Float,
        rotationDegrees: Float = 0f
    ): String? {
        return runCatching {
            val source = context.contentResolver.openInputStream(sourceUri)?.use { input ->
                BitmapFactory.decodeStream(input)
            } ?: return null
            val rotated = rotateBitmap(source, rotationDegrees)
            val safeScale = scale.coerceAtLeast(1f)
            val sourceAspectRatio = rotated.width.toFloat() / rotated.height.toFloat()
            val baseCropWidth: Float
            val baseCropHeight: Float
            if (sourceAspectRatio > cropAspectRatio) {
                baseCropHeight = rotated.height.toFloat()
                baseCropWidth = baseCropHeight * cropAspectRatio
            } else {
                baseCropWidth = rotated.width.toFloat()
                baseCropHeight = baseCropWidth / cropAspectRatio
            }
            val cropWidth = (baseCropWidth / safeScale).coerceAtLeast(1f)
            val cropHeight = (baseCropHeight / safeScale).coerceAtLeast(1f)
            val maxLeft = (rotated.width - cropWidth).coerceAtLeast(0f)
            val maxTop = (rotated.height - cropHeight).coerceAtLeast(0f)
            val normalizedX = ((offsetX + 1f) / 2f).coerceIn(0f, 1f)
            val normalizedY = ((offsetY + 1f) / 2f).coerceIn(0f, 1f)
            val left = (maxLeft * normalizedX).toInt().coerceIn(0, (rotated.width - 1).coerceAtLeast(0))
            val top = (maxTop * normalizedY).toInt().coerceIn(0, (rotated.height - 1).coerceAtLeast(0))
            val width = cropWidth.toInt().coerceIn(1, rotated.width - left)
            val height = cropHeight.toInt().coerceIn(1, rotated.height - top)
            val cropped = Bitmap.createBitmap(rotated, left, top, width, height)
            val dir = File(context.filesDir, folder).apply { mkdirs() }
            val target = File(dir, "image_${System.currentTimeMillis()}.jpg")
            target.outputStream().use { output ->
                cropped.compress(Bitmap.CompressFormat.JPEG, 92, output)
            }
            if (rotated !== source) source.recycle()
            cropped.recycle()
            Uri.fromFile(target).toString()
        }.getOrNull()
    }

    private fun rotateBitmap(source: Bitmap, degrees: Float): Bitmap {
        if (degrees == 0f) return source
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }
}

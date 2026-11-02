package com.memoriabox.utils

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

object VideoThumbnailUtils {
    fun thumbnailUri(context: Context, videoUri: String): String? = runCatching {
        val uri = Uri.parse(videoUri)
        val videoFile = if (uri.scheme == "file") File(uri.path.orEmpty()) else null
        val output = videoFile?.let { File(it.parentFile, "${it.nameWithoutExtension}_cover.jpg") }
            ?: File(context.filesDir, "video_covers/${videoUri.hashCode()}.jpg").apply { parentFile?.mkdirs() }
        if (output.isFile && output.length() > 0) return output.toURI().toString()

        val retriever = MediaMetadataRetriever()
        try {
            if (uri.scheme == "file") retriever.setDataSource(videoFile?.absolutePath)
            else retriever.setDataSource(context, uri)
            val bitmap = retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                ?: retriever.getFrameAtTime(1_000_000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                ?: return null
            try {
                FileOutputStream(output).use { stream ->
                    check(bitmap.compress(Bitmap.CompressFormat.JPEG, 86, stream))
                }
            } finally {
                bitmap.recycle()
            }
            output.toURI().toString()
        } finally {
            retriever.release()
        }
    }.getOrNull()
}

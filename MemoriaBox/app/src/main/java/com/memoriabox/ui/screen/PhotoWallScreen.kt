package com.memoriabox.ui.screen

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.Typeface
import androidx.core.graphics.createBitmap
import com.memoriabox.data.model.Event
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun generateEventCardBitmap(
    context: android.content.Context,
    event: Event,
    daysLeft: Long,
    template: String = "CLASSIC"
): Bitmap {
    val width = 1080
    val height = 1920
    val bitmap = createBitmap(width, height)
    val canvas = Canvas(bitmap)

    val isMinimal = template == "MINIMAL"
    val isPoster = template == "POSTER"

    val bgPaint = Paint().apply {
        isAntiAlias = true
        if (isMinimal) {
            color = android.graphics.Color.WHITE
        } else {
            shader = LinearGradient(
                0f,
                0f,
                width.toFloat(),
                height.toFloat(),
                intArrayOf(
                    android.graphics.Color.parseColor(if (isPoster) "#FF7A00" else "#1677FF"),
                    android.graphics.Color.parseColor(if (isPoster) "#FFB020" else "#13C2C2")
                ),
                null,
                Shader.TileMode.CLAMP
            )
        }
    }
    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

    val mainColor = if (isMinimal) android.graphics.Color.parseColor("#111827") else android.graphics.Color.WHITE
    val subColor = if (isMinimal) android.graphics.Color.parseColor("#52677D") else android.graphics.Color.argb(220, 255, 255, 255)

    if (isPoster) {
        val decorPaint = Paint().apply {
            color = android.graphics.Color.argb(42, 255, 255, 255)
            isAntiAlias = true
        }
        canvas.drawCircle(920f, 260f, 260f, decorPaint)
        canvas.drawCircle(110f, 1660f, 220f, decorPaint)
    }

    if (isMinimal) {
        val linePaint = Paint().apply {
            color = android.graphics.Color.parseColor("#1677FF")
            strokeWidth = 12f
            isAntiAlias = true
        }
        canvas.drawLine(120f, 180f, 980f, 180f, linePaint)
    }

    val titlePaint = Paint().apply {
        color = mainColor
        textSize = if (isPoster) 88f else 76f
        isAntiAlias = true
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }
    canvas.drawText(event.name.take(18), width / 2f, if (isPoster) 360f else 420f, titlePaint)

    val daysPaint = Paint().apply {
        color = mainColor
        textSize = if (isPoster) 260f else 220f
        isAntiAlias = true
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }
    canvas.drawText(if (daysLeft == 0L) "今天" else "$daysLeft", width / 2f, if (isPoster) 900f else 880f, daysPaint)

    val labelPaint = Paint().apply {
        color = subColor
        textSize = 60f
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
    }
    canvas.drawText(if (daysLeft == 0L) "就是今天" else "天", width / 2f + if (isPoster) 180f else 130f, if (isPoster) 900f else 880f, labelPaint)

    val datePaint = Paint().apply {
        color = subColor
        textSize = 50f
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
    }
    val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(event.date))
    canvas.drawText(dateStr, width / 2f, 1180f, datePaint)
    if (event.note.isNotBlank()) {
        canvas.drawText(event.note.take(24), width / 2f, 1280f, datePaint)
    }

    val sigPaint = Paint().apply {
        color = if (isMinimal) android.graphics.Color.parseColor("#52677D") else android.graphics.Color.argb(180, 255, 255, 255)
        textSize = 40f
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
    }
    canvas.drawText("念记", width / 2f, height - 100f, sigPaint)

    return bitmap
}

fun shareBitmap(context: android.content.Context, bitmap: Bitmap, caption: String = "") {
    try {
        val file = File(context.cacheDir, "share_image_${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { fos ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
        }

        val uri = androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            if (caption.isNotBlank()) putExtra(Intent.EXTRA_TEXT, caption)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(shareIntent, "分享到"))
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

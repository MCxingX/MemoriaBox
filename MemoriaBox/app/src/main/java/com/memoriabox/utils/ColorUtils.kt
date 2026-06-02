package com.memoriabox.utils

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

object ColorUtils {
    fun hexToColor(hex: String): Color {
        return try {
            val hexClean = hex.replace("#", "")
            when (hexClean.length) {
                3 -> {
                    val r = hexClean[0].toString().repeat(2)
                    val g = hexClean[1].toString().repeat(2)
                    val b = hexClean[2].toString().repeat(2)
                    parseHexColor("$r$g$b")
                }
                6 -> parseHexColor(hexClean)
                8 -> parseHexColorAlpha(hexClean)
                else -> Color(0xFF7C4DFF)
            }
        } catch (e: Exception) {
            Color(0xFF7C4DFF)
        }
    }

    private fun parseHexColor(hex: String): Color {
        val r = hex.substring(0, 2).toInt(16) / 255f
        val g = hex.substring(2, 4).toInt(16) / 255f
        val b = hex.substring(4, 6).toInt(16) / 255f
        return Color(r, g, b)
    }

    private fun parseHexColorAlpha(hex: String): Color {
        val a = hex.substring(0, 2).toInt(16) / 255f
        val r = hex.substring(2, 4).toInt(16) / 255f
        val g = hex.substring(4, 6).toInt(16) / 255f
        val b = hex.substring(6, 8).toInt(16) / 255f
        return Color(r, g, b, a)
    }

    fun colorToHex(color: Color): String {
        return "#${color.toArgb().toString(16).padStart(8, '0').uppercase()}"
    }
}

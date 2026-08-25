package com.gamechest.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Universal color hex parser compatible across Android and Desktop platforms.
 */
fun parseHexColor(hex: String): Color {
    val cleanHex = hex.removePrefix("#")
    val colorLong = when (cleanHex.length) {
        6 -> "FF$cleanHex".toLong(16)
        8 -> cleanHex.toLong(16)
        else -> 0xFFFFFFFFL
    }
    return Color(colorLong)
}

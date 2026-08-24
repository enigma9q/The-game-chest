package com.gamechest.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color

val DarkBackground = Color(0xFF0F172A)
val SurfaceDark = Color(0xFF1E293B)
val SurfaceDarkCard = Color(0xFF334155)

val PrimaryNeon = Color(0xFF38BDF8)
val SecondaryOrange = Color(0xFFF97316)
val AccentYellow = Color(0xFFFBBF24)
val NitroGreen = Color(0xFF10B981)
val HazardRed = Color(0xFFEF4444)
val TurboCyan = Color(0xFF06B6D4)
val CheckpointPurple = Color(0xFF8B5CF6)

val TextPrimary = Color(0xFFF8FAFC)
val TextSecondary = Color(0xFF94A3B8)
val TextMuted = Color(0xFF64748B)

val GameChestDarkColorScheme = darkColorScheme(
    primary = PrimaryNeon,
    onPrimary = Color(0xFF00354E),
    primaryContainer = Color(0xFF004D71),
    onPrimaryContainer = Color(0xFFC2E8FF),
    secondary = SecondaryOrange,
    onSecondary = Color(0xFF4E1600),
    secondaryContainer = Color(0xFF722800),
    onSecondaryContainer = Color(0xFFFFDBCF),
    background = DarkBackground,
    onBackground = TextPrimary,
    surface = SurfaceDark,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceDarkCard,
    onSurfaceVariant = TextSecondary,
    error = HazardRed,
    onError = Color.White
)

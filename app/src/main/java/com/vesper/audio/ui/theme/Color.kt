package com.vesper.audio.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

@Immutable
data class VesperColors(
    val accent: Color,
    val onAccent: Color,
    val accentContainer: Color,
    val onAccentContainer: Color,
    val accentMuted: Color,
    val background: Color,
    val backgroundSecondary: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val surfaceHigher: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val outline: Color,
    val borderAccent: Color,
    val isDark: Boolean
)

// Fixed VESPER Theme (S1a: Near-black AMOLED & Wine Accent)
val FixedVesperColors = VesperColors(
    accent = Color(0xFF8C2F48),
    onAccent = Color(0xFFF2E9EA),
    accentContainer = Color(0xFF4A1824),
    onAccentContainer = Color(0xFFF2E9EA),
    accentMuted = Color(0x338C2F48),
    background = Color(0xFF000000),
    backgroundSecondary = Color(0xFF0B0B0D),
    surface = Color(0xFF0B0B0D),
    surfaceElevated = Color(0xFF141418),
    surfaceHigher = Color(0xFF1E1E24),
    textPrimary = Color(0xFFF2E9EA),
    textSecondary = Color(0xFFD6CBCD),
    textMuted = Color(0xFF9A8F92),
    outline = Color(0xFF242024),
    borderAccent = Color(0x558C2F48),
    isDark = true
)

val LocalVesperColors = compositionLocalOf { FixedVesperColors }
val LocalVesperAccentColor = compositionLocalOf { Color(0xFF8C2F48) }

// Top-level constants (fixed AMOLED black + wine accent)
val VesperBackground = Color(0xFF000000)
val VesperBackgroundSecondary = Color(0xFF0B0B0D)
val VesperSurface = Color(0xFF0B0B0D)
val VesperSurfaceElevated = Color(0xFF141418)
val VesperSurfaceHigher = Color(0xFF1E1E24)

val VesperBurgundyAccent = Color(0xFF8C2F48)
val VesperDeepBurgundy = Color(0xFF4A1824)
val VesperBurgundyHighlight = Color(0xFFB8506A)
val VesperBurgundySubtle = Color(0x338C2F48)

val VesperTextPrimary = Color(0xFFF2E9EA)
val VesperTextSecondary = Color(0xFFD6CBCD)
val VesperTextMuted = Color(0xFF9A8F92)
val VesperWarmGold = Color(0xFFB8506A)

val VesperOutline = Color(0xFF242024)
val VesperBorderAccent = Color(0x558C2F48)

val VesperBurgundyGradient = Brush.linearGradient(
    colors = listOf(VesperBurgundyAccent, VesperDeepBurgundy)
)

val VesperCosmicCardGradient = Brush.verticalGradient(
    colors = listOf(VesperSurfaceElevated, VesperBackgroundSecondary)
)

val VesperHaloGlowGradient = Brush.radialGradient(
    colors = listOf(VesperBurgundyAccent.copy(alpha = 0.35f), Color.Transparent)
)

val VesperWarmGradient = Brush.linearGradient(
    colors = listOf(VesperTextPrimary, VesperBurgundyHighlight)
)

// Legacy Aliases
val SonoraBackground = VesperBackground
val SonoraSurface = VesperSurface
val SonoraSurfaceElevated = VesperSurfaceElevated
val SonoraSurfaceHigher = VesperSurfaceHigher
val SonoraGoldPrimary = VesperBurgundyAccent
val SonoraGoldBright = VesperTextPrimary
val SonoraGoldMuted = VesperDeepBurgundy
val SonoraGoldDeep = VesperDeepBurgundy
val SonoraGoldSubtle = VesperBurgundySubtle
val SonoraTextPrimary = VesperTextPrimary
val SonoraTextSecondary = VesperTextSecondary
val SonoraTextTertiary = VesperTextMuted
val SonoraDivider = VesperOutline
val SonoraBorderGold = VesperBorderAccent
val SonoraGoldGradient = VesperBurgundyGradient
val SonoraDarkCardGradient = VesperCosmicCardGradient
val SonoraHaloGlowGradient = VesperHaloGlowGradient

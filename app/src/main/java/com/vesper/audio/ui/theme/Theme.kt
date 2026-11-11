package com.vesper.audio.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

val MaterialTheme.vesperAccent: Color
    @Composable
    get() = LocalVesperAccentColor.current

val MaterialTheme.vesperColors: VesperColors
    @Composable
    get() = LocalVesperColors.current

@Composable
fun VesperTheme(
    accentColorHex: String = "#8C2F48",
    themeMode: String = "Dark",
    content: @Composable () -> Unit
) {
    val vesperColors = FixedVesperColors
    val accent = vesperColors.accent

    val colorScheme: ColorScheme = darkColorScheme(
        primary = accent,
        onPrimary = vesperColors.onAccent,
        primaryContainer = vesperColors.accentContainer,
        onPrimaryContainer = vesperColors.onAccentContainer,
        secondary = vesperColors.borderAccent,
        onSecondary = vesperColors.textPrimary,
        secondaryContainer = vesperColors.surfaceElevated,
        onSecondaryContainer = vesperColors.textSecondary,
        tertiary = VesperBurgundyHighlight,
        onTertiary = vesperColors.textPrimary,
        background = vesperColors.background,
        onBackground = vesperColors.textPrimary,
        surface = vesperColors.surface,
        onSurface = vesperColors.textPrimary,
        surfaceVariant = vesperColors.surfaceElevated,
        onSurfaceVariant = vesperColors.textSecondary,
        outline = vesperColors.outline,
        outlineVariant = vesperColors.borderAccent
    )

    CompositionLocalProvider(
        LocalVesperColors provides vesperColors,
        LocalVesperAccentColor provides accent
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = VesperTypography,
            content = content
        )
    }
}

@Composable
fun SonoraTheme(
    accentColorHex: String = "#8C2F48",
    themeMode: String = "Dark",
    content: @Composable () -> Unit
) {
    VesperTheme(
        accentColorHex = accentColorHex,
        themeMode = themeMode,
        content = content
    )
}

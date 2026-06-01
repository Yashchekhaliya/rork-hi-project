package com.rork.workpulse.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val WorkPulseColorScheme = darkColorScheme(
    primary = WP.Cyan,
    onPrimary = WP.BgDeep,
    secondary = WP.Purple,
    onSecondary = WP.TextPrimary,
    tertiary = WP.Magenta,
    background = WP.BgDeep,
    onBackground = WP.TextPrimary,
    surface = WP.BgMid,
    onSurface = WP.TextPrimary,
    surfaceVariant = WP.BgRaise,
    onSurfaceVariant = WP.TextSecondary,
    error = WP.Danger,
    outline = WP.GlassBorder,
)

private val WorkPulseTypography = Typography(
    displayLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 44.sp, letterSpacing = (-1).sp),
    headlineLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 30.sp, letterSpacing = (-0.5).sp),
    headlineMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 24.sp, letterSpacing = (-0.5).sp),
    titleLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 20.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
    bodyLarge = TextStyle(fontWeight = FontWeight.Normal, fontSize = 16.sp),
    bodyMedium = TextStyle(fontWeight = FontWeight.Normal, fontSize = 14.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 14.sp, letterSpacing = 0.5.sp),
    labelMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 12.sp, letterSpacing = 0.5.sp),
    labelSmall = TextStyle(fontWeight = FontWeight.Medium, fontSize = 11.sp, letterSpacing = 1.sp),
)

@Composable
fun AppTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = WorkPulseColorScheme,
        typography = WorkPulseTypography,
        content = content
    )
}

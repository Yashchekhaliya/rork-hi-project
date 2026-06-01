package com.rork.workpulse.ui.theme

import androidx.compose.ui.graphics.Color

/** Clean white / blue / red palette for WorkPulse. */
object WP {
    // Backgrounds — kept dark so glassmorphism still pops
    val BgDeep = Color(0xFF05070E)
    val BgMid = Color(0xFF0C1224)
    val BgRaise = Color(0xFF131B33)

    // Primary: crisp corporate blue
    val Cyan = Color(0xFF3B82F6)
    val ElectricBlue = Color(0xFF2563EB)
    val Purple = Color(0xFF60A5FA)

    // Red for danger / warnings
    val Magenta = Color(0xFFEF4444)
    val Danger = Color(0xFFEF4444)

    // Positives & neutrals in white / silver
    val Lime = Color(0xFFF1F5F9)
    val Amber = Color(0xFFCBD5E1)

    // Text — white-dominant
    val TextPrimary = Color(0xFFF8FAFC)
    val TextSecondary = Color(0xFF94A3B8)
    val TextDim = Color(0xFF475569)

    // Glass surfaces — bright white highlights
    val GlassTop = Color(0x22FFFFFF)
    val GlassBottom = Color(0x0AFFFFFF)
    val GlassBorder = Color(0x38FFFFFF)
    val GlassBorderSoft = Color(0x20FFFFFF)
    val Stroke = Color(0x14FFFFFF)
}

package com.rork.workpulse.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Premium grading color system — Blue / White / Green / Red.
 *
 * Deep navy base with royal blue primary, crisp white text,
 * emerald green for success, and crimson red for urgency.
 */
object WP {
    // ── Backgrounds ──────────────────────────────────────────
    val BgDeep   = Color(0xFF060810)   // Deepest navy-black
    val BgMid    = Color(0xFF0C1228)   // Midnight panel base
    val BgRaise  = Color(0xFF131B3A)   // Elevated surface

    // ── Primary: Royal Blue family ───────────────────────────
    val Blue         = Color(0xFF3B82F6)   // Standard primary blue
    val RoyalBlue    = Color(0xFF2563EB)   // Deeper corporate blue
    val ElectricBlue = Color(0xFF1D4ED8)   // Intense electric
    val SkyBlue      = Color(0xFF60A5FA)   // Light accent

    // ── Success: Emerald Green family ────────────────────────
    val Green       = Color(0xFF10B981)   // Bright emerald
    val ForestGreen = Color(0xFF059669)   // Deep forest
    val MintGreen   = Color(0xFF34D399)   // Soft mint accent

    // ── Danger / Alert: Crimson Red family ───────────────────
    val Red       = Color(0xFFEF4444)   // Bright crimson
    val DeepRed   = Color(0xFFDC2626)   // Deep danger red
    val RoseRed   = Color(0xFFF87171)   // Soft rose warning

    // ── Text — crisp white hierarchy ─────────────────────────
    val TextPrimary   = Color(0xFFF8FAFC)   // Pure white
    val TextSecondary = Color(0xFF94A3B8)   // Soft silver
    val TextDim       = Color(0xFF475569)   // Muted steel

    // ── Surfaces & strokes — frosted glass ───────────────────
    val GlassTop         = Color(0x28FFFFFF)   // Bright highlight edge
    val GlassMid         = Color(0x14FFFFFF)   // Mid-layer frost
    val GlassBottom      = Color(0x08FFFFFF)   // Deep frost
    val GlassBorder      = Color(0x38FFFFFF)   // Bright border
    val GlassBorderSoft  = Color(0x1AFFFFFF)   // Subtle border

    // ── Accent glows ─────────────────────────────────────────
    val BlueGlow   = Color(0x403B82F6)   // Blue ambient glow
    val GreenGlow  = Color(0x4010B981)   // Green ambient glow
    val RedGlow    = Color(0x40EF4444)   // Red ambient glow

    // ── Backward-compatible aliases (deprecated → new names) ──
    @Deprecated("Use Blue", ReplaceWith("Blue"))
    val Cyan: Color get() = Blue
    @Deprecated("Use SkyBlue", ReplaceWith("SkyBlue"))
    val Purple: Color get() = SkyBlue
    @Deprecated("Use Red", ReplaceWith("Red"))
    val Magenta: Color get() = Red
    @Deprecated("Use Red", ReplaceWith("Red"))
    val Danger: Color get() = Red
    @Deprecated("Use Green", ReplaceWith("Green"))
    val Lime: Color get() = Green
    @Deprecated("Use RoseRed", ReplaceWith("RoseRed"))
    val Amber: Color get() = RoseRed
    @Deprecated("Use GlassBorderSoft", ReplaceWith("GlassBorderSoft"))
    val Stroke: Color get() = GlassBorderSoft
}

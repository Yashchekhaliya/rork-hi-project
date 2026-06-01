package com.rork.workpulse.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Premium frosted glass surface — multi-layer gradient fill with
 * a bright top-border highlight and deep base frost.
 *
 * Colors cascade from crisp white → blue-tinted frost → deep navy base.
 */
fun Modifier.glass(
    cornerRadius: Dp = 24.dp,
    fillTop: Color = WP.GlassTop,
    fillBottom: Color = WP.GlassBottom,
    borderColor: Color = WP.GlassBorder,
): Modifier = this
    .clip(RoundedCornerShape(cornerRadius))
    .background(
        Brush.verticalGradient(
            listOf(
                fillTop,                                    // Crisp highlight
                Color.White.copy(alpha = 0.04f),           // Mid frost
                fillBottom,                                 // Deep glass base
                WP.BgDeep.copy(alpha = 0.30f),             // Subtle navy anchor
            ),
        )
    )
    .border(
        width = 1.dp,
        brush = Brush.verticalGradient(
            listOf(borderColor, borderColor.copy(alpha = borderColor.alpha * 0.18f))
        ),
        shape = RoundedCornerShape(cornerRadius),
    )

/**
 * Neon-tinted glass surface — used for hero/highlighted panels.
 * Adds a blue glow aura for the new premium palette.
 */
fun Modifier.neonGlass(
    accent: Color,
    cornerRadius: Dp = 24.dp,
): Modifier = this
    .clip(RoundedCornerShape(cornerRadius))
    .background(
        Brush.verticalGradient(
            listOf(
                accent.copy(alpha = 0.14f),                 // Colored highlight
                accent.copy(alpha = 0.06f),                 // Mid tint
                accent.copy(alpha = 0.02f),                 // Fade to base
            )
        )
    )
    .border(
        width = 1.dp,
        brush = Brush.verticalGradient(
            listOf(accent.copy(alpha = 0.50f), accent.copy(alpha = 0.08f))
        ),
        shape = RoundedCornerShape(cornerRadius),
    )

/**
 * Premium glass panel with a green success tint.
 */
fun Modifier.successGlass(cornerRadius: Dp = 24.dp): Modifier = this
    .clip(RoundedCornerShape(cornerRadius))
    .background(
        Brush.verticalGradient(
            listOf(
                WP.Green.copy(alpha = 0.12f),
                WP.Green.copy(alpha = 0.04f),
                WP.Green.copy(alpha = 0.01f),
            )
        )
    )
    .border(
        width = 1.dp,
        brush = Brush.verticalGradient(
            listOf(WP.Green.copy(alpha = 0.45f), WP.Green.copy(alpha = 0.06f))
        ),
        shape = RoundedCornerShape(cornerRadius),
    )

/**
 * Premium glass panel with a red danger tint.
 */
fun Modifier.dangerGlass(cornerRadius: Dp = 24.dp): Modifier = this
    .clip(RoundedCornerShape(cornerRadius))
    .background(
        Brush.verticalGradient(
            listOf(
                WP.Red.copy(alpha = 0.12f),
                WP.Red.copy(alpha = 0.04f),
                WP.Red.copy(alpha = 0.01f),
            )
        )
    )
    .border(
        width = 1.dp,
        brush = Brush.verticalGradient(
            listOf(WP.Red.copy(alpha = 0.45f), WP.Red.copy(alpha = 0.06f))
        ),
        shape = RoundedCornerShape(cornerRadius),
    )

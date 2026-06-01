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
 * Frosted glass surface that works on every API level (no RenderEffect dependency).
 * Layers a soft vertical highlight gradient over a translucent fill with a bright top border.
 */
fun Modifier.glass(
    cornerRadius: Dp = 24.dp,
    fillTop: Color = WP.GlassTop,
    fillBottom: Color = WP.GlassBottom,
    borderColor: Color = WP.GlassBorder,
): Modifier = this
    .clip(RoundedCornerShape(cornerRadius))
    .background(Brush.verticalGradient(listOf(fillTop, fillBottom)))
    .border(
        width = 1.dp,
        brush = Brush.verticalGradient(
            listOf(borderColor, borderColor.copy(alpha = borderColor.alpha * 0.25f))
        ),
        shape = RoundedCornerShape(cornerRadius),
    )

/** A glass surface tinted with a neon accent — used for hero / highlighted panels. */
fun Modifier.neonGlass(
    accent: Color,
    cornerRadius: Dp = 24.dp,
): Modifier = this
    .clip(RoundedCornerShape(cornerRadius))
    .background(
        Brush.verticalGradient(
            listOf(accent.copy(alpha = 0.20f), accent.copy(alpha = 0.04f))
        )
    )
    .border(
        width = 1.dp,
        brush = Brush.verticalGradient(
            listOf(accent.copy(alpha = 0.55f), accent.copy(alpha = 0.10f))
        ),
        shape = RoundedCornerShape(cornerRadius),
    )

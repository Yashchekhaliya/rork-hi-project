package com.rork.workpulse.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.rork.workpulse.ui.theme.WP
import kotlin.math.cos
import kotlin.math.sin

/**
 * Living, cinematic mesh-gradient backdrop. Several soft neon orbs drift slowly
 * over the deep space base, giving the frosted-glass panels something luminous
 * to sit on top of.
 */
@Composable
fun MeshBackground(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "mesh")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 18000, easing = { it }),
            repeatMode = RepeatMode.Restart,
        ),
        label = "phase",
    )

    androidx.compose.foundation.layout.Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(WP.BgDeep, WP.BgMid, WP.BgDeep))
            )
            .drawBehind {
                val w = size.width
                val h = size.height

                fun orb(color: Color, cx: Float, cy: Float, radius: Float) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(color, Color.Transparent),
                            center = Offset(cx, cy),
                            radius = radius,
                        ),
                        radius = radius,
                        center = Offset(cx, cy),
                    )
                }

                orb(
                    WP.Purple.copy(alpha = 0.30f),
                    cx = w * (0.25f + 0.12f * cos(t)),
                    cy = h * (0.18f + 0.06f * sin(t)),
                    radius = w * 0.85f,
                )
                orb(
                    WP.Cyan.copy(alpha = 0.22f),
                    cx = w * (0.85f + 0.10f * sin(t * 0.8f)),
                    cy = h * (0.30f + 0.08f * cos(t * 0.9f)),
                    radius = w * 0.75f,
                )
                orb(
                    WP.ElectricBlue.copy(alpha = 0.20f),
                    cx = w * (0.55f + 0.14f * cos(t * 0.6f)),
                    cy = h * (0.92f + 0.05f * sin(t * 1.1f)),
                    radius = w * 0.95f,
                )
                orb(
                    WP.Magenta.copy(alpha = 0.12f),
                    cx = w * (0.12f + 0.10f * sin(t * 1.3f)),
                    cy = h * (0.78f + 0.06f * cos(t)),
                    radius = w * 0.6f,
                )
                // Subtle vignette to deepen the edges.
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.Transparent, WP.BgDeep.copy(alpha = 0.55f)),
                        center = Offset(w / 2, h / 2),
                        radius = maxOf(w, h) * 0.75f,
                    ),
                    size = Size(w, h),
                )
            }
    )
}

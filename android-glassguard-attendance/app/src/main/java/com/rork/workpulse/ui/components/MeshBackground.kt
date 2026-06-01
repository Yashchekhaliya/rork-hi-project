package com.rork.workpulse.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.rork.workpulse.ui.theme.WP
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Premium live layered background.
 *
 * Six animated colour orbs (royal blue, emerald green, crimson red, crisp white,
 * electric blue, mint green) drift across the deep navy canvas at different speeds
 * and orbit radii, creating a living, breathing atmosphere.
 *
 * Subtle floating hex rings and a deep vignette complete the cinematic look.
 */
@Composable
fun MeshBackground(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "premium_mesh")

    // ── Orb phases — each orb moves on its own independent path ──
    val phase1 by transition.animateFloat(
        initialValue = 0f, targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(22000, easing = LinearEasing), RepeatMode.Restart),
        label = "orb1",
    )
    val phase2 by transition.animateFloat(
        initialValue = 0f, targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(18000, easing = LinearEasing), RepeatMode.Restart),
        label = "orb2",
    )
    val phase3 by transition.animateFloat(
        initialValue = 0f, targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(26000, easing = LinearEasing), RepeatMode.Restart),
        label = "orb3",
    )
    val phase4 by transition.animateFloat(
        initialValue = 0f, targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(15000, easing = LinearEasing), RepeatMode.Restart),
        label = "orb4",
    )
    val phase5 by transition.animateFloat(
        initialValue = 0f, targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(30000, easing = LinearEasing), RepeatMode.Restart),
        label = "orb5",
    )
    val phase6 by transition.animateFloat(
        initialValue = 0f, targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(20000, easing = LinearEasing), RepeatMode.Restart),
        label = "orb6",
    )

    // Hex ring rotation
    val hexRot by transition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(40000, easing = LinearEasing), RepeatMode.Restart),
        label = "hex",
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(WP.BgDeep, Color(0xFF080E20), WP.BgMid, WP.BgDeep)
                )
            )
            .drawBehind {
                val w = size.width
                val h = size.height
                val cx = w / 2f
                val cy = h / 2f

                // ── Layer 1: Royal Blue — large, slow, top-left ──
                drawOrb(
                    color = WP.RoyalBlue.copy(alpha = 0.28f),
                    centerX = w * (0.22f + 0.14f * cos(phase1)),
                    centerY = h * (0.15f + 0.08f * sin(phase1 * 1.3f)),
                    radius = w * 0.90f,
                )

                // ── Layer 2: Emerald Green — mid, bottom-right ──
                drawOrb(
                    color = WP.ForestGreen.copy(alpha = 0.20f),
                    centerX = w * (0.82f + 0.10f * sin(phase2)),
                    centerY = h * (0.78f + 0.07f * cos(phase2 * 0.9f)),
                    radius = w * 0.75f,
                )

                // ── Layer 3: Crimson Red — subtle, bottom-left ──
                drawOrb(
                    color = WP.DeepRed.copy(alpha = 0.10f),
                    centerX = w * (0.10f + 0.08f * cos(phase3 * 1.1f)),
                    centerY = h * (0.85f + 0.06f * sin(phase3)),
                    radius = w * 0.55f,
                )

                // ── Layer 4: Crisp White — bright highlight, top-right ──
                drawOrb(
                    color = Color.White.copy(alpha = 0.07f),
                    centerX = w * (0.75f + 0.12f * sin(phase4 * 0.8f)),
                    centerY = h * (0.20f + 0.10f * cos(phase4)),
                    radius = w * 0.70f,
                )

                // ── Layer 5: Electric Blue — center-right pulse ──
                drawOrb(
                    color = WP.ElectricBlue.copy(alpha = 0.18f),
                    centerX = w * (0.60f + 0.10f * cos(phase5 * 0.7f)),
                    centerY = h * (0.48f + 0.10f * sin(phase5 * 1.2f)),
                    radius = w * 0.85f,
                )

                // ── Layer 6: Mint Green — subtle, top-center ──
                drawOrb(
                    color = WP.MintGreen.copy(alpha = 0.09f),
                    centerX = w * (0.48f + 0.06f * sin(phase6 * 1.4f)),
                    centerY = h * (0.08f + 0.04f * cos(phase6 * 0.6f)),
                    radius = w * 0.50f,
                )

                // ── Floating hex ring — subtle geometry ──
                drawHexRing(
                    center = Offset(cx, cy),
                    radius = w * 0.38f,
                    rotation = hexRot,
                    color = WP.RoyalBlue.copy(alpha = 0.06f),
                )
                drawHexRing(
                    center = Offset(w * 0.18f, h * 0.22f),
                    radius = w * 0.14f,
                    rotation = -hexRot * 0.7f,
                    color = WP.Green.copy(alpha = 0.05f),
                )

                // ── Deep vignette — anchors the edges ──
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.Transparent, WP.BgDeep.copy(alpha = 0.65f)),
                        center = Offset(cx, cy),
                        radius = maxOf(w, h) * 0.70f,
                    ),
                    size = Size(w, h),
                )
            }
    )
}

/** Draw a soft radial-gradient orb at the given position. */
private fun DrawScope.drawOrb(
    color: Color,
    centerX: Float,
    centerY: Float,
    radius: Float,
) {
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(color, Color.Transparent),
            center = Offset(centerX, centerY),
            radius = radius,
        ),
        radius = radius,
        center = Offset(centerX, centerY),
    )
}

/** Draw a subtle floating hex ring. */
private fun DrawScope.drawHexRing(
    center: Offset,
    radius: Float,
    rotation: Float,
    color: Color,
) {
    val path = Path()
    val sides = 6
    for (i in 0 until sides) {
        val angle = Math.toRadians(rotation.toDouble() + i * 60.0)
        val x = center.x + radius * cos(angle).toFloat()
        val y = center.y + radius * sin(angle).toFloat()
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    drawPath(path, color, style = Stroke(width = 1.5f))
}

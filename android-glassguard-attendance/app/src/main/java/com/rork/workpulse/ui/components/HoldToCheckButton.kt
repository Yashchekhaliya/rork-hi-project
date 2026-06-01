package com.rork.workpulse.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.Column
import com.rork.workpulse.ui.theme.WP
import kotlinx.coroutines.delay

/**
 * A biometric-style "press & hold to confirm" ring. The user holds the orb; a
 * progress arc fills over ~1.1s with a rotating scan sweep, then [onConfirmed]
 * fires with haptic confirmation. Releasing early cancels.
 */
@Composable
fun HoldToCheckButton(
    label: String,
    sublabel: String,
    accent: Color,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    onConfirmed: () -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    var holding by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    var confirmed by remember { mutableStateOf(false) }

    val animatedProgress by animateFloatAsState(
        targetValue = if (holding) 1f else 0f,
        animationSpec = tween(durationMillis = if (holding) 1100 else 260, easing = LinearEasing),
        label = "hold",
    )

    LaunchedEffect(holding) {
        if (holding) {
            confirmed = false
            // Light tick at start.
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            delay(1100)
            if (holding && !confirmed) {
                confirmed = true
                holding = false
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onConfirmed()
            }
        }
    }

    progress = animatedProgress

    val infinite = rememberInfiniteTransition(label = "scan")
    val sweep by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(2600, easing = LinearEasing), RepeatMode.Restart),
        label = "sweep",
    )
    val pulse by infinite.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(tween(1800, easing = LinearEasing), RepeatMode.Reverse),
        label = "pulse",
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = modifier
                .size(208.dp)
                .pointerInput(enabled) {
                    if (!enabled) return@pointerInput
                    detectTapGestures(
                        onPress = {
                            holding = true
                            val released = tryAwaitRelease()
                            holding = false
                            if (!released) holding = false
                        },
                    )
                }
                .drawBehind {
                    val stroke = 14.dp.toPx()
                    val r = (size.minDimension - stroke) / 2f
                    val center = Offset(size.width / 2, size.height / 2)
                    val ringScale = if (enabled) pulse else 1f

                    // Outer glow halo
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(accent.copy(alpha = 0.35f), Color.Transparent),
                            center = center,
                            radius = r * 1.5f * ringScale,
                        ),
                        radius = r * 1.5f * ringScale,
                        center = center,
                    )
                    // Track
                    drawCircle(
                        color = WP.GlassBorder,
                        radius = r,
                        center = center,
                        style = Stroke(width = stroke * 0.5f),
                    )
                    // Inner glass disc
                    drawCircle(
                        brush = Brush.verticalGradient(
                            listOf(accent.copy(alpha = 0.20f), accent.copy(alpha = 0.04f)),
                        ),
                        radius = r - stroke * 0.4f,
                        center = center,
                    )
                    // Progress arc
                    if (progress > 0f) {
                        drawArc(
                            brush = Brush.sweepGradient(
                                listOf(accent, WP.Blue, accent),
                                center = center,
                            ),
                            startAngle = -90f,
                            sweepAngle = 360f * progress,
                            useCenter = false,
                            topLeft = Offset(center.x - r, center.y - r),
                            size = androidx.compose.ui.geometry.Size(r * 2, r * 2),
                            style = Stroke(width = stroke),
                        )
                    }
                    // Rotating scan sweep when enabled
                    if (enabled) {
                        rotate(sweep, center) {
                            drawArc(
                                brush = Brush.sweepGradient(
                                    listOf(Color.Transparent, accent.copy(alpha = 0.7f), Color.Transparent),
                                    center = center,
                                ),
                                startAngle = 0f,
                                sweepAngle = 70f,
                                useCenter = false,
                                topLeft = Offset(center.x - r + stroke, center.y - r + stroke),
                                size = androidx.compose.ui.geometry.Size((r - stroke) * 2, (r - stroke) * 2),
                                style = Stroke(width = 3.dp.toPx()),
                            )
                        }
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = label,
                    color = if (enabled) WP.TextPrimary else WP.TextDim,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = sublabel,
                    color = if (enabled) accent else WP.TextDim,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.sp,
                )
            }
        }
    }
}

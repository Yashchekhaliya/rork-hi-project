package com.rork.workpulse.ui.screens.admin

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rork.workpulse.data.AttendanceLog
import com.rork.workpulse.data.Employee
import com.rork.workpulse.data.Geofence
import com.rork.workpulse.data.PresenceStatus
import com.rork.workpulse.data.WorkPulseRepository
import com.rork.workpulse.ui.components.GlassPanel
import com.rork.workpulse.ui.theme.WP
import com.rork.workpulse.ui.theme.glass
import androidx.compose.foundation.Canvas

/**
 * Stylized radar map verifying where active check-ins occurred relative to the
 * geofence center — a clean anti-spoofing view without external map keys.
 */
@Composable
fun GeofenceMap(employees: List<Employee>, logs: List<AttendanceLog>, modifier: Modifier = Modifier) {
    val site = WorkPulseRepository.workSite
    val active = logs.filter { it.isOpen }

    val transition = rememberInfiniteTransition(label = "radar")
    val sweep by transition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(4000, easing = LinearEasing), RepeatMode.Restart),
        label = "sweep",
    )

    GlassPanel(modifier = modifier.fillMaxWidth()) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.MyLocation, null, tint = WP.Cyan, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Location verification", color = WP.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Spacer(Modifier.weight(1f))
                Text("${active.size} live pings", color = WP.Cyan, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
            Spacer(Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Canvas(modifier = Modifier.fillMaxWidth().height(220.dp)) {
                    val center = Offset(size.width / 2, size.height / 2)
                    val maxR = size.minDimension / 2 * 0.92f

                    // concentric grid rings
                    for (i in 1..3) {
                        drawCircle(
                            color = WP.GlassBorderSoft,
                            radius = maxR * i / 3f,
                            center = center,
                            style = Stroke(width = 1.dp.toPx()),
                        )
                    }
                    // crosshair
                    drawLine(WP.GlassBorderSoft, Offset(center.x - maxR, center.y), Offset(center.x + maxR, center.y), 1.dp.toPx())
                    drawLine(WP.GlassBorderSoft, Offset(center.x, center.y - maxR), Offset(center.x, center.y + maxR), 1.dp.toPx())

                    // geofence radius fill (maps to ~80% of maxR)
                    val fenceR = maxR * 0.62f
                    drawCircle(
                        brush = Brush.radialGradient(
                            listOf(WP.Cyan.copy(alpha = 0.18f), Color.Transparent),
                            center = center, radius = fenceR,
                        ),
                        radius = fenceR, center = center,
                    )
                    drawCircle(WP.Cyan.copy(alpha = 0.5f), fenceR, center, style = Stroke(2.dp.toPx()))

                    // radar sweep
                    rotate(sweep, center) {
                        drawArc(
                            brush = Brush.sweepGradient(listOf(Color.Transparent, WP.Cyan.copy(alpha = 0.4f), Color.Transparent), center),
                            startAngle = 0f, sweepAngle = 60f, useCenter = true,
                            topLeft = Offset(center.x - maxR, center.y - maxR),
                            size = androidx.compose.ui.geometry.Size(maxR * 2, maxR * 2),
                        )
                    }

                    // employee pings — distance scaled to radius
                    active.forEachIndexed { idx, log ->
                        val dist = log.distanceFromSite
                        val ratio = (dist / site.radiusMeters).coerceIn(0.0, 1.3).toFloat()
                        val r = fenceR * ratio
                        val angle = (idx * 70 + 25) * Math.PI / 180
                        val px = center.x + r * kotlin.math.cos(angle).toFloat()
                        val py = center.y + r * kotlin.math.sin(angle).toFloat()
                        val inside = dist <= site.radiusMeters
                        val c = if (inside) WP.Lime else WP.Danger
                        drawCircle(c.copy(alpha = 0.3f), 12.dp.toPx(), Offset(px, py))
                        drawCircle(c, 5.dp.toPx(), Offset(px, py))
                    }

                    // HQ marker
                    drawCircle(WP.Purple, 7.dp.toPx(), center)
                    drawCircle(Color.White, 3.dp.toPx(), center)
                }
            }
            Spacer(Modifier.height(12.dp))
            Row {
                LegendDot(WP.Purple, "HQ")
                Spacer(Modifier.width(16.dp))
                LegendDot(WP.Lime, "Verified inside")
                Spacer(Modifier.width(16.dp))
                LegendDot(WP.Danger, "Flagged")
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).clip(CircleShape).glass(cornerRadius = 8.dp, fillTop = color, fillBottom = color))
        Spacer(Modifier.width(6.dp))
        Text(label, color = WP.TextSecondary, fontSize = 11.sp)
    }
}

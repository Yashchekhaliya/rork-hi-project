package com.rork.workpulse.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rork.workpulse.data.Role
import com.rork.workpulse.ui.components.MeshBackground
import com.rork.workpulse.ui.theme.WP
import com.rork.workpulse.ui.theme.glass

@Composable
fun RoleSelectScreen(onSelect: (Role) -> Unit) {
    val infinite = rememberInfiniteTransition(label = "logo")
    val pulse by infinite.animateFloat(
        initialValue = 0.92f, targetValue = 1.05f,
        animationSpec = infiniteRepeatable(tween(2200, easing = LinearEasing), RepeatMode.Reverse),
        label = "pulse",
    )

    Box(Modifier.fillMaxSize()) {
        MeshBackground()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .scale(pulse)
                    .drawBehind {
                        drawCircle(
                            brush = Brush.radialGradient(listOf(WP.Cyan.copy(alpha = 0.5f), Color.Transparent)),
                            radius = size.minDimension * 0.7f,
                        )
                    },
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier.size(96.dp).clip(CircleShape)
                        .glass(cornerRadius = 96.dp, fillTop = WP.Cyan.copy(alpha = 0.3f), fillBottom = WP.Purple.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.Fingerprint, null, tint = WP.Cyan, modifier = Modifier.size(48.dp))
                }
            }
            Spacer(Modifier.height(28.dp))
            Text("Med Lion HR", color = WP.TextPrimary, fontSize = 36.sp, fontWeight = FontWeight.Bold)
            Text("Presence. Verified. Effortless.", color = WP.TextSecondary, fontSize = 14.sp, letterSpacing = 0.5.sp)
            Spacer(Modifier.height(48.dp))

            RoleCard(
                icon = Icons.Filled.Person,
                accent = WP.Cyan,
                title = "I'm an Employee",
                subtitle = "Geofenced check-in, leave & salary",
                onClick = { onSelect(Role.EMPLOYEE) },
            )
            Spacer(Modifier.height(16.dp))
            RoleCard(
                icon = Icons.Filled.AdminPanelSettings,
                accent = WP.Purple,
                title = "I'm an Admin",
                subtitle = "Live workforce, approvals & payroll",
                onClick = { onSelect(Role.ADMIN) },
            )
        }
    }
}

@Composable
private fun RoleCard(icon: ImageVector, accent: Color, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .glass(cornerRadius = 22.dp, fillTop = accent.copy(alpha = 0.16f), fillBottom = WP.GlassBottom, borderColor = accent.copy(alpha = 0.4f))
            .clickable { onClick() }
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(52.dp).clip(CircleShape)
                .glass(cornerRadius = 52.dp, fillTop = accent.copy(alpha = 0.32f), fillBottom = accent.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, null, tint = accent, modifier = Modifier.size(26.dp)) }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = WP.TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, color = WP.TextSecondary, fontSize = 13.sp)
        }
        Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = accent)
    }
}

package com.rork.workpulse.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rork.workpulse.data.ConnectionStatus
import com.rork.workpulse.data.Role
import com.rork.workpulse.data.ServerSync
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
                .verticalScroll(rememberScrollState())
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
                            brush = Brush.radialGradient(listOf(WP.Blue.copy(alpha = 0.5f), Color.Transparent)),
                            radius = size.minDimension * 0.7f,
                        )
                    },
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier.size(96.dp).clip(CircleShape)
                        .glass(cornerRadius = 96.dp, fillTop = WP.Blue.copy(alpha = 0.3f), fillBottom = WP.SkyBlue.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.Fingerprint, null, tint = WP.Blue, modifier = Modifier.size(48.dp))
                }
            }
            Spacer(Modifier.height(28.dp))
            Text("Med Lion HR", color = WP.TextPrimary, fontSize = 36.sp, fontWeight = FontWeight.Bold)
            Text("Presence. Verified. Effortless.", color = WP.TextSecondary, fontSize = 14.sp, letterSpacing = 0.5.sp)
            Spacer(Modifier.height(48.dp))

            RoleCard(
                icon = Icons.Filled.Person,
                accent = WP.Blue,
                title = "Sign In",
                subtitle = "Enter your employee ID & password",
                onClick = { onSelect(Role.EMPLOYEE) },
            )

            Spacer(Modifier.height(20.dp))

            ServerEntryCard()

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun ServerEntryCard() {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val status by ServerSync.status.collectAsState()
    val savedUrl by ServerSync.serverUrl.collectAsState()
    var expanded by remember { mutableStateOf(false) }
    var address by remember(savedUrl) { mutableStateOf(savedUrl?.removePrefix("http://")?.removePrefix("https://") ?: "") }

    val (dotColor, statusLabel) = when (status) {
        ConnectionStatus.CONNECTED -> WP.Lime to "Connected to PC"
        ConnectionStatus.CONNECTING -> WP.Amber to "Connecting…"
        ConnectionStatus.OFFLINE -> WP.TextDim to "Offline · local mode"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .glass(cornerRadius = 22.dp, fillTop = WP.GlassTop, fillBottom = WP.GlassBottom, borderColor = WP.GlassBorder)
            .clickable {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                expanded = !expanded
            }
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(36.dp).clip(CircleShape)
                    .glass(cornerRadius = 36.dp, fillTop = WP.Cyan.copy(alpha = 0.25f), fillBottom = WP.Cyan.copy(alpha = 0.06f)),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Filled.Dns, null, tint = WP.Cyan, modifier = Modifier.size(18.dp)) }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("PC Server", color = WP.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .glass(cornerRadius = 7.dp, fillTop = dotColor, fillBottom = dotColor),
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(statusLabel, color = dotColor, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                }
            }
            Text(
                if (expanded) "▲" else "▼",
                color = WP.TextSecondary,
                fontSize = 10.sp,
            )
        }

        AnimatedVisibility(visible = expanded) {
            Column {
                Spacer(Modifier.height(12.dp))
                Box(Modifier.fillMaxWidth().height(1.dp).glass(cornerRadius = 1.dp))
                Spacer(Modifier.height(12.dp))
                Text(
                    "Enter your PC's local network IP and port.\nExample: 192.168.1.20:8080",
                    color = WP.TextDim,
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                )
                Spacer(Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .glass(cornerRadius = 12.dp, fillTop = WP.GlassTop, fillBottom = WP.GlassBottom, borderColor = WP.GlassBorder),
                ) {
                    TextField(
                        value = address,
                        onValueChange = { address = it },
                        placeholder = { Text("IP:port", color = WP.TextDim, fontSize = 13.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = WP.TextPrimary,
                            unfocusedTextColor = WP.TextPrimary,
                            cursorColor = WP.Cyan,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                        ),
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp),
                    )
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .glass(
                                cornerRadius = 12.dp,
                                fillTop = WP.Cyan.copy(alpha = 0.25f),
                                fillBottom = WP.Cyan.copy(alpha = 0.06f),
                                borderColor = WP.Cyan.copy(alpha = 0.5f),
                            )
                            .clickable {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                ServerSync.setServer(address)
                                Toast.makeText(context, "Connecting to $address…", Toast.LENGTH_SHORT).show()
                            }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("Connect", color = WP.Cyan, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Box(
                        modifier = Modifier
                            .glass(
                                cornerRadius = 12.dp,
                                fillTop = WP.Danger.copy(alpha = 0.15f),
                                fillBottom = WP.Danger.copy(alpha = 0.03f),
                                borderColor = WP.Danger.copy(alpha = 0.35f),
                            )
                            .clickable {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                ServerSync.disconnect()
                                address = ""
                                Toast.makeText(context, "Disconnected — local mode", Toast.LENGTH_SHORT).show()
                            }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("Disconnect", color = WP.Danger, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
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

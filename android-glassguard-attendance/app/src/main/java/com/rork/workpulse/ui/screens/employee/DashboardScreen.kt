package com.rork.workpulse.ui.screens.employee

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rork.workpulse.data.AttendanceLog
import com.rork.workpulse.data.Format
import com.rork.workpulse.data.PresenceStatus
import com.rork.workpulse.data.WorkPulseRepository
import com.rork.workpulse.ui.AttendanceViewModel
import com.rork.workpulse.ui.components.GlassPanel
import com.rork.workpulse.ui.components.HoldToCheckButton
import com.rork.workpulse.ui.components.StatusPill
import com.rork.workpulse.ui.theme.WP
import com.rork.workpulse.ui.theme.glass
import kotlinx.coroutines.delay

@Composable
fun DashboardScreen(modifier: Modifier = Modifier) {
    val vm: AttendanceViewModel = viewModel()
    val ui by vm.ui.collectAsState()
    val employees by WorkPulseRepository.employees.collectAsState()
    val logs by WorkPulseRepository.logs.collectAsState()

    val me = employees.first { it.id == WorkPulseRepository.currentEmployeeId() }
    val openLog = logs.firstOrNull { it.employeeId == me.id && it.isOpen }
    val checkedIn = openLog != null

    // Live ticking clock so the active session timer animates.
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1000)
        }
    }
    LaunchedEffect(Unit) { vm.refreshLocation() }

    val accent = if (checkedIn) WP.Lime else WP.Cyan

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Spacer(Modifier.height(8.dp))

        // Greeting header
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .glass(cornerRadius = 48.dp, fillTop = Color(me.avatarColor).copy(alpha = 0.4f), fillBottom = Color(me.avatarColor).copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(me.name.first().toString(), color = WP.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text("Welcome back", color = WP.TextSecondary, fontSize = 13.sp)
                Text(me.name, color = WP.TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.weight(1f))
            StatusPill(
                text = when {
                    checkedIn -> "On site"
                    me.status == PresenceStatus.ON_LEAVE -> "On leave"
                    else -> "Off"
                },
                accent = accent,
            )
        }

        // Hero check-in panel
        GlassPanel(modifier = Modifier.fillMaxWidth()) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = if (checkedIn) "ACTIVE SESSION" else "READY TO START",
                    color = accent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = if (checkedIn) Format.duration(openLog!!.durationMillis(now)) else "00h 00m",
                    color = WP.TextPrimary,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(20.dp))
                HoldToCheckButton(
                    label = if (checkedIn) "HOLD TO\nCLOCK OUT" else "HOLD TO\nCLOCK IN",
                    sublabel = if (ui.resolving) "SCANNING…" else "BIOMETRIC SECURE",
                    accent = accent,
                    enabled = !ui.resolving,
                    onConfirmed = { vm.toggleCheckInOut(me.id) },
                )
                Spacer(Modifier.height(16.dp))
                ui.message?.let {
                    Text(
                        text = it,
                        color = if (ui.rejected) WP.Danger else WP.Lime,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }

        // Geofence status row
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            GeoStatCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Filled.LocationOn,
                accent = if (ui.insideFence) WP.Lime else WP.Amber,
                label = "Geofence",
                value = if (ui.insideFence) "Inside" else "Outside",
                sub = ui.distanceMeters?.let { "${it.toInt()}m from HQ" } ?: "Locating…",
            )
            GeoStatCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Filled.Schedule,
                accent = WP.Cyan,
                label = "This week",
                value = "${weeklyHours(logs, me.id, now)}h",
                sub = "${WorkPulseRepository.logsFor(me.id).count { it.checkOutMillis != null }} sessions",
            )
        }

        // Worksite card
        GlassPanel(modifier = Modifier.fillMaxWidth()) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Verified, null, tint = WP.Cyan, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Assigned Worksite", color = WP.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                }
                Spacer(Modifier.height(12.dp))
                Text(WorkPulseRepository.workSite.name, color = WP.TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Geofence radius ${WorkPulseRepository.workSite.radiusMeters.toInt()}m · GPS verified check-ins only",
                    color = WP.TextSecondary,
                    fontSize = 13.sp,
                )
            }
        }

        // Recent activity
        Text("Recent activity", color = WP.TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        val recent = WorkPulseRepository.logsFor(me.id).take(4)
        recent.forEach { log -> ActivityRow(log, now) }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun GeoStatCard(
    modifier: Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accent: Color,
    label: String,
    value: String,
    sub: String,
) {
    GlassPanel(modifier = modifier) {
        Column {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .glass(cornerRadius = 36.dp, fillTop = accent.copy(alpha = 0.3f), fillBottom = accent.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center,
            ) { Icon(icon, null, tint = accent, modifier = Modifier.size(18.dp)) }
            Spacer(Modifier.height(12.dp))
            Text(label, color = WP.TextSecondary, fontSize = 12.sp)
            Text(value, color = WP.TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text(sub, color = accent, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun ActivityRow(log: AttendanceLog, now: Long) {
    GlassPanel(modifier = Modifier.fillMaxWidth(), cornerRadius = 18.dp, contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .glass(cornerRadius = 10.dp, fillTop = if (log.isOpen) WP.Lime else WP.Cyan, fillBottom = if (log.isOpen) WP.Lime else WP.Cyan),
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(Format.dayMonth(log.checkInMillis), color = WP.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text(
                    "${Format.time(log.checkInMillis)} → ${log.checkOutMillis?.let { Format.time(it) } ?: "active"}",
                    color = WP.TextSecondary,
                    fontSize = 12.sp,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(Format.duration(log.durationMillis(now)), color = WP.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("${log.distanceFromSite.toInt()}m · verified", color = WP.Lime, fontSize = 11.sp)
            }
        }
    }
}

private fun weeklyHours(logs: List<AttendanceLog>, employeeId: String, now: Long): String {
    val weekMs = 7L * 24 * 60 * 60 * 1000
    val total = logs.filter { it.employeeId == employeeId && now - it.checkInMillis < weekMs }
        .sumOf { it.durationMillis(now) }
    return String.format("%.1f", Format.hoursDecimal(total))
}

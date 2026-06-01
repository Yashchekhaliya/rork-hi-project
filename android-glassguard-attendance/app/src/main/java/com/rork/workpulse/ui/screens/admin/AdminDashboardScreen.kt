package com.rork.workpulse.ui.screens.admin

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rork.workpulse.data.AttendanceLog
import com.rork.workpulse.data.Employee
import com.rork.workpulse.data.Format
import com.rork.workpulse.data.PresenceStatus
import com.rork.workpulse.data.WorkPulseRepository
import com.rork.workpulse.ui.components.GlassPanel
import com.rork.workpulse.ui.components.StatusPill
import com.rork.workpulse.ui.theme.WP
import com.rork.workpulse.ui.theme.glass

@Composable
fun AdminDashboardScreen(modifier: Modifier = Modifier) {
    val employees by WorkPulseRepository.employees.collectAsState()
    val logs by WorkPulseRepository.logs.collectAsState()
    val now = System.currentTimeMillis()

    val onSite = employees.count { it.status == PresenceStatus.CHECKED_IN }
    val onLeave = employees.count { it.status == PresenceStatus.ON_LEAVE }
    val off = employees.count { it.status == PresenceStatus.CHECKED_OUT }

    // Track which employee's logs are expanded and which log is being edited.
    var expandedEmpId by remember { mutableStateOf<String?>(null) }
    var editingLog by remember { mutableStateOf<AttendanceLog?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Spacer(Modifier.height(4.dp))
        Text("Command center", color = WP.TextSecondary, fontSize = 14.sp)
        Text("Live workforce", color = WP.TextPrimary, fontSize = 26.sp, fontWeight = FontWeight.Bold)

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile(Modifier.weight(1f), "$onSite", "On site", WP.Lime)
            StatTile(Modifier.weight(1f), "$onLeave", "On leave", WP.Amber)
            StatTile(Modifier.weight(1f), "$off", "Off", WP.TextSecondary)
        }

        // Location verification map
        GeofenceMap(employees, logs)

        Text("Team status", color = WP.TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        Text("Tap an employee to view & edit attendance logs", color = WP.TextDim, fontSize = 12.sp)

        employees.forEach { emp ->
            val open = logs.firstOrNull { it.employeeId == emp.id && it.isOpen }
            val isExpanded = expandedEmpId == emp.id
            val empLogs = remember(emp.id, logs.size) {
                WorkPulseRepository.logsFor(emp.id)
            }

            Column {
                EmployeeRow(
                    emp = emp,
                    duration = open?.let { Format.duration(it.durationMillis(now)) },
                    distance = open?.distanceFromSite?.toInt(),
                    isExpanded = isExpanded,
                    onClick = {
                        expandedEmpId = if (isExpanded) null else emp.id
                    },
                )

                AnimatedVisibility(
                    visible = isExpanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut(),
                ) {
                    Column(modifier = Modifier.padding(top = 10.dp)) {
                        empLogs.take(6).forEach { log ->
                            LogRow(log = log, now = now, onEdit = { editingLog = log })
                        }
                        if (empLogs.isEmpty()) {
                            GlassPanel(Modifier.fillMaxWidth(), cornerRadius = 16.dp) {
                                Text("No attendance records yet.", color = WP.TextDim, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }

    // Edit dialog
    editingLog?.let { log ->
        EditAttendanceDialog(
            log = log,
            onDismiss = { editingLog = null },
        )
    }
}

@Composable
private fun StatTile(modifier: Modifier, value: String, label: String, accent: Color) {
    GlassPanel(modifier = modifier, contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)) {
        Column {
            Text(value, color = accent, fontSize = 30.sp, fontWeight = FontWeight.Bold)
            Text(label, color = WP.TextSecondary, fontSize = 12.sp)
        }
    }
}

@Composable
private fun EmployeeRow(
    emp: Employee,
    duration: String?,
    distance: Int?,
    isExpanded: Boolean,
    onClick: () -> Unit,
) {
    val accent = when (emp.status) {
        PresenceStatus.CHECKED_IN -> WP.Lime
        PresenceStatus.ON_LEAVE -> WP.Amber
        PresenceStatus.CHECKED_OUT -> WP.TextSecondary
    }
    val borderColor = if (isExpanded) WP.Purple.copy(alpha = 0.5f) else WP.GlassBorder

    GlassPanel(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        cornerRadius = 18.dp,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .glass(cornerRadius = 42.dp, fillTop = Color(emp.avatarColor).copy(alpha = 0.4f), fillBottom = Color(emp.avatarColor).copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center,
            ) { Text(emp.name.first().toString(), color = WP.TextPrimary, fontWeight = FontWeight.Bold) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(emp.name, color = WP.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Text(
                    when (emp.status) {
                        PresenceStatus.CHECKED_IN -> "Active · ${duration ?: ""} · ${distance ?: 0}m from HQ"
                        PresenceStatus.ON_LEAVE -> "On approved leave"
                        PresenceStatus.CHECKED_OUT -> emp.role
                    },
                    color = WP.TextSecondary,
                    fontSize = 12.sp,
                )
            }
            StatusPill(
                when (emp.status) {
                    PresenceStatus.CHECKED_IN -> "In"
                    PresenceStatus.ON_LEAVE -> "Leave"
                    PresenceStatus.CHECKED_OUT -> "Out"
                },
                accent,
            )
        }
    }
}

@Composable
private fun LogRow(
    log: AttendanceLog,
    now: Long,
    onEdit: () -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    GlassPanel(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 14.dp,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .glass(cornerRadius = 8.dp, fillTop = if (log.isOpen) WP.Lime else WP.Cyan, fillBottom = if (log.isOpen) WP.Lime else WP.Cyan),
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(Format.dayMonth(log.checkInMillis), color = WP.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Text(
                    "${Format.time(log.checkInMillis)} → ${log.checkOutMillis?.let { Format.time(it) } ?: "active"}",
                    color = WP.TextSecondary,
                    fontSize = 11.sp,
                )
            }
            Text(Format.duration(log.durationMillis(now)), color = WP.Cyan, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Spacer(Modifier.width(10.dp))
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .glass(cornerRadius = 36.dp, fillTop = WP.Purple.copy(alpha = 0.25f), fillBottom = WP.Purple.copy(alpha = 0.06f), borderColor = WP.Purple.copy(alpha = 0.4f))
                    .clickable {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onEdit()
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Edit, null, tint = WP.Purple, modifier = Modifier.size(16.dp))
            }
        }
    }
}

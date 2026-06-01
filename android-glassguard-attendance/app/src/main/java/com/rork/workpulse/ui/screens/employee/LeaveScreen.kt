package com.rork.workpulse.ui.screens.employee

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rork.workpulse.data.Format
import com.rork.workpulse.data.LeaveRequest
import com.rork.workpulse.data.LeaveStatus
import com.rork.workpulse.data.LeavePayType
import com.rork.workpulse.data.WorkPulseRepository
import com.rork.workpulse.ui.components.GlassPanel
import com.rork.workpulse.ui.components.StatusPill
import com.rork.workpulse.ui.theme.WP
import com.rork.workpulse.ui.theme.glass

private val leaveTypes = listOf("Vacation", "Sick", "Personal", "Work")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaveScreen(modifier: Modifier = Modifier) {
    val haptics = LocalHapticFeedback.current
    val leaves by WorkPulseRepository.leaves.collectAsState()
    val myId = WorkPulseRepository.currentEmployeeId()
    val mine = leaves.filter { it.employeeId == myId }

    var start by remember { mutableStateOf<Long?>(null) }
    var end by remember { mutableStateOf<Long?>(null) }
    var reason by remember { mutableStateOf(TextFieldValue("")) }
    var type by remember { mutableStateOf(leaveTypes.first()) }
    var picking by remember { mutableStateOf<String?>(null) } // "start" | "end"

    val canSubmit = start != null && end != null && reason.text.isNotBlank() && end!! >= start!!

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Spacer(Modifier.height(4.dp))
        Text("Request leave", color = WP.TextPrimary, fontSize = 26.sp, fontWeight = FontWeight.Bold)

        GlassPanel(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("LEAVE TYPE", color = WP.TextSecondary, fontSize = 11.sp, letterSpacing = 1.sp, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    leaveTypes.forEach { t ->
                        val selected = t == type
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .glass(
                                    cornerRadius = 14.dp,
                                    fillTop = if (selected) WP.Cyan.copy(alpha = 0.28f) else WP.GlassTop,
                                    fillBottom = if (selected) WP.Cyan.copy(alpha = 0.08f) else WP.GlassBottom,
                                    borderColor = if (selected) WP.Cyan.copy(alpha = 0.6f) else WP.GlassBorderSoft,
                                )
                                .clickable {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    type = t
                                }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(t, color = if (selected) WP.Cyan else WP.TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                Text("DURATION", color = WP.TextSecondary, fontSize = 11.sp, letterSpacing = 1.sp, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    DateField(Modifier.weight(1f), "From", start) { picking = "start" }
                    DateField(Modifier.weight(1f), "To", end) { picking = "end" }
                }

                Text("REASON", color = WP.TextSecondary, fontSize = 11.sp, letterSpacing = 1.sp, fontWeight = FontWeight.Bold)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(96.dp)
                        .glass(cornerRadius = 16.dp)
                        .padding(14.dp),
                ) {
                    if (reason.text.isEmpty()) {
                        Text("Briefly describe your reason…", color = WP.TextDim, fontSize = 14.sp)
                    }
                    BasicTextField(
                        value = reason,
                        onValueChange = { reason = it },
                        textStyle = TextStyle(color = WP.TextPrimary, fontSize = 14.sp),
                        cursorBrush = SolidColor(WP.Cyan),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .glass(
                            cornerRadius = 16.dp,
                            fillTop = if (canSubmit) WP.Cyan.copy(alpha = 0.35f) else WP.GlassTop,
                            fillBottom = if (canSubmit) WP.Purple.copy(alpha = 0.25f) else WP.GlassBottom,
                            borderColor = if (canSubmit) WP.Cyan.copy(alpha = 0.7f) else WP.GlassBorderSoft,
                        )
                        .clickable(enabled = canSubmit) {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            WorkPulseRepository.submitLeave(myId, start!!, end!!, reason.text.trim(), type)
                            start = null; end = null; reason = TextFieldValue(""); type = leaveTypes.first()
                        }
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "Submit request",
                        color = if (canSubmit) WP.TextPrimary else WP.TextDim,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                    )
                }
            }
        }

        Text("My requests", color = WP.TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        if (mine.isEmpty()) {
            Text("No leave requests yet.", color = WP.TextSecondary, fontSize = 14.sp)
        }
        mine.forEach { LeaveCard(it) }
        Spacer(Modifier.height(24.dp))
    }

    if (picking != null) {
        val state = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { picking = null },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { sel ->
                        if (picking == "start") start = sel else end = sel
                    }
                    picking = null
                }) { Text("OK", color = WP.Cyan) }
            },
            dismissButton = { TextButton(onClick = { picking = null }) { Text("Cancel", color = WP.TextSecondary) } },
        ) {
            DatePicker(state = state)
        }
    }
}

@Composable
private fun DateField(modifier: Modifier, label: String, millis: Long?, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .glass(cornerRadius = 16.dp)
            .clickable { onClick() }
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.CalendarMonth, null, tint = WP.Cyan, modifier = Modifier.width(18.dp))
            Spacer(Modifier.width(8.dp))
            Column {
                Text(label, color = WP.TextSecondary, fontSize = 11.sp)
                Text(millis?.let { Format.shortDate(it) } ?: "Pick date", color = WP.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun LeaveCard(leave: LeaveRequest, modifier: Modifier = Modifier) {
    val accent = when (leave.status) {
        LeaveStatus.APPROVED -> WP.Lime
        LeaveStatus.REJECTED -> WP.Danger
        LeaveStatus.PENDING -> WP.Amber
    }
    GlassPanel(modifier = modifier.fillMaxWidth()) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(leave.type, color = WP.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Spacer(Modifier.weight(1f))
                StatusPill(leave.status.name, accent)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "${Format.shortDate(leave.startMillis)} → ${Format.shortDate(leave.endMillis)} · ${leave.days} day${if (leave.days > 1) "s" else ""}",
                color = WP.TextSecondary,
                fontSize = 13.sp,
            )
            Spacer(Modifier.height(6.dp))
            Text(leave.reason, color = WP.TextSecondary, fontSize = 13.sp)
            leave.payType?.let {
                Spacer(Modifier.height(8.dp))
                StatusPill(
                    if (it == LeavePayType.WITH_PAY) "Leave with pay" else "Leave without pay",
                    if (it == LeavePayType.WITH_PAY) WP.Lime else WP.Amber,
                )
            }
        }
    }
}

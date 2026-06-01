package com.rork.workpulse.ui.screens.admin

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.rork.workpulse.data.AttendanceLog
import com.rork.workpulse.data.Format
import com.rork.workpulse.data.WorkPulseRepository
import com.rork.workpulse.ui.components.GlassPanel
import com.rork.workpulse.ui.theme.WP
import com.rork.workpulse.ui.theme.glass
import com.rork.workpulse.ui.theme.neonGlass
import java.util.Calendar

/**
 * Fullscreen dialog for admins to adjust check-in / check-out timestamps
 * on any attendance log. Uses scrollable number-picker-style selectors
 * for hour and minute of both in and out times, plus a date shortcut.
 */
@Composable
fun EditAttendanceDialog(
    log: AttendanceLog,
    onDismiss: () -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    val cal = Calendar.getInstance()

    fun calAt(millis: Long): Calendar = Calendar.getInstance().apply { timeInMillis = millis }

    val inCal = calAt(log.checkInMillis)
    val outCal = log.checkOutMillis?.let { calAt(it) }

    var editInHour by remember { mutableStateOf(inCal.get(Calendar.HOUR_OF_DAY)) }
    var editInMin by remember { mutableStateOf(inCal.get(Calendar.MINUTE)) }
    var editOutHour by remember { mutableStateOf(outCal?.get(Calendar.HOUR_OF_DAY) ?: 18) }
    var editOutMin by remember { mutableStateOf(outCal?.get(Calendar.MINUTE) ?: 0) }
    var hasOut by remember { mutableStateOf(log.checkOutMillis != null) }

    fun buildInMillis(): Long {
        val c = inCal.clone() as Calendar
        c.set(Calendar.HOUR_OF_DAY, editInHour)
        c.set(Calendar.MINUTE, editInMin)
        c.set(Calendar.SECOND, 0)
        c.set(Calendar.MILLISECOND, 0)
        return c.timeInMillis
    }

    fun buildOutMillis(): Long? {
        if (!hasOut) return null
        val base = outCal?.clone() as? Calendar ?: Calendar.getInstance().apply {
            timeInMillis = buildInMillis()
        }
        base.set(Calendar.HOUR_OF_DAY, editOutHour)
        base.set(Calendar.MINUTE, editOutMin)
        base.set(Calendar.SECOND, 0)
        base.set(Calendar.MILLISECOND, 0)
        return base.timeInMillis
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(WP.BgDeep.copy(alpha = 0.97f))
                .padding(20.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Header
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Edit attendance", color = WP.TextPrimary, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            Format.dayMonth(log.checkInMillis),
                            color = WP.Purple,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .glass(cornerRadius = 40.dp)
                            .clickable { onDismiss() },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Filled.Close, null, tint = WP.TextSecondary, modifier = Modifier.size(20.dp))
                    }
                }

                // Employee info
                val emp = WorkPulseRepository.employees.value.firstOrNull { it.id == log.employeeId }
                Text("Employee: ${emp?.name ?: log.employeeId}", color = WP.TextSecondary, fontSize = 14.sp)
                Text(
                    "ID: ${log.id} · Distance: ${log.distanceFromSite.toInt()}m · Verified: ${if (log.verified) "Yes" else "No"}",
                    color = WP.TextDim,
                    fontSize = 12.sp,
                )

                // Check-in time picker
                GlassPanel(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Text("CHECK-IN TIME", color = WP.Cyan, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                        Spacer(Modifier.height(16.dp))
                        TimePickerRow(
                            hour = editInHour,
                            minute = editInMin,
                            onHourChange = { editInHour = it },
                            onMinuteChange = { editInMin = it },
                            accent = WP.Cyan,
                        )
                    }
                }

                // Check-out time picker
                GlassPanel(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "CHECK-OUT TIME",
                                color = if (hasOut) WP.Magenta else WP.TextDim,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.5.sp,
                                modifier = Modifier.weight(1f),
                            )
                            Box(
                                modifier = Modifier
                                    .glass(cornerRadius = 999.dp, fillTop = if (hasOut) WP.Magenta.copy(alpha = 0.28f) else WP.GlassTop, fillBottom = if (hasOut) WP.Magenta.copy(alpha = 0.08f) else WP.GlassBottom)
                                    .clickable {
                                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                        hasOut = !hasOut
                                    }
                                    .padding(horizontal = 14.dp, vertical = 6.dp),
                            ) {
                                Text(
                                    if (hasOut) "SET" else "NO OUT",
                                    color = if (hasOut) WP.Magenta else WP.TextDim,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                )
                            }
                        }
                        if (hasOut) {
                            Spacer(Modifier.height(16.dp))
                            TimePickerRow(
                                hour = editOutHour,
                                minute = editOutMin,
                                onHourChange = { editOutHour = it },
                                onMinuteChange = { editOutMin = it },
                                accent = WP.Magenta,
                            )
                        }
                    }
                }

                // Preview of resulting duration
                val previewIn = buildInMillis()
                val previewOut = buildOutMillis()
                val previewDuration = if (previewOut != null) previewOut - previewIn else 0L
                val previewHours = previewDuration / 3_600_000.0
                val fractionalDays = if (previewOut != null) {
                    previewHours.coerceAtMost(WorkPulseRepository.DAILY_WORKING_HOURS) / WorkPulseRepository.DAILY_WORKING_HOURS
                } else 0.0

                Box(modifier = Modifier.fillMaxWidth().neonGlass(WP.Purple, 20.dp).padding(16.dp)) {
                    Column {
                        Row {
                            Column(Modifier.weight(1f)) {
                                Text("PREVIEW", color = WP.Purple, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    Format.duration(previewDuration.coerceAtLeast(0)),
                                    color = WP.TextPrimary,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("FRACTIONAL DAYS", color = WP.TextDim, fontSize = 10.sp, letterSpacing = 1.sp)
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    Format.fracDays(fractionalDays),
                                    color = WP.Lime,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "${previewHours.coerceAtLeast(0.0).let { "%.1f".format(it) }}h ÷ ${WorkPulseRepository.DAILY_WORKING_HOURS.toInt()}h full day",
                            color = WP.TextDim,
                            fontSize = 11.sp,
                        )
                    }
                }

                // Action buttons
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ActionButton("Cancel", WP.TextSecondary, Modifier.weight(1f)) {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onDismiss()
                    }
                    ActionButton("Save Changes", WP.Lime, Modifier.weight(1f)) {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        WorkPulseRepository.updateAttendanceLog(
                            logId = log.id,
                            newCheckInMillis = previewIn,
                            newCheckOutMillis = previewOut,
                        )
                        onDismiss()
                    }
                }

                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun TimePickerRow(
    hour: Int,
    minute: Int,
    onHourChange: (Int) -> Unit,
    onMinuteChange: (Int) -> Unit,
    accent: Color,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        WheelPicker(
            value = hour,
            range = 0..23,
            format = { "%02d".format(it) },
            label = "H",
            accent = accent,
            onValueChange = onHourChange,
        )
        Text(
            ":",
            color = accent.copy(alpha = 0.7f),
            fontSize = 32.sp,
            fontWeight = FontWeight.Light,
            modifier = Modifier.padding(horizontal = 8.dp),
        )
        WheelPicker(
            value = minute,
            range = 0..59,
            format = { "%02d".format(it) },
            label = "M",
            accent = accent,
            onValueChange = onMinuteChange,
        )
    }
}

@Composable
private fun WheelPicker(
    value: Int,
    range: IntRange,
    format: (Int) -> String,
    label: String,
    accent: Color,
    onValueChange: (Int) -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // Up button
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .glass(cornerRadius = 48.dp, fillTop = accent.copy(alpha = 0.18f), fillBottom = accent.copy(alpha = 0.04f))
                .clickable {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    if (value < range.last) onValueChange(value + 1)
                    else onValueChange(range.first)
                },
            contentAlignment = Alignment.Center,
        ) {
            Text("▲", color = accent, fontSize = 14.sp)
        }

        Spacer(Modifier.height(8.dp))

        // Display value
        Box(
            modifier = Modifier
                .width(80.dp)
                .glass(cornerRadius = 20.dp, fillTop = accent.copy(alpha = 0.14f), fillBottom = accent.copy(alpha = 0.03f), borderColor = accent.copy(alpha = 0.3f))
                .padding(vertical = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    format(value),
                    color = WP.TextPrimary,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(label, color = accent.copy(alpha = 0.5f), fontSize = 10.sp, fontWeight = FontWeight.Medium)
            }
        }

        Spacer(Modifier.height(8.dp))

        // Down button
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .glass(cornerRadius = 48.dp, fillTop = accent.copy(alpha = 0.18f), fillBottom = accent.copy(alpha = 0.04f))
                .clickable {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    if (value > range.first) onValueChange(value - 1)
                    else onValueChange(range.last)
                },
            contentAlignment = Alignment.Center,
        ) {
            Text("▼", color = accent, fontSize = 14.sp)
        }
    }
}

@Composable
private fun ActionButton(label: String, accent: Color, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .glass(cornerRadius = 16.dp, fillTop = accent.copy(alpha = 0.28f), fillBottom = accent.copy(alpha = 0.08f), borderColor = accent.copy(alpha = 0.6f))
            .clickable { onClick() }
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = accent, fontWeight = FontWeight.Bold, fontSize = 15.sp)
    }
}

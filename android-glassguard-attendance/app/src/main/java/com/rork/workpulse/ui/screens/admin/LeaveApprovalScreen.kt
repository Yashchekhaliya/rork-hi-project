package com.rork.workpulse.ui.screens.admin

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rork.workpulse.data.Format
import com.rork.workpulse.data.LeavePayType
import com.rork.workpulse.data.LeaveRequest
import com.rork.workpulse.data.LeaveStatus
import com.rork.workpulse.data.WorkPulseRepository
import com.rork.workpulse.ui.components.GlassPanel
import com.rork.workpulse.ui.components.StatusPill
import com.rork.workpulse.ui.theme.WP
import com.rork.workpulse.ui.theme.glass

@Composable
fun LeaveApprovalScreen(modifier: Modifier = Modifier) {
    val leaves by WorkPulseRepository.leaves.collectAsState()
    val pending = leaves.filter { it.status == LeaveStatus.PENDING }
    val decided = leaves.filter { it.status != LeaveStatus.PENDING }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Spacer(Modifier.height(4.dp))
        Text("Approvals", color = WP.TextPrimary, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Text("${pending.size} pending request${if (pending.size != 1) "s" else ""}", color = WP.Amber, fontSize = 14.sp)

        pending.forEach { ApprovalCard(it) }
        if (pending.isEmpty()) {
            GlassPanel(Modifier.fillMaxWidth()) {
                Text("All caught up — no pending requests.", color = WP.TextSecondary, fontSize = 14.sp)
            }
        }

        if (decided.isNotEmpty()) {
            Text("Decided", color = WP.TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            decided.forEach { DecidedCard(it) }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun ApprovalCard(leave: LeaveRequest) {
    val haptics = LocalHapticFeedback.current
    var choosingPay by remember { mutableStateOf(false) }

    GlassPanel(modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(leave.employeeName, color = WP.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("${leave.type} leave · requested ${Format.shortDate(leave.requestedAtMillis)}", color = WP.TextSecondary, fontSize = 12.sp)
                }
                StatusPill("${leave.days}d", WP.Cyan)
            }
            Spacer(Modifier.height(10.dp))
            Text("${Format.dayMonth(leave.startMillis)} → ${Format.dayMonth(leave.endMillis)}", color = WP.TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(6.dp))
            Text(leave.reason, color = WP.TextSecondary, fontSize = 13.sp)
            Spacer(Modifier.height(16.dp))

            if (!choosingPay) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ActionButton("Reject", WP.Danger, Modifier.weight(1f)) {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        WorkPulseRepository.decideLeave(leave.id, LeaveStatus.REJECTED, null)
                    }
                    ActionButton("Approve", WP.Lime, Modifier.weight(1f)) {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        choosingPay = true
                    }
                }
            } else {
                Text("Categorize this approved leave", color = WP.TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ActionButton("With pay", WP.Lime, Modifier.weight(1f)) {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        WorkPulseRepository.decideLeave(leave.id, LeaveStatus.APPROVED, LeavePayType.WITH_PAY)
                    }
                    ActionButton("Without pay", WP.Amber, Modifier.weight(1f)) {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        WorkPulseRepository.decideLeave(leave.id, LeaveStatus.APPROVED, LeavePayType.WITHOUT_PAY)
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionButton(label: String, accent: Color, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .glass(cornerRadius = 14.dp, fillTop = accent.copy(alpha = 0.28f), fillBottom = accent.copy(alpha = 0.08f), borderColor = accent.copy(alpha = 0.6f))
            .clickable { onClick() }
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = accent, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}

@Composable
private fun DecidedCard(leave: LeaveRequest) {
    val accent = if (leave.status == LeaveStatus.APPROVED) WP.Lime else WP.Danger
    GlassPanel(modifier = Modifier.fillMaxWidth(), cornerRadius = 18.dp, contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(leave.employeeName, color = WP.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text(
                    "${leave.type} · ${Format.shortDate(leave.startMillis)} → ${Format.shortDate(leave.endMillis)}",
                    color = WP.TextSecondary, fontSize = 12.sp,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                StatusPill(leave.status.name, accent)
                leave.payType?.let {
                    Spacer(Modifier.height(6.dp))
                    Text(if (it == LeavePayType.WITH_PAY) "Paid" else "Unpaid", color = if (it == LeavePayType.WITH_PAY) WP.Lime else WP.Amber, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

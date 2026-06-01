package com.rork.workpulse.ui.screens.employee

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rork.workpulse.data.Format
import com.rork.workpulse.data.SalaryBreakdown
import com.rork.workpulse.data.WorkPulseRepository
import com.rork.workpulse.ui.components.GlassPanel
import com.rork.workpulse.ui.theme.WP
import com.rork.workpulse.ui.theme.glass
import com.rork.workpulse.ui.theme.neonGlass

@Composable
fun SalaryScreen(modifier: Modifier = Modifier) {
    // Recompute when logs or leaves change.
    val logs by WorkPulseRepository.logs.collectAsState()
    val leaves by WorkPulseRepository.leaves.collectAsState()
    val myId = WorkPulseRepository.currentEmployeeId()
    val salary = rememberSalary(myId, logs.size, leaves.size)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Spacer(Modifier.height(4.dp))
        Text("Estimated payout", color = WP.TextSecondary, fontSize = 14.sp)

        // Hero salary card
        Box(modifier = Modifier.fillMaxWidth().neonGlass(WP.Cyan, 28.dp).padding(24.dp)) {
            Column {
                Text("THIS MONTH (NET)", color = WP.Cyan, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                Spacer(Modifier.height(6.dp))
                AnimatedMoney(salary.netPayable)
                Spacer(Modifier.height(12.dp))
                Row {
                    MiniStat("Base", WorkPulseRepository.formatMoney(salary.baseSalary), Modifier.weight(1f))
                    MiniStat("Per day", WorkPulseRepository.formatMoney(salary.perDayRate), Modifier.weight(1f))
                    MiniStat("Days worked", "${Format.fracDays(salary.daysWorked)}/${salary.totalWorkingDays}", Modifier.weight(1f))
                }
            }
        }

        // Progress toward full month
        GlassPanel(modifier = Modifier.fillMaxWidth()) {
            Column {
                Row {
                    Text("Earnings progress", color = WP.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    Spacer(Modifier.weight(1f))
                    val pct = if (salary.baseSalary > 0) (salary.netPayable / salary.baseSalary * 100).toInt() else 0
                    Text("$pct%", color = WP.Cyan, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
                Spacer(Modifier.height(12.dp))
                val frac = if (salary.baseSalary > 0) (salary.netPayable / salary.baseSalary).toFloat().coerceIn(0f, 1f) else 0f
                ProgressBar(frac)
                Spacer(Modifier.height(6.dp))
                Text(
                    "${WorkPulseRepository.formatMoney(salary.netPayable)} of ${WorkPulseRepository.formatMoney(salary.baseSalary)} potential",
                    color = WP.TextSecondary,
                    fontSize = 12.sp,
                )
            }
        }

        // Breakdown
        Text("Breakdown", color = WP.TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        GlassPanel(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                BreakdownRow("Days worked", "${Format.fracDays(salary.daysWorked)} days (${salary.dailyWorkingHours.toInt()}h = 1 day)", WP.Lime)
                BreakdownRow("Paid leave", "${salary.paidLeaveDays} days", WP.Cyan)
                BreakdownRow("Unpaid leave", "${salary.unpaidLeaveDays} days", WP.Amber)
                BreakdownRow("Unauthorized absences", "${Format.fracDays(salary.absentDays)} days", WP.Danger)
                Box(Modifier.fillMaxWidth().height(1.dp).glass(cornerRadius = 1.dp))
                BreakdownRow("Gross earned", WorkPulseRepository.formatMoney(salary.grossEarned), WP.TextPrimary, bold = true)
                BreakdownRow("Deductions", "−${WorkPulseRepository.formatMoney(salary.deductions)}", WP.Danger, bold = true)
            }
        }

        // Attendance history
        Text("Attendance history", color = WP.TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        WorkPulseRepository.logsFor(myId).take(8).forEach { log ->
            GlassPanel(modifier = Modifier.fillMaxWidth(), cornerRadius = 16.dp, contentPadding = androidx.compose.foundation.layout.PaddingValues(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(Format.dayMonth(log.checkInMillis), color = WP.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text("${Format.time(log.checkInMillis)} → ${log.checkOutMillis?.let { Format.time(it) } ?: "active"}", color = WP.TextSecondary, fontSize = 12.sp)
                    }
                    Text(Format.duration(log.durationMillis(System.currentTimeMillis())), color = WP.Cyan, fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun rememberSalary(id: String, logsKey: Int, leavesKey: Int): SalaryBreakdown {
    // Keys force recomputation when underlying data changes.
    return androidx.compose.runtime.remember(id, logsKey, leavesKey) {
        WorkPulseRepository.salaryFor(id)
    }
}

@Composable
private fun AnimatedMoney(value: Double) {
    val animated by animateFloatAsState(targetValue = value.toFloat(), animationSpec = tween(900), label = "money")
    Text(
        WorkPulseRepository.formatMoney(animated.toDouble()),
        color = WP.TextPrimary,
        fontSize = 44.sp,
        fontWeight = FontWeight.Bold,
    )
}

@Composable
private fun MiniStat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(label, color = WP.TextSecondary, fontSize = 11.sp)
        Text(value, color = WP.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ProgressBar(fraction: Float) {
    val animated by animateFloatAsState(targetValue = fraction, animationSpec = tween(900), label = "bar")
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(12.dp)
            .clip(RoundedCornerShape(999.dp))
            .glass(cornerRadius = 999.dp)
            .drawBehind {
                drawRoundRect(
                    brush = Brush.horizontalGradient(listOf(WP.Cyan, WP.Purple)),
                    size = androidx.compose.ui.geometry.Size(size.width * animated, size.height),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height),
                    topLeft = Offset.Zero,
                )
            },
    )
}

@Composable
private fun BreakdownRow(label: String, value: String, accent: Color, bold: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = WP.TextSecondary, fontSize = 14.sp, fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal)
        Spacer(Modifier.weight(1f))
        Text(value, color = accent, fontSize = 14.sp, fontWeight = if (bold) FontWeight.Bold else FontWeight.SemiBold)
    }
}

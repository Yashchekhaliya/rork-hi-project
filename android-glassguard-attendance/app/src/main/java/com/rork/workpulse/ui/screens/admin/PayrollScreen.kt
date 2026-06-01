package com.rork.workpulse.ui.screens.admin

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rork.workpulse.data.CsvExporter
import com.rork.workpulse.data.Format
import com.rork.workpulse.data.SalaryBreakdown
import com.rork.workpulse.data.WorkPulseRepository
import com.rork.workpulse.ui.components.GlassPanel
import com.rork.workpulse.ui.theme.WP
import com.rork.workpulse.ui.theme.glass
import com.rork.workpulse.ui.theme.neonGlass
import java.util.Calendar

@Composable
fun PayrollScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val employees by WorkPulseRepository.employees.collectAsState()
    val logs by WorkPulseRepository.logs.collectAsState()
    val leaves by WorkPulseRepository.leaves.collectAsState()

    val now = Calendar.getInstance()
    var selectedMonth by remember { mutableIntStateOf(now.get(Calendar.MONTH)) }
    var selectedYear by remember { mutableIntStateOf(now.get(Calendar.YEAR)) }

    // Recompute when data or month changes
    val salaries = remember(employees.size, logs.size, leaves.size, selectedMonth, selectedYear) {
        WorkPulseRepository.allSalaries(selectedMonth, selectedYear)
    }
    val totalNet = salaries.sumOf { it.netPayable }
    val totalDeductions = salaries.sumOf { it.deductions }
    val workingDays = WorkPulseRepository.standardWorkingDays(selectedMonth, selectedYear)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Spacer(Modifier.height(4.dp))

        // Month selector
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .glass(cornerRadius = 36.dp)
                    .clickable {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        if (selectedMonth == 0) {
                            selectedMonth = 11
                            selectedYear--
                        } else {
                            selectedMonth--
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.ChevronLeft, null, tint = WP.TextSecondary, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                Text("Payroll engine", color = WP.TextPrimary, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                Text(
                    Format.monthYear(selectedMonth, selectedYear),
                    color = WP.Purple,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Spacer(Modifier.width(12.dp))
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .glass(cornerRadius = 36.dp)
                    .clickable {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        if (selectedMonth == 11) {
                            selectedMonth = 0
                            selectedYear++
                        } else {
                            selectedMonth++
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.ChevronRight, null, tint = WP.TextSecondary, modifier = Modifier.size(20.dp))
            }
        }

        // Export quick button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .glass(cornerRadius = 16.dp, fillTop = WP.Lime.copy(alpha = 0.12f), fillBottom = WP.Lime.copy(alpha = 0.04f), borderColor = WP.Lime.copy(alpha = 0.35f))
                .clickable {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    CsvExporter.exportMonthlyPayroll(context, selectedMonth, selectedYear)
                    Toast.makeText(context, "Exported ${Format.monthName(selectedMonth)} payroll", Toast.LENGTH_SHORT).show()
                }
                .padding(12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.FileDownload, null, tint = WP.Lime, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Export ${Format.monthName(selectedMonth)} CSV", color = WP.Lime, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        // Summary card
        Box(modifier = Modifier.fillMaxWidth().neonGlass(WP.Purple, 28.dp).padding(24.dp)) {
            Column {
                Text("TOTAL NET PAYABLE", color = WP.Purple, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                Spacer(Modifier.height(6.dp))
                Text(WorkPulseRepository.formatMoney(totalNet), color = WP.TextPrimary, fontSize = 40.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                Row {
                    Column(Modifier.weight(1f)) {
                        Text("Headcount", color = WP.TextSecondary, fontSize = 12.sp)
                        Text("${salaries.size}", color = WP.TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                    Column(Modifier.weight(1f)) {
                        Text("Auto deductions", color = WP.TextSecondary, fontSize = 12.sp)
                        Text(WorkPulseRepository.formatMoney(totalDeductions), color = WP.Danger, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                    Column(Modifier.weight(1f)) {
                        Text("Working days", color = WP.TextSecondary, fontSize = 12.sp)
                        Text("$workingDays", color = WP.TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Text(
            "Formula: (${WorkPulseRepository.DAILY_WORKING_HOURS.toInt()}h = 1 day) · (Base ÷ working days) × (fractional days + paid leave)",
            color = WP.TextSecondary,
            fontSize = 12.sp,
        )

        salaries.forEach { PayrollCard(it) }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun PayrollCard(s: SalaryBreakdown) {
    var expanded by remember { mutableStateOf(false) }
    GlassPanel(modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded }) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(s.employeeName, color = WP.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(
                        "${Format.fracDays(s.daysWorked)} worked · ${s.paidLeaveDays} paid leave · ${Format.fracDays(s.absentDays)} absent",
                        color = WP.TextSecondary,
                        fontSize = 12.sp,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        WorkPulseRepository.formatMoney(s.netPayable),
                        color = if (s.netPayable >= s.baseSalary) WP.Lime else WP.Amber,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                    )
                    Text(
                        "of ${WorkPulseRepository.formatMoney(s.baseSalary)}",
                        color = WP.TextSecondary,
                        fontSize = 11.sp,
                    )
                }
            }
            AnimatedVisibility(visible = expanded) {
                Column {
                    Spacer(Modifier.height(14.dp))
                    Box(Modifier.fillMaxWidth().height(1.dp).glass(cornerRadius = 1.dp))
                    Spacer(Modifier.height(14.dp))
                    DetailRow("Base salary", WorkPulseRepository.formatMoney(s.baseSalary))
                    DetailRow("Per-day rate", WorkPulseRepository.formatMoney(s.perDayRate))
                    DetailRow("Days worked", "${Format.fracDays(s.daysWorked)} / ${s.totalWorkingDays}")
                    DetailRow("Paid leave days", "${s.paidLeaveDays}")
                    DetailRow("Unpaid leave days", "${s.unpaidLeaveDays}")
                    DetailRow("Absent (deducted)", "${Format.fracDays(s.absentDays)}")
                    Spacer(Modifier.height(8.dp))
                    DetailRow("Gross earned", WorkPulseRepository.formatMoney(s.grossEarned), WP.TextPrimary)
                    DetailRow("Deductions", "−${WorkPulseRepository.formatMoney(s.deductions)}", WP.Danger)
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String, accent: Color = WP.TextSecondary) {
    Row(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(label, color = WP.TextSecondary, fontSize = 13.sp)
        Spacer(Modifier.weight(1f))
        Text(value, color = accent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

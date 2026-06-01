package com.rork.workpulse.ui.screens.admin

import android.app.Activity
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.TableChart

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rork.workpulse.data.ConnectionStatus
import com.rork.workpulse.data.CsvExporter
import com.rork.workpulse.data.Format
import com.rork.workpulse.data.ServerSync
import com.rork.workpulse.data.WorkPulseRepository
import com.rork.workpulse.ui.components.GlassPanel
import com.rork.workpulse.ui.theme.WP
import com.rork.workpulse.ui.theme.glass
import com.rork.workpulse.ui.theme.neonGlass

/**
 * Admin hub for exports, password management, worksite config, and logout.
 */
@Composable
fun AdminSettingsScreen(
    onLogout: () -> Unit,
    onToggleImmersive: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val now = Format.currentMonth()
    val year = Format.currentYear()

    var selectedExportMonth by remember { mutableStateOf(now) }
    var selectedExportYear by remember { mutableStateOf(year) }
    var showPasswordSection by remember { mutableStateOf(false) }
    var showWorksiteSection by remember { mutableStateOf(false) }
    var isImmersive by remember { mutableStateOf(false) }
    var newPassword by remember { mutableStateOf("") }
    var siteName by remember { mutableStateOf(WorkPulseRepository.workSite.name) }
    var siteLat by remember { mutableStateOf(WorkPulseRepository.workSite.center.latitude.toString()) }
    var siteLon by remember { mutableStateOf(WorkPulseRepository.workSite.center.longitude.toString()) }
    var siteRadius by remember { mutableStateOf(WorkPulseRepository.workSite.radiusMeters.toInt().toString()) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Spacer(Modifier.height(4.dp))
        Text("Admin panel", color = WP.TextPrimary, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Text("Export, configure & manage", color = WP.TextSecondary, fontSize = 14.sp)

        // ── Employee Management ──
        SectionTitle("Employees")
        AdminEmployeeManagerScreen(modifier = Modifier)

        // ── Local Server Section ──
        SectionTitle("Local Server")
        ServerConfigCard()

        // ── Export Section ──
        SectionTitle("Reports & Export")

        // Month picker for export
        GlassPanel(modifier = Modifier.fillMaxWidth(), cornerRadius = 18.dp, contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)) {
            Column {
                Text("Select month to export", color = WP.TextSecondary, fontSize = 12.sp)
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    MonthChip(selectedExportMonth, selectedExportYear, Modifier.weight(1f)) {
                        // Simple cycling
                    }
                }
                Spacer(Modifier.height(6.dp))
                // Horizontal month scroller
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Format.shortMonthNames.forEachIndexed { idx, name ->
                        val isSel = idx == selectedExportMonth
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                                .then(
                                    if (isSel) Modifier.glass(cornerRadius = 12.dp, fillTop = WP.Purple.copy(alpha = 0.35f), fillBottom = WP.Purple.copy(alpha = 0.08f), borderColor = WP.Purple.copy(alpha = 0.6f))
                                    else Modifier.glass(cornerRadius = 12.dp)
                                )
                                .clickable {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    selectedExportMonth = idx
                                }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(name, color = if (isSel) WP.Purple else WP.TextSecondary, fontSize = 11.sp, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }
            }
        }

        // Export buttons
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ExportButton(
                icon = Icons.Filled.Description,
                label = "Monthly\nPayroll",
                accent = WP.Cyan,
                modifier = Modifier.weight(1f),
            ) {
                CsvExporter.exportMonthlyPayroll(context, selectedExportMonth, selectedExportYear)
                toast(context, "Payroll CSV for ${Format.monthName(selectedExportMonth)} $selectedExportYear")
            }
            ExportButton(
                icon = Icons.Filled.TableChart,
                label = "Yearly\nSummary",
                accent = WP.Lime,
                modifier = Modifier.weight(1f),
            ) {
                CsvExporter.exportYearlySummary(context, selectedExportYear)
                toast(context, "Yearly summary CSV for $selectedExportYear")
            }
            ExportButton(
                icon = Icons.Filled.CalendarMonth,
                label = "Attendance\nLog",
                accent = WP.Amber,
                modifier = Modifier.weight(1f),
            ) {
                CsvExporter.exportAttendanceLog(context, selectedExportMonth, selectedExportYear)
                toast(context, "Attendance log CSV for ${Format.monthName(selectedExportMonth)} $selectedExportYear")
            }
        }

        // ── Display Section ──
        SectionTitle("Display")

        GlassPanel(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    isImmersive = !isImmersive
                    onToggleImmersive(isImmersive)
                },
            cornerRadius = 18.dp,
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(40.dp).clip(CircleShape)
                        .glass(
                            cornerRadius = 40.dp,
                            fillTop = if (isImmersive) WP.Lime.copy(alpha = 0.3f) else WP.TextSecondary.copy(alpha = 0.15f),
                            fillBottom = if (isImmersive) WP.Lime.copy(alpha = 0.08f) else WP.TextDim.copy(alpha = 0.05f),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        if (isImmersive) "[ ]" else "[X]",
                        color = if (isImmersive) WP.Lime else WP.TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-1).sp,
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Fullscreen mode", color = WP.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Text(
                        if (isImmersive) "Immersive — swipe edge to show bars" else "Edge-to-edge with notch support",
                        color = WP.TextDim,
                        fontSize = 12.sp,
                    )
                }
                Box(
                    modifier = Modifier
                        .size(48.dp, 28.dp)
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(14.dp))
                        .then(
                            if (isImmersive) Modifier.glass(
                                cornerRadius = 14.dp,
                                fillTop = WP.Lime.copy(alpha = 0.4f),
                                fillBottom = WP.Lime.copy(alpha = 0.1f),
                                borderColor = WP.Lime.copy(alpha = 0.6f),
                            ) else Modifier.glass(cornerRadius = 14.dp)
                        ),
                    contentAlignment = if (isImmersive) Alignment.CenterEnd else Alignment.CenterStart,
                ) {
                    Box(
                        modifier = Modifier
                            .padding(3.dp)
                            .size(22.dp)
                            .clip(CircleShape)
                            .then(
                                if (isImmersive) Modifier.glass(
                                    cornerRadius = 22.dp,
                                    fillTop = WP.Lime.copy(alpha = 0.6f),
                                    fillBottom = WP.Lime.copy(alpha = 0.2f),
                                ) else Modifier.glass(
                                    cornerRadius = 22.dp,
                                    fillTop = WP.TextSecondary.copy(alpha = 0.3f),
                                    fillBottom = WP.TextDim.copy(alpha = 0.1f),
                                )
                            ),
                    )
                }
            }
        }

        // ── Configuration Section ──
        SectionTitle("Configuration")

        // Password change
        GlassPanel(
            modifier = Modifier.fillMaxWidth().clickable {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                showPasswordSection = !showPasswordSection
            },
            cornerRadius = 18.dp,
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(40.dp).clip(CircleShape)
                        .glass(cornerRadius = 40.dp, fillTop = WP.Purple.copy(alpha = 0.3f), fillBottom = WP.Purple.copy(alpha = 0.08f)),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Filled.Lock, null, tint = WP.Purple, modifier = Modifier.size(20.dp)) }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Change admin password", color = WP.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Text("Current: ${"*".repeat(WorkPulseRepository.adminPassword.length)}", color = WP.TextDim, fontSize = 12.sp)
                }
                Icon(Icons.Filled.Edit, null, tint = WP.TextSecondary, modifier = Modifier.size(18.dp))
            }

            AnimatedVisibility(visible = showPasswordSection) {
                Column {
                    Spacer(Modifier.height(14.dp))
                    Box(Modifier.fillMaxWidth().height(1.dp).glass(cornerRadius = 1.dp))
                    Spacer(Modifier.height(14.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextField(
                            value = newPassword,
                            onValueChange = { newPassword = it },
                            placeholder = { Text("New password (min 4 chars)", color = WP.TextDim, fontSize = 14.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            colors = TextFieldDefaults.colors(
                                focusedTextColor = WP.TextPrimary,
                                unfocusedTextColor = WP.TextPrimary,
                                cursorColor = WP.Purple,
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                            ),
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(Modifier.width(10.dp))
                        Box(
                            modifier = Modifier
                                .glass(cornerRadius = 14.dp, fillTop = WP.Lime.copy(alpha = 0.3f), fillBottom = WP.Lime.copy(alpha = 0.08f), borderColor = WP.Lime.copy(alpha = 0.6f))
                                .clickable {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    if (WorkPulseRepository.updateAdminPassword(newPassword)) {
                                        toast(context, "Password updated")
                                        showPasswordSection = false
                                        newPassword = ""
                                    } else {
                                        toast(context, "Password must be at least 4 characters")
                                    }
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                        ) {
                            Text("Save", color = WP.Lime, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            }
        }

        // Worksite config
        GlassPanel(
            modifier = Modifier.fillMaxWidth().clickable {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                showWorksiteSection = !showWorksiteSection
            },
            cornerRadius = 18.dp,
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(40.dp).clip(CircleShape)
                        .glass(cornerRadius = 40.dp, fillTop = WP.Cyan.copy(alpha = 0.3f), fillBottom = WP.Cyan.copy(alpha = 0.08f)),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Filled.LocationOn, null, tint = WP.Cyan, modifier = Modifier.size(20.dp)) }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Worksite geofence", color = WP.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Text("${WorkPulseRepository.workSite.name} · ${WorkPulseRepository.workSite.radiusMeters.toInt()}m radius", color = WP.TextDim, fontSize = 12.sp)
                }
                Icon(Icons.Filled.Edit, null, tint = WP.TextSecondary, modifier = Modifier.size(18.dp))
            }

            AnimatedVisibility(visible = showWorksiteSection) {
                Column {
                    Spacer(Modifier.height(14.dp))
                    Box(Modifier.fillMaxWidth().height(1.dp).glass(cornerRadius = 1.dp))
                    Spacer(Modifier.height(14.dp))
                    ConfigField("Site name", siteName) { siteName = it }
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ConfigField("Latitude", siteLat, Modifier.weight(1f)) { siteLat = it }
                        ConfigField("Longitude", siteLon, Modifier.weight(1f)) { siteLon = it }
                    }
                    Spacer(Modifier.height(10.dp))
                    ConfigField("Radius (meters)", siteRadius) { siteRadius = it }
                    Spacer(Modifier.height(14.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .glass(cornerRadius = 14.dp, fillTop = WP.Cyan.copy(alpha = 0.3f), fillBottom = WP.Cyan.copy(alpha = 0.08f), borderColor = WP.Cyan.copy(alpha = 0.6f))
                            .clickable {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                val lat = siteLat.toDoubleOrNull() ?: WorkPulseRepository.workSite.center.latitude
                                val lon = siteLon.toDoubleOrNull() ?: WorkPulseRepository.workSite.center.longitude
                                val rad = siteRadius.toDoubleOrNull() ?: WorkPulseRepository.workSite.radiusMeters
                                WorkPulseRepository.updateWorkSite(siteName.ifBlank { WorkPulseRepository.workSite.name }, lat, lon, rad.coerceAtLeast(50.0))
                                toast(context, "Worksite updated")
                                showWorksiteSection = false
                            }
                            .padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("Save worksite", color = WP.Cyan, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
            }
        }

        // ── Account Section ──
        SectionTitle("Account")

        // Logout
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .glass(cornerRadius = 18.dp, fillTop = WP.Danger.copy(alpha = 0.18f), fillBottom = WP.Danger.copy(alpha = 0.04f), borderColor = WP.Danger.copy(alpha = 0.4f))
                .clickable {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLogout()
                }
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Logout, null, tint = WP.Danger, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Text("Log out of admin", color = WP.Danger, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun ServerConfigCard() {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val status by ServerSync.status.collectAsState()
    val savedUrl by ServerSync.serverUrl.collectAsState()
    var expanded by remember { mutableStateOf(false) }
    var address by remember { mutableStateOf(savedUrl?.removePrefix("http://")?.removePrefix("https://") ?: "") }

    val (dotColor, statusLabel) = when (status) {
        ConnectionStatus.CONNECTED -> WP.Lime to "Connected"
        ConnectionStatus.CONNECTING -> WP.Amber to "Connecting…"
        ConnectionStatus.OFFLINE -> WP.TextDim to "Offline · local mode"
    }

    GlassPanel(
        modifier = Modifier.fillMaxWidth().clickable {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            expanded = !expanded
        },
        cornerRadius = 18.dp,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape)
                    .glass(cornerRadius = 40.dp, fillTop = WP.Cyan.copy(alpha = 0.3f), fillBottom = WP.Cyan.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Filled.Dns, null, tint = WP.Cyan, modifier = Modifier.size(20.dp)) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("PC server connection", color = WP.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(8.dp).clip(CircleShape).glass(cornerRadius = 8.dp, fillTop = dotColor, fillBottom = dotColor))
                    Spacer(Modifier.width(6.dp))
                    Text(statusLabel, color = dotColor, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
            }
            Icon(Icons.Filled.Edit, null, tint = WP.TextSecondary, modifier = Modifier.size(18.dp))
        }

        AnimatedVisibility(visible = expanded) {
            Column {
                Spacer(Modifier.height(14.dp))
                Box(Modifier.fillMaxWidth().height(1.dp).glass(cornerRadius = 1.dp))
                Spacer(Modifier.height(14.dp))
                Text(
                    "Enter your PC's local IP and port. Run the server with \"bun start\" in the server folder, then use the address it prints (e.g. 192.168.1.20:8080).",
                    color = WP.TextDim, fontSize = 12.sp,
                )
                Spacer(Modifier.height(12.dp))
                ConfigField("Server address (IP:port)", address) { address = it }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .glass(cornerRadius = 14.dp, fillTop = WP.Cyan.copy(alpha = 0.3f), fillBottom = WP.Cyan.copy(alpha = 0.08f), borderColor = WP.Cyan.copy(alpha = 0.6f))
                            .clickable {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                ServerSync.setServer(address)
                                toast(context, "Connecting to $address…")
                                expanded = false
                            }
                            .padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center,
                    ) { Text("Connect", color = WP.Cyan, fontWeight = FontWeight.Bold, fontSize = 15.sp) }
                    Box(
                        modifier = Modifier
                            .glass(cornerRadius = 14.dp, fillTop = WP.Danger.copy(alpha = 0.18f), fillBottom = WP.Danger.copy(alpha = 0.04f), borderColor = WP.Danger.copy(alpha = 0.4f))
                            .clickable {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                ServerSync.disconnect()
                                address = ""
                                toast(context, "Disconnected — local mode")
                            }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        contentAlignment = Alignment.Center,
                    ) { Text("Disconnect", color = WP.Danger, fontWeight = FontWeight.Bold, fontSize = 15.sp) }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Spacer(Modifier.height(4.dp))
    Text(title, color = WP.TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
}

@Composable
private fun ExportButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    Box(
        modifier = modifier
            .glass(cornerRadius = 20.dp, fillTop = accent.copy(alpha = 0.16f), fillBottom = accent.copy(alpha = 0.04f), borderColor = accent.copy(alpha = 0.4f))
            .clickable {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            }
            .padding(14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = accent, modifier = Modifier.size(28.dp))
            Spacer(Modifier.height(8.dp))
            Text(label, color = accent, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun MonthChip(month: Int, year: Int, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .glass(cornerRadius = 16.dp, fillTop = WP.Purple.copy(alpha = 0.12f), fillBottom = WP.GlassBottom)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "${Format.monthName(month)} $year",
            color = WP.TextPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun ConfigField(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    onChange: (String) -> Unit,
) {
    Column(modifier) {
        Text(label, color = WP.TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Medium, letterSpacing = 1.sp)
        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .glass(cornerRadius = 14.dp, fillTop = WP.GlassTop, fillBottom = WP.GlassBottom, borderColor = WP.GlassBorder),
        ) {
            TextField(
                value = value,
                onValueChange = onChange,
                colors = TextFieldDefaults.colors(
                    focusedTextColor = WP.TextPrimary,
                    unfocusedTextColor = WP.TextPrimary,
                    cursorColor = WP.Purple,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                ),
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp),
            )
        }
    }
}

private fun toast(context: android.content.Context, message: String) {
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
}

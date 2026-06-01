package com.rork.workpulse.ui.screens.admin

import android.widget.Toast
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rork.workpulse.data.NewEmployeeRequest
import com.rork.workpulse.data.WorkPulseRepository
import com.rork.workpulse.ui.components.GlassPanel
import com.rork.workpulse.ui.theme.WP
import com.rork.workpulse.ui.theme.glass

/**
 * Admin screen for creating new employees and deleting existing ones.
 * Each employee gets a login Employee ID and temporary password assigned by the admin.
 */
@Composable
fun AdminEmployeeManagerScreen(
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val employees by WorkPulseRepository.employees.collectAsState()

    // ── Create form state ──
    var showCreate by remember { mutableStateOf(false) }
    var newEmployeeId by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var newName by remember { mutableStateOf("") }
    var newRole by remember { mutableStateOf("") }
    var newSalary by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }

    // ── Confirm delete state ──
    var deleteTargetId by remember { mutableStateOf<String?>(null) }
    var deleteTargetName by remember { mutableStateOf("") }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // ── Header ──
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .glass(cornerRadius = 42.dp, fillTop = WP.Purple.copy(alpha = 0.3f), fillBottom = WP.Purple.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Group, null, tint = WP.Purple, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text("Employee Management", color = WP.TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Text("${employees.size} registered employee${if (employees.size != 1) "s" else ""}", color = WP.TextSecondary, fontSize = 12.sp)
            }
        }

        // ── Add employee button ──
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .glass(cornerRadius = 18.dp, fillTop = WP.Lime.copy(alpha = 0.16f), fillBottom = WP.Lime.copy(alpha = 0.04f), borderColor = WP.Lime.copy(alpha = 0.4f))
                .clickable {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    showCreate = !showCreate
                }
                .padding(vertical = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.PersonAdd, null, tint = WP.Lime, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(10.dp))
                Text(
                    if (showCreate) "Cancel" else "Create New Employee",
                    color = WP.Lime,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                )
            }
        }

        // ── Create form ──
        AnimatedVisibility(
            visible = showCreate,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            GlassPanel(modifier = Modifier.fillMaxWidth(), cornerRadius = 20.dp, contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    AdminField("Employee ID (e.g. EMP006)", newEmployeeId) { newEmployeeId = it }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AdminField("Temporary Password", newPassword, Modifier.weight(1f)) { newPassword = it }
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .clickable { showPassword = !showPassword },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                if (showPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                null,
                                tint = WP.TextSecondary,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                    AdminField("Full Name", newName) { newName = it }
                    AdminField("Role / Designation", newRole) { newRole = it }
                    AdminField("Monthly Salary (₹)", newSalary, keyboardType = KeyboardType.Decimal) { newSalary = it }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .glass(cornerRadius = 14.dp, fillTop = WP.Lime.copy(alpha = 0.3f), fillBottom = WP.Lime.copy(alpha = 0.08f), borderColor = WP.Lime.copy(alpha = 0.6f))
                            .clickable {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                val salary = newSalary.toDoubleOrNull()
                                if (newEmployeeId.isBlank() || newPassword.length < 4 || newName.isBlank() || newRole.isBlank() || salary == null || salary <= 0) {
                                    Toast.makeText(context, "Fill all fields correctly. Password must be 4+ chars.", Toast.LENGTH_SHORT).show()
                                    return@clickable
                                }
                                val result = WorkPulseRepository.createEmployee(
                                    NewEmployeeRequest(
                                        employeeId = newEmployeeId.trim(),
                                        password = newPassword,
                                        name = newName.trim(),
                                        role = newRole.trim(),
                                        baseSalary = salary,
                                    )
                                )
                                if (result != null) {
                                    Toast.makeText(context, "${newName.trim()} created! ID: ${newEmployeeId.trim()}", Toast.LENGTH_SHORT).show()
                                    newEmployeeId = ""; newPassword = ""; newName = ""; newRole = ""; newSalary = ""
                                    showCreate = false
                                } else {
                                    Toast.makeText(context, "Employee ID already taken or invalid data", Toast.LENGTH_SHORT).show()
                                }
                            }
                            .padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("Create Employee", color = WP.Lime, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
            }
        }

        // ── Employee list ──
        employees.forEach { emp ->
            GlassPanel(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 16.dp,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(14.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .glass(cornerRadius = 40.dp, fillTop = Color(emp.avatarColor).copy(alpha = 0.4f), fillBottom = Color(emp.avatarColor).copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(emp.name.first().toString(), color = WP.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(emp.name, color = WP.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text("${emp.employeeId} · ${emp.role}", color = WP.TextSecondary, fontSize = 12.sp)
                        Text("Salary: ₹${emp.baseSalary.toInt()}", color = WP.TextDim, fontSize = 11.sp)
                    }
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .glass(cornerRadius = 40.dp, fillTop = WP.Danger.copy(alpha = 0.25f), fillBottom = WP.Danger.copy(alpha = 0.06f), borderColor = WP.Danger.copy(alpha = 0.4f))
                            .clickable {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                deleteTargetId = emp.id
                                deleteTargetName = emp.name
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Filled.Delete, null, tint = WP.Danger, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }

    // ── Delete confirmation dialog ──
    if (deleteTargetId != null) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { deleteTargetId = null },
            containerColor = WP.BgRaise,
            title = {
                Text("Delete Employee", color = WP.TextPrimary, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    "Are you sure you want to delete $deleteTargetName?\n\nThis will also remove all their attendance logs and leave records. This action cannot be undone.",
                    color = WP.TextSecondary,
                    fontSize = 14.sp,
                )
            },
            confirmButton = {
                Box(
                    modifier = Modifier
                        .glass(cornerRadius = 12.dp, fillTop = WP.Danger.copy(alpha = 0.25f), fillBottom = WP.Danger.copy(alpha = 0.06f), borderColor = WP.Danger.copy(alpha = 0.5f))
                        .clickable {
                            WorkPulseRepository.deleteEmployee(deleteTargetId!!)
                            Toast.makeText(context, "$deleteTargetName deleted", Toast.LENGTH_SHORT).show()
                            deleteTargetId = null
                        }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                ) {
                    Text("Delete", color = WP.Danger, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            },
            dismissButton = {
                Box(
                    modifier = Modifier
                        .glass(cornerRadius = 12.dp)
                        .clickable { deleteTargetId = null }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                ) {
                    Text("Cancel", color = WP.TextSecondary, fontSize = 14.sp)
                }
            },
        )
    }
}

@Composable
private fun AdminField(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    onChange: (String) -> Unit,
) {
    Column(modifier) {
        Text(label, color = WP.TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.5.sp)
        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .glass(cornerRadius = 14.dp, fillTop = WP.GlassTop, fillBottom = WP.GlassBottom, borderColor = WP.GlassBorder),
        ) {
            TextField(
                value = value,
                onValueChange = onChange,
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp),
                colors = TextFieldDefaults.colors(
                    focusedTextColor = WP.TextPrimary,
                    unfocusedTextColor = WP.TextPrimary,
                    cursorColor = WP.Purple,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                ),
            )
        }
    }
}

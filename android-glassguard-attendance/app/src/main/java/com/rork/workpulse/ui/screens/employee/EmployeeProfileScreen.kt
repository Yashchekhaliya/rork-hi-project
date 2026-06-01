package com.rork.workpulse.ui.screens.employee

import android.widget.Toast
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
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rork.workpulse.data.WorkPulseRepository
import com.rork.workpulse.ui.components.GlassPanel
import com.rork.workpulse.ui.theme.WP
import com.rork.workpulse.ui.theme.glass

/**
 * Employee profile & security screen. Employees can view their info
 * and change their password. Also provides logout.
 */
@Composable
fun EmployeeProfileScreen(
    onLogout: () -> Unit,
    onToggleImmersive: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val employeeId = WorkPulseRepository.requireLoggedEmployeeId()
    val employees by WorkPulseRepository.employees.collectAsState()
    val emp = employees.firstOrNull { it.id == employeeId } ?: return

    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showCurrent by remember { mutableStateOf(false) }
    var showNew by remember { mutableStateOf(false) }
    var showConfirm by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Spacer(Modifier.height(8.dp))
        Text("My Profile", color = WP.TextPrimary, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Text("Manage your account & security", color = WP.TextSecondary, fontSize = 14.sp)

        // Employee info card
        GlassPanel(modifier = Modifier.fillMaxWidth(), cornerRadius = 20.dp, contentPadding = androidx.compose.foundation.layout.PaddingValues(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .glass(cornerRadius = 56.dp, fillTop = Color(emp.avatarColor).copy(alpha = 0.4f), fillBottom = Color(emp.avatarColor).copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(emp.name.first().toString(), color = WP.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(emp.name, color = WP.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text(emp.role, color = WP.TextSecondary, fontSize = 13.sp)
                    Spacer(Modifier.height(4.dp))
                    Text("ID: ${emp.employeeId}", color = WP.Cyan, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
            }
        }

        // Change password section
        GlassPanel(modifier = Modifier.fillMaxWidth(), cornerRadius = 20.dp, contentPadding = androidx.compose.foundation.layout.PaddingValues(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(40.dp).clip(CircleShape)
                        .glass(cornerRadius = 40.dp, fillTop = WP.Cyan.copy(alpha = 0.3f), fillBottom = WP.Cyan.copy(alpha = 0.08f)),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Filled.Lock, null, tint = WP.Cyan, modifier = Modifier.size(20.dp)) }
                Spacer(Modifier.width(12.dp))
                Text("Change Password", color = WP.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            }

            Spacer(Modifier.height(16.dp))

            // Current password
            PasswordField(
                label = "Current password",
                value = currentPassword,
                onValueChange = { currentPassword = it },
                showPassword = showCurrent,
                onToggleVisibility = { showCurrent = !showCurrent },
            )
            Spacer(Modifier.height(10.dp))

            // New password
            PasswordField(
                label = "New password (min 4 chars)",
                value = newPassword,
                onValueChange = { newPassword = it },
                showPassword = showNew,
                onToggleVisibility = { showNew = !showNew },
            )
            Spacer(Modifier.height(10.dp))

            // Confirm new password
            PasswordField(
                label = "Confirm new password",
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                showPassword = showConfirm,
                onToggleVisibility = { showConfirm = !showConfirm },
            )

            Spacer(Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .glass(cornerRadius = 14.dp, fillTop = WP.Lime.copy(alpha = 0.3f), fillBottom = WP.Lime.copy(alpha = 0.08f), borderColor = WP.Lime.copy(alpha = 0.6f))
                    .clickable {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        when {
                            currentPassword.isBlank() || newPassword.isBlank() || confirmPassword.isBlank() -> {
                                Toast.makeText(context, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                                return@clickable
                            }
                            newPassword != confirmPassword -> {
                                Toast.makeText(context, "New password and confirm password do not match", Toast.LENGTH_SHORT).show()
                                return@clickable
                            }
                        }
                        val ok = WorkPulseRepository.changeEmployeePassword(emp.employeeId, currentPassword, newPassword)
                        if (ok) {
                            Toast.makeText(context, "Password changed successfully", Toast.LENGTH_SHORT).show()
                            currentPassword = ""
                            newPassword = ""
                            confirmPassword = ""
                        } else {
                            Toast.makeText(context, "Current password is wrong or new password too short", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("Update Password", color = WP.Lime, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }

        // Logout
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .glass(cornerRadius = 18.dp, fillTop = WP.Danger.copy(alpha = 0.18f), fillBottom = WP.Danger.copy(alpha = 0.04f), borderColor = WP.Danger.copy(alpha = 0.4f))
                .clickable {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    WorkPulseRepository.logoutEmployee()
                    onLogout()
                }
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Logout, null, tint = WP.Danger, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Text("Sign Out", color = WP.Danger, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun PasswordField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    showPassword: Boolean,
    onToggleVisibility: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .glass(cornerRadius = 16.dp, borderColor = WP.GlassBorder),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Spacer(Modifier.width(12.dp))
            Icon(Icons.Filled.Lock, null, tint = WP.TextSecondary, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            TextField(
                value = value,
                onValueChange = onValueChange,
                placeholder = { Text(label, color = WP.TextDim, fontSize = 14.sp) },
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedTextColor = WP.TextPrimary,
                    unfocusedTextColor = WP.TextPrimary,
                    cursorColor = WP.Cyan,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                ),
                modifier = Modifier.weight(1f),
            )
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable { onToggleVisibility() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (showPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                    null,
                    tint = WP.TextSecondary,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

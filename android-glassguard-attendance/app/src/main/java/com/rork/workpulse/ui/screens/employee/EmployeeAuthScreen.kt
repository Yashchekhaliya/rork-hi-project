package com.rork.workpulse.ui.screens.employee

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rork.workpulse.data.WorkPulseRepository
import com.rork.workpulse.ui.components.MeshBackground
import com.rork.workpulse.ui.theme.WP
import com.rork.workpulse.ui.theme.glass
import com.rork.workpulse.ui.theme.neonGlass

/**
 * Employee login gate. Employee enters their assigned Employee ID & password.
 * On first login, the internal id is stored. Only after successful auth
 * does the employee shell appear.
 */
@Composable
fun EmployeeAuthScreen(
    onAuthenticated: (internalId: String) -> Unit,
    onBack: () -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    var employeeId by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    Box(Modifier.fillMaxSize()) {
        MeshBackground()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // Back button
            Row(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .glass(cornerRadius = 44.dp)
                        .clickable { onBack() },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.ArrowBack, null, tint = WP.TextSecondary, modifier = Modifier.size(22.dp))
                }
            }

            Spacer(Modifier.height(36.dp))

            // Biometric-style pulse icon
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .neonGlass(WP.Cyan, 100.dp),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Fingerprint, null, tint = WP.Cyan, modifier = Modifier.size(44.dp))
            }

            Spacer(Modifier.height(28.dp))
            Text("Employee Login", color = WP.TextPrimary, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text("Enter your credentials to check in", color = WP.TextSecondary, fontSize = 14.sp)

            Spacer(Modifier.height(32.dp))

            // Employee ID field
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .glass(cornerRadius = 20.dp, borderColor = WP.GlassBorder)
                    .padding(4.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Spacer(Modifier.width(12.dp))
                    Icon(Icons.Filled.Badge, null, tint = WP.Cyan, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    TextField(
                        value = employeeId,
                        onValueChange = {
                            employeeId = it
                            if (error != null) error = null
                        },
                        placeholder = {
                            Text("Employee ID (e.g. EMP001)", color = WP.TextDim, fontSize = 15.sp)
                        },
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
                }
            }

            Spacer(Modifier.height(12.dp))

            // Password field
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .glass(cornerRadius = 20.dp, borderColor = if (error != null) WP.Danger.copy(alpha = 0.6f) else WP.GlassBorder)
                    .padding(4.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Spacer(Modifier.width(12.dp))
                    Icon(Icons.Filled.Lock, null, tint = if (error != null) WP.Danger else WP.Cyan, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    TextField(
                        value = password,
                        onValueChange = {
                            password = it
                            if (error != null) error = null
                        },
                        placeholder = {
                            Text("Password", color = WP.TextDim, fontSize = 15.sp)
                        },
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
            }

            // Error message
            AnimatedVisibility(
                visible = error != null,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut(),
            ) {
                Text(
                    error ?: "",
                    color = WP.Danger,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 10.dp),
                    textAlign = TextAlign.Center,
                )
            }

            Spacer(Modifier.height(24.dp))

            // Login button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .neonGlass(WP.Cyan, 20.dp)
                    .clickable {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        if (employeeId.isBlank() || password.isBlank()) {
                            error = "Please fill in both fields"
                            return@clickable
                        }
                        val internalId = WorkPulseRepository.verifyEmployeeCredentials(employeeId.trim(), password)
                        if (internalId != null) {
                            WorkPulseRepository.setLoggedInEmployee(internalId)
                            onAuthenticated(internalId)
                        } else {
                            error = "Invalid employee ID or password"
                            password = ""
                        }
                    }
                    .padding(vertical = 18.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("Sign In", color = WP.TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

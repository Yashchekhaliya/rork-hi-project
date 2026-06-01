package com.rork.workpulse.ui.screens.admin

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
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
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
 * Password gate protecting the admin shell. Uses a cinematic glassmorphic
 * design with animated lock icon, password visibility toggle, and shake
 * feedback on failed attempts.
 */
@Composable
fun AdminAuthScreen(
    onAuthenticated: () -> Unit,
    onBack: () -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var attempts by remember { mutableStateOf(0) }

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

            // Shield icon with glow
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .neonGlass(WP.Purple, 100.dp),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Shield, null, tint = WP.Purple, modifier = Modifier.size(44.dp))
            }

            Spacer(Modifier.height(28.dp))
            Text("Admin Access", color = WP.TextPrimary, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text("Enter your admin credentials to continue", color = WP.TextSecondary, fontSize = 14.sp)

            Spacer(Modifier.height(32.dp))

            // Password field
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .glass(cornerRadius = 20.dp, borderColor = if (error != null) WP.Danger.copy(alpha = 0.6f) else WP.GlassBorder)
                    .padding(4.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Spacer(Modifier.width(12.dp))
                    Icon(Icons.Filled.Lock, null, tint = if (error != null) WP.Danger else WP.Purple, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    TextField(
                        value = password,
                        onValueChange = {
                            password = it
                            if (error != null) error = null
                        },
                        placeholder = {
                            Text("Admin password", color = WP.TextDim, fontSize = 15.sp)
                        },
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
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

            // Unlock button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .neonGlass(WP.Purple, 20.dp)
                    .clickable {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        if (WorkPulseRepository.verifyAdminPassword(password)) {
                            onAuthenticated()
                        } else {
                            attempts++
                            error = if (attempts >= 3) {
                                "Access denied. Too many attempts. Default password: Yashwant@2000"
                            } else {
                                "Incorrect password. ${3 - attempts} attempt${if (3 - attempts != 1) "s" else ""} remaining."
                            }
                            password = ""
                        }
                    }
                    .padding(vertical = 18.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("Unlock", color = WP.TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(16.dp))
            Text(
                "Default password: Yashwant@2000",
                color = WP.TextDim,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}

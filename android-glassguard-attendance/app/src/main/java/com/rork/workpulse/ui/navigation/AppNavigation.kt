package com.rork.workpulse.ui.navigation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SpaceDashboard
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.rork.workpulse.data.Role
import com.rork.workpulse.ui.screens.RoleSelectScreen
import com.rork.workpulse.ui.screens.admin.AdminAuthScreen
import com.rork.workpulse.ui.screens.admin.AdminDashboardScreen
import com.rork.workpulse.ui.screens.admin.AdminSettingsScreen
import com.rork.workpulse.ui.screens.admin.LeaveApprovalScreen
import com.rork.workpulse.ui.screens.admin.PayrollScreen
import com.rork.workpulse.ui.screens.employee.DashboardScreen
import com.rork.workpulse.ui.screens.employee.EmployeeAuthScreen
import com.rork.workpulse.ui.screens.employee.EmployeeProfileScreen
import com.rork.workpulse.ui.screens.employee.LeaveScreen
import com.rork.workpulse.ui.screens.employee.SalaryScreen
import com.rork.workpulse.ui.theme.WP

/** Represents the app-level navigation state. */
private sealed class AppScreen {
    data object RoleSelect : AppScreen()
    data object AdminAuth : AppScreen()
    data object EmployeeAuth : AppScreen()
    data object EmployeeShell : AppScreen()
    data object AdminShell : AppScreen()
}

@Composable
fun AppNavigation(
    onToggleImmersive: (Boolean) -> Unit = {},
) {
    var screen by remember { mutableStateOf<AppScreen>(AppScreen.RoleSelect) }

    AnimatedContent(
        targetState = screen,
        transitionSpec = {
            (fadeIn(tween(500)) + scaleIn(tween(500), initialScale = 0.96f))
                .togetherWith(fadeOut(tween(300)))
        },
        label = "rootNav",
    ) { current ->
        when (current) {
            AppScreen.RoleSelect -> RoleSelectScreen(
                onSelect = { role ->
                    screen = when (role) {
                        Role.EMPLOYEE -> AppScreen.EmployeeAuth
                        Role.ADMIN -> AppScreen.AdminAuth
                    }
                },
            )
            AppScreen.AdminAuth -> AdminAuthScreen(
                onAuthenticated = { screen = AppScreen.AdminShell },
                onBack = { screen = AppScreen.RoleSelect },
            )
            AppScreen.EmployeeAuth -> EmployeeAuthScreen(
                onAuthenticated = { _ -> screen = AppScreen.EmployeeShell },
                onBack = { screen = AppScreen.RoleSelect },
            )
            AppScreen.EmployeeShell -> EmployeeShell(
                onLogout = { screen = AppScreen.RoleSelect },
                onToggleImmersive = onToggleImmersive,
            )
            AppScreen.AdminShell -> AdminShell(
                onLogout = { screen = AppScreen.RoleSelect },
                onToggleImmersive = onToggleImmersive,
            )
        }
    }
}

@Composable
private fun EmployeeShell(
    onLogout: () -> Unit,
    onToggleImmersive: (Boolean) -> Unit,
) {
    val tabs = listOf(
        TabItem("Pulse", Icons.Filled.Dashboard),
        TabItem("Leave", Icons.Filled.CalendarMonth),
        TabItem("Salary", Icons.Filled.Payments),
        TabItem("Profile", Icons.Filled.Person),
    )
    MainShell(tabs = tabs, accent = WP.Cyan) { index ->
        ScreenFade(index) {
            when (index) {
                0 -> DashboardScreen()
                1 -> LeaveScreen()
                2 -> SalaryScreen()
                else -> EmployeeProfileScreen(
                    onLogout = onLogout,
                    onToggleImmersive = onToggleImmersive,
                )
            }
        }
    }
}

@Composable
private fun AdminShell(
    onLogout: () -> Unit,
    onToggleImmersive: (Boolean) -> Unit,
) {
    val tabs = listOf(
        TabItem("Live", Icons.Filled.SpaceDashboard),
        TabItem("Approvals", Icons.Filled.FactCheck),
        TabItem("Payroll", Icons.Filled.Payments),
        TabItem("More", Icons.Filled.Settings),
    )
    MainShell(tabs = tabs, accent = WP.Purple) { index ->
        ScreenFade(index) {
            when (index) {
                0 -> AdminDashboardScreen()
                1 -> LeaveApprovalScreen()
                2 -> PayrollScreen()
                else -> AdminSettingsScreen(
                    onLogout = onLogout,
                    onToggleImmersive = onToggleImmersive,
                )
            }
        }
    }
}

@Composable
private fun ScreenFade(key: Int, content: @Composable () -> Unit) {
    AnimatedContent(
        targetState = key,
        transitionSpec = {
            (fadeIn(tween(350)) + scaleIn(tween(350), initialScale = 0.98f))
                .togetherWith(fadeOut(tween(180)))
        },
        label = "screenFade",
    ) { _ -> content() }
}

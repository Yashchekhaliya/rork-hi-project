package com.rork.workpulse.ui.navigation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rork.workpulse.ui.components.MeshBackground
import com.rork.workpulse.ui.theme.WP
import com.rork.workpulse.ui.theme.glass

data class TabItem(val label: String, val icon: ImageVector)

/**
 * Shared shell: living mesh background, a swappable content area, and a floating
 * glass bottom navigation bar with an animated neon selection.
 */
@Composable
fun MainShell(
    tabs: List<TabItem>,
    accent: Color,
    content: @Composable (Int) -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    var selected by remember { mutableIntStateOf(0) }
    // Combine status-bar + display-cutout insets so content never hides behind a notch,
    // punch-hole camera, or dynamic island.
    val topInset = WindowInsets.statusBars
        .union(WindowInsets.displayCutout)
        .asPaddingValues()
        .calculateTopPadding()
    val bottomInset = WindowInsets.navigationBars
        .union(WindowInsets.displayCutout)
        .asPaddingValues()
        .calculateBottomPadding()

    Box(Modifier.fillMaxSize()) {
        MeshBackground()
        Column(
            Modifier
                .fillMaxSize()
                .padding(top = topInset),
        ) {
            Box(Modifier.weight(1f)) {
                content(selected)
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = bottomInset + 12.dp)
                    .glass(cornerRadius = 26.dp)
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                tabs.forEachIndexed { index, tab ->
                    val isSelected = index == selected
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(androidx.compose.foundation.shape.RoundedCornerShape(20.dp))
                            .then(
                                if (isSelected) Modifier.glass(
                                    cornerRadius = 20.dp,
                                    fillTop = accent.copy(alpha = 0.28f),
                                    fillBottom = accent.copy(alpha = 0.06f),
                                    borderColor = accent.copy(alpha = 0.5f),
                                ) else Modifier
                            )
                            .clickable {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                selected = index
                            }
                            .padding(vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(
                            tab.icon, null,
                            tint = if (isSelected) accent else WP.TextSecondary,
                            modifier = Modifier.size(22.dp),
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            tab.label,
                            color = if (isSelected) accent else WP.TextSecondary,
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        )
                    }
                }
            }
        }
    }
}

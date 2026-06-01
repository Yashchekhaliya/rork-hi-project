package com.rork.workpulse.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rork.workpulse.ui.theme.WP
import com.rork.workpulse.ui.theme.glass

/** Standard frosted-glass panel used everywhere in the app. */
@Composable
fun GlassPanel(
    modifier: Modifier = Modifier,
    cornerRadius: androidx.compose.ui.unit.Dp = 24.dp,
    contentPadding: PaddingValues = PaddingValues(20.dp),
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .glass(cornerRadius = cornerRadius)
            .padding(contentPadding),
    ) { content() }
}

/** A small uppercase neon pill used for statuses and tags. */
@Composable
fun StatusPill(
    text: String,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .glass(cornerRadius = 999.dp, fillTop = accent.copy(alpha = 0.22f), fillBottom = accent.copy(alpha = 0.08f), borderColor = accent.copy(alpha = 0.5f))
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(
            text = text.uppercase(),
            color = accent,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
        )
    }
}

/** Section heading used at the top of stacked card groups. */
@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        modifier = modifier,
        color = WP.TextPrimary,
        fontSize = 18.sp,
        fontWeight = FontWeight.SemiBold,
    )
}

package com.pixstop.mobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pixstop.mobile.ui.theme.PixColors
import com.pixstop.mobile.ui.theme.PixTypography

/**
 * Divider retro com texto centralizado ("OU" por padrão).
 *
 * ─────────── OU ───────────
 */
@Composable
fun PixelDivider(
    modifier: Modifier = Modifier,
    text: String = "OU"
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(2.dp)
                .background(PixColors.Gray700)
        )
        Text(
            text = text,
            style = PixTypography.badgeText.copy(color = PixColors.Gray500),
            modifier = Modifier
                .background(PixColors.Gray900)
                .padding(horizontal = 8.dp)
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(2.dp)
                .background(PixColors.Gray700)
        )
    }
}


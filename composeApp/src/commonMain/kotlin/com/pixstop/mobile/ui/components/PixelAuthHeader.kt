package com.pixstop.mobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.pixstop.mobile.ui.theme.PixColors
import com.pixstop.mobile.ui.theme.PixTypography

/**
 * Header de autenticação com ícone colorido, glow, título e subtítulo.
 */
@Composable
fun PixelAuthHeader(
    icon: AppIconType,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    themeColor: Color = PixColors.Cyan
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Icon container 64x64 with border, bg and glow
        Box(
            modifier = Modifier
                .neonGlow(color = themeColor, radius = 12.dp, alpha = 0.3f)
                .size(64.dp)
                .background(themeColor)
                .drawBehind {
                    // 4dp border
                    val bw = 4.dp.toPx()
                    drawRect(themeColor, Offset.Zero, Size(size.width, bw))
                    drawRect(themeColor, Offset(0f, size.height - bw), Size(size.width, bw))
                    drawRect(themeColor, Offset.Zero, Size(bw, size.height))
                    drawRect(themeColor, Offset(size.width - bw, 0f), Size(bw, size.height))
                },
            contentAlignment = Alignment.Center
        ) {
            AppIcon(
                icon = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = PixColors.Dark
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Title (Press Start 2P)
        Text(
            text = title.uppercase(),
            style = PixTypography.authTitle.copy(color = themeColor)
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Subtitle (Inter)
        Text(
            text = subtitle,
            style = PixTypography.bodySecondary
        )
    }
}


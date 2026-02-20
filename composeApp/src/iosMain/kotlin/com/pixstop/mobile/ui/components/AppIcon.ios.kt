package com.pixstop.mobile.ui.components

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp

/**
 * Implementação iOS usando emojis como fallback
 *
 * Nota: Para usar SF Symbols nativos, seria necessário
 * UIKitView que tem limitações no Compose Multiplatform.
 * Esta implementação usa emojis que funcionam em todas as versões.
 */
@Composable
actual fun AppIcon(
    icon: AppIconType,
    contentDescription: String?,
    modifier: Modifier,
    tint: Color?
) {
    val emoji = when (icon) {
        // Navigation
        AppIconType.Menu -> "☰"
        AppIconType.Home -> "🏠"
        AppIconType.Search -> "🔍"
        AppIconType.Notifications -> "🔔"
        AppIconType.Settings -> "⚙️"

        // User
        AppIconType.Person -> "👤"
        AppIconType.Logout -> "🚪"

        // Auth
        AppIconType.Lock -> "🔐"
        AppIconType.Visibility -> "👁️"
        AppIconType.VisibilityOff -> "🙈"

        // Actions
        AppIconType.Refresh -> "🔄"
        AppIconType.Add -> "➕"
        AppIconType.Edit -> "✏️"
        AppIconType.Delete -> "🗑️"
        AppIconType.Share -> "📤"

        // Status
        AppIconType.Check -> "✓"
        AppIconType.Close -> "✕"
        AppIconType.Info -> "ℹ️"
        AppIconType.Warning -> "⚠️"
    }

    Text(
        text = emoji,
        modifier = modifier,
        fontSize = 24.sp,
        color = tint ?: LocalContentColor.current
    )
}

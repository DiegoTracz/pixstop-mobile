package com.pixstop.mobile.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/**
 * Implementação Android usando Material Icons
 */
@Composable
actual fun AppIcon(
    icon: AppIconType,
    contentDescription: String?,
    modifier: Modifier,
    tint: Color?
) {
    val imageVector = when (icon) {
        // Navigation
        AppIconType.Menu -> Icons.Filled.Menu
        AppIconType.Home -> Icons.Filled.Home
        AppIconType.Search -> Icons.Filled.Search
        AppIconType.Notifications -> Icons.Filled.Notifications
        AppIconType.Settings -> Icons.Filled.Settings

        // User
        AppIconType.Person -> Icons.Filled.Person
        AppIconType.Logout -> Icons.AutoMirrored.Filled.Logout

        // Auth
        AppIconType.Lock -> Icons.Filled.Lock
        AppIconType.Visibility -> Icons.Filled.Visibility
        AppIconType.VisibilityOff -> Icons.Filled.VisibilityOff

        // Actions
        AppIconType.Refresh -> Icons.Filled.Refresh
        AppIconType.Add -> Icons.Filled.Add
        AppIconType.Edit -> Icons.Filled.Edit
        AppIconType.Delete -> Icons.Filled.Delete
        AppIconType.Share -> Icons.Filled.Share

        // Status
        AppIconType.Check -> Icons.Filled.Check
        AppIconType.Close -> Icons.Filled.Close
        AppIconType.Info -> Icons.Filled.Info
        AppIconType.Warning -> Icons.Filled.Warning
    }

    Icon(
        imageVector = imageVector,
        contentDescription = contentDescription,
        modifier = modifier,
        tint = tint ?: LocalContentColor.current
    )
}

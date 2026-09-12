package com.pixstop.mobile.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Componente de ícone multiplataforma.
 *
 * No Android: usa Material Icons
 * No iOS: usa emoji/texto como fallback
 */
@Composable
expect fun AppIcon(
    icon: AppIconType,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: androidx.compose.ui.graphics.Color? = null
)

/**
 * Tipos de ícones disponíveis no app
 *
 * Adicione novos ícones aqui e implemente em:
 * - androidMain/AppIcon.android.kt (Material Icons)
 * - iosMain/AppIcon.ios.kt (SF Symbols)
 */
enum class AppIconType {
    // Navigation
    Menu,
    Home,
    Search,
    Notifications,
    Settings,

    // User
    Person,
    PersonAdd,
    Logout,

    // Auth
    Lock,
    /** Cadeado aberto: a porta da geladeira destravada, e o contrário do Lock. */
    LockOpen,
    Email,
    Visibility,
    VisibilityOff,

    // Actions
    Refresh,
    Add,
    Edit,
    Delete,
    Share,
    QrCodeScanner,
    Camera,
    /** A geladeira na rede, e a mesma sem rede. */
    Wifi,
    WifiOff,
    /** A fita LED acesa. */
    Bulb,
    /** O que há dentro da geladeira. */
    Box,
    CameraSwitch,
    Remove,

    // Loja
    Store,
    Cart,
    CartAdd,

    // Status
    Check,
    Close,
    Info,
    Warning,

    // Navigation extras
    ChevronDown,
    ChevronUp,
    ArrowBack
}

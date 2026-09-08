package com.pixstop.mobile.core.storage

import com.pixstop.mobile.ui.theme.AppThemeMode
import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Claro ou escuro, lembrado entre aberturas.
 *
 * Fica fora da sessão de propósito: sair da conta não devolve a pessoa a um
 * aplicativo que ela não escolheu.
 */
class ThemeStore(private val settings: Settings) {

    private val _mode = MutableStateFlow(AppThemeMode.fromStored(settings.getStringOrNull(KEY)))
    val mode: StateFlow<AppThemeMode> = _mode.asStateFlow()

    fun set(mode: AppThemeMode) {
        settings.putString(KEY, mode.name)
        _mode.value = mode
    }

    private companion object {
        const val KEY = "app_theme_mode"
    }
}

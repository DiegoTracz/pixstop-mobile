package com.pixstop.mobile.core.di

import com.russhwolf.settings.ExperimentalSettingsImplementation
import com.russhwolf.settings.KeychainSettings
import com.russhwolf.settings.Settings
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module

/**
 * O Keychain sobrevive à reinstalação e não entra em backup em texto claro.
 */
@OptIn(ExperimentalSettingsImplementation::class)
import com.pixstop.mobile.domain.access.FridgeNetworkConnector
import com.pixstop.mobile.domain.access.ManualFridgeNetworkConnector

actual val platformModule: Module = module {
    // No iOS a troca de rede exige NEHotspotConfiguration; até lá a pessoa entra à mão.
    single<FridgeNetworkConnector> { ManualFridgeNetworkConnector() }
    single<Settings>(named(SECURE_SETTINGS)) { KeychainSettings(service = KEYCHAIN_SERVICE) }
}

private const val KEYCHAIN_SERVICE = "com.pixstop.mobile"

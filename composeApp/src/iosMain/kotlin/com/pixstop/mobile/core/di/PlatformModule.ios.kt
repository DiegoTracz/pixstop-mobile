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
actual val platformModule: Module = module {
    single<Settings>(named(SECURE_SETTINGS)) { KeychainSettings(service = KEYCHAIN_SERVICE) }
}

private const val KEYCHAIN_SERVICE = "com.pixstop.mobile"

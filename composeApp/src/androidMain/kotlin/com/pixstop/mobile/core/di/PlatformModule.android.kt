package com.pixstop.mobile.core.di

import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.pixstop.mobile.domain.access.AndroidFridgeNetworkConnector
import com.pixstop.mobile.domain.access.FridgeNetworkConnector
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module

/**
 * A chave mestra fica no Android Keystore, que é respaldado por hardware nos
 * aparelhos que o têm. O arquivo em disco guarda só o texto cifrado.
 */
actual val platformModule: Module = module {
    single<Settings>(named(SECURE_SETTINGS)) {
        val context = androidContext()

        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        val preferences = EncryptedSharedPreferences.create(
            context,
            SECURE_FILE,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )

        SharedPreferencesSettings(preferences)
    }

    // Entrar na rede da geladeira (Fase 9.3): só o Android 10+ deixa o app fazer isso.
    single<FridgeNetworkConnector> { AndroidFridgeNetworkConnector(androidContext()) }
}

/** Arquivo próprio: o cache do perfil continua no armazenamento comum. */
private const val SECURE_FILE = "pixstop_secure"

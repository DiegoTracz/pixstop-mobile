package com.pixstop.mobile.core.di

import org.koin.core.module.Module

/**
 * Qualificador do armazenamento cifrado.
 *
 * Existem dois armazenamentos: este, para o token, e o comum, para o cache do
 * perfil. Separá-los é o que permite guardar a credencial com a criptografia
 * do sistema sem pagar esse custo em tudo o mais.
 */
const val SECURE_SETTINGS = "secure-settings"

/**
 * O que só cada plataforma sabe montar.
 *
 * Hoje é o armazenamento do token: `EncryptedSharedPreferences` no Android,
 * Keychain no iOS. Nos dois casos a chave fica sob a guarda do sistema, fora
 * do alcance de outro aplicativo e de um backup em texto claro.
 */
expect val platformModule: Module

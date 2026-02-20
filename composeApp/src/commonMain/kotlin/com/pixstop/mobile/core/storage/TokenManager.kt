package com.pixstop.mobile.core.storage

import com.russhwolf.settings.Settings
import com.russhwolf.settings.set
import com.russhwolf.settings.get
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import com.pixstop.mobile.data.model.CachedUserData

/**
 * Gerenciador de token de autenticação e cache de dados do usuário.
 * Usa multiplatform-settings para persistência cross-platform.
 */
class TokenManager(private val settings: Settings = Settings()) {

    companion object {
        private const val KEY_TOKEN = "auth_token"
        private const val KEY_USER_DATA = "cached_user_data"
    }

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    /**
     * Salva o token de autenticação
     */
    fun saveToken(token: String) {
        settings[KEY_TOKEN] = token
    }

    /**
     * Recupera o token salvo
     */
    fun getToken(): String? {
        return settings.getStringOrNull(KEY_TOKEN)
    }

    /**
     * Verifica se existe um token salvo
     */
    fun hasToken(): Boolean {
        return getToken() != null
    }

    /**
     * Remove o token (logout)
     */
    fun clearToken() {
        settings.remove(KEY_TOKEN)
    }

    /**
     * Salva dados do usuário para cache offline
     */
    fun saveUserData(userData: CachedUserData) {
        val dataWithTimestamp = userData.copy(lastUpdated = currentTimeMillis())
        settings[KEY_USER_DATA] = json.encodeToString(dataWithTimestamp)
    }

    /**
     * Recupera dados do usuário do cache
     */
    fun getUserData(): CachedUserData? {
        val jsonString = settings.getStringOrNull(KEY_USER_DATA) ?: return null
        return try {
            json.decodeFromString<CachedUserData>(jsonString)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Remove dados do usuário do cache
     */
    fun clearUserData() {
        settings.remove(KEY_USER_DATA)
    }

    /**
     * Limpa todos os dados (logout completo)
     */
    fun clearAll() {
        clearToken()
        clearUserData()
    }

    /**
     * Verifica se os dados em cache são recentes (menos de 24 horas)
     */
    fun isCacheValid(): Boolean {
        val userData = getUserData() ?: return false
        val cacheAge = currentTimeMillis() - userData.lastUpdated
        val maxAge = 24 * 60 * 60 * 1000L // 24 horas em milissegundos
        return cacheAge < maxAge
    }
}

/**
 * Função expect para obter o timestamp atual em milissegundos
 */
expect fun currentTimeMillis(): Long

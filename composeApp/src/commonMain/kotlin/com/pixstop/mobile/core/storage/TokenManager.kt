package com.pixstop.mobile.core.storage

import com.russhwolf.settings.Settings
import com.russhwolf.settings.set
import com.russhwolf.settings.get
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import com.pixstop.mobile.data.model.CachedUserData

/**
 * Token de autenticação e cache do perfil.
 *
 * São dois armazenamentos de propósito. O token é credencial e vai no
 * `secure` — cifrado pelo sistema, fora do alcance de outro aplicativo e de um
 * backup em texto claro. O cache do perfil é conveniência e fica no comum:
 * nome e e-mail já aparecem na tela, e pagar criptografia por eles seria custo
 * sem ganho.
 */
class TokenManager(
    private val settings: Settings,
    private val secure: Settings,
) {

    companion object {
        private const val KEY_TOKEN = "auth_token"
        private const val KEY_USER_DATA = "cached_user_data"
    }

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun saveToken(token: String) {
        secure[KEY_TOKEN] = token
    }

    /**
     * Recupera o token, trazendo para o armazenamento cifrado o que ficou no
     * comum.
     *
     * A migração acontece na leitura porque é o único momento garantido: sem
     * ela, a atualização do aplicativo deslogaria todo mundo que já estava
     * dentro — o token continuaria lá, no lugar antigo, e ninguém iria buscá-lo.
     */
    fun getToken(): String? {
        secure.getStringOrNull(KEY_TOKEN)?.let { return it }

        return settings.getStringOrNull(KEY_TOKEN)?.also { legacy ->
            secure[KEY_TOKEN] = legacy
            settings.remove(KEY_TOKEN)
        }
    }

    /**
     * Verifica se existe um token salvo
     */
    fun hasToken(): Boolean {
        return getToken() != null
    }

    /**
     * Apaga o token dos dois lugares: quem sai da conta não pode deixar uma
     * credencial esquecida no armazenamento antigo.
     */
    fun clearToken() {
        secure.remove(KEY_TOKEN)
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

/**
 * Hora local do aparelho, de 0 a 23.
 *
 * Serve à saudação da tela de início. O epoch sozinho não bastaria: ele é UTC,
 * e diria "boa noite" a quem está de manhã em Brasília.
 */
expect fun currentHourOfDay(): Int

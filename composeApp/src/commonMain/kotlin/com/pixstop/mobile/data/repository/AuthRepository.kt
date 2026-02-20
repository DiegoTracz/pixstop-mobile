package com.pixstop.mobile.data.repository

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import com.pixstop.mobile.core.config.ApiConfig
import com.pixstop.mobile.core.network.HttpClientFactory
import com.pixstop.mobile.core.storage.TokenManager
import com.pixstop.mobile.data.model.*

/**
 * Resultado de operação que pode ser sucesso ou erro
 */
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val message: String, val isOffline: Boolean = false) : Result<Nothing>()
}

/**
 * Repositório de autenticação
 */
class AuthRepository(
    private val tokenManager: TokenManager = TokenManager()
) {
    private var httpClient: HttpClient = HttpClientFactory.create(tokenManager)

    /**
     * Recria o HttpClient (necessário após salvar o token)
     */
    private fun refreshHttpClient() {
        httpClient.close()
        httpClient = HttpClientFactory.create(tokenManager)
    }

    /**
     * Realiza login na API
     */
    suspend fun login(email: String, password: String): Result<User> {
        return try {
            val response = httpClient.post(ApiConfig.Endpoints.LOGIN) {
                setBody(LoginRequest(user = email, password = password))
            }

            if (response.status == HttpStatusCode.OK) {
                val apiResponse = response.body<ApiResponse<LoginData>>()
                if (apiResponse.success && apiResponse.data != null) {
                    // Salva o token
                    tokenManager.saveToken(apiResponse.data.token)

                    // Recria o client com o novo token
                    refreshHttpClient()

                    // Busca dados do perfil
                    return fetchProfile()
                } else {
                    Result.Error(apiResponse.error?.message ?: "Erro ao fazer login")
                }
            } else if (response.status == HttpStatusCode.Unauthorized) {
                Result.Error("Credenciais inválidas")
            } else {
                Result.Error("Erro do servidor: ${response.status.value}")
            }
        } catch (e: Exception) {
            // Verifica se tem dados em cache para modo offline
            val cachedData = tokenManager.getUserData()
            if (cachedData != null && tokenManager.hasToken()) {
                Result.Error(
                    message = "Sem conexão. Usando dados offline.",
                    isOffline = true
                )
            } else {
                Result.Error("Erro de conexão: ${e.message ?: "Verifique sua internet"}")
            }
        }
    }

    /**
     * Busca dados do perfil do usuário
     */
    suspend fun fetchProfile(): Result<User> {
        return try {
            val response = httpClient.get(ApiConfig.Endpoints.PROFILE)

            if (response.status == HttpStatusCode.OK) {
                val apiResponse = response.body<ApiResponse<ProfileData>>()
                if (apiResponse.success && apiResponse.data != null) {
                    val profileData = apiResponse.data

                    // Salva no cache para uso offline
                    tokenManager.saveUserData(
                        CachedUserData(
                            user = profileData.user
                        )
                    )

                    Result.Success(profileData.user)
                } else {
                    Result.Error(apiResponse.error?.message ?: "Erro ao buscar perfil")
                }
            } else if (response.status == HttpStatusCode.Unauthorized) {
                // Token inválido, limpa dados
                tokenManager.clearAll()
                Result.Error("Sessão expirada. Faça login novamente.")
            } else {
                Result.Error("Erro do servidor: ${response.status.value}")
            }
        } catch (e: Exception) {
            // Tenta usar cache offline
            val cachedData = tokenManager.getUserData()
            if (cachedData != null) {
                Result.Success(cachedData.user)
            } else {
                Result.Error("Erro de conexão: ${e.message ?: "Verifique sua internet"}")
            }
        }
    }

    /**
     * Realiza logout
     */
    suspend fun logout(): Result<Unit> {
        return try {
            httpClient.post(ApiConfig.Endpoints.LOGOUT)
            tokenManager.clearAll()
            refreshHttpClient()
            Result.Success(Unit)
        } catch (e: Exception) {
            // Mesmo com erro de rede, limpa dados locais
            tokenManager.clearAll()
            refreshHttpClient()
            Result.Success(Unit)
        }
    }

    /**
     * Verifica se o usuário está autenticado
     */
    fun isAuthenticated(): Boolean {
        return tokenManager.hasToken()
    }

    /**
     * Retorna dados do usuário em cache (para modo offline)
     */
    fun getCachedUser(): User? {
        return tokenManager.getUserData()?.user
    }

    /**
     * Retorna o TokenManager para uso em outros repositórios
     */
    fun getTokenManager(): TokenManager = tokenManager
}

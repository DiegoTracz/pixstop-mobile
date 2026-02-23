package com.pixstop.mobile.data.repository

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.json.Json
import com.pixstop.mobile.core.config.ApiConfig
import com.pixstop.mobile.core.network.HttpClientFactory
import com.pixstop.mobile.core.storage.TokenManager
import com.pixstop.mobile.data.model.*

/**
 * Resultado de operação que pode ser sucesso ou erro
 */
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(
        val message: String,
        val isOffline: Boolean = false,
        val fieldErrors: Map<String, String> = emptyMap()
    ) : Result<Nothing>()
}

/**
 * Repositório de autenticação
 */
class AuthRepository(
    private val tokenManager: TokenManager = TokenManager()
) {
    private var httpClient: HttpClient = HttpClientFactory.create(tokenManager)

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    /**
     * Recria o HttpClient (necessário após salvar o token)
     */
    private fun refreshHttpClient() {
        httpClient.close()
        httpClient = HttpClientFactory.create(tokenManager)
    }

    /**
     * Salva dados de autenticação (token + user + tenant) no cache
     */
    private fun saveAuthData(authData: AuthResponseData) {
        tokenManager.saveToken(authData.token)
        tokenManager.saveUserData(
            CachedUserData(
                user = authData.user,
                tenant = authData.tenant
            )
        )
        refreshHttpClient()
    }

    /**
     * Realiza login na API.
     * Retorna user diretamente (sem chamada extra ao /me).
     */
    suspend fun login(email: String, password: String): Result<User> {
        return try {
            val response = httpClient.post(ApiConfig.Endpoints.LOGIN) {
                setBody(LoginRequest(user = email, password = password))
            }

            when (response.status) {
                HttpStatusCode.OK -> {
                    val apiResponse = response.body<ApiResponse<AuthResponseData>>()
                    if (apiResponse.success && apiResponse.data != null) {
                        saveAuthData(apiResponse.data)
                        Result.Success(apiResponse.data.user)
                    } else {
                        Result.Error(apiResponse.error?.message ?: "Erro ao fazer login")
                    }
                }
                HttpStatusCode.Unauthorized -> {
                    Result.Error("Credenciais inválidas")
                }
                HttpStatusCode.UnprocessableEntity -> {
                    parseValidationError(response)
                }
                else -> {
                    Result.Error("Erro do servidor: ${response.status.value}")
                }
            }
        } catch (e: Exception) {
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
     * Registra um novo usuário.
     * Opcionalmente vincula a uma empresa via company_code.
     */
    suspend fun registerUser(request: RegisterUserRequest): Result<User> {
        return try {
            val response = httpClient.post(ApiConfig.Endpoints.REGISTER_USER) {
                setBody(request)
            }

            when (response.status) {
                HttpStatusCode.Created, HttpStatusCode.OK -> {
                    val apiResponse = response.body<ApiResponse<AuthResponseData>>()
                    if (apiResponse.success && apiResponse.data != null) {
                        saveAuthData(apiResponse.data)
                        Result.Success(apiResponse.data.user)
                    } else {
                        Result.Error(apiResponse.error?.message ?: "Erro ao registrar")
                    }
                }
                HttpStatusCode.UnprocessableEntity -> {
                    parseValidationError(response)
                }
                else -> {
                    Result.Error("Erro do servidor: ${response.status.value}")
                }
            }
        } catch (e: Exception) {
            Result.Error("Erro de conexão: ${e.message ?: "Verifique sua internet"}")
        }
    }

    /**
     * Solicita envio de email para reset de senha.
     */
    suspend fun forgotPassword(email: String): Result<String> {
        return try {
            val response = httpClient.post(ApiConfig.Endpoints.FORGOT_PASSWORD) {
                setBody(ForgotPasswordRequest(email = email))
            }

            when (response.status) {
                HttpStatusCode.OK -> {
                    val apiResponse = response.body<ApiResponse<Unit>>()
                    Result.Success(apiResponse.message ?: "Email enviado com sucesso")
                }
                HttpStatusCode.UnprocessableEntity -> {
                    val body = response.bodyAsText()
                    try {
                        val validationError = json.decodeFromString<ValidationErrorResponse>(body)
                        val message = validationError.errors?.values?.flatten()?.firstOrNull()
                            ?: validationError.message
                            ?: "Email não encontrado"
                        Result.Error(message)
                    } catch (_: Exception) {
                        Result.Error("Email não encontrado")
                    }
                }
                else -> Result.Error("Erro do servidor: ${response.status.value}")
            }
        } catch (e: Exception) {
            Result.Error("Erro de conexão: ${e.message ?: "Verifique sua internet"}")
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

                    tokenManager.saveUserData(
                        CachedUserData(
                            user = profileData.user,
                            tenant = profileData.activeTenant
                        )
                    )

                    Result.Success(profileData.user)
                } else {
                    Result.Error(apiResponse.error?.message ?: "Erro ao buscar perfil")
                }
            } else if (response.status == HttpStatusCode.Unauthorized) {
                tokenManager.clearAll()
                Result.Error("Sessão expirada. Faça login novamente.")
            } else {
                Result.Error("Erro do servidor: ${response.status.value}")
            }
        } catch (e: Exception) {
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

    /**
     * Faz parse de erros de validação (422) da API Laravel
     */
    private suspend fun parseValidationError(response: HttpResponse): Result.Error {
        return try {
            val body = response.bodyAsText()
            // Tenta o formato com "errors" map (padrão Laravel)
            try {
                val validationError = json.decodeFromString<ValidationErrorResponse>(body)
                val fieldErrors = mutableMapOf<String, String>()
                validationError.errors?.forEach { (field, messages) ->
                    fieldErrors[field] = messages.firstOrNull() ?: ""
                }
                val generalMessage = validationError.message
                    ?: validationError.error?.message
                    ?: fieldErrors.values.firstOrNull()
                    ?: "Erro de validação"
                Result.Error(
                    message = generalMessage,
                    fieldErrors = fieldErrors
                )
            } catch (_: Exception) {
                // Tenta o formato { "success": false, "error": { "message": "...", "company_code": "..." } }
                val apiResponse = json.decodeFromString<ApiResponse<Unit>>(body)
                val fieldErrors = mutableMapOf<String, String>()
                apiResponse.error?.companyCode?.let {
                    fieldErrors["company_code"] = it
                }
                Result.Error(
                    message = apiResponse.error?.message ?: "Erro de validação",
                    fieldErrors = fieldErrors
                )
            }
        } catch (_: Exception) {
            Result.Error("Erro de validação")
        }
    }
}

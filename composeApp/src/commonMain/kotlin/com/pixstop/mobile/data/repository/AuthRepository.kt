package com.pixstop.mobile.data.repository

import com.pixstop.mobile.core.config.ApiConfig
import com.pixstop.mobile.core.logging.AppLogger
import com.pixstop.mobile.core.network.safeCall
import com.pixstop.mobile.core.network.safeCallUnit
import com.pixstop.mobile.core.storage.SessionStore
import com.pixstop.mobile.core.storage.TokenManager
import com.pixstop.mobile.data.model.AuthResponseData
import com.pixstop.mobile.data.model.CachedUserData
import com.pixstop.mobile.data.model.ForgotPasswordRequest
import com.pixstop.mobile.data.model.LoginRequest
import com.pixstop.mobile.data.model.RegisterUserRequest
import com.pixstop.mobile.data.model.User
import com.pixstop.mobile.domain.model.Outcome
import com.pixstop.mobile.domain.model.map
import com.pixstop.mobile.domain.model.onFailure
import com.pixstop.mobile.domain.model.onSuccess
import io.ktor.client.HttpClient
import io.ktor.client.plugins.auth.authProviders
import io.ktor.client.plugins.auth.providers.BearerAuthProvider
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody

private const val TAG = "Auth"

/**
 * Entrada e saída da conta.
 *
 * O tratamento de erro vive no `safeCall`; aqui fica só o que é próprio da
 * autenticação — guardar o token e o cache do perfil, e avisar o cliente HTTP
 * de que o token mudou.
 */
class AuthRepository(
    private val client: HttpClient,
    private val tokens: TokenManager,
    private val session: SessionStore,
) {

    suspend fun login(email: String, password: String): Outcome<User> =
        safeCall<AuthResponseData>(TAG) {
            client.post(ApiConfig.Endpoints.LOGIN) {
                setBody(LoginRequest(user = email, password = password))
            }
        }.onSuccess(::persist).map { it.user }

    suspend fun registerUser(request: RegisterUserRequest): Outcome<User> =
        safeCall<AuthResponseData>(TAG) {
            client.post(ApiConfig.Endpoints.REGISTER_USER) { setBody(request) }
        }.onSuccess(::persist).map { it.user }

    suspend fun forgotPassword(email: String): Outcome<Unit> =
        safeCallUnit(TAG) {
            client.post(ApiConfig.Endpoints.FORGOT_PASSWORD) {
                setBody(ForgotPasswordRequest(email))
            }
        }

    /**
     * Busca o perfil e atualiza o cache offline.
     */
    suspend fun fetchProfile(): Outcome<User> =
        safeCall<AuthResponseData>(TAG) {
            client.get(ApiConfig.Endpoints.PROFILE)
        }.onSuccess { data ->
            tokens.saveUserData(CachedUserData(user = data.user, tenant = data.tenant))
        }.map { it.user }

    /**
     * Sai da conta.
     *
     * O estado local é limpo mesmo que o servidor não responda: quem pediu para
     * sair não pode continuar dentro porque a rede caiu.
     */
    suspend fun logout(): Outcome<Unit> {
        val result = safeCallUnit(TAG) { client.post(ApiConfig.Endpoints.LOGOUT) }

        clearSession()

        return result.onFailure { AppLogger.w("Logout remoto falhou; sessão local encerrada assim mesmo.", tag = TAG) }
    }

    fun isAuthenticated(): Boolean = session.isLoggedIn()

    fun cachedUser(): User? = tokens.getUserData()?.user

    /**
     * Guarda o token e limpa o cache do plugin de autenticação.
     *
     * Sem essa limpeza o Ktor continuaria usando o token que leu na primeira
     * chamada — antes do login, quando não havia nenhum.
     */
    private fun persist(data: AuthResponseData) {
        session.save(data.token)
        tokens.saveUserData(CachedUserData(user = data.user, tenant = data.tenant))
        clearBearerCache()
    }

    private fun clearSession() {
        tokens.clearAll()
        clearBearerCache()
    }

    private fun clearBearerCache() {
        runCatching { client.authProviders.filterIsInstance<BearerAuthProvider>().forEach { it.clearToken() } }
    }
}

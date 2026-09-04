package com.pixstop.mobile.data.repository

import com.pixstop.mobile.core.config.ApiConfig
import com.pixstop.mobile.core.network.safeCall
import com.pixstop.mobile.data.mapper.toDomain
import com.pixstop.mobile.data.remote.dto.InviteDto
import com.pixstop.mobile.data.remote.dto.JoinTenantRequest
import com.pixstop.mobile.data.remote.dto.MeDto
import com.pixstop.mobile.data.remote.dto.SwitchTenantRequest
import com.pixstop.mobile.data.remote.dto.TenantRefDto
import com.pixstop.mobile.domain.model.Account
import com.pixstop.mobile.domain.model.Outcome
import com.pixstop.mobile.domain.model.map
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody

private const val TAG = "Account"

/**
 * Conta e empresas.
 *
 * Trocar ou entrar numa empresa muda o que toda a API devolve depois, então
 * quem chama sempre recarrega o `/me` em seguida — quem cuida disso é o
 * `SessionViewModel`, não cada tela.
 */
class AccountRepository(private val client: HttpClient) {

    suspend fun me(): Outcome<Account> =
        safeCall<MeDto>(TAG) { client.get(ApiConfig.Endpoints.PROFILE) }.map { it.toDomain() }

    suspend fun switchCompany(companyId: String): Outcome<TenantRefDto> =
        safeCall(TAG) {
            client.post(ApiConfig.Endpoints.SWITCH_TENANT) { setBody(SwitchTenantRequest(companyId)) }
        }

    /**
     * Entra numa empresa pelo código, digitado ou lido do QR.
     *
     * O servidor já deixa a empresa nova como ativa, então não é preciso
     * trocar em seguida.
     */
    suspend fun joinCompany(companyCode: String): Outcome<TenantRefDto> =
        safeCall(TAG) {
            client.post(ApiConfig.Endpoints.JOIN_TENANT) {
                setBody(JoinTenantRequest(companyCode.trim().uppercase()))
            }
        }

    /** Código e link de convite. Só o administrador da empresa alcança. */
    suspend fun invite(): Outcome<InviteDto> =
        safeCall(TAG) { client.get(ApiConfig.Endpoints.TENANT_INVITE) }
}

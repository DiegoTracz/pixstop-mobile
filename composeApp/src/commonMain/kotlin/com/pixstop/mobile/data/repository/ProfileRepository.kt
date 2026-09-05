package com.pixstop.mobile.data.repository

import com.pixstop.mobile.core.config.ApiConfig
import com.pixstop.mobile.core.network.safeCall
import com.pixstop.mobile.core.network.safeCallUnit
import com.pixstop.mobile.data.remote.dto.DeleteAccountRequest
import com.pixstop.mobile.data.remote.dto.DeleteAccountResultDto
import com.pixstop.mobile.data.remote.dto.UpdatePasswordRequest
import com.pixstop.mobile.data.remote.dto.UpdateProfileRequest
import com.pixstop.mobile.data.remote.dto.UpdatedProfileDto
import com.pixstop.mobile.domain.model.Outcome
import com.pixstop.mobile.domain.model.map
import io.ktor.client.HttpClient
import io.ktor.client.request.delete
import io.ktor.client.request.put
import io.ktor.client.request.setBody

private const val TAG = "Profile"

/**
 * Nome, e-mail e senha.
 *
 * Trocar a senha derruba as outras sessões no servidor, mas não a que fez a
 * troca — quem está no app continua dentro.
 */
class ProfileRepository(private val client: HttpClient) {

    suspend fun update(name: String, email: String): Outcome<UpdatedProfileDto> =
        safeCall(TAG) {
            client.put(ApiConfig.Endpoints.UPDATE_PROFILE) {
                setBody(UpdateProfileRequest(name.trim(), email.trim()))
            }
        }

    suspend fun updatePassword(current: String, new: String, confirmation: String): Outcome<Unit> =
        safeCallUnit(TAG) {
            client.put(ApiConfig.Endpoints.UPDATE_PASSWORD) {
                setBody(UpdatePasswordRequest(current, new, confirmation))
            }
        }

    /**
     * Exclui a conta a pedido do titular — exigência das lojas e da LGPD.
     *
     * @return quantos dias o servidor guarda os dados antes do expurgo
     *   definitivo, que é o prazo em que ainda dá para voltar atrás.
     */
    suspend fun deleteAccount(password: String): Outcome<Int> =
        safeCall<DeleteAccountResultDto>(TAG) {
            client.delete(ApiConfig.Endpoints.DELETE_ACCOUNT) { setBody(DeleteAccountRequest(password)) }
        }.map { it.purgeAfterDays }
}

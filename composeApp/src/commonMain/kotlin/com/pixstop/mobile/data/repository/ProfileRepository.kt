package com.pixstop.mobile.data.repository

import com.pixstop.mobile.core.config.ApiConfig
import com.pixstop.mobile.core.network.safeCall
import com.pixstop.mobile.core.network.safeCallUnit
import com.pixstop.mobile.data.remote.dto.UpdatePasswordRequest
import com.pixstop.mobile.data.remote.dto.UpdateProfileRequest
import com.pixstop.mobile.data.remote.dto.UpdatedProfileDto
import com.pixstop.mobile.domain.model.Outcome
import io.ktor.client.HttpClient
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
}

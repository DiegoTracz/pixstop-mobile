package com.pixstop.mobile.data.repository

import com.pixstop.mobile.core.config.ApiConfig
import com.pixstop.mobile.core.network.safeCall
import com.pixstop.mobile.data.mapper.toDomain
import com.pixstop.mobile.data.remote.dto.ChallengePageDto
import com.pixstop.mobile.data.remote.dto.ChallengeVisibilityRequest
import com.pixstop.mobile.domain.model.ChallengePage
import com.pixstop.mobile.domain.model.Outcome
import com.pixstop.mobile.domain.model.map
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import kotlinx.serialization.json.JsonObject

/**
 * O desafio coletivo da empresa e as medalhas da pessoa.
 */
class ChallengeRepository(private val client: HttpClient) {

    suspend fun current(): Outcome<ChallengePage> =
        safeCall<ChallengePageDto>(TAG) { client.get(ApiConfig.Endpoints.CHALLENGE_CURRENT) }
            .map { it.toDomain() }

    /** Sai (ou volta) da lista de participantes. As compras continuam contando. */
    suspend fun setHidden(challengeId: Long, hidden: Boolean): Outcome<JsonObject> =
        safeCall(TAG) {
            client.post(ApiConfig.Endpoints.challengeVisibility(challengeId)) {
                setBody(ChallengeVisibilityRequest(hidden))
            }
        }

    private companion object {
        const val TAG = "Challenge"
    }
}

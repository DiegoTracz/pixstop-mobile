package com.pixstop.mobile.data.repository

import com.pixstop.mobile.core.config.ApiConfig
import com.pixstop.mobile.core.network.safeCall
import com.pixstop.mobile.data.mapper.toDomain
import com.pixstop.mobile.data.remote.dto.PrizeSpinResultDto
import com.pixstop.mobile.data.remote.dto.PrizeWheelPageDto
import com.pixstop.mobile.domain.model.Outcome
import com.pixstop.mobile.domain.model.PrizeSpinResult
import com.pixstop.mobile.domain.model.PrizeWheel
import com.pixstop.mobile.domain.model.map
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.post

/**
 * A roleta de prêmios da empresa (docs/plans/LANCAMENTO_LOCARMAIS.md no
 * servidor). `current` devolve nulo quando não há roleta girando.
 */
class PrizeWheelRepository(private val client: HttpClient) {

    suspend fun current(): Outcome<PrizeWheel?> =
        safeCall<PrizeWheelPageDto>(TAG) { client.get(ApiConfig.Endpoints.PRIZE_WHEEL) }
            .map { it.wheel?.toDomain() }

    /** Gira o próximo giro: o de ouro primeiro. O resultado vem do servidor. */
    suspend fun spin(): Outcome<PrizeSpinResult> =
        safeCall<PrizeSpinResultDto>(TAG) { client.post(ApiConfig.Endpoints.PRIZE_WHEEL_SPIN) }
            .map { it.toDomain() }

    private companion object {
        const val TAG = "PrizeWheel"
    }
}

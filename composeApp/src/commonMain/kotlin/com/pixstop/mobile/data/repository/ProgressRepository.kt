package com.pixstop.mobile.data.repository

import com.pixstop.mobile.core.config.ApiConfig
import com.pixstop.mobile.core.network.safeCall
import com.pixstop.mobile.core.network.safeCallPaged
import com.pixstop.mobile.data.mapper.toDomain
import com.pixstop.mobile.data.remote.dto.Page
import com.pixstop.mobile.data.remote.dto.ProgressionDto
import com.pixstop.mobile.data.remote.dto.XpEventDto
import com.pixstop.mobile.domain.model.Outcome
import com.pixstop.mobile.domain.model.map
import com.pixstop.mobile.domain.model.Progression
import com.pixstop.mobile.domain.model.XpEntry
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter

/**
 * A progressão por XP: onde a pessoa está e como chegou lá.
 */
class ProgressRepository(private val client: HttpClient) {

    /** `null` no sucesso quer dizer que a progressão não está ligada aqui. */
    suspend fun progress(): Outcome<Progression?> =
        safeCall<ProgressionDto>(TAG) { client.get(ApiConfig.Endpoints.PIXELS_PROGRESS) }
            .map { it.toDomain() }

    suspend fun history(page: Int = 1): Outcome<Page<XpEntry>> =
        safeCallPaged<XpEventDto>(TAG) {
            client.get(ApiConfig.Endpoints.PIXELS_XP_HISTORY) { parameter("page", page) }
        }.map { result -> Page(result.items.map { it.toDomain() }, result.meta) }

    private companion object {
        const val TAG = "Progress"
    }
}

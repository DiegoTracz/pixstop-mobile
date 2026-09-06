package com.pixstop.mobile.data.repository

import com.pixstop.mobile.core.config.ApiConfig
import com.pixstop.mobile.core.network.safeCall
import com.pixstop.mobile.data.remote.dto.CheckinRequest
import com.pixstop.mobile.data.remote.dto.CheckinResultDto
import com.pixstop.mobile.data.remote.dto.StaffMemberDto
import com.pixstop.mobile.data.remote.dto.TodayCheckinDto
import com.pixstop.mobile.domain.model.Outcome
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody

/**
 * O balcão: achar a pessoa e registrar a visita.
 */
class StaffRepository(private val client: HttpClient) {

    suspend fun findMembers(query: String): Outcome<List<StaffMemberDto>> =
        safeCall(TAG) { client.get(ApiConfig.Endpoints.STAFF_MEMBERS) { parameter("q", query) } }

    suspend fun register(userId: Long, kind: String, amount: Double?, description: String?): Outcome<CheckinResultDto> =
        safeCall(TAG) {
            client.post(ApiConfig.Endpoints.STAFF_CHECKINS) {
                setBody(CheckinRequest(userId, kind, amount, description?.takeIf { it.isNotBlank() }))
            }
        }

    suspend fun today(): Outcome<List<TodayCheckinDto>> =
        safeCall(TAG) { client.get(ApiConfig.Endpoints.STAFF_CHECKINS) }

    private companion object {
        const val TAG = "Staff"
    }
}

package com.pixstop.mobile.data.repository

import com.pixstop.mobile.core.config.ApiConfig
import com.pixstop.mobile.core.network.safeCall
import com.pixstop.mobile.core.network.safeCallPaged
import com.pixstop.mobile.core.text.IsoInstant
import com.pixstop.mobile.data.remote.dto.DepartmentDto
import com.pixstop.mobile.data.remote.dto.DistributeRequest
import com.pixstop.mobile.data.remote.dto.Page
import com.pixstop.mobile.data.remote.dto.PixelEntryDto
import com.pixstop.mobile.data.remote.dto.TeamMemberDto
import com.pixstop.mobile.domain.model.Department
import com.pixstop.mobile.domain.model.Outcome
import com.pixstop.mobile.domain.model.PixelEntry
import com.pixstop.mobile.domain.model.PixelSource
import com.pixstop.mobile.domain.model.TeamMember
import com.pixstop.mobile.domain.model.map
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody

private const val TAG = "Team"

/**
 * Extrato de pixels e a área do gestor.
 *
 * Quem não gere departamento nenhum recebe uma lista vazia, não um erro: é
 * assim que o app decide se mostra a área de equipe.
 */
class TeamRepository(private val client: HttpClient) {

    suspend fun pixelHistory(page: Int = 1): Outcome<Page<PixelEntry>> =
        safeCallPaged<PixelEntryDto>(TAG) {
            client.get(ApiConfig.Endpoints.PIXELS_HISTORY) { parameter("page", page) }
        }.map { result -> Page(result.items.map { it.toDomain() }, result.meta) }

    suspend fun departments(): Outcome<List<Department>> =
        safeCall<List<DepartmentDto>>(TAG) {
            client.get(ApiConfig.Endpoints.TEAM)
        }.map { list -> list.map { it.toDomain() } }

    suspend fun members(departmentId: Long): Outcome<List<TeamMember>> =
        safeCall<List<TeamMemberDto>>(TAG) {
            client.get(ApiConfig.Endpoints.teamMembers(departmentId))
        }.map { list -> list.map { it.toDomain() } }

    /**
     * Distribui a mesma quantidade para cada pessoa escolhida.
     *
     * O servidor faz tudo ou nada, então uma recusa aqui significa que
     * ninguém recebeu — e a tela pode dizer isso sem ressalva.
     */
    suspend fun distribute(
        departmentId: Long,
        userIds: List<Long>,
        amount: Int,
        reason: String?,
    ): Outcome<Unit> =
        safeCall<kotlinx.serialization.json.JsonElement>(TAG) {
            client.post(ApiConfig.Endpoints.teamDistribute(departmentId)) {
                setBody(DistributeRequest(userIds, amount, reason?.takeIf { it.isNotBlank() }))
            }
        }.map { }
}

private fun PixelEntryDto.toDomain() = PixelEntry(
    id = id,
    amount = amount,
    balanceAfter = balanceAfter,
    sourceLabel = PixelSource.labelFor(source),
    description = description,
    expiresAt = IsoInstant.toEpochMillis(expiresAt),
    createdAt = IsoInstant.toEpochMillis(createdAt),
)

private fun DepartmentDto.toDomain() = Department(
    id = id,
    name = name,
    description = description,
    pixelBalance = pixelBalance,
    membersCount = membersCount,
    isActive = isActive,
)

private fun TeamMemberDto.toDomain() = TeamMember(
    userId = userId,
    name = name,
    email = email,
    isManager = role == "manager",
    isActive = isActive,
    pixelAvailable = pixelAvailable,
)

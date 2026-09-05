package com.pixstop.mobile.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Uma linha do extrato de pixels.
 *
 * O `amount` é assinado: negativo é saída. O `balance_after` é o saldo depois
 * dela, que é o que permite conferir o extrato sem refazer a soma.
 */
@Serializable
data class PixelEntryDto(
    val id: Long,
    val amount: Int,
    @SerialName("balance_after") val balanceAfter: Int = 0,
    val source: String? = null,
    val description: String? = null,
    @SerialName("expires_at") val expiresAt: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
)

@Serializable
data class DepartmentDto(
    val id: Long,
    val name: String,
    val description: String? = null,
    @SerialName("pixel_balance") val pixelBalance: Int = 0,
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("members_count") val membersCount: Int = 0,
)

@Serializable
data class TeamMemberDto(
    @SerialName("user_id") val userId: Long,
    val name: String,
    val email: String? = null,
    val role: String? = null,
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("pixel_balance") val pixelBalance: Int = 0,
    @SerialName("pixel_available") val pixelAvailable: Int = 0,
)

/**
 * Distribuição de pixels da verba do departamento.
 *
 * A lista vai sempre, mesmo para uma pessoa só: o servidor trata tudo ou nada,
 * e metade da equipe recebendo seria pior que ninguém receber.
 */
@Serializable
data class DistributeRequest(
    @SerialName("user_ids") val userIds: List<Long>,
    val amount: Int,
    val reason: String? = null,
)

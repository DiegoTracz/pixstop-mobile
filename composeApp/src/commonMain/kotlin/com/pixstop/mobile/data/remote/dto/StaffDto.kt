package com.pixstop.mobile.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Quem o balcão achou. */
@Serializable
data class StaffMemberDto(
    val id: Long,
    val name: String,
    val email: String? = null,
    @SerialName("member_code") val memberCode: String? = null,
    val kind: String? = null,
    val phone: String? = null,
)

@Serializable
data class CheckinRequest(
    @SerialName("user_id") val userId: Long,
    val kind: String,
    val amount: Double? = null,
    val description: String? = null,
)

/** O que a visita rendeu, para o balcão dizer na hora. */
@Serializable
data class CheckinResultDto(
    val id: Long,
    @SerialName("user_id") val userId: Long,
    val name: String,
    val kind: String,
    val cashback: Int = 0,
    val xp: Int = 0,
)

@Serializable
data class TodayCheckinDto(
    val id: Long,
    @SerialName("user_id") val userId: Long,
    val name: String,
    val kind: String,
    @SerialName("kind_label") val kindLabel: String,
    val amount: Double? = null,
    val description: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
)

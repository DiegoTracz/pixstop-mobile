package com.pixstop.mobile.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Resposta do `GET /me`.
 *
 * Traz numa chamada só tudo que o app precisa para montar a tela: quem é a
 * pessoa, de quais empresas ela participa, e o que vale na empresa escolhida.
 */
@Serializable
data class MeDto(
    val user: UserDto,
    val tenants: List<MembershipDto> = emptyList(),
    @SerialName("active_tenant") val activeTenant: ActiveTenantDto? = null,
    @SerialName("has_pending_consent") val hasPendingConsent: Boolean = false,
    /** Quem opera geladeiras (Fase 16); ausente em servidor antigo. */
    val operator: OperatorAccountDto? = null,
)

/** A conta de operador de quem entrou. */
@Serializable
data class OperatorAccountDto(
    val id: Long,
    val name: String,
    val role: String? = null,
    @SerialName("sees_money") val seesMoney: Boolean = false,
)

@Serializable
data class UserDto(
    val id: Long,
    val name: String,
    val email: String,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("is_admin_master") val isAdminMaster: Boolean = false,
    @SerialName("member_code") val memberCode: String? = null,
)

/** O vínculo da pessoa com uma empresa, como aparece na lista de troca. */
@Serializable
data class MembershipDto(
    val id: String,
    val name: String,
    @SerialName("company_code") val companyCode: String? = null,
    val role: String? = null,
    val balance: Double = 0.0,
    @SerialName("pixel_balance") val pixelBalance: Int = 0,
    @SerialName("pixel_available") val pixelAvailable: Int = 0,
    val active: Boolean = false,
    @SerialName("is_active") val isActive: Boolean = true,
    /** Opera esta empresa (Fase 16): pertence ao operador dela. */
    val operates: Boolean = false,
)

/**
 * A empresa escolhida, com o que decide o que a tela mostra: papel, carteira,
 * departamentos geridos, situação da assinatura e funcionalidades do plano.
 */
@Serializable
data class ActiveTenantDto(
    val id: String,
    val name: String,
    @SerialName("company_code") val companyCode: String? = null,
    @SerialName("logo_url") val logoUrl: String? = null,
    val role: String? = null,
    @SerialName("is_manager") val isManager: Boolean = false,
    val balance: Double = 0.0,
    @SerialName("pixel_balance") val pixelBalance: Int = 0,
    @SerialName("pixel_available") val pixelAvailable: Int = 0,
    val department: DepartmentSummaryDto? = null,
    @SerialName("managed_departments") val managedDepartments: List<ManagedDepartmentDto> = emptyList(),
    @SerialName("subscription_status") val subscriptionStatus: String? = null,
    @SerialName("plan_active") val planActive: Boolean = true,
    @SerialName("mercadopago_connected") val mercadoPagoConnected: Boolean = false,
    @SerialName("cashback_enabled") val cashbackEnabled: Boolean = false,
    val features: Map<String, Boolean> = emptyMap(),
    val vertical: String? = null,
    val modules: List<String>? = null,
    @SerialName("member_noun") val memberNoun: MemberNounDto? = null,
    val kind: String? = null,
    @SerialName("is_staff") val isStaff: Boolean = false,
    /** Opera esta empresa (Fase 16), que não é o mesmo que administrá-la. */
    val operates: Boolean = false,
    /** Progressão por XP; ausente ou desligada vira `null` no domínio. */
    val progression: ProgressionDto? = null,
    @SerialName("pixels_expiring_soon") val pixelsExpiringSoon: Int = 0,
)

@Serializable
data class DepartmentSummaryDto(val id: Long, val name: String)

@Serializable
data class ManagedDepartmentDto(
    val id: Long,
    val name: String,
    @SerialName("pixel_balance") val pixelBalance: Int = 0,
)

/** Corpo do `POST /tenant/switch`. */
@Serializable
data class SwitchTenantRequest(@SerialName("tenant_id") val tenantId: String)

/** Corpo do `POST /tenant/join`. */
@Serializable
data class JoinTenantRequest(@SerialName("company_code") val companyCode: String)

/** Resposta das rotas de troca e entrada em empresa. */
@Serializable
data class TenantRefDto(
    val id: String,
    val name: String,
    @SerialName("company_code") val companyCode: String? = null,
    val role: String? = null,
)

/** Resposta do `GET /tenant/invite`. */
@Serializable
data class InviteDto(
    @SerialName("company_code") val companyCode: String,
    @SerialName("invite_url") val inviteUrl: String,
)

/** Como se chama quem usa: colaborador na geladeira, cliente no resto. */
@Serializable
data class MemberNounDto(val singular: String = "colaborador", val plural: String = "colaboradores")

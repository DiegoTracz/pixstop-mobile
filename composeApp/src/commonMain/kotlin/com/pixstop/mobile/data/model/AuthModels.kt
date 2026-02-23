package com.pixstop.mobile.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Resposta padrão da API Laravel
 *
 * Formato esperado:
 * { "success": true, "data": {...} }
 * { "success": false, "error": { "message": "..." } }
 */
@Serializable
data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val error: ApiError? = null,
    val message: String? = null
)

@Serializable
data class ApiError(
    val message: String? = null,
    @SerialName("company_code")
    val companyCode: String? = null
)

/**
 * Resposta de erro de validação 422
 * { "message": "...", "errors": { "field": ["msg"] } }
 */
@Serializable
data class ValidationErrorResponse(
    val message: String? = null,
    val errors: Map<String, List<String>>? = null,
    val success: Boolean? = null,
    val error: ApiError? = null
)

/**
 * Request de login
 */
@Serializable
data class LoginRequest(
    val user: String,
    val password: String
)

/**
 * Request de registro de usuário
 */
@Serializable
data class RegisterUserRequest(
    val name: String,
    val email: String,
    val password: String,
    @SerialName("password_confirmation")
    val passwordConfirmation: String,
    @SerialName("company_code")
    val companyCode: String? = null
)

/**
 * Request de esqueci a senha
 */
@Serializable
data class ForgotPasswordRequest(
    val email: String
)

/**
 * Tenant (empresa) — presente nas respostas de login, registro e perfil.
 */
@Serializable
data class Tenant(
    val id: String,
    val name: String,
    @SerialName("company_code")
    val companyCode: String? = null,
    val role: String? = null,
    val balance: Double? = null,
    val active: Boolean? = null,
    @SerialName("is_active")
    val isActive: Boolean? = null
)

/**
 * Response de auth (login e register) — contém token + user + tenant.
 *
 * Formato:
 * { "token": "...", "type": "Bearer", "user": {...}, "tenant": {...} }
 */
@Serializable
data class AuthResponseData(
    val token: String,
    val type: String? = "Bearer",
    val user: User,
    val tenant: Tenant? = null
)

/**
 * Dados do usuário — Modelo padrão do Laravel
 */
@Serializable
data class User(
    val id: Int,
    val name: String,
    val email: String,
    @SerialName("email_verified_at")
    val emailVerifiedAt: String? = null,
    @SerialName("avatar_url")
    val avatarUrl: String? = null,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null
)

/**
 * Response do endpoint /me
 *
 * Formato:
 * { "success": true, "data": { "user": {...}, "tenants": [...], "active_tenant": {...} } }
 */
@Serializable
data class ProfileData(
    val user: User,
    val tenants: List<Tenant>? = null,
    @SerialName("active_tenant")
    val activeTenant: Tenant? = null
)

/**
 * Dados do usuário para cache offline
 */
@Serializable
data class CachedUserData(
    val user: User,
    val tenant: Tenant? = null,
    val lastUpdated: Long = 0L
)

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
    val message: String? = null
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
 * Response do login (Laravel Sanctum)
 */
@Serializable
data class LoginData(
    val token: String,
    val type: String? = "Bearer"
)

/**
 * Dados do usuário - Modelo padrão do Laravel
 *
 * Adicione mais campos conforme seu modelo User do Laravel:
 * - phone, avatar_url, created_at, etc.
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
 * Formato esperado:
 * { "success": true, "data": { "user": {...} } }
 *
 * Se sua API retorna o user diretamente, use User ao invés de ProfileData
 */
@Serializable
data class ProfileData(
    val user: User
)

/**
 * Dados do usuário para cache offline
 */
@Serializable
data class CachedUserData(
    val user: User,
    val lastUpdated: Long = 0L
)

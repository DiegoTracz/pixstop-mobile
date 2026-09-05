package com.pixstop.mobile.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UpdateProfileRequest(
    val name: String,
    val email: String,
)

@Serializable
data class UpdatedProfileDto(
    val id: Long,
    val name: String,
    val email: String,
)

/**
 * Troca de senha.
 *
 * A confirmação vai junto porque o servidor a exige (`confirmed`): conferir
 * só no app deixaria a regra em um lugar que o servidor não vê.
 */
@Serializable
data class UpdatePasswordRequest(
    @SerialName("current_password") val currentPassword: String,
    val password: String,
    @SerialName("password_confirmation") val passwordConfirmation: String,
)

/**
 * Exclusão de conta pedida pela própria pessoa.
 *
 * A senha vai junto porque é o que separa "pedi para excluir" de "alguém
 * pegou meu celular destravado".
 */
@Serializable
data class DeleteAccountRequest(val password: String)

@Serializable
data class DeleteAccountResultDto(
    @SerialName("purge_after_days") val purgeAfterDays: Int = 0,
)

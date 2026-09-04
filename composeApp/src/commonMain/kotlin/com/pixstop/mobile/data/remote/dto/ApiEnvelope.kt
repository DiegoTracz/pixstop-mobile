package com.pixstop.mobile.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * O envelope que toda resposta da API usa.
 *
 * Documentado em `docs/API_MOBILE.md` do backend. Campos podem ser
 * acrescentados lá, nunca removidos — por isso `ignoreUnknownKeys`.
 */
@Serializable
data class ApiEnvelope<T>(
    val success: Boolean = false,
    val data: T? = null,
    val message: String? = null,
    val error: ApiErrorBody? = null,
    val meta: PageMeta? = null,
)

/**
 * O `code` é o que o app deve olhar; a `message` é para mostrar a quem usa e
 * pode ser reescrita a qualquer momento no servidor.
 */
@Serializable
data class ApiErrorBody(
    val code: String? = null,
    val message: String? = null,
)

@Serializable
data class PageMeta(
    @SerialName("current_page") val currentPage: Int = 1,
    @SerialName("last_page") val lastPage: Int = 1,
    @SerialName("per_page") val perPage: Int = 20,
    val total: Int = 0,
    val unread: Int? = null,
) {
    val hasNextPage: Boolean get() = currentPage < lastPage
}

/** Resposta de validação do Laravel: 422 com os erros por campo. */
@Serializable
data class ValidationErrorBody(
    val message: String? = null,
    val errors: Map<String, List<String>>? = null,
)

/** Uma página de resultados, já com os dados e o rodapé de paginação. */
data class Page<T>(
    val items: List<T>,
    val meta: PageMeta,
)

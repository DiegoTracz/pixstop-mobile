package com.pixstop.mobile.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Documento legal em vigor, como vem do `GET /legal/documents`.
 *
 * O conteúdo chega inteiro na mesma resposta: são poucos documentos e a pessoa
 * precisa poder ler antes de aceitar, mesmo sem uma segunda chamada.
 */
@Serializable
data class LegalDocumentDto(
    val id: Long,
    val type: String,
    @SerialName("type_label") val typeLabel: String,
    val title: String,
    val version: String,
    val content: String,
    @SerialName("published_at") val publishedAt: String? = null,
    val accepted: Boolean = false,
)

/**
 * Aceite. Sem lista, o servidor registra tudo que está em vigor.
 */
@Serializable
data class LegalConsentRequest(
    @SerialName("document_ids") val documentIds: List<Long>? = null,
)

@Serializable
data class LegalConsentResultDto(
    val recorded: Int = 0,
    val pending: Int = 0,
)

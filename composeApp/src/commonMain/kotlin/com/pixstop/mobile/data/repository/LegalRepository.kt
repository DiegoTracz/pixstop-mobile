package com.pixstop.mobile.data.repository

import com.pixstop.mobile.core.config.ApiConfig
import com.pixstop.mobile.core.network.safeCall
import com.pixstop.mobile.core.text.HtmlText
import com.pixstop.mobile.data.remote.dto.LegalConsentRequest
import com.pixstop.mobile.data.remote.dto.LegalConsentResultDto
import com.pixstop.mobile.data.remote.dto.LegalDocumentDto
import com.pixstop.mobile.domain.model.LegalDocument
import com.pixstop.mobile.domain.model.Outcome
import com.pixstop.mobile.domain.model.map
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody

private const val TAG = "Legal"

/**
 * Documentos legais e o aceite.
 *
 * As duas rotas ficam fora do bloqueio de consentimento no servidor — se não
 * ficassem, a tela de aceite não conseguiria nem carregar o que pede aceite.
 */
class LegalRepository(private val client: HttpClient) {

    suspend fun documents(): Outcome<List<LegalDocument>> =
        safeCall<List<LegalDocumentDto>>(TAG) {
            client.get(ApiConfig.Endpoints.LEGAL_DOCUMENTS)
        }.map { list -> list.map { it.toDomain() } }

    /**
     * Aceita os documentos. Sem lista, o servidor registra tudo em vigor.
     */
    suspend fun accept(documentIds: List<Long>? = null): Outcome<LegalConsentResultDto> =
        safeCall(TAG) {
            client.post(ApiConfig.Endpoints.LEGAL_CONSENT) {
                setBody(LegalConsentRequest(documentIds))
            }
        }
}

private fun LegalDocumentDto.toDomain() = LegalDocument(
    id = id,
    type = type,
    typeLabel = typeLabel,
    title = title,
    version = version,
    // O texto chega marcado porque o mesmo conteúdo alimenta o site.
    content = HtmlText.toPlainText(content),
    accepted = accepted,
)

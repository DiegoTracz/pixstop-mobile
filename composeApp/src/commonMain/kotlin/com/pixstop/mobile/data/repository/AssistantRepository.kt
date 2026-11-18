package com.pixstop.mobile.data.repository

import com.pixstop.mobile.core.config.ApiConfig
import com.pixstop.mobile.core.network.safeCall
import com.pixstop.mobile.data.mapper.AssistantMapper
import com.pixstop.mobile.data.remote.dto.AssistantAttachmentEnvelopeDto
import com.pixstop.mobile.data.remote.dto.AssistantConversationListDto
import com.pixstop.mobile.data.remote.dto.AssistantDecideDto
import com.pixstop.mobile.data.remote.dto.AssistantSendDto
import com.pixstop.mobile.data.remote.dto.AssistantThreadDto
import com.pixstop.mobile.domain.model.ChatAttachment
import com.pixstop.mobile.domain.model.ChatConversation
import com.pixstop.mobile.domain.model.ChatThread
import com.pixstop.mobile.domain.model.Outcome
import com.pixstop.mobile.domain.model.map
import io.ktor.client.HttpClient
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders

private const val TAG = "Assistant"

/**
 * O chat do assistente de IA (ASSISTENTE_IA.md, A8): as mesmas rotas do
 * painel, pela API do app. O servidor guarda tudo; o app só mostra.
 */
class AssistantRepository(private val client: HttpClient) {

    suspend fun conversations(): Outcome<List<ChatConversation>> =
        safeCall<AssistantConversationListDto>(TAG) {
            client.get(ApiConfig.Endpoints.COMPANY_ASSISTANT_CONVERSATIONS)
        }.map { dto -> dto.conversations.map { ChatConversation(it.id, it.title) } }

    suspend fun conversation(id: Long): Outcome<ChatThread> =
        safeCall<AssistantThreadDto>(TAG) {
            client.get(ApiConfig.Endpoints.companyAssistantConversation(id))
        }.map(AssistantMapper::thread)

    /** Sem conversa, o servidor abre uma nova com o começo da mensagem como título. */
    suspend fun send(conversationId: Long?, content: String, attachmentIds: List<Long>): Outcome<ChatThread> =
        safeCall<AssistantThreadDto>(TAG) {
            client.post(ApiConfig.Endpoints.COMPANY_ASSISTANT_MESSAGES) {
                setBody(AssistantSendDto(conversationId, content, attachmentIds))
            }
        }.map(AssistantMapper::thread)

    /** O botão do card: confirma ou cancela o resumo, e a conversa volta inteira. */
    suspend fun decide(conversationId: Long, toolName: String, token: String, confirm: Boolean): Outcome<ChatThread> =
        safeCall<AssistantThreadDto>(TAG) {
            client.post(ApiConfig.Endpoints.companyAssistantDecide(conversationId)) {
                setBody(AssistantDecideDto(toolName, token, if (confirm) "confirm" else "cancel"))
            }
        }.map(AssistantMapper::thread)

    suspend fun uploadPhoto(jpeg: ByteArray): Outcome<ChatAttachment> =
        safeCall<AssistantAttachmentEnvelopeDto>(TAG) {
            client.post(ApiConfig.Endpoints.COMPANY_ASSISTANT_ATTACHMENTS) {
                setBody(
                    MultiPartFormDataContent(
                        formData {
                            append(
                                key = "photo",
                                value = jpeg,
                                headers = Headers.build {
                                    append(HttpHeaders.ContentType, ContentType.Image.JPEG.toString())
                                    // O Laravel valida pela extensão: sem nome, recusa antes de olhar.
                                    append(HttpHeaders.ContentDisposition, "filename=\"produto.jpg\"")
                                },
                            )
                        },
                    ),
                )
            }
        }.map { ChatAttachment(it.attachment.id) }
}

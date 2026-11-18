package com.pixstop.mobile.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * O chat do assistente de IA (ASSISTENTE_IA.md, A8 no servidor).
 *
 * O resultado de cada ferramenta (`result`) chega como veio do servidor: o
 * formato muda de ferramenta para ferramenta, e quem o transforma em card é
 * o `AssistantMapper`.
 */
@Serializable
data class AssistantConversationListDto(val conversations: List<AssistantConversationDto> = emptyList())

@Serializable
data class AssistantConversationDto(
    val id: Long,
    val title: String = "",
    @SerialName("last_message_at") val lastMessageAt: String? = null,
)

/** Uma conversa com as mensagens; é o que o envio e o botão do card devolvem. */
@Serializable
data class AssistantThreadDto(
    val conversation: AssistantConversationDto? = null,
    val messages: List<AssistantMessageDto> = emptyList(),
)

@Serializable
data class AssistantMessageDto(
    val id: Long,
    val role: String,
    val content: String? = null,
    @SerialName("tool_name") val toolName: String? = null,
    val result: JsonObject? = null,
    @SerialName("is_confirmation") val isConfirmation: Boolean = false,
    val attachments: List<AssistantAttachmentDto> = emptyList(),
)

@Serializable
data class AssistantAttachmentDto(
    val id: Long,
    val mime: String = "",
    val url: String = "",
)

@Serializable
data class AssistantAttachmentEnvelopeDto(val attachment: AssistantAttachmentDto)

@Serializable
data class AssistantSendDto(
    @SerialName("conversation_id") val conversationId: Long? = null,
    val content: String,
    @SerialName("attachment_ids") val attachmentIds: List<Long> = emptyList(),
)

@Serializable
data class AssistantDecideDto(
    @SerialName("tool_name") val toolName: String,
    @SerialName("confirmation_token") val confirmationToken: String,
    val decision: String,
)

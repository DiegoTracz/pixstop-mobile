package com.pixstop.mobile.domain.model

/**
 * O chat do assistente de IA, como o app mostra (ASSISTENTE_IA.md, A8).
 */
enum class ChatRole { User, Assistant, Tool, Confirmation }

data class ChatMessage(
    val id: Long,
    val role: ChatRole,
    val text: String,
    val photos: Int = 0,
    val card: ToolCard? = null,
    val isError: Boolean = false,
)

/**
 * O resultado de uma ferramenta, pronto para a tela: o resumo de uma escrita
 * com os botões, as listas e o que mudou.
 */
data class ToolCard(
    val toolName: String,
    val title: String,
    val message: String,
    val success: Boolean,
    val fields: List<Pair<String, String>> = emptyList(),
    val changes: List<String> = emptyList(),
    val notes: List<String> = emptyList(),
    val rows: List<CardRow> = emptyList(),
    val confirmationToken: String? = null,
    val decision: String? = null,
) {
    /** O resumo ainda espera o "sim" de quem pediu. */
    val awaitingConfirmation: Boolean get() = confirmationToken != null && decision == null
}

data class CardRow(val title: String, val detail: String)

data class ChatConversation(val id: Long, val title: String)

data class ChatThread(val conversation: ChatConversation?, val messages: List<ChatMessage>)

data class ChatAttachment(val id: Long)

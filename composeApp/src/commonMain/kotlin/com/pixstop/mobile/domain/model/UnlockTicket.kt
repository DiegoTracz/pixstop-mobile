package com.pixstop.mobile.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import com.pixstop.mobile.core.text.IsoInstant
import kotlinx.serialization.json.Json

/**
 * O bilhete de abertura por Bluetooth (Fase 9.7), do ponto de vista do app.
 *
 * O celular é o carteiro: recebe o bilhete assinado pelo servidor e o entrega
 * à geladeira, que confere a assinatura sem rede. O app não valida nada — não
 * tem a chave, e não deveria ter — mas **lê** o conteúdo, que é público, para
 * saber duas coisas: qual geladeira procurar no rádio e até quando o bilhete
 * vale. Assim a tela não precisa perguntar isso ao servidor de novo.
 */
data class UnlockTicket(
    /** O bilhete inteiro, como veio: é isto que se escreve na geladeira. */
    val raw: String,
    /** O identificador do aparelho, que diz qual geladeira procurar. */
    val deviceIdentifier: String,
    val orderId: Long,
    val commandId: Int,
    /** Fim da validade, em milissegundos. */
    val expiresAt: Long?,
) {
    /**
     * O nome que a geladeira anuncia: `Pixelstop` mais os quatro primeiros
     * caracteres do identificador. É o que separa uma geladeira da outra numa
     * copa com duas.
     */
    val advertisedName: String get() = "Pixelstop " + deviceIdentifier.take(NAME_PREFIX_LENGTH)

    companion object {
        /** Quantos caracteres do identificador cabem no nome anunciado. */
        const val NAME_PREFIX_LENGTH = 4

        private val json = Json { ignoreUnknownKeys = true }

        /**
         * Lê o conteúdo do bilhete. Nulo quando não dá para ler: um bilhete
         * ilegível é um bilhete que não serve, e a tela volta ao caminho
         * normal em vez de oferecer um Bluetooth que não vai a lugar nenhum.
         */
        fun parse(ticket: String?): UnlockTicket? {
            val payload = ticket?.substringBefore('.')?.takeIf { it.isNotBlank() && it != ticket } ?: return null
            val decoded = decodeBase64Url(payload) ?: return null

            val body = runCatching { json.decodeFromString<TicketBody>(decoded) }.getOrNull() ?: return null

            if (body.device.isBlank()) {
                return null
            }

            return UnlockTicket(
                raw = ticket,
                deviceIdentifier = body.device,
                orderId = body.order,
                commandId = body.command,
                expiresAt = IsoInstant.toEpochMillis(body.expiresAt),
            )
        }
    }
}

/**
 * O que o servidor assina. Só o que o app precisa ler; o resto é para a
 * geladeira conferir.
 */
@Serializable
private data class TicketBody(
    @SerialName("dev") val device: String,
    @SerialName("order") val order: Long = 0,
    @SerialName("cmd") val command: Int = 0,
    @SerialName("exp") val expiresAt: String? = null,
)

/**
 * Base64 na variante de URL, sem o preenchimento.
 *
 * Escrito à mão de propósito: são doze linhas, funcionam igual em toda
 * plataforma, e devolvem nulo em vez de estourar quando a entrada é lixo —
 * que é exatamente o que se quer de um dado que veio de fora.
 */
private fun decodeBase64Url(text: String): String? {
    val alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_"
    val bytes = ArrayList<Byte>(text.length * 3 / 4)
    var buffer = 0
    var bits = 0

    for (char in text) {
        if (char == '=') break

        val value = alphabet.indexOf(char)

        if (value < 0) return null

        buffer = (buffer shl 6) or value
        bits += 6

        if (bits >= 8) {
            bits -= 8
            bytes.add(((buffer shr bits) and 0xFF).toByte())
        }
    }

    return runCatching { bytes.toByteArray().decodeToString() }.getOrNull()
}

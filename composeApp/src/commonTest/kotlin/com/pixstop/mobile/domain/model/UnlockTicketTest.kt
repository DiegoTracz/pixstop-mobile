package com.pixstop.mobile.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * O bilhete que o celular leva até a porta (Fase 9.7).
 *
 * O app não valida nada — não tem a chave, e não deveria ter —, mas lê o
 * conteúdo para saber qual geladeira procurar. Um bilhete ilegível tem de
 * virar nulo, e não exceção: a tela volta ao caminho normal em vez de
 * oferecer um Bluetooth que não vai a lugar nenhum.
 */
class UnlockTicketTest {

    /** O mesmo formato que o `UnlockTicketService` assina: corpo.assinatura. */
    private fun ticketOf(body: String, signature: String = "YXNzaW5hdHVyYQ"): String =
        base64Url(body) + "." + signature

    private fun base64Url(text: String): String {
        val alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_"
        val bytes = text.encodeToByteArray()
        val out = StringBuilder()
        var buffer = 0
        var bits = 0

        for (byte in bytes) {
            buffer = (buffer shl 8) or (byte.toInt() and 0xFF)
            bits += 8

            while (bits >= 6) {
                bits -= 6
                out.append(alphabet[(buffer shr bits) and 0x3F])
            }
        }

        if (bits > 0) {
            out.append(alphabet[(buffer shl (6 - bits)) and 0x3F])
        }

        return out.toString()
    }

    @Test
    fun `le o aparelho, o pedido e a validade do bilhete`() {
        val ticket = UnlockTicket.parse(
            ticketOf(
                """{"v":1,"kid":"k-teste","dev":"NHMBE0CXJ5","order":42,"cmd":7,"pulse_ms":400,
                   "melody":"mario","nonce":"01M2","iat":"2026-09-09T18:00:00+00:00","exp":"2026-09-09T18:10:00+00:00"}""",
            ),
        )

        assertEquals("NHMBE0CXJ5", ticket?.deviceIdentifier)
        assertEquals(42L, ticket?.orderId)
        assertEquals(7, ticket?.commandId)
        assertEquals(1788977400000L, ticket?.expiresAt)
    }

    @Test
    fun `o nome anunciado tem os quatro primeiros do identificador`() {
        val ticket = UnlockTicket.parse(ticketOf("""{"kid":"k","dev":"NHMBE0CXJ5","exp":"2026-09-09T18:10:00+00:00"}"""))

        // É por este nome que o app separa uma geladeira da outra na copa.
        //
        // Este nome é contrato com o agente do Pi, que o anuncia por Bluetooth:
        // mudou nos dois no mesmo commit. Só existe Pi na bancada, então não há
        // geladeira antiga para deixar de ser achada.
        assertEquals("Pixelstop NHMB", ticket?.advertisedName)
    }

    @Test
    fun `o bilhete inteiro viaja como veio, sem o app tocar nele`() {
        val raw = ticketOf("""{"kid":"k","dev":"ABCDEFGHIJ","exp":"2026-09-09T18:10:00+00:00"}""")

        assertEquals(raw, UnlockTicket.parse(raw)?.raw)
    }

    @Test
    fun `lixo, vazio e bilhete sem aparelho viram nulo`() {
        assertNull(UnlockTicket.parse(null))
        assertNull(UnlockTicket.parse(""))
        assertNull(UnlockTicket.parse("sem-ponto-nenhum"))
        assertNull(UnlockTicket.parse("!!!.assinatura"))
        assertNull(UnlockTicket.parse(ticketOf("nem json")))
        assertNull(UnlockTicket.parse(ticketOf("""{"kid":"k","dev":""}""")))
    }
}

package com.pixstop.mobile.core.text

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * O contador da reserva do carrinho depende inteiramente desta conta. Errar o
 * fuso por uma hora faria o app dizer que o item já venceu — ou que ainda há
 * tempo quando o servidor já devolveu o estoque.
 */
class IsoInstantTest {

    @Test
    fun `converte o instante em UTC`() {
        assertEquals(0L, IsoInstant.toEpochMillis("1970-01-01T00:00:00+00:00"))
        assertEquals(1_000L, IsoInstant.toEpochMillis("1970-01-01T00:00:01Z"))
    }

    @Test
    fun `desconta o deslocamento do fuso`() {
        val utc = IsoInstant.toEpochMillis("2026-09-05T13:34:32+00:00")
        val brasilia = IsoInstant.toEpochMillis("2026-09-05T10:34:32-03:00")

        assertEquals(utc, brasilia, "o mesmo instante escrito em dois fusos")
    }

    @Test
    fun `aceita fracao de segundo e deslocamento sem dois pontos`() {
        val comFracao = IsoInstant.toEpochMillis("2026-09-05T13:34:32.123456Z")
        val semFracao = IsoInstant.toEpochMillis("2026-09-05T13:34:32Z")

        assertEquals(semFracao, comFracao)
        assertEquals(semFracao, IsoInstant.toEpochMillis("2026-09-05T13:34:32+0000"))
    }

    @Test
    fun `acerta ano bissexto`() {
        val vinteNoveFev = IsoInstant.toEpochMillis("2024-02-29T00:00:00Z")!!
        val primeiroMarco = IsoInstant.toEpochMillis("2024-03-01T00:00:00Z")!!

        assertEquals(86_400_000L, primeiroMarco - vinteNoveFev)
    }

    @Test
    fun `texto invalido devolve nulo em vez de estourar`() {
        assertNull(IsoInstant.toEpochMillis(null))
        assertNull(IsoInstant.toEpochMillis(""))
        assertNull(IsoInstant.toEpochMillis("ontem"))
        assertNull(IsoInstant.toEpochMillis("2026-09-05"))
    }
}

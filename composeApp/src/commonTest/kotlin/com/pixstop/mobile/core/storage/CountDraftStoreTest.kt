package com.pixstop.mobile.core.storage

import com.pixstop.mobile.support.InMemorySettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * O rascunho da contagem (CONCILIACAO_MOBILE.md).
 *
 * Uma copa sem sinal é comum, e refazer trinta linhas é o que faz alguém
 * parar de contar. O `clientId` viaja junto porque é ele que torna o reenvio
 * inofensivo: a mesma contagem entregue duas vezes vira um documento só.
 */
class CountDraftStoreTest {

    @Test
    fun `o rascunho volta como ficou, com o mesmo client id`() {
        val store = CountDraftStore(InMemorySettings())

        store.save(3, CountDraft(clientId = "01J", startedAt = "2026-09-20T11:00:00Z", counts = mapOf("1" to 6, "2" to 0)))

        val restored = store.load(3)

        assertEquals("01J", restored?.clientId)
        assertEquals(6, restored?.counts?.get("1"))
        // Zero contado é uma afirmação, e sobrevive à volta.
        assertEquals(0, restored?.counts?.get("2"))
    }

    @Test
    fun `cada geladeira tem o seu, e apagar um nao mexe no outro`() {
        val store = CountDraftStore(InMemorySettings())

        store.save(3, CountDraft("a", "2026-09-20T11:00:00Z", mapOf("1" to 6)))
        store.save(4, CountDraft("b", "2026-09-20T11:00:00Z", mapOf("9" to 2)))

        store.clear(3)

        assertNull(store.load(3))
        assertEquals("b", store.load(4)?.clientId)
    }

    @Test
    fun `rascunho ilegivel e o mesmo que rascunho nenhum`() {
        val settings = InMemorySettings()
        val store = CountDraftStore(settings)

        settings.putString("count_draft_3", "{isto não é json")

        assertNull(store.load(3))
    }
}

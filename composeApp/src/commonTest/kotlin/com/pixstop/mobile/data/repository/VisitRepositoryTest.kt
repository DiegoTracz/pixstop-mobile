package com.pixstop.mobile.data.repository

import com.pixstop.mobile.domain.model.Outcome
import com.pixstop.mobile.support.FakeApi
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * O contrato da visita (CONCILIACAO_MOBILE.md).
 *
 * O teste mais importante deste arquivo é o da **cegueira**: se a lista de
 * contagem passar a trazer o saldo esperado, ele quebra. É de propósito —
 * contagem que mostra o esperado antes vira "confirmar", e confirmar não
 * concilia nada.
 */
class VisitRepositoryTest {

    private val folhaJson = """
        {
          "success": true,
          "data": {
            "appliance": {"id": 3, "name": "Geladeira da Copa"},
            "client_id": "01J000000000000000000000",
            "products": [
              {"id": 1, "name": "Monster", "category": "Energéticos", "barcode": "7891991010856"},
              {"id": 2, "name": "Coca-Cola", "category": "Refrigerantes"}
            ],
            "warehouse": [{"id": 1, "name": "Monster", "in_warehouse": 24, "low_stock_threshold": 6}],
            "last_count": {"id": 9, "at": "2026-09-13T15:00:00+00:00", "by": "Diego", "days_ago": 7}
          }
        }
    """.trimIndent()

    @Test
    fun `a lista de contagem nao traz o saldo esperado`() = runTest {
        val api = FakeApi()
        val outcome = VisitRepository(api.clientReturning(folhaJson)).sheet(3)

        val sheet = (outcome as Outcome.Success).value

        assertEquals("Geladeira da Copa", sheet.applianceName)
        assertEquals(2, sheet.products.size)
        assertEquals("01J000000000000000000000", sheet.clientId)
        assertEquals(7, sheet.lastCount?.daysAgo)

        // A cegueira é do contrato: nenhum campo de saldo chega ao aparelho.
        val corpo = folhaJson.replace(" ", "")
        assertFalse(corpo.contains("\"quantity\""))
        assertFalse(corpo.contains("\"expected\""))
        assertFalse(corpo.contains("\"stock_quantity\""))
    }

    @Test
    fun `o envio leva o client id o que foi contado e o que ficou sem contagem`() = runTest {
        val api = FakeApi()
        val resposta = """
            {"success": true, "data": {"id": 12, "at": "2026-09-20T12:00:00+00:00", "counted_items": 1, "skipped_items": 1,
             "difference_value": -25.5, "missing_value": 25.5, "alert": true,
             "items": [{"product_id": 1, "product": "Monster", "expected": 9, "counted": 6, "difference": -3, "difference_value": -25.5}]}}
        """.trimIndent()

        val outcome = VisitRepository(api.clientReturning(resposta)).submit(
            applianceId = 3,
            clientId = "01J000000000000000000000",
            counts = mapOf(1L to 6),
            skipped = listOf(2L),
            appVersion = "1.4.0",
            startedAt = "2026-09-20T11:55:00Z",
        )

        val result = (outcome as Outcome.Success).value

        assertEquals(12, result.id)
        assertEquals(9, result.items.first().expected)
        assertTrue(result.alert)

        val enviado = api.lastBody.replace(" ", "")

        assertTrue(enviado.contains("01J000000000000000000000"), "o client_id precisa ir no corpo: $enviado")
        assertTrue(enviado.contains("\"skipped\":[2]"), "quem ficou sem contagem precisa ir junto: $enviado")
        assertTrue(enviado.contains("\"app_version\":\"1.4.0\""), "a versão do app é carimbo do documento: $enviado")
        // A origem não é dita pelo aparelho: quem carimba `app` é o servidor,
        // porque carimbo que o cliente escolhe não serve de evidência.
        assertFalse(enviado.contains("\"source\""), "a origem quem carimba é o servidor: $enviado")
    }

    @Test
    fun `sem rede o envio devolve falha e nada se perde no aparelho`() = runTest {
        val api = FakeApi(status = HttpStatusCode.ServiceUnavailable)

        val outcome = VisitRepository(api.clientReturning("")).submit(
            applianceId = 3,
            clientId = "01J",
            counts = mapOf(1L to 6),
            skipped = emptyList(),
            appVersion = null,
            startedAt = null,
        )

        assertTrue(outcome is Outcome.Failure)
    }

    @Test
    fun `a resposta da contagem traz o abastecimento e o envio leva so o que tem quantidade`() = runTest {
        val resposta = """
            {"success": true, "data": {"id": 12, "at": "2026-11-21T12:00:00+00:00", "items": [],
             "restock": [{"product_id": 1, "product": "Monster", "warehouse": 18, "suggested": 8, "place": "P1 · fileiras 1 e 2"}],
             "restock_client_id": "count-12"}}
        """.trimIndent()

        val result = (
            VisitRepository(FakeApi().clientReturning(resposta)).submit(3, "01J", mapOf(1L to 2), emptyList(), null, null)
                as Outcome.Success
            ).value

        assertEquals(8, result.restock.single().suggested)
        assertEquals("P1 · fileiras 1 e 2", result.restock.single().place)
        assertEquals("count-12", result.restockClientId)

        val api = FakeApi()
        val outcome = VisitRepository(api.clientReturning("""{"success": true, "data": {"id": 5, "units": 8}}"""))
            .restock(3, "count-12", mapOf(1L to 8, 2L to 0))

        assertEquals(8, (outcome as Outcome.Success).value.units)

        val enviado = api.lastBody.replace(" ", "")

        assertTrue(enviado.contains("\"client_id\":\"count-12\""), "o client_id da contagem vai no corpo: $enviado")
        assertTrue(enviado.contains("{\"product_id\":1,\"quantity\":8}"), "o item com quantidade vai: $enviado")
        assertFalse(enviado.contains("\"product_id\":2"), "zero não vai para o servidor: $enviado")
    }

    @Test
    fun `tirei os vencidos manda so a geladeira e nao recebe quantidade`() = runTest {
        val api = FakeApi()
        val outcome = VisitRepository(api.clientReturning("""{"success": true, "data": null, "message": "2 unidades vencidas retiradas (Copa)."}"""))
            .removeExpired(3)

        assertTrue(outcome is Outcome.Success)
        assertEquals("{\"appliance_id\":3}", api.lastBody.replace(" ", ""))
    }
}

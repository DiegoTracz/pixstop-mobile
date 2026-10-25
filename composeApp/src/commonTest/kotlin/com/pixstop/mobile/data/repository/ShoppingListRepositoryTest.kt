package com.pixstop.mobile.data.repository

import com.pixstop.mobile.domain.model.Outcome
import com.pixstop.mobile.support.FakeApi
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** O contrato da lista de compras (COMPRAS_MOBILE.md, A0). */
class ShoppingListRepositoryTest {

    private val listaJson = """
        {"success": true, "data": {
          "appliances": [{"id": 1, "name": "Geladeira da Bancada"}],
          "appliance": null, "company": "Locarmais", "weekday": 1,
          "list": {
            "rows": [{"product_id": 7, "name": "Monster Ultra Fiest Mango Loco 473ml", "category": "Energéticos", "image_url": null,
                      "stock": 0, "warehouse_stock": 0, "sold_window": 39, "sold_recent": 12, "weekly_rate": 9.8,
                      "zeroed_since": "2026-10-16", "suggested": 12, "unit_cost": 8.99, "estimated_cost": 107.88}],
            "covered": [{"product_id": 9, "name": "Kit Kat", "stock": 19, "suggested": 0, "unit_cost": 4.49}],
            "cover_days": 7, "safety_margin": 0.2, "selling_days_per_week": 5.0, "total_cost": 107.88
          }
        }}
    """.trimIndent()

    @Test
    fun `a lista vem pronta do servidor`() = runTest {
        val list = (ShoppingListRepository(FakeApi().clientReturning(listaJson)).list() as Outcome.Success).value

        assertEquals("Locarmais", list.company)
        assertEquals(20, list.safetyPercent)
        assertEquals(1, list.weekday)
        assertEquals("2026-10-16", list.rows.single().zeroedSince)
        assertEquals(12, list.rows.single().suggested)
        assertEquals("Kit Kat", list.covered.single().name)
        assertNull(list.applianceId)
    }

    @Test
    fun `uma geladeira vai como parametro`() = runTest {
        val api = FakeApi()
        ShoppingListRepository(api.clientReturning(listaJson)).list(applianceId = 1)

        assertEquals("1", api.lastRequest?.url?.parameters?.get("appliance"))
    }

    @Test
    fun `nao avisar vai como nulo explicito, nao como ausente`() = runTest {
        val api = FakeApi()
        ShoppingListRepository(api.clientReturning("""{"success": true, "data": null}""")).saveSettings(14, 10, null)

        assertTrue(api.lastBody.contains("\"weekday\":null"))
        assertTrue(api.lastBody.contains("\"cover_days\":14"))
    }

    @Test
    fun `vou comprar leva so o que tem quantidade, com a esperada`() = runTest {
        val api = FakeApi()
        val repository = ShoppingListRepository(api.clientReturning(listaJson))
        val list = (repository.list() as Outcome.Success).value

        api.nextBody = """{"success": true, "data": {"id": 15, "status": "draft"}}"""
        val id = (repository.startPurchase(mapOf(list.rows.single() to 10, list.covered.single() to 0)) as Outcome.Success).value

        assertEquals(15L, id)
        assertTrue(api.lastBody.contains("\"source\":\"list\""))
        assertTrue(api.lastBody.contains("\"expected_quantity\":10"))
        assertFalse(api.lastBody.contains("\"product_id\":9"))
    }

    @Test
    fun `o operador sem dinheiro recebe a lista sem custo`() = runTest {
        val api = FakeApi()
        val shopping = (
            OperatorRepository(
                api.clientReturning(
                    """{"success": true, "data": {"total_cost": null, "lines": [
                       {"key": "op:1", "name": "Monster Ultra 473ml", "suggested": 15, "zeroed": 1,
                        "companies": [{"tenant": "Empresa A", "tenant_id": "a", "suggested": 9, "stock": 3, "zeroed_since": null},
                                      {"tenant": "Empresa B", "tenant_id": "b", "suggested": 6, "stock": 0, "zeroed_since": "2026-10-23"}]}
                    ]}}""",
                ),
            ).shoppingList() as Outcome.Success
            ).value

        assertNull(shopping.totalCost)
        assertNull(shopping.lines.single().estimatedCost)
        assertEquals(2, shopping.lines.single().companies.size)
    }
}

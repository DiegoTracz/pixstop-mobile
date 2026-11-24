package com.pixstop.mobile.data.mapper

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** O card do app tem de mostrar o mesmo resumo do painel. */
class AssistantMapperTest {

    private fun json(text: String): JsonObject = Json.parseToJsonElement(text).jsonObject

    @Test
    fun `o resumo de cadastro mostra campos avisos e espera o sim`() {
        val card = AssistantMapper.card(
            "create_product",
            json(
                """{"success": true, "message": "Vou cadastrar", "preview": true, "requires_user_confirmation": true,
                   "confirmation_token": "abc", "data": {"name": "Fanta Uva", "price": 5, "cost": 2.8, "margin_percent": 44,
                   "review_notes": ["Sem categoria."]}}""",
            ),
            "",
        )

        assertTrue(card.awaitingConfirmation)
        assertEquals("abc", card.confirmationToken)
        assertEquals(listOf("Nome" to "Fanta Uva", "Preço" to "R$ 5,00", "Custo" to "R$ 2,80", "Margem (%)" to "44"), card.fields)
        assertEquals(listOf("Sem categoria."), card.notes)
    }

    @Test
    fun `depois do botao o card nao pede confirmacao de novo`() {
        val card = AssistantMapper.card(
            "create_product",
            json("""{"success": true, "message": "ok", "requires_user_confirmation": false, "confirmation_token": null, "decision": "confirmed", "data": {}}"""),
            "",
        )

        assertFalse(card.awaitingConfirmation)
        assertNull(card.confirmationToken)
        assertEquals("confirmed", card.decision)
    }

    @Test
    fun `parecidos e catalogo aparecem juntos e as geladeiras com a empresa`() {
        val lookalikes = AssistantMapper.card(
            "create_product_from_photo",
            json(
                """{"success": true, "message": "Já existe", "data": {
                   "similar_products": [{"product_id": 13, "name": "Coca-Cola 200ml", "price": 4.5, "total_stock": 24}],
                   "catalog_matches": [{"catalog_product_id": 2, "name": "Coca-Cola Original 200ml", "has_photo": true}]}}""",
            ),
            "",
        )
        assertEquals(listOf("Coca-Cola 200ml", "Coca-Cola Original 200ml"), lookalikes.rows.map { it.title })
        assertTrue(lookalikes.rows[0].detail.contains("já cadastrado, R$ 4,50"))
        assertTrue(lookalikes.rows[1].detail.contains("catálogo da plataforma, com foto"))

        val overview = AssistantMapper.card(
            "companies_overview",
            json("""{"success": true, "message": "2 pontos", "data": {"appliances": [{"company": "Fábrica Norte", "appliance": "Refeitório", "orders": 1, "units": 3, "revenue": 25}]}}"""),
            "",
        )
        assertEquals("Refeitório", overview.rows.single().title)
        assertEquals("Fábrica Norte · 1 pedidos · 3 un. · R$ 25,00", overview.rows.single().detail)
    }

    @Test
    fun datasDoServidorAparecemNoJeitoBrasileiro() {
        assertEquals("16/11/2026", AssistantMapper.brazilianDate("2026-11-16"))
        assertEquals("16/11/2026 14:05", AssistantMapper.brazilianDate("2026-11-16T14:05:00-03:00"))
        assertEquals("ontem", AssistantMapper.brazilianDate("ontem"))
    }
}

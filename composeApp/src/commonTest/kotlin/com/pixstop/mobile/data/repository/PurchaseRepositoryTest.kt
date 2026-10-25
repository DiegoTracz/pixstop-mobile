package com.pixstop.mobile.data.repository

import com.pixstop.mobile.domain.model.DomainError
import com.pixstop.mobile.domain.model.Outcome
import com.pixstop.mobile.domain.model.PurchaseStatus
import com.pixstop.mobile.support.FakeApi
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * O contrato da compra (COMPRAS_MOBILE.md, A0).
 *
 * As respostas copiam o que o servidor devolveu para a nota real do Assaí de
 * 14/out/2026: desconto por item, sacola como "não é estoque", Sprite casada
 * pelo código de barras.
 */
class PurchaseRepositoryTest {

    private val folhaJson = """
        {"success": true, "data": {
          "purchase": {"id": 3, "label": "Compra #3", "status": "draft", "status_label": "Rascunho", "source": "receipt",
            "editable": true, "supplier_name": null, "issued_at": "2026-10-14", "document_number": null, "access_key": null,
            "total": 566.12, "appliance_id": null,
            "items": [
              {"line_no": 1, "raw_description": "CH KIT KAT 41,5G LEI", "raw_code": "10657130001", "raw_quantity": 24.0, "raw_unit": "Un",
               "raw_unit_price": 4.79, "raw_discount": 7.2, "raw_total": 107.76, "product_id": 58, "product_name": "Kit Kat",
               "quantity_units": 24, "pack_size": 1, "unit_cost": 4.49, "status": "matched", "match_source": "ai", "confidence": 0.9},
              {"line_no": 2, "raw_description": "SAC ASS VD OXI 58X70", "raw_quantity": 3.0, "raw_total": 0.9, "product_id": null,
               "quantity_units": 3, "pack_size": 1, "unit_cost": 0.3, "status": "ignored"},
              {"line_no": 3, "raw_description": "SPRITE 350ML S/ACUC", "raw_code": "7894900060010", "product_id": 77,
               "quantity_units": 6, "pack_size": 1, "unit_cost": 3.49, "status": "matched", "match_source": "barcode", "confidence": 1.0}
            ]},
          "destination": "Depósito",
          "duplicate": {"id": 1, "label": "Nota 288377 · Assaí"},
          "products": [{"id": 58, "name": "Kit Kat", "barcode": null, "price_cost": 4.49, "stock": 19}],
          "appliances": [{"id": 1, "name": "Geladeira da Copa"}],
          "reading": false,
          "receipt": {"url": "https://exemplo.test/api/company/purchases/3/receipt", "kind": "image"},
          "ai": {"error": null, "warnings": ["A data lida (14/10/2023) parece errada. Confira a data da nota."], "model": "gpt-5-mini", "cost_usd": 0.0094}
        }}
    """.trimIndent()

    @Test
    fun `a foto da nota sobe em multipart e volta so o id`() = runTest {
        val api = FakeApi(HttpStatusCode.Accepted)
        val outcome = PurchaseRepository(api.clientReturning("""{"success": true, "data": {"id": 12, "status": "reading"}}"""))
            .uploadReceipt(byteArrayOf(1, 2, 3))

        assertEquals(12L, (outcome as Outcome.Success).value)
        assertEquals(HttpMethod.Post, api.lastRequest?.method)
        assertTrue(api.lastRequest!!.url.encodedPath.endsWith("company/purchases/receipt"))
        assertTrue(api.lastRequest!!.body.contentType.toString().startsWith("multipart/form-data"))
    }

    @Test
    fun `a folha traz o que a ia leu, a nota e os avisos`() = runTest {
        val sheet = (PurchaseRepository(FakeApi().clientReturning(folhaJson)).show(3) as Outcome.Success).value

        assertEquals(PurchaseStatus.Draft, sheet.status)
        assertFalse(sheet.reading)
        assertEquals(3, sheet.lines.size)
        assertEquals(7.2, sheet.lines[0].rawDiscount)
        assertEquals(4.49, sheet.lines[0].unitCost)
        assertEquals("ai", sheet.lines[0].matchSource)
        assertTrue(sheet.lines[1].ignored)
        assertEquals("barcode", sheet.lines[2].matchSource)
        assertEquals("Nota 288377 · Assaí", sheet.duplicate?.name)
        assertEquals("https://exemplo.test/api/company/purchases/3/receipt", sheet.receiptUrl)
        assertEquals(1, sheet.warnings.size)
        assertNull(sheet.readingError)
        assertEquals("", sheet.supplierName)
    }

    @Test
    fun `salvar devolve os campos crus da nota e a linha ignorada sem produto`() = runTest {
        val api = FakeApi()
        val repository = PurchaseRepository(api.clientReturning(folhaJson))
        val sheet = (repository.show(3) as Outcome.Success).value

        api.nextBody = """{"success": true, "data": null, "message": "Rascunho salvo."}"""
        val outcome = repository.save(sheet.copy(supplierName = "Assaí Atacadista"))

        assertIs<Outcome.Success<Unit>>(outcome)
        assertEquals(HttpMethod.Patch, api.lastRequest?.method)
        val body = api.lastBody
        assertTrue(body.contains("\"supplier_name\":\"Assaí Atacadista\""))
        assertTrue(body.contains("\"raw_description\":\"CH KIT KAT 41,5G LEI\""))
        assertTrue(body.contains("\"raw_discount\":7.2"))
        assertTrue(body.contains("\"ignored\":true"))
        assertFalse(body.contains("\"product_id\":null"))
    }

    @Test
    fun `o que impede a entrada volta como mensagem do servidor`() = runTest {
        val api = FakeApi(HttpStatusCode.BadRequest)
        val outcome = PurchaseRepository(
            api.clientReturning("""{"success": false, "error": {"message": "Falta escolher o produto das linhas 8, ou marcá-las como \"não é estoque\"."}}"""),
        ).confirm(3)

        val error = (outcome as Outcome.Failure).error
        assertIs<DomainError.Rule>(error)
        assertTrue(error.message.startsWith("Falta escolher o produto das linhas 8"))
    }

    @Test
    fun `confirmar uma nota parecida manda a licenca explicita`() = runTest {
        val api = FakeApi()
        PurchaseRepository(api.clientReturning("""{"success": true, "data": null, "message": "ok"}""")).confirm(3, allowDuplicate = true)

        assertTrue(api.lastBody.contains("\"allow_duplicate\":true"))
    }

    @Test
    fun `a lista de compras usa o total da nota e cai na soma dos itens`() = runTest {
        val api = FakeApi()
        val index = (
            PurchaseRepository(
                api.clientReturning(
                    """{"success": true, "data": {"canReadReceipts": true, "purchases": {"current_page": 1, "last_page": 1, "data": [
                       {"id": 3, "label": "Compra #3 · Assaí", "issued_at": "2026-10-14", "status": "confirmed", "status_label": "Confirmada", "total": 566.12, "items_total": 565.22, "items": 12},
                       {"id": 4, "label": "Compra #4", "issued_at": null, "created_at": "2026-10-25T14:00:00-03:00", "status": "draft", "status_label": "Rascunho", "total": null, "items_total": 54.0, "items": 1}
                    ]}}}""",
                ),
            ).index() as Outcome.Success
            ).value

        assertTrue(index.canReadReceipts)
        assertEquals(566.12, index.purchases[0].total)
        assertEquals(54.0, index.purchases[1].total)
        assertEquals("2026-10-25", index.purchases[1].date)
        assertEquals(PurchaseStatus.Draft, index.purchases[1].status)
    }
}

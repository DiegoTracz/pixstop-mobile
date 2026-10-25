package com.pixstop.mobile.ui.viewmodel

import com.pixstop.mobile.domain.model.CatalogProduct
import com.pixstop.mobile.domain.model.Named
import com.pixstop.mobile.domain.model.PurchaseLine
import com.pixstop.mobile.domain.model.PurchaseSheet
import com.pixstop.mobile.domain.model.PurchaseStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * As regras da conferência (COMPRAS_MOBILE.md, A1), sem rede e sem Compose.
 * Os números vêm da nota real do Assaí: Kit Kat 24 × 4,79 − 7,20.
 */
class PurchaseUiStateTest {

    private val kitkat = CatalogProduct(58, "Kit Kat", null, 4.49, 19)
    private val monster = CatalogProduct(10, "Monster Ultra", null, 8.99, 0)

    private fun sheet(duplicate: Named? = null) = PurchaseSheet(
        id = 3, label = "Compra #3", status = PurchaseStatus.Draft, editable = true, reading = false,
        supplierName = "", documentNumber = "", issuedAt = "", total = 566.12, accessKey = "", applianceId = null,
        lines = emptyList(), products = listOf(kitkat, monster), appliances = emptyList(), duplicate = duplicate,
        receiptUrl = null, readingError = null, warnings = emptyList(),
    )

    private val kitkatLine = PurchaseLine(
        rawDescription = "CH KIT KAT 41,5G LEI", rawQuantity = 24.0, rawUnitPrice = 4.79, rawDiscount = 7.2, rawTotal = 107.76,
        productId = 58, productName = "Kit Kat", quantityUnits = 24, packSize = 1, unitCost = 4.49, matchSource = "ai", confidence = 0.9,
    )
    private val bag = PurchaseLine(rawDescription = "SAC ASS VD OXI", rawQuantity = 3.0, rawTotal = 0.9, ignored = true, quantityUnits = 3, unitCost = 0.3)
    private val unknown = PurchaseLine(rawDescription = "BEB MONSTER 473ML MG", rawQuantity = 6.0, rawTotal = 53.94, quantityUnits = 6, unitCost = 8.99)

    private fun state(vararg lines: PurchaseLine, total: String = "566,12", duplicate: Named? = null) =
        PurchaseUiState(step = PurchaseStep.Review, sheet = sheet(duplicate), lines = lines.toList(), totalText = total)

    @Test
    fun `pendente e so a linha sem produto que nao foi marcada como nao e estoque`() {
        val current = state(kitkatLine, bag, unknown)

        assertEquals(1, current.pending)
        assertFalse(current.canConfirm)
        assertTrue(state(kitkatLine, bag, unknown.copy(ignored = true)).canConfirm)
    }

    @Test
    fun `unidades e soma nao contam o que nao e estoque`() {
        val current = state(kitkatLine, bag)

        assertEquals(24, current.units)
        assertEquals(107.76, current.itemsTotal, 0.001)
    }

    @Test
    fun `a diferenca para o total da nota so aparece acima de um por cento`() {
        assertNull(state(kitkatLine, total = "108,00").totalGap)
        assertEquals(-12.24, state(kitkatLine, total = "120,00").totalGap!!, 0.001)
        assertNull(state(kitkatLine, total = "").totalGap)
    }

    @Test
    fun `nota parecida pede licenca antes de confirmar`() {
        val twin = Named(1, "Nota 288377 · Assaí")

        assertFalse(state(kitkatLine, duplicate = twin).canConfirm)
        assertTrue(state(kitkatLine, duplicate = twin).copy(allowDuplicate = true).canConfirm)
    }

    @Test
    fun `linha casada sem quantidade ou sem custo nao confirma`() {
        assertFalse(state(kitkatLine.copy(quantityUnits = 0)).canConfirm)
        assertFalse(state(kitkatLine.copy(unitCost = null)).canConfirm)
    }

    @Test
    fun `mudar a embalagem refaz unidades e custo pelo que a nota cobrou`() {
        // Duas caixas de seis por R$ 107,88 são doze latas a R$ 8,99.
        val box = PurchaseLine(rawQuantity = 2.0, rawTotal = 107.88, quantityUnits = 2, unitCost = 53.94)
        val changed = box.withPack(6)

        assertEquals(12, changed.quantityUnits)
        assertEquals(8.99, changed.unitCost!!, 0.0001)
        assertEquals(6, changed.packSize)

        // Linha digitada à mão não tem quantidade da nota: só guarda a embalagem.
        val typed = PurchaseLine(quantityUnits = 5, unitCost = 2.0).withPack(6)
        assertEquals(5, typed.quantityUnits)
    }

    @Test
    fun `escolher o produto traz o custo medio so quando o custo esta vazio`() {
        assertEquals(8.99, PurchaseLine(quantityUnits = 6).withProduct(monster).unitCost)
        assertEquals(8.5, PurchaseLine(quantityUnits = 6, unitCost = 8.5).withProduct(monster).unitCost)
        assertEquals("manual", PurchaseLine(ignored = true).withProduct(monster).matchSource)
        assertFalse(PurchaseLine(ignored = true).withProduct(monster).ignored)
    }

    @Test
    fun `o custo medio resultante e o mesmo da web`() {
        // Oito a R$ 8,50 e seis a R$ 9,00: R$ 8,71.
        val stock = CatalogProduct(1, "Monster", null, 8.5, 8)
        val average = PurchaseLine(productId = 1, quantityUnits = 6, unitCost = 9.0).averageAfter(stock)!!

        assertEquals(8.714, average, 0.001)
        assertEquals(9.0, PurchaseLine(productId = 10, quantityUnits = 6, unitCost = 9.0).averageAfter(monster))
    }

    @Test
    fun `dinheiro digitado com virgula ou ponto vira numero`() {
        assertEquals(566.12, parseMoney("566,12"))
        assertEquals(1234.56, parseMoney("1.234,56"))
        assertEquals(4.49, parseMoney("R$ 4.49"))
        assertNull(parseMoney(""))
        assertEquals("566,12", formatMoneyInput(566.12))
        assertEquals("4,49", formatMoneyInput(4.49))
    }
}

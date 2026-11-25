package com.pixstop.mobile.ui.viewmodel

import com.pixstop.mobile.domain.model.CatalogProduct
import com.pixstop.mobile.domain.model.Destination
import com.pixstop.mobile.domain.model.DestinationOptions
import com.pixstop.mobile.domain.model.FillPreview
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
    fun `a conferencia anda de item sem produto em item sem produto`() {
        val agua = PurchaseLine(rawDescription = "AGUA MIN 500ML", quantityUnits = 12)
        val current = state(kitkatLine, unknown, bag, agua)

        assertEquals(listOf(1, 3), current.pendingIndices)
        assertEquals(1, current.nextPending(null))
        assertEquals(3, current.nextPending(1))
        // Do último volta para o que sobrou atrás.
        assertEquals(1, current.nextPending(3))
        // O pulado não volta nesta conferência.
        assertNull(current.copy(skipped = setOf(1)).nextPending(3))
        assertEquals(2 to 2, current.walkPosition(3))
    }

    @Test
    fun `o tamanho da embalagem sai do nome da nota e do catalogo`() {
        assertEquals(1000.0 to "ml", packageSize("COCA COLA 1LT"))
        assertEquals(350.0 to "ml", packageSize("Coca-Cola Lata 350ml"))
        assertEquals(2000.0 to "ml", packageSize("Coca-Cola Zero Açúcar 2L"))
        assertEquals(36.0 to "g", packageSize("BISC OREO 4X36G ORIG"))
        assertEquals(41.5 to "g", packageSize("CH KIT KAT 41,5G"))
        assertNull(packageSize("SAC ASS VD OXI 58X70"))
    }

    @Test
    fun `o catalogo de outro tamanho nao vira foto do item da nota`() {
        val lata = com.pixstop.mobile.domain.model.CatalogSuggestion(1, "Coca-Cola Lata 350ml", "Bebidas", hasPhoto = true)
        val dois = com.pixstop.mobile.domain.model.CatalogSuggestion(2, "Coca-Cola Zero Açúcar 2L", "Bebidas", hasPhoto = true)
        val umLitro = com.pixstop.mobile.domain.model.CatalogSuggestion(3, "Coca-Cola Garrafa 1 Litro", "Bebidas", hasPhoto = true)
        val semTamanho = com.pixstop.mobile.domain.model.CatalogSuggestion(4, "Coca-Cola", "Bebidas", hasPhoto = true)
        val line = PurchaseLine(rawDescription = "COCA COLA 1LT")
        val current = state(line).copy(lineCatalog = listOf(lata, dois, umLitro, semTamanho))

        assertEquals(listOf(3L, 4L), current.catalogFor(line).map { it.id })
        assertEquals(2, current.otherSizes(line))
    }

    @Test
    fun `parece ser acha pelo nome e nao so pelo tamanho`() {
        val current = state(unknown)

        assertEquals(listOf(10L), current.likelyMatches(unknown).map { it.id })
        assertTrue(current.likelyMatches(PurchaseLine(rawDescription = "REFRIG LT 350ML")).isEmpty())
        assertEquals(listOf(58L), current.likelyMatches(PurchaseLine(rawDescription = "CH KIT KAT 41,5G")).map { it.id })
        // "ORIG" da nota não é nome de produto: o Oreo não vira Coca-Cola Original.
        val cocaOriginal = CatalogProduct(13, "Coca-Cola Sabor Original 200ml", null, 2.5, 24)
        assertTrue(state(unknown).copy(createdProducts = listOf(cocaOriginal)).likelyMatches(PurchaseLine(rawDescription = "BISC OREO 4X36G ORIG")).isEmpty())
    }

    @Test
    fun `o item da nota sem produto ja traz nome e custo para o cadastro`() {
        val draft = unknown.copy(rawCode = "7898938890397").newProductDraft()

        assertEquals("BEB MONSTER 473ML MG", draft.name)
        assertEquals(8.99, draft.cost)
        assertEquals("7898938890397", draft.barcode)
        // Código interno do mercado não é código de barras.
        assertNull(unknown.copy(rawCode = "1234").newProductDraft().barcode)
        // Sem custo da linha, sai do preço da nota dividido pelo fardo.
        assertEquals(2.0, PurchaseLine(rawDescription = "AGUA 6X", rawUnitPrice = 12.0, packSize = 6).newProductDraft().cost)
    }

    @Test
    fun `o produto cadastrado pela nota entra na lista sem duplicar o da folha`() {
        val novo = CatalogProduct(99, "Monster Mango", null, 8.99, 0)
        val current = state(unknown).copy(createdProducts = listOf(novo, kitkat))

        assertEquals(listOf(58L, 10L, 99L), current.products.map { it.id })
    }

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

    @Test
    fun `somar dias atravessa mes ano e fevereiro`() {
        assertEquals("2026-12-20", addDays("2026-11-20", 30))
        assertEquals("2027-01-19", addDays("2026-11-20", 60))
        assertEquals("2028-02-29", addDays("2028-02-28", 1))
        assertEquals("2027-03-01", addDays("2027-02-28", 1))
        assertNull(addDays("", 30))
        assertNull(addDays("20/11/2026", 30))
    }

    @Test
    fun `a validade sugerida e a data da nota mais o prazo so para quem controla`() {
        val pudim = CatalogProduct(7, "Pudim", null, 4.0, 0, tracksExpiry = true, shelfLifeDays = 30)
        val linha = PurchaseLine(productId = 7, quantityUnits = 6, unitCost = 4.0)

        assertEquals("2026-12-20", linha.withSuggestedExpiry(pudim, "2026-11-20").expiresOn)
        // A data digitada é a da embalagem: o palpite não passa por cima.
        assertEquals("2026-12-01", linha.copy(expiresOn = "2026-12-01").withSuggestedExpiry(pudim, "2026-11-20").expiresOn)
        assertNull(linha.withSuggestedExpiry(monster, "2026-11-20").expiresOn)
        assertNull(linha.withSuggestedExpiry(pudim.copy(shelfLifeDays = null), "2026-11-20").expiresOn)
    }

    @Test
    fun `a folha do destino abre em encher quando a geladeira tem planograma`() {
        val opcoes = DestinationOptions(Destination.Fill, applianceId = 1, warehouseInUse = true, fill = listOf(FillPreview(1, 30, 18)))
        val comPlanograma = sheet().copy(applianceId = 1, appliances = listOf(Named(1, "Copa"), Named(2, "Térreo")), destinationOptions = opcoes)

        assertEquals(Destination.Fill to 1L, comPlanograma.startingDestination())

        val semPlanograma = comPlanograma.copy(destinationOptions = opcoes.copy(default = Destination.Appliance, fill = emptyList()))
        assertEquals(Destination.Appliance to 1L, semPlanograma.startingDestination())

        // Compra sem geladeira: a marcada é a que tem planograma, não a primeira da lista.
        val semGeladeiraNaCompra = comPlanograma.copy(applianceId = null, appliances = listOf(Named(2, "ESP32"), Named(1, "Copa")),
            destinationOptions = opcoes.copy(default = Destination.Warehouse, applianceId = null))
        assertEquals(Destination.Warehouse to 1L, semGeladeiraNaCompra.startingDestination())

        // E quando o servidor manda "encher" para a compra sem geladeira, a folha abre nele.
        val encherSemGeladeira = semGeladeiraNaCompra.copy(destinationOptions = semGeladeiraNaCompra.destinationOptions!!.copy(default = Destination.Fill))
        assertEquals(Destination.Fill to 1L, encherSemGeladeira.startingDestination())

        val semGeladeira = sheet().copy(destinationOptions = opcoes.copy(default = Destination.Warehouse, fill = emptyList()))
        assertEquals(Destination.Warehouse to null, semGeladeira.startingDestination())
    }

    @Test
    fun `encher so vale para a geladeira que tem a previa`() {
        val opcoes = DestinationOptions(Destination.Fill, 1, false, listOf(FillPreview(1, 30, 18)))
        val estado = state(kitkatLine).copy(
            sheet = sheet().copy(destinationOptions = opcoes),
            destination = Destination.Fill,
            destinationApplianceId = 1,
        )

        assertTrue(estado.canChooseDestination)
        assertEquals(18, estado.fillPreview?.toWarehouse)
        assertFalse(estado.copy(destinationApplianceId = 2).canChooseDestination)
        assertTrue(estado.copy(destination = Destination.Warehouse, destinationApplianceId = null).canChooseDestination)
    }
}

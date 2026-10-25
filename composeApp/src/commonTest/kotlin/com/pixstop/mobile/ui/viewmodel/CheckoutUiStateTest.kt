package com.pixstop.mobile.ui.viewmodel

import com.pixstop.mobile.domain.checkout.PaymentMethod
import com.pixstop.mobile.domain.model.Checkout
import com.pixstop.mobile.domain.model.LastPayment
import com.pixstop.mobile.domain.model.SavedCard
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * A tela de pagamento só pode oferecer o que o envio consegue cumprir. Uma
 * forma que aparece e falha no fim é pior que uma forma ausente: a pessoa já
 * decidiu quando descobre.
 */
class CheckoutUiStateTest {

    private fun checkout(
        gateway: Boolean = true,
        cards: List<SavedCard> = emptyList(),
        publicKey: Boolean = true,
        lastPayment: LastPayment? = null,
    ) = Checkout(
        productsTotal = 10.0,
        walletPixels = 0,
        walletPixelsAsMoney = 0,
        maxPixels = 0,
        pixelsPerReal = 100,
        minPixelsRedeem = 100,
        maxDiscountPercentage = 50.0,
        pixelsEnabled = true,
        cashbackPercentage = 0.0,
        cardFeePercentage = 4.0,
        cardFeeFixed = 0.5,
        gatewayAvailable = gateway,
        cardTokenizationAvailable = gateway && publicKey,
        savedCards = cards,
        itemCount = 1,
        lastPayment = lastPayment,
    )

    private val cartao = SavedCard(id = 4, lastFour = "1234", brand = "visa", isDefault = false, expires = "12/30")
    private val padrao = SavedCard(id = 7, lastFour = "9999", brand = "master", isDefault = true, expires = null)

    @Test
    fun `sem cartao guardado e sem chave publica a forma cartao nem aparece`() {
        // Sem nenhum dos dois não há o que enviar, e a forma daria 422 depois
        // de a pessoa já ter escolhido — foi exatamente o que existia.
        val state = CheckoutUiState(checkout = checkout(cards = emptyList(), publicKey = false))

        assertFalse(state.availableMethods.contains(PaymentMethod.Card))
    }

    @Test
    fun `so a chave publica ja permite pagar com cartao novo`() {
        val state = CheckoutUiState(checkout = checkout(cards = emptyList(), publicKey = true))

        assertContains(state.availableMethods, PaymentMethod.Card)
        assertEquals(listOf(CardMode.New), state.cardModes)
    }

    @Test
    fun `sem chave publica so resta o cartao guardado`() {
        // A empresa recebe, mas este aparelho não tem como tokenizar: oferecer
        // "novo cartão" seria um formulário que não leva a lugar nenhum.
        val state = CheckoutUiState(checkout = checkout(cards = listOf(cartao), publicKey = false))

        assertEquals(listOf(CardMode.Saved), state.cardModes)
    }

    @Test
    fun `com os dois caminhos a pessoa escolhe`() {
        val state = CheckoutUiState(checkout = checkout(cards = listOf(cartao), publicKey = true))

        assertEquals(listOf(CardMode.Saved, CardMode.New), state.cardModes)
    }

    @Test
    fun `cartao novo nao trava o botao antes de tentar`() {
        // Os erros do formulário aparecem no envio, campo a campo. Segurar o
        // botão até o último dígito esconderia o que falta preencher.
        val state = CheckoutUiState(
            checkout = checkout(cards = emptyList(), publicKey = true),
            method = PaymentMethod.Card,
            cardMode = CardMode.New,
            savedCardId = null,
        )

        assertTrue(state.canPlace)
        assertTrue(state.usingNewCard)
    }

    @Test
    fun `com cartao guardado a forma aparece`() {
        val state = CheckoutUiState(checkout = checkout(cards = listOf(cartao)))

        assertContains(state.availableMethods, PaymentMethod.Card)
    }

    @Test
    fun `sem gateway nao ha PIX nem cartao`() {
        val state = CheckoutUiState(checkout = checkout(gateway = false, cards = listOf(cartao)))

        assertEquals(listOf(PaymentMethod.Pixels), state.availableMethods)
    }

    @Test
    fun `cartao sem escolher qual nao fecha o pedido`() {
        val state = CheckoutUiState(
            checkout = checkout(cards = listOf(cartao)),
            method = PaymentMethod.Card,
            savedCardId = null,
        )

        assertFalse(state.canPlace)
        assertTrue(state.copy(savedCardId = 4).canPlace)
    }

    @Test
    fun `parcelar so faz sentido no cartao e com valor a cobrar`() {
        val comCartao = CheckoutUiState(
            checkout = checkout(cards = listOf(cartao)),
            method = PaymentMethod.Card,
            savedCardId = 4,
        )

        assertTrue(comCartao.canChooseInstallments)
        assertFalse(comCartao.copy(method = PaymentMethod.Money).canChooseInstallments)

        // Pixels cobrindo tudo: não sobra nada para o cartão parcelar.
        val semCobranca = comCartao.copy(pixels = 1_000)

        assertFalse(semCobranca.canChooseInstallments)
    }

    @Test
    fun `a lista de cartoes vem do fechamento`() {
        val state = CheckoutUiState(checkout = checkout(cards = listOf(cartao, padrao)))

        assertEquals(listOf(cartao, padrao), state.savedCards)
        assertEquals(emptyList(), CheckoutUiState().savedCards)
    }

    /* ── O pagamento lembra da última vez (CHECKOUT_RAPIDO.md) ───────────── */

    @Test
    fun `parte da forma da ultima vez, com as opcoes recolhidas`() {
        val state = CheckoutUiState(
            checkout = checkout(cards = listOf(padrao), lastPayment = LastPayment(PaymentMethod.Card, 0)),
            savedCardId = padrao.id,
            cardMode = CardMode.Saved,
        ).withRememberedPayment()

        assertEquals(PaymentMethod.Card, state.method)
        assertFalse(state.methodsExpanded)
        // O botão diz com qual cartão: 10,00 + 4% + 0,50 = 10,90.
        assertEquals("Pagar R$ 10,90 no cartão •••• 9999", state.payButtonLabel)
    }

    @Test
    fun `forma lembrada que a empresa nao aceita mais nao e escolhida, e as opcoes abrem`() {
        val state = CheckoutUiState(
            checkout = checkout(cards = emptyList(), publicKey = false, lastPayment = LastPayment(PaymentMethod.Card, 0)),
        ).withRememberedPayment()

        assertEquals(PaymentMethod.Money, state.method)
        assertTrue(state.methodsExpanded)
    }

    @Test
    fun `sem lembranca as opcoes vem abertas e o botao diz com PIX`() {
        val state = CheckoutUiState(checkout = checkout()).withRememberedPayment()

        assertTrue(state.methodsExpanded)
        assertEquals("Pagar R$ 10,00 com PIX", state.payButtonLabel)
    }

    @Test
    fun `os pixels lembrados respeitam o teto de hoje`() {
        // Carteira com 300 pixels, teto de 50% de R$ 10,00 = 500 pixels: o teto é a carteira.
        val base = checkout(lastPayment = LastPayment(PaymentMethod.Mixed, 999)).copy(walletPixels = 300, maxPixels = 300)
        val state = CheckoutUiState(checkout = base).withRememberedPayment()

        // "Misto" do servidor vira PIX com o controle de pixels preenchido.
        assertEquals(PaymentMethod.Money, state.method)
        assertEquals(state.maxPixels, state.pixels)
        assertTrue(state.pixels <= 300)
        assertEquals("Pagar R$ 7,00 com PIX e pixels", state.payButtonLabel)
    }

    @Test
    fun `nada a cobrar vira concluir pedido`() {
        // Sem teto de desconto e com pixels para a compra inteira, não sobra nada a cobrar.
        val base = checkout(lastPayment = LastPayment(PaymentMethod.Pixels, 1000))
            .copy(walletPixels = 1000, walletPixelsAsMoney = 1000, maxPixels = 1000, maxDiscountPercentage = 100.0)
        val state = CheckoutUiState(checkout = base).withRememberedPayment()

        assertEquals("Concluir pedido", state.payButtonLabel)
    }
}

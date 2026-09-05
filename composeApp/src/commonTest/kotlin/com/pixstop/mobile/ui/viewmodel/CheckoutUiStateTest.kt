package com.pixstop.mobile.ui.viewmodel

import com.pixstop.mobile.domain.checkout.PaymentMethod
import com.pixstop.mobile.domain.model.Checkout
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
        balance: Double = 0.0,
        publicKey: Boolean = true,
    ) = Checkout(
        productsTotal = 10.0,
        walletPixels = 0,
        walletBalance = balance,
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

        assertEquals(listOf(PaymentMethod.Balance), state.availableMethods)
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

        // Saldo cobrindo tudo: não sobra nada para o cartão parcelar.
        val semCobranca = comCartao.copy(
            checkout = checkout(cards = listOf(cartao), balance = 50.0),
            useBalance = true,
        )

        assertFalse(semCobranca.canChooseInstallments)
    }

    @Test
    fun `a lista de cartoes vem do fechamento`() {
        val state = CheckoutUiState(checkout = checkout(cards = listOf(cartao, padrao)))

        assertEquals(listOf(cartao, padrao), state.savedCards)
        assertEquals(emptyList(), CheckoutUiState().savedCards)
    }
}

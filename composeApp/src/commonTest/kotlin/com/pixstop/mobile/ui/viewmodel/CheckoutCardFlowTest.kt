package com.pixstop.mobile.ui.viewmodel

import com.pixstop.mobile.data.payment.CardTokenizer
import com.pixstop.mobile.data.repository.AppConfigRepository
import com.pixstop.mobile.data.repository.OrderRepository
import com.pixstop.mobile.domain.checkout.PaymentMethod
import com.pixstop.mobile.domain.payment.CardInput
import com.pixstop.mobile.support.FakeApi
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * O caminho do cartão novo tem duas idas à rede em sequência, e a ordem
 * importa: primeiro o gateway devolve um token, só então o pedido é criado.
 * Invertido, uma tokenização recusada deixaria pedidos órfãos no servidor.
 */
class CheckoutCardFlowTest {

    private val checkoutBody = """
        {"success":true,"data":{
          "items":[{"id":1,"quantity":1,"unit_price":10.0,"subtotal":10.0,
            "product":{"id":3,"name":"Barra de Cereal"}}],
          "totals":{"products":10.0,"cost":5.0},
          "wallet":{"pixels":0,"balance":0.0},
          "checkout":{"max_pixels":0,"pixels_per_real":100,"min_pixels_redeem":100,
            "max_discount_percentage":50,"pixels_enabled":true,"cashback_percentage":0,
            "gateway_available":true,"mp_public_key":"APP_USR-abc",
            "fees":{"card":{"percentage":4.0,"fixed":0.5}}},
          "saved_cards":[],
          "reservation_minutes":5}}
    """.trimIndent()

    private val orderBody = """
        {"success":true,"data":{"id":9,"transaction_id":"ORD-X","status":"paid","status_label":"Pago",
         "payment_method":"card","totals":{"products":10.0,"pixels":0,"balance":0.0,"money":10.9,"card_fee":0.9},
         "items":[],"cancellation_reason":null,"is_cancelable":false,"created_at":null}}
    """.trimIndent()

    private val configBody = """
        {"success":true,"data":{
          "app_name":"Pixstop",
          "cart":{"reservation_minutes":5,"pix_expiration_minutes":30,"low_stock_threshold":5},
          "payment":{"max_installments":12,"public_key":"APP_USR-abc","is_sandbox":false},
          "pixels":{"pixels_per_real":100},
          "account":{"purge_after_days":90},
          "legal":{"terms_url":null,"privacy_url":null},
          "upload":{"max_image_kb":5120},
          "trial_days":30}}
    """.trimIndent()

    private val cartaoBom = CardInput(
        number = "4235 6477 2802 5682",
        expiry = "11/30",
        securityCode = "123",
        holderName = "ANA COSTA",
        documentNumber = "529.982.247-25",
    )

    /** 5 de setembro de 2026, para a validade do cartão não depender de hoje. */
    private val hoje = 1_788_000_000_000L

    /**
     * O `viewModelScope` vive no dispatcher principal, que o teste precisa
     * controlar. O relógio virtual do `runTest`, porém, não alcança o cliente
     * HTTP — ele responde em outra thread —, então quem espera de verdade é o
     * [awaitState] abaixo, que aguarda o estado e não o agendador.
     */
    private fun cardTest(block: suspend TestScope.() -> Unit) = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))

        try {
            block()
        } finally {
            Dispatchers.resetMain()
        }
    }

    private suspend fun TestScope.viewModel(
        orderApi: FakeApi,
        gateway: FakeApi,
        gatewayBody: String = """{"id":"tok-abc"}""",
    ): CheckoutViewModel {
        val config = AppConfigRepository(FakeApi().clientReturning(configBody)).also { it.refresh() }

        val model = CheckoutViewModel(
            orders = OrderRepository(orderApi.clientReturning(checkoutBody)),
            config = config,
            cards = CardTokenizer(gateway.clientReturning(gatewayBody), config),
            now = { hoje },
        )

        model.awaitState { it.checkout != null }

        return model
    }

    /**
     * Espera o ViewModel chegar ao estado esperado.
     *
     * Esperar o estado, e não um número de passos do agendador, é o que torna
     * o teste indiferente a quantas idas à rede a operação faz por dentro.
     */
    private suspend fun CheckoutViewModel.awaitState(
        predicate: (CheckoutUiState) -> Boolean,
    ): CheckoutUiState = withContext(Dispatchers.Default) {
        withTimeout(5_000) { uiState.first(predicate) }
    }

    /** O desfecho de um envio: pedido criado, erro geral ou erro de campo. */
    private suspend fun CheckoutViewModel.awaitPlaceResult(): CheckoutUiState =
        awaitState { !it.isPlacing && (it.placedOrder != null || it.error != null || it.cardErrors.isNotEmpty()) }

    @Test
    fun `o pedido leva o token, o documento e o pedido de guardar`() = cardTest {
        val orderApi = FakeApi()
        val gateway = FakeApi()
        val model = viewModel(orderApi, gateway)

        model.onMethodChange(PaymentMethod.Card)
        model.onCardModeChange(CardMode.New)
        model.onCardChange(cartaoBom)

        orderApi.nextBody = orderBody
        model.place()
        model.awaitPlaceResult()

        assertContains(orderApi.lastBody, "\"card_token\":\"tok-abc\"")
        assertContains(orderApi.lastBody, "\"doc_type\":\"CPF\"")
        // Sem máscara: o servidor guarda o documento, não o que a tela mostrou.
        assertContains(orderApi.lastBody, "\"doc_number\":\"52998224725\"")
        assertContains(orderApi.lastBody, "\"save_card\":true")
        assertNotNull(model.uiState.value.placedOrder)
    }

    @Test
    fun `cartao recusado pelo gateway nao cria pedido`() = cardTest {
        val orderApi = FakeApi()
        val gateway = FakeApi(HttpStatusCode.BadRequest)
        val model = viewModel(orderApi, gateway, """{"cause":[{"code":"205"}]}""")

        model.onMethodChange(PaymentMethod.Card)
        model.onCardModeChange(CardMode.New)
        model.onCardChange(cartaoBom)

        orderApi.forget()
        model.place()
        model.awaitPlaceResult()

        assertNull(orderApi.lastRequest, "não deveria ter criado pedido")
        assertNull(model.uiState.value.placedOrder)
        assertEquals("Confira o número do cartão.", model.uiState.value.error)
        assertFalse(model.uiState.value.isPlacing)
    }

    @Test
    fun `cartao mal preenchido nem chega ao gateway`() = cardTest {
        val orderApi = FakeApi()
        val gateway = FakeApi()
        val model = viewModel(orderApi, gateway)

        model.onMethodChange(PaymentMethod.Card)
        model.onCardModeChange(CardMode.New)
        model.onCardChange(cartaoBom.copy(number = "4235 6477 2802 5683", documentNumber = "111"))

        orderApi.forget()
        model.place()
        model.awaitPlaceResult()

        assertNull(gateway.lastRequest, "não deveria ter falado com o gateway")
        assertNull(orderApi.lastRequest)

        val errors = model.uiState.value.cardErrors

        assertContains(errors.keys, CardInput.FIELD_NUMBER)
        assertContains(errors.keys, CardInput.FIELD_DOCUMENT)
        assertFalse(errors.containsKey(CardInput.FIELD_CVV))
    }

    @Test
    fun `corrigir um campo apaga so o aviso dele`() = cardTest {
        val model = viewModel(FakeApi(), FakeApi())

        model.onMethodChange(PaymentMethod.Card)
        model.onCardModeChange(CardMode.New)
        model.onCardChange(cartaoBom.copy(number = "4235 6477 2802 5683", documentNumber = "111"))
        model.place()
        model.awaitPlaceResult()

        model.onCardChange(model.uiState.value.card.copy(number = "4235 6477 2802 5682"))

        val errors = model.uiState.value.cardErrors

        assertFalse(errors.containsKey(CardInput.FIELD_NUMBER), "o aviso do número deveria ter sumido")
        assertTrue(errors.containsKey(CardInput.FIELD_DOCUMENT), "o do CPF ainda vale")
    }

    @Test
    fun `trocar para cartao guardado nao manda token nenhum`() = cardTest {
        val orderApi = FakeApi()
        val gateway = FakeApi()
        val model = viewModel(orderApi, gateway)

        model.onMethodChange(PaymentMethod.Card)
        model.onCardModeChange(CardMode.New)
        model.onCardChange(cartaoBom)
        model.onCardModeChange(CardMode.Saved)
        model.onSavedCardChange(7)

        orderApi.nextBody = orderBody
        model.place()
        model.awaitPlaceResult()

        assertNull(gateway.lastRequest, "cartão guardado não tokeniza nada")
        assertContains(orderApi.lastBody, "\"saved_card_id\":7")
        assertFalse(orderApi.lastBody.contains("card_token"), orderApi.lastBody)
    }
}

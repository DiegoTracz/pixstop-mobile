package com.pixstop.mobile.data.repository

import com.pixstop.mobile.core.config.ApiConfig
import com.pixstop.mobile.core.network.safeCall
import com.pixstop.mobile.core.network.safeCallPaged
import com.pixstop.mobile.data.mapper.toDomain
import com.pixstop.mobile.data.remote.dto.CheckoutDto
import com.pixstop.mobile.data.remote.dto.OrderDto
import com.pixstop.mobile.data.remote.dto.OrderStatusDto
import com.pixstop.mobile.data.remote.dto.OrderStoreRequest
import com.pixstop.mobile.data.remote.dto.UnlockResponseDto
import com.pixstop.mobile.data.remote.dto.UnlockTicketDto
import com.pixstop.mobile.data.remote.dto.Page
import com.pixstop.mobile.domain.checkout.PaymentMethod
import com.pixstop.mobile.domain.model.Checkout
import com.pixstop.mobile.domain.model.Order
import com.pixstop.mobile.domain.model.OrderStatus
import com.pixstop.mobile.domain.model.Pickup
import com.pixstop.mobile.domain.model.UnlockAttempt
import com.pixstop.mobile.domain.model.UnlockTicket
import com.pixstop.mobile.domain.model.Outcome
import com.pixstop.mobile.domain.model.map
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody

private const val TAG = "Orders"

/**
 * Fechamento e acompanhamento do pedido.
 *
 * O cartão ainda não é enviado daqui: tokenizar exige o SDK do MercadoPago no
 * aparelho, que o app não tem.
 */
class OrderRepository(private val client: HttpClient) {

    suspend fun checkout(): Outcome<Checkout> =
        safeCall<CheckoutDto>(TAG) { client.get(ApiConfig.Endpoints.CHECKOUT) }.map { it.toDomain() }

    suspend fun place(
        method: PaymentMethod,
        pixels: Int,
        balance: Double,
        savedCardId: Long? = null,
        installments: Int? = null,
        cardToken: String? = null,
        documentType: String? = null,
        documentNumber: String? = null,
        saveCard: Boolean? = null,
    ): Outcome<Order> =
        safeCall<OrderDto>(TAG) {
            val payingByCard = method == PaymentMethod.Card

            client.post(ApiConfig.Endpoints.ORDERS) {
                setBody(
                    OrderStoreRequest(
                        paymentMethod = method.apiValue,
                        pixels = pixels.takeIf { it > 0 },
                        balance = balance.takeIf { it > 0 },
                        // Tudo de cartão só sai quando o método é cartão;
                        // mandá-los no PIX faria o servidor guardar parcelas de
                        // um pagamento à vista.
                        savedCardId = savedCardId.takeIf { payingByCard },
                        installments = installments?.takeIf { payingByCard && it > 1 },
                        cardToken = cardToken?.takeIf { payingByCard },
                        documentType = documentType?.takeIf { payingByCard && cardToken != null },
                        documentNumber = documentNumber?.takeIf { payingByCard && cardToken != null },
                        saveCard = saveCard?.takeIf { payingByCard && cardToken != null },
                    ),
                )
            }
        }.map { it.toDomain() }

    suspend fun order(id: Long): Outcome<Order> =
        safeCall<OrderDto>(TAG) { client.get(ApiConfig.Endpoints.order(id)) }.map { it.toDomain() }

    /**
     * Consulta enxuta para a espera do PIX: o servidor pergunta ao gateway e
     * confirma na hora, sem depender do webhook.
     */
    suspend fun status(id: Long): Outcome<OrderStatus> =
        safeCall<OrderStatusDto>(TAG) {
            client.get(ApiConfig.Endpoints.orderStatus(id))
        }.map { OrderStatus.from(it.status) }

    /**
     * A mesma consulta, com a retirada junto (Fase 9.8). É o que a tela do
     * pedido acompanha enquanto a porta não abre: o estado vira sozinho
     * quando o sensor conta que abriu.
     */
    suspend fun pickupStatus(id: Long): Outcome<UnlockAttempt> =
        safeCall<OrderStatusDto>(TAG) {
            client.get(ApiConfig.Endpoints.orderStatus(id))
        }.map { UnlockAttempt(it.pickup?.toDomain() ?: Pickup.None, UnlockTicket.parse(it.unlockTicket)) }

    /**
     * "Abrir a geladeira" (Fase 9.8): a pessoa está na frente da porta.
     *
     * A recusa também traz o estado, e é por isso que ela não é tratada aqui:
     * quem chamou precisa da mensagem *e* do bloco para atualizar a tela.
     */
    suspend fun unlock(id: Long): Outcome<UnlockAttempt> =
        safeCall<UnlockResponseDto>(TAG) {
            client.post(ApiConfig.Endpoints.orderUnlock(id))
        }.map { UnlockAttempt(it.pickup?.toDomain() ?: Pickup.None, UnlockTicket.parse(it.unlockTicket)) }

    /**
     * Um bilhete novo para levar até a porta (Fase 9.7): o anterior venceu na
     * fila, ou o celular travou no meio.
     */
    suspend fun reissueTicket(id: Long): Outcome<UnlockTicket?> =
        safeCall<UnlockTicketDto>(TAG) {
            client.post(ApiConfig.Endpoints.orderUnlockTicket(id))
        }.map { UnlockTicket.parse(it.unlockTicket) }

    suspend fun orders(page: Int = 1): Outcome<Page<Order>> =
        safeCallPaged<OrderDto>(TAG) {
            client.get(ApiConfig.Endpoints.ORDERS) { parameter("page", page) }
        }.map { result -> Page(result.items.map { it.toDomain() }, result.meta) }
}

package com.pixstop.mobile.data.repository

import com.pixstop.mobile.core.config.ApiConfig
import com.pixstop.mobile.core.network.safeCall
import com.pixstop.mobile.core.network.safeCallPaged
import com.pixstop.mobile.data.mapper.toDomain
import com.pixstop.mobile.data.remote.dto.CheckoutDto
import com.pixstop.mobile.data.remote.dto.OrderDto
import com.pixstop.mobile.data.remote.dto.OrderStatusDto
import com.pixstop.mobile.data.remote.dto.OrderStoreRequest
import com.pixstop.mobile.data.remote.dto.Page
import com.pixstop.mobile.domain.checkout.PaymentMethod
import com.pixstop.mobile.domain.model.Checkout
import com.pixstop.mobile.domain.model.Order
import com.pixstop.mobile.domain.model.OrderStatus
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

    suspend fun place(method: PaymentMethod, pixels: Int, balance: Double): Outcome<Order> =
        safeCall<OrderDto>(TAG) {
            client.post(ApiConfig.Endpoints.ORDERS) {
                setBody(
                    OrderStoreRequest(
                        paymentMethod = method.apiValue,
                        pixels = pixels.takeIf { it > 0 },
                        balance = balance.takeIf { it > 0 },
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

    suspend fun orders(page: Int = 1): Outcome<Page<Order>> =
        safeCallPaged<OrderDto>(TAG) {
            client.get(ApiConfig.Endpoints.ORDERS) { parameter("page", page) }
        }.map { result -> Page(result.items.map { it.toDomain() }, result.meta) }
}

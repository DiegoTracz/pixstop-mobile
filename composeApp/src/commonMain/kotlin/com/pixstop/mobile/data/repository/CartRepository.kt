package com.pixstop.mobile.data.repository

import com.pixstop.mobile.core.config.ApiConfig
import com.pixstop.mobile.core.network.safeCall
import com.pixstop.mobile.core.network.safeCallUnit
import com.pixstop.mobile.data.mapper.toDomain
import com.pixstop.mobile.data.remote.dto.CartAddRequest
import com.pixstop.mobile.data.remote.dto.CartDto
import com.pixstop.mobile.data.remote.dto.CartItemDto
import com.pixstop.mobile.data.remote.dto.CartUpdateRequest
import com.pixstop.mobile.domain.model.Cart
import com.pixstop.mobile.domain.model.CartLine
import com.pixstop.mobile.domain.model.Outcome
import com.pixstop.mobile.domain.model.map
import io.ktor.client.HttpClient
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody

private const val TAG = "Cart"

/**
 * Carrinho.
 *
 * Cada item fica com o estoque reservado por alguns minutos. Passado o prazo o
 * servidor devolve o estoque sozinho, então a tela recarrega o carrinho depois
 * de qualquer mudança em vez de emendar o que acha que aconteceu.
 */
class CartRepository(private val client: HttpClient) {

    suspend fun cart(): Outcome<Cart> =
        safeCall<CartDto>(TAG) { client.get(ApiConfig.Endpoints.CART) }.map { it.toDomain() }

    suspend fun add(productId: Long, quantity: Int = 1): Outcome<CartLine> =
        safeCall<CartItemDto>(TAG) {
            client.post(ApiConfig.Endpoints.CART_ADD) { setBody(CartAddRequest(productId, quantity)) }
        }.map { it.toDomain() }

    suspend fun updateQuantity(itemId: Long, quantity: Int): Outcome<CartLine> =
        safeCall<CartItemDto>(TAG) {
            client.patch(ApiConfig.Endpoints.cartItem(itemId)) { setBody(CartUpdateRequest(quantity)) }
        }.map { it.toDomain() }

    suspend fun remove(itemId: Long): Outcome<Unit> =
        safeCallUnit(TAG) { client.delete(ApiConfig.Endpoints.cartItem(itemId)) }
}

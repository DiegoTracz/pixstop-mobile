package com.pixstop.mobile.data.repository

import com.pixstop.mobile.core.config.ApiConfig
import com.pixstop.mobile.core.network.safeCall
import com.pixstop.mobile.core.network.safeCallUnit
import com.pixstop.mobile.data.mapper.toDomain
import com.pixstop.mobile.data.remote.dto.SavedCardDto
import com.pixstop.mobile.domain.model.Outcome
import com.pixstop.mobile.domain.model.SavedCard
import com.pixstop.mobile.domain.model.map
import io.ktor.client.HttpClient
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post

/**
 * Os cartões guardados da pessoa na empresa ativa: listar, escolher o padrão
 * e remover. O cartão em si nunca passa por aqui — só o token que o
 * MercadoPago guardou, com a bandeira e os quatro últimos dígitos.
 */
class CardRepository(private val client: HttpClient) {

    suspend fun cards(): Outcome<List<SavedCard>> =
        safeCall<List<SavedCardDto>>(TAG) { client.get(ApiConfig.Endpoints.PAYMENT_CARDS) }
            .map { list -> list.map { it.toDomain() } }

    suspend fun setDefault(id: Long): Outcome<Unit> =
        safeCallUnit(TAG) { client.post(ApiConfig.Endpoints.paymentCardDefault(id)) }

    suspend fun remove(id: Long): Outcome<Unit> =
        safeCallUnit(TAG) { client.delete(ApiConfig.Endpoints.paymentCard(id)) }

    private companion object {
        const val TAG = "Cards"
    }
}

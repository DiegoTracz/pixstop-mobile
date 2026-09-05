package com.pixstop.mobile.data.payment

import com.pixstop.mobile.core.logging.AppLogger
import com.pixstop.mobile.data.repository.AppConfigRepository
import com.pixstop.mobile.domain.model.DomainError
import com.pixstop.mobile.domain.model.Outcome
import com.pixstop.mobile.domain.payment.CardInput
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.parameter
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.random.Random

/**
 * Transforma o cartão digitado num token de uso único.
 *
 * O número do cartão sai do aparelho direto para o gateway e nunca passa pelo
 * nosso servidor — é isso que mantém a operação fora do escopo de PCI. O que o
 * nosso backend recebe é o token, que serve para uma cobrança só.
 *
 * A chave pública é o que autoriza esta chamada; ela não dá acesso a nada além
 * de criar tokens, e é por isso que pode viver no aplicativo.
 */
class CardTokenizer(
    private val client: HttpClient,
    private val config: AppConfigRepository,
    private val randomToken: () -> String = { Random.nextLong().toString(36) },
) {

    suspend fun tokenize(card: CardInput): Outcome<String> {
        val settings = config.config.value

        // Ambiente de demonstração: o servidor usa um gateway de mentira, e
        // pedir um token real ao MercadoPago daria erro. É o mesmo desvio que
        // o site faz.
        if (settings.isSandbox || settings.paymentPublicKey.isNullOrBlank()) {
            AppLogger.d("Ambiente de demonstração: token gerado localmente.", tag = TAG)

            return Outcome.Success("sandbox-card-token-${randomToken()}")
        }

        return try {
            val response = client.post(TOKEN_URL) {
                parameter("public_key", settings.paymentPublicKey)
                setBody(
                    CardTokenRequest(
                        cardNumber = card.digits,
                        expirationMonth = card.expiryMonth,
                        expirationYear = card.expiryYear,
                        securityCode = card.securityCode,
                        cardholder = Cardholder(
                            name = card.holderName.trim(),
                            identification = Identification(card.documentType, card.document),
                        ),
                    ),
                )
            }

            val body = response.bodyAsText()

            if (response.status.value !in 200..299) {
                // O corpo pode trazer o número do cartão de volta; só o motivo
                // interessa, e só ele vai para o log.
                val reason = json.decodeFromStringOrNull<CardTokenError>(body)
                AppLogger.w("Gateway recusou o cartão: ${reason?.message ?: response.status.value}", tag = TAG)

                return Outcome.Failure(DomainError.Rule("card_token_failed", reasonFor(reason)))
            }

            val token = json.decodeFromStringOrNull<CardTokenResponse>(body)?.id

            if (token.isNullOrBlank()) {
                Outcome.Failure(DomainError.Rule("card_token_failed", GENERIC_FAILURE))
            } else {
                Outcome.Success(token)
            }
        } catch (cancellation: kotlinx.coroutines.CancellationException) {
            throw cancellation
        } catch (error: Throwable) {
            // A mensagem do erro nunca é registrada aqui: ela pode carregar o
            // corpo da requisição, e o corpo é o cartão.
            AppLogger.e("Falha ao falar com o gateway: ${error::class.simpleName}", null, TAG)

            Outcome.Failure(DomainError.Offline())
        }
    }

    /**
     * Traduz a recusa do gateway em algo acionável.
     *
     * O texto do MercadoPago vem em inglês e fala de campos que a pessoa não
     * conhece; o código é estável e diz qual campo consertar.
     */
    private fun reasonFor(error: CardTokenError?): String {
        val cause = error?.cause?.firstOrNull()?.code

        return when (cause) {
            "205", "E301" -> "Confira o número do cartão."
            "208", "209", "325", "326" -> "Confira a validade do cartão."
            "212", "213", "214", "220", "221", "316" -> "Confira o nome e o CPF do titular."
            "224", "E302" -> "Confira o código de segurança."
            else -> GENERIC_FAILURE
        }
    }

    private companion object {
        const val TAG = "Card"

        /** Rota pública de tokenização; a chave pública vai na query. */
        const val TOKEN_URL = "https://api.mercadopago.com/v1/card_tokens"

        const val GENERIC_FAILURE = "Não foi possível validar o cartão. Confira os dados e tente de novo."

        val json = Json {
            ignoreUnknownKeys = true
            explicitNulls = false
            // O gateway às vezes manda o código da causa como número e às
            // vezes entre aspas.
            isLenient = true
        }

        inline fun <reified T> Json.decodeFromStringOrNull(body: String): T? =
            runCatching { decodeFromString<T>(body) }.getOrNull()
    }
}

@Serializable
private data class CardTokenRequest(
    @SerialName("card_number") val cardNumber: String,
    @SerialName("expiration_month") val expirationMonth: Int,
    @SerialName("expiration_year") val expirationYear: Int,
    @SerialName("security_code") val securityCode: String,
    val cardholder: Cardholder,
)

@Serializable
private data class Cardholder(val name: String, val identification: Identification)

@Serializable
private data class Identification(val type: String, val number: String)

@Serializable
private data class CardTokenResponse(val id: String? = null)

@Serializable
private data class CardTokenError(
    val message: String? = null,
    val cause: List<CardTokenCause> = emptyList(),
)

@Serializable
private data class CardTokenCause(val code: String? = null, val description: String? = null)

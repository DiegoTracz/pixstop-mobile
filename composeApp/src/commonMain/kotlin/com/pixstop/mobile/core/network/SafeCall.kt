package com.pixstop.mobile.core.network

import com.pixstop.mobile.core.logging.AppLogger
import com.pixstop.mobile.data.remote.dto.ApiEnvelope
import com.pixstop.mobile.data.remote.dto.Page
import com.pixstop.mobile.data.remote.dto.ValidationErrorBody
import com.pixstop.mobile.domain.model.DomainError
import com.pixstop.mobile.domain.model.ErrorCode
import com.pixstop.mobile.domain.model.Outcome
import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.json.Json

/**
 * Um lugar só para transformar resposta HTTP em `Outcome`.
 *
 * Toda a API fala o mesmo envelope, então nenhum repositório precisa repetir
 * tratamento de erro. O que muda de rota para rota é só o tipo de `data`.
 */
val apiJson: Json = Json {
    ignoreUnknownKeys = true
    isLenient = true
    explicitNulls = false
}

/**
 * Executa a chamada e devolve o `data` do envelope.
 *
 * ```
 * suspend fun me(): Outcome<MeDto> = safeCall("Auth") { client.get("me") }
 * ```
 */
suspend inline fun <reified T> safeCall(
    tag: String = "API",
    crossinline block: suspend () -> HttpResponse,
): Outcome<T> = runCatchingNetwork(tag) {
    val response = block()

    if (!response.status.isOk()) {
        return@runCatchingNetwork Outcome.Failure(response.toDomainError(tag))
    }

    val envelope = apiJson.decodeFromString<ApiEnvelope<T>>(response.bodyAsText())
    val data = envelope.data

    if (envelope.success && data != null) {
        Outcome.Success(data)
    } else {
        AppLogger.w("Resposta sem dados: ${envelope.error?.message ?: envelope.message}", tag = tag)
        Outcome.Failure(envelope.toDomainError())
    }
}

/**
 * Igual ao `safeCall`, mas para listagens: devolve os itens junto do rodapé de
 * paginação, que é o que a tela precisa para saber se há próxima página.
 */
suspend inline fun <reified T> safeCallPaged(
    tag: String = "API",
    crossinline block: suspend () -> HttpResponse,
): Outcome<Page<T>> = runCatchingNetwork(tag) {
    val response = block()

    if (!response.status.isOk()) {
        return@runCatchingNetwork Outcome.Failure(response.toDomainError(tag))
    }

    val envelope = apiJson.decodeFromString<ApiEnvelope<List<T>>>(response.bodyAsText())
    val items = envelope.data

    if (envelope.success && items != null) {
        Outcome.Success(Page(items, envelope.meta ?: com.pixstop.mobile.data.remote.dto.PageMeta(total = items.size)))
    } else {
        Outcome.Failure(envelope.toDomainError())
    }
}

/**
 * Para as poucas rotas que respondem sem corpo útil, como marcar tudo lido.
 */
suspend inline fun safeCallUnit(
    tag: String = "API",
    crossinline block: suspend () -> HttpResponse,
): Outcome<Unit> = runCatchingNetwork(tag) {
    val response = block()

    if (response.status.isOk()) {
        Outcome.Success(Unit)
    } else {
        Outcome.Failure(response.toDomainError(tag))
    }
}

/**
 * Envolve a chamada e traduz queda de rede em `Offline`, que a tela trata
 * diferente de erro do servidor: um pede para tentar de novo, o outro não.
 */
@PublishedApi
internal suspend inline fun <T> runCatchingNetwork(
    tag: String,
    crossinline block: suspend () -> Outcome<T>,
): Outcome<T> = try {
    block()
} catch (cancellation: kotlinx.coroutines.CancellationException) {
    throw cancellation
} catch (error: Throwable) {
    AppLogger.e("Falha na chamada: ${error::class.simpleName}: ${error.message}", error, tag)

    if (error.isNetworkFailure()) {
        Outcome.Failure(DomainError.Offline())
    } else {
        Outcome.Failure(DomainError.Server(0, "Não foi possível completar a operação."))
    }
}

@PublishedApi
internal fun Throwable.isNetworkFailure(): Boolean {
    val name = this::class.simpleName.orEmpty()
    val text = message.orEmpty()

    return listOf("IO", "Connect", "Timeout", "UnresolvedAddress", "UnknownHost", "Socket")
        .any { name.contains(it) } ||
        listOf("Unable to resolve host", "Failed to connect", "Network is unreachable")
            .any { text.contains(it, ignoreCase = true) }
}

@PublishedApi
internal fun io.ktor.http.HttpStatusCode.isOk(): Boolean = value in 200..299

/**
 * Traduz o corpo do erro em `DomainError`, pelo `error.code` quando existe.
 */
@PublishedApi
internal suspend fun HttpResponse.toDomainError(tag: String): DomainError {
    val body = runCatching { bodyAsText() }.getOrNull().orEmpty()
    AppLogger.w("HTTP ${status.value}: $body", tag = tag)

    if (status.value == 422) {
        val validation = runCatching { apiJson.decodeFromString<ValidationErrorBody>(body) }.getOrNull()
        val fields = validation?.errors?.mapValues { it.value.firstOrNull().orEmpty() }.orEmpty()

        if (fields.isNotEmpty()) {
            return DomainError.Validation(
                fieldErrors = fields,
                message = fields.values.firstOrNull() ?: validation?.message ?: "Confira os campos.",
            )
        }
    }

    val envelope = runCatching { apiJson.decodeFromString<ApiEnvelope<Unit>>(body) }.getOrNull()
    val code = envelope?.error?.code
    val message = envelope?.error?.message ?: envelope?.message

    return when {
        code == ErrorCode.NO_ACTIVE_TENANT -> DomainError.NoActiveCompany(message ?: "Escolha uma empresa para continuar.")
        code == ErrorCode.USER_INACTIVE -> DomainError.UserInactive(message ?: "Seu acesso a esta empresa está desativado.")
        code == ErrorCode.PLAN_INACTIVE -> DomainError.PlanInactive(message ?: "A assinatura da empresa está irregular.")
        code == ErrorCode.CONSENT_REQUIRED -> DomainError.ConsentRequired(message ?: "Aceite os termos para continuar.")
        status.value == 401 -> DomainError.Unauthorized()
        status.value == 404 -> DomainError.NotFound(message ?: "Não encontramos o que você procura.")
        status.value == 429 -> DomainError.RateLimited(message ?: "Muitas tentativas. Aguarde um instante.")
        code != null -> DomainError.Rule(code, message ?: "Não foi possível completar a operação.")
        status.value in 400..499 -> DomainError.Rule("client_error", message ?: "Não foi possível completar a operação.")
        else -> DomainError.Server(status.value, message ?: "O servidor não respondeu como esperado.")
    }
}

@PublishedApi
internal fun ApiEnvelope<*>.toDomainError(): DomainError {
    val code = error?.code
    val text = error?.message ?: message ?: "Não foi possível completar a operação."

    return if (code != null) DomainError.Rule(code, text) else DomainError.Server(200, text)
}

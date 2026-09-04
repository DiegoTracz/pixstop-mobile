package com.pixstop.mobile.domain.model

/**
 * O que pode dar errado, em termos que a tela entende.
 *
 * Os códigos vêm do `error.code` da API e estão documentados em
 * `docs/API_MOBILE.md` do backend. A tela decide o que fazer pelo tipo, nunca
 * pela mensagem — a mensagem existe para ser lida por gente.
 */
sealed interface DomainError {

    val message: String

    /** Sessão caiu: o token venceu ou foi revogado. */
    data class Unauthorized(override val message: String = "Sua sessão expirou. Entre de novo.") : DomainError

    /** Nenhuma empresa escolhida. A tela deve levar à seleção de empresa. */
    data class NoActiveCompany(override val message: String = "Escolha uma empresa para continuar.") : DomainError

    /** O vínculo com a empresa foi desativado. */
    data class UserInactive(override val message: String) : DomainError

    /** A assinatura da empresa está irregular. */
    data class PlanInactive(override val message: String) : DomainError

    /** Falta aceitar os documentos legais. A tela deve abrir o aceite. */
    data class ConsentRequired(override val message: String) : DomainError

    /** Regra de negócio recusou; `code` diz qual. */
    data class Rule(val code: String, override val message: String) : DomainError

    /** Erro de preenchimento, com a mensagem de cada campo. */
    data class Validation(
        val fieldErrors: Map<String, String>,
        override val message: String,
    ) : DomainError

    /** Excesso de chamadas. */
    data class RateLimited(override val message: String) : DomainError

    data class NotFound(override val message: String = "Não encontramos o que você procura.") : DomainError

    data class Server(val status: Int, override val message: String) : DomainError

    /** Sem internet ou servidor inalcançável. */
    data class Offline(override val message: String = "Sem conexão. Verifique sua internet.") : DomainError
}

/**
 * Códigos de erro combinados com o backend. Mudar um valor daqui quebra
 * versões do app que já estão na mão das pessoas.
 */
object ErrorCode {
    const val NO_ACTIVE_TENANT = "no_active_tenant"
    const val USER_INACTIVE = "user_inactive"
    const val PLAN_INACTIVE = "plan_inactive"
    const val CONSENT_REQUIRED = "consent_required"
    const val INSUFFICIENT_PIXELS = "insufficient_pixels"
    const val INSUFFICIENT_BALANCE = "insufficient_balance"
    const val OUT_OF_STOCK = "out_of_stock"
    const val MERCADOPAGO_NOT_CONNECTED = "mercadopago_not_connected"
    const val PLAN_LIMIT_REACHED = "plan_limit_reached"
    const val TOO_MANY_REQUESTS = "too_many_requests"
}

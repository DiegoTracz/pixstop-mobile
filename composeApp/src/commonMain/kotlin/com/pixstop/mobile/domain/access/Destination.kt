package com.pixstop.mobile.domain.access

/**
 * Os lugares do aplicativo que dependem de permissão para existir.
 *
 * Estar aqui não é o mesmo que ter rota: é o que a matriz de acesso sabe
 * responder sobre. Telas que qualquer pessoa autenticada abre sempre — o
 * fechamento, o detalhe de um produto — não precisam entrar.
 */
enum class Destination(
    /** Funcionalidade do plano que o destino exige, quando exige alguma. */
    val requiredFeature: String? = null,
    /** Módulo do segmento (Fase 14) que o destino exige: a barbearia não tem loja. */
    val requiredModule: String? = null,
) {
    Home,
    Shop(requiredFeature = "products", requiredModule = "shop"),
    Cart(requiredFeature = "products", requiredModule = "shop"),
    Orders(requiredFeature = "orders", requiredModule = "shop"),
    Pixels(requiredFeature = "pixels"),
    /** A barra de XP por dentro: níveis e histórico. Vive junto dos pixels. */
    Progress(requiredFeature = "pixels"),
    Notifications,
    Team(requiredFeature = "departments", requiredModule = "departments"),
    /** Recompensas por voucher: o que os pixels compram onde não há loja. */
    Rewards(requiredFeature = "pixels", requiredModule = "vouchers"),
    /** O balcão: registrar a visita de quem está na frente. */
    Staff(requiredModule = "checkin"),
    Company,
    Profile,
}

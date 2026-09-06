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
) {
    Home,
    Shop(requiredFeature = "products"),
    Cart(requiredFeature = "products"),
    Orders(requiredFeature = "orders"),
    Pixels(requiredFeature = "pixels"),
    /** A barra de XP por dentro: níveis e histórico. Vive junto dos pixels. */
    Progress(requiredFeature = "pixels"),
    Notifications,
    Team(requiredFeature = "departments"),
    Company,
    Profile,
}

package com.pixstop.mobile.ui.navigation

/**
 * Rotas de navegação do app
 */
object Routes {
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val HOME = "home"

    /** Detalhe do produto, alcançado pela vitrine. */
    const val PRODUCT = "product"

    fun product(id: Long) = "product/$id"

    /** Carrinho, alcançado pelo ícone da barra superior. */
    const val CART = "cart"

    /** Fechamento do pedido, alcançado pelo carrinho. */
    const val CHECKOUT = "checkout"

    /** Desfecho de um pedido. */
    const val ORDER = "order"

    fun order(id: Long) = "order/$id"

    /** Histórico de pedidos. */
    const val ORDERS = "orders"

    /** Caixa de avisos, alcançada pelo sino da barra superior. */
    const val NOTIFICATIONS = "notifications"

    /** Aceite dos documentos legais: bloqueia o app enquanto faltar. */
    const val LEGAL_CONSENT = "legal-consent"

    /** Entrar numa empresa: única saída de quem está logado sem nenhuma. */
    const val JOIN_COMPANY = "join-company"
}

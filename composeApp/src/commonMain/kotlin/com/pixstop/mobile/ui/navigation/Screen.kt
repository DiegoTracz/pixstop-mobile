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

    /** Carteira e extrato de pixels. */
    const val PIXELS = "pixels"

    /** Comprar pixels para a carteira. */
    const val BUY_PIXELS = "pixels/buy"

    /** Progressão por XP: nível, o que falta e o histórico. */
    const val PROGRESS = "progress"

    /** Cartões guardados, alcançados pelo perfil. */
    const val CARDS = "cards"

    /** Recompensas por voucher. */
    const val REWARDS = "rewards"

    /** O balcão: registrar visitas. */
    const val STAFF = "staff"

    /** Um convite com pixels, alcançado pelo link. */
    const val INVITE = "invite/{code}"

    fun invite(code: String) = "invite/$code"

    /** Área do gestor: verba do time e distribuição. */
    const val TEAM = "team"

    /** Painel do administrador da empresa. */
    const val COMPANY = "company"

    /** Conectar e acompanhar as geladeiras (Fase 9.3). */
    const val FRIDGES = "fridges"

    /** O painel de quem opera geladeiras em várias empresas (Fase 16). */
    const val OPERATOR = "operator"

    /** Caixa de avisos, alcançada pelo sino da barra superior. */
    const val NOTIFICATIONS = "notifications"

    /** Aceite dos documentos legais: bloqueia o app enquanto faltar. */
    const val LEGAL_CONSENT = "legal-consent"

    /** Entrar numa empresa: única saída de quem está logado sem nenhuma. */
    const val JOIN_COMPANY = "join-company"
}

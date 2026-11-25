package com.pixstop.mobile.ui.navigation

/**
 * Rotas de navegação do app
 */
object Routes {
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val REGISTER = "register"

    /** Escolha entre cadastrar uma empresa ou uma pessoa, como na web. */
    const val REGISTER_CHOICE = "register/choice"
    const val REGISTER_COMPANY = "register/company"
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

    /** Roleta de prêmios (docs/plans/LANCAMENTO_LOCARMAIS.md no servidor). */
    const val PRIZE_WHEEL = "prize-wheel"

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

    /** O chat do assistente de IA (ASSISTENTE_IA.md, A8). */

    /**
     * A visita do repositor a uma geladeira: contar, ver as diferenças e
     * abastecer (docs/plans/CONCILIACAO_MOBILE.md). Alcançada a partir de uma
     * geladeira, nunca do menu: visita sem geladeira não existe.
     */
    const val VISIT = "visit"

    fun visit(applianceId: Long) = "visit/$applianceId"

    /** O painel de quem opera geladeiras em várias empresas (Fase 16). */
    /** A lista de compras da semana (docs/plans/COMPRAS_MOBILE.md). */
    const val SHOPPING_LIST = "shopping-list"

    /** Cadastrar produto pelo celular (APP_OPERADOR_PRODUTOS.md, G4). */
    const val PRODUCT_NEW = "products/new"

    /** Vários produtos numa foto (G4.1). */
    const val PRODUCT_BATCH = "products/batch"

    /** Os produtos de uma geladeira: ver, pôr e cadastrar (G2). */
    const val FRIDGE_PRODUCTS = "fridge-products"

    fun fridgeProducts(applianceId: Long) = "fridge-products/$applianceId"

    /** A validade por lugar (VALIDADE.md, V5 c). */
    const val EXPIRY = "expiry"

    /** A validade de cada produto (V5 d). */
    const val EXPIRY_SETTINGS = "expiry/products"

    /** As compras da empresa; `purchase-new` abre a câmera da nota. */
    const val PURCHASES = "purchases"

    const val PURCHASE_NEW = "purchase-new"

    fun purchase(id: Long) = "purchases/$id"

    const val OPERATOR = "operator"

    /** Caixa de avisos, alcançada pelo sino da barra superior. */
    const val NOTIFICATIONS = "notifications"

    /** Aceite dos documentos legais: bloqueia o app enquanto faltar. */
    const val LEGAL_CONSENT = "legal-consent"

    /** Entrar numa empresa: única saída de quem está logado sem nenhuma. */
    const val JOIN_COMPANY = "join-company"
}

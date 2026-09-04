package com.pixstop.mobile.core.config

import com.pixstop.mobile.BuildKonfig

/**
 * ╔════════════════════════════════════════════════════════════════════════════╗
 * ║                    CONFIGURAÇÃO DA API - AMBIENTES                        ║
 * ╠════════════════════════════════════════════════════════════════════════════╣
 * ║  As URLs são configuradas automaticamente via BuildKonfig por ambiente.    ║
 * ║                                                                           ║
 * ║  Android:                                                                 ║
 * ║    - Staging:   Build variant "stagingDebug" ou "stagingRelease"           ║
 * ║    - Produção:  Build variant "productionDebug" ou "productionRelease"     ║
 * ║                                                                           ║
 * ║  iOS:                                                                     ║
 * ║    - Staging:   ./gradlew ... -Penvironment=staging                       ║
 * ║    - Produção:  ./gradlew ... -Penvironment=production                    ║
 * ║                                                                           ║
 * ║  URLs configuradas em: composeApp/build.gradle.kts (seção BuildKonfig)    ║
 * ║                                                                           ║
 * ║  Para dev local com ngrok, adicione no local.properties:                  ║
 * ║    NGROK_URL=https://xxxx.ngrok-free.app/api                              ║
 * ╚════════════════════════════════════════════════════════════════════════════╝
 */
object ApiConfig {

    // ══════════════════════════════════════════════════════════════════════════
    // 🌍 AMBIENTE ATUAL (definido em tempo de compilação via BuildKonfig)
    // ══════════════════════════════════════════════════════════════════════════

    enum class Environment {
        LOCAL,
        STAGING,
        PRODUCTION;

        val isLocal: Boolean get() = this == LOCAL
        val isStaging: Boolean get() = this == STAGING
        val isProduction: Boolean get() = this == PRODUCTION
    }

    /**
     * Ambiente atual, definido em tempo de compilação.
     */
    val currentEnvironment: Environment = when (BuildKonfig.ENVIRONMENT) {
        "local" -> Environment.LOCAL
        "staging" -> Environment.STAGING
        else -> Environment.PRODUCTION
    }

    /** Verifica se está em modo de produção */
    val isProduction: Boolean get() = BuildKonfig.IS_PRODUCTION

    /** Verifica se está em modo de staging */
    val isStaging: Boolean get() = currentEnvironment == Environment.STAGING

    /** Verifica se está em modo local (dev) */
    val isLocal: Boolean get() = currentEnvironment == Environment.LOCAL

    // ══════════════════════════════════════════════════════════════════════════
    // 🔧 URL BASE (definida automaticamente pelo ambiente)
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * URL base da API, configurada automaticamente pelo BuildKonfig.
     *
     * ┌─────────────────────┬──────────────────────────────────────────┐
     * │ Ambiente            │ URL                                      │
     * ├─────────────────────┼──────────────────────────────────────────┤
     * │ Local               │ NGROK_URL do local.properties            │
     * │ Staging             │ https://staging.pixstop.com.br/api       │
     * │ Produção            │ https://pixstop.com.br/api               │
     * └─────────────────────┴──────────────────────────────────────────┘
     *
     * Para dev local com ngrok, adicione no local.properties:
     *   NGROK_URL=https://xxxx.ngrok-free.app/api
     */
    var baseUrl: String = BuildKonfig.BASE_URL
        private set

    // ══════════════════════════════════════════════════════════════════════════
    // 📡 ENDPOINTS DA API
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Endpoints de autenticação.
     * Altere se sua API Laravel usar rotas diferentes.
     */
    object Endpoints {

        // ── Abertas ───────────────────────────────────────────────────────
        const val CONFIG = "config"
        const val LOGIN = "auth/login"
        const val REGISTER_USER = "auth/register/user"
        const val REGISTER_COMPANY = "auth/register/company"
        const val FORGOT_PASSWORD = "auth/forgot-password"

        // ── Conta ─────────────────────────────────────────────────────────
        const val PROFILE = "me"
        const val LOGOUT = "auth/logout"
        const val REFRESH = "auth/refresh"
        const val DELETE_ACCOUNT = "account"
        const val UPDATE_PROFILE = "me"
        const val UPDATE_PASSWORD = "me/password"
        const val UPDATE_AVATAR = "me/avatar"

        // ── Empresa ───────────────────────────────────────────────────────
        const val SWITCH_TENANT = "tenant/switch"
        const val JOIN_TENANT = "tenant/join"
        const val TENANT_INVITE = "tenant/invite"

        // ── Notificações e push ───────────────────────────────────────────
        const val NOTIFICATIONS = "notifications"
        const val NOTIFICATIONS_READ_ALL = "notifications/read-all"

        fun notificationRead(id: String) = "notifications/$id/read"

        const val DEVICES = "devices"

        fun device(token: String) = "devices/$token"

        // ── Documentos legais ─────────────────────────────────────────────
        const val LEGAL_DOCUMENTS = "legal/documents"
        const val LEGAL_CONSENT = "legal/consent"

        // ── Loja ──────────────────────────────────────────────────────────
        const val SHOP_CATEGORIES = "shop/categories"
        const val SHOP_PRODUCTS = "shop/products"

        fun shopProduct(id: Long) = "shop/products/$id"

        // ── Carrinho ──────────────────────────────────────────────────────
        const val CART = "cart"
        const val CART_ADD = "cart/add"

        fun cartItem(id: Long) = "cart/$id"

        // ── Checkout e pedidos ────────────────────────────────────────────
        const val CHECKOUT = "checkout"
        const val ORDERS = "orders"

        fun order(id: Long) = "orders/$id"

        fun orderStatus(id: Long) = "orders/$id/status"

        fun orderCancel(id: Long) = "orders/$id/cancel"

        // ── Pagamento ─────────────────────────────────────────────────────
        const val PAYMENT_CARDS = "payment/cards"
        const val PAYMENT_CONFIG = "payment/config"

        fun paymentCard(id: Long) = "payment/cards/$id"

        fun paymentCardDefault(id: Long) = "payment/cards/$id/default"

        // ── Pixels ────────────────────────────────────────────────────────
        const val PIXELS_BALANCE = "pixels/balance"
        const val PIXELS_HISTORY = "pixels/history"

        // ── Gestor ────────────────────────────────────────────────────────
        const val TEAM = "team"

        fun teamMembers(departmentId: Long) = "team/$departmentId/members"

        fun teamHistory(departmentId: Long) = "team/$departmentId/history"

        fun teamDistribute(departmentId: Long) = "team/$departmentId/distribute"

        // ── Admin da empresa ──────────────────────────────────────────────
        const val COMPANY_DASHBOARD = "company/dashboard"
        const val COMPANY_ORDERS = "company/orders"
        const val COMPANY_USERS = "company/users"
        const val COMPANY_DEPARTMENTS = "company/departments"
        const val COMPANY_BALANCE = "company/users/balance"
        const val COMPANY_PIXELS_DISTRIBUTE = "company/pixels/distribute"
        const val COMPANY_PIXELS_HISTORY = "company/pixels/history"

        fun companyOrderStatus(id: Long) = "company/orders/$id/status"

        fun companyUserToggle(id: Long) = "company/users/$id/toggle"

        fun companyDepartmentAllocate(id: Long) = "company/departments/$id/allocate"
    }

    // ══════════════════════════════════════════════════════════════════════════
    // ⏱️ CONFIGURAÇÕES DE TIMEOUT
    // ══════════════════════════════════════════════════════════════════════════

    /** Timeout de conexão em milissegundos (padrão: 30 segundos) */
    const val CONNECTION_TIMEOUT_MS = 30_000L

    /** Timeout de requisição em milissegundos (padrão: 30 segundos) */
    const val REQUEST_TIMEOUT_MS = 30_000L

    // ══════════════════════════════════════════════════════════════════════════
    // 🔒 CONFIGURAÇÕES INTERNAS
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Configura a URL base da API em runtime.
     * Útil para testes ou override manual.
     *
     * @param url URL base da API (ex: "https://api.exemplo.com/api")
     */
    fun configure(url: String) {
        baseUrl = url.trimEnd('/')
    }

    /**
     * Reseta a URL para o valor padrão do ambiente atual (definido pelo BuildKonfig).
     */
    fun reset() {
        baseUrl = BuildKonfig.BASE_URL
    }
}

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
        /** POST - Login do usuário. Body: { "user": "email", "password": "senha" } */
        const val LOGIN = "auth/login"

        /** POST - Logout do usuário. Header: Bearer token */
        const val LOGOUT = "auth/logout"

        /** GET - Dados do perfil do usuário logado. Header: Bearer token */
        const val PROFILE = "me"

        /** POST - Registro de novo usuário. Body: { name, email, password, password_confirmation, company_code? } */
        const val REGISTER_USER = "auth/register/user"

        /** POST - Solicitar reset de senha. Body: { email } */
        const val FORGOT_PASSWORD = "auth/forgot-password"
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

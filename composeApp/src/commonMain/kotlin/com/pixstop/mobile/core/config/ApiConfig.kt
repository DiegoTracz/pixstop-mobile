package com.pixstop.mobile.core.config

/**
 * ╔════════════════════════════════════════════════════════════════════════════╗
 * ║                    CONFIGURAÇÃO DA API - ALTERE AQUI                       ║
 * ╠════════════════════════════════════════════════════════════════════════════╣
 * ║  Este arquivo centraliza todas as URLs e configurações de rede do app.     ║
 * ║  Altere os valores abaixo conforme seu ambiente (dev/prod).                ║
 * ╚════════════════════════════════════════════════════════════════════════════╝
 */
object ApiConfig {

    // ══════════════════════════════════════════════════════════════════════════
    // 🔧 CONFIGURAÇÃO PRINCIPAL - ALTERE AQUI
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * URL base da API Laravel.
     *
     * ⚠️ IMPORTANTE: Altere para a URL do SEU servidor!
     *
     * Exemplos por ambiente:
     * ┌─────────────────────┬──────────────────────────────────────────┐
     * │ Ambiente            │ URL                                      │
     * ├─────────────────────┼──────────────────────────────────────────┤
     * │ Android Emulator    │ "http://10.0.2.2/api"                    │
     * │ iOS Simulator       │ "http://localhost/api"                   │
     * │ Laravel Sail        │ "http://localhost:80/api"                │
     * │ Dispositivo físico  │ "http://192.168.x.x/api" (IP da máquina) │
     * │ Produção            │ "https://seu-dominio.com/api"            │
     * └─────────────────────┴──────────────────────────────────────────┘
     */
    private const val BASE_URL = "https://pixstop.com.br/api"

    // ══════════════════════════════════════════════════════════════════════════
    // 📡 ENDPOINTS DA API - ALTERE SE SUA API TIVER ROTAS DIFERENTES
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

        // Adicione novos endpoints aqui conforme necessidade:
        // const val REGISTER = "/auth/register"
        // const val FORGOT_PASSWORD = "/auth/forgot-password"
        // const val PRODUCTS = "/products"
    }

    // ══════════════════════════════════════════════════════════════════════════
    // ⏱️ CONFIGURAÇÕES DE TIMEOUT
    // ══════════════════════════════════════════════════════════════════════════

    /** Timeout de conexão em milissegundos (padrão: 30 segundos) */
    const val CONNECTION_TIMEOUT_MS = 30_000L

    /** Timeout de requisição em milissegundos (padrão: 30 segundos) */
    const val REQUEST_TIMEOUT_MS = 30_000L

    // ══════════════════════════════════════════════════════════════════════════
    // 🔒 CONFIGURAÇÕES INTERNAS - NÃO ALTERE
    // ══════════════════════════════════════════════════════════════════════════

    /** URL base atual (pode ser alterada em runtime via configure()) */
    var baseUrl: String = BASE_URL
        private set

    /**
     * Configura a URL base da API em runtime.
     * Útil para alternar entre ambientes sem recompilar.
     *
     * Exemplo de uso no MainActivity.kt:
     * ```kotlin
     * override fun onCreate(savedInstanceState: Bundle?) {
     *     super.onCreate(savedInstanceState)
     *
     *     // Configura a URL antes de iniciar o app
     *     ApiConfig.configure("https://api.meuapp.com/api")
     *
     *     setContent { App() }
     * }
     * ```
     *
     * @param url URL base da API (ex: "https://api.exemplo.com/api")
     */
    fun configure(url: String) {
        baseUrl = url.trimEnd('/')
    }

    /**
     * Reseta a URL para o valor padrão definido em BASE_URL.
     */
    fun reset() {
        baseUrl = BASE_URL
    }
}

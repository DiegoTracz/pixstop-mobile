package com.pixstop.mobile.domain.model

/**
 * As regras que o servidor publica para o app não precisar adivinhá-las.
 *
 * Cada empresa configura as suas, e elas mudam sem nova versão do aplicativo —
 * um número escrito no código ficaria errado no dia em que o servidor mudasse,
 * sem ninguém perceber.
 */
data class AppConfig(
    val cartReservationMinutes: Int,
    val pixExpirationMinutes: Int,
    val lowStockThreshold: Int,
    val maxInstallments: Int,
    val paymentPublicKey: String?,
    val isSandbox: Boolean,
    val pixelsPerReal: Int,
    val accountPurgeAfterDays: Int,
    val termsUrl: String?,
    val privacyUrl: String?,
    val maxImageKb: Int,
    val trialDays: Int,
) {
    /** Sem chave pública não há como tokenizar cartão neste aparelho. */
    val canTokenizeCard: Boolean get() = !paymentPublicKey.isNullOrBlank()

    companion object {
        /**
         * O que vale antes da primeira resposta, e quando ela não vem.
         *
         * São os mesmos padrões do servidor. Existirem aqui é o que permite a
         * tela abrir sem esperar a rede — e continuar abrindo quando a rota
         * estiver fora do ar.
         */
        val Defaults = AppConfig(
            cartReservationMinutes = 5,
            pixExpirationMinutes = 30,
            lowStockThreshold = 5,
            maxInstallments = 12,
            paymentPublicKey = null,
            isSandbox = false,
            pixelsPerReal = 100,
            accountPurgeAfterDays = 90,
            termsUrl = null,
            privacyUrl = null,
            maxImageKb = 5_120,
            trialDays = 30,
        )
    }
}

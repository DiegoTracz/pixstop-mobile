package com.pixstop.mobile.data.repository

import com.pixstop.mobile.core.config.ApiConfig
import com.pixstop.mobile.core.logging.AppLogger
import com.pixstop.mobile.core.network.safeCall
import com.pixstop.mobile.data.remote.dto.AppConfigDto
import com.pixstop.mobile.domain.model.AppConfig
import com.pixstop.mobile.domain.model.Outcome
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val TAG = "Config"

/**
 * As regras que o servidor publica.
 *
 * Começa com os padrões embutidos e revalida no arranque. A rota é pública, e
 * de propósito: a consulta acontece antes do login. Uma falha aqui não é erro
 * de tela nenhuma — o app segue com os padrões, que é o pior caso aceitável.
 */
class AppConfigRepository(private val client: HttpClient) {

    private val _config = MutableStateFlow(AppConfig.Defaults)
    val config: StateFlow<AppConfig> = _config.asStateFlow()

    suspend fun refresh() {
        when (val result = safeCall<AppConfigDto>(TAG) { client.get(ApiConfig.Endpoints.CONFIG) }) {
            is Outcome.Success -> _config.value = result.value.toDomain()

            is Outcome.Failure -> AppLogger.w(
                "Configuração do servidor indisponível; seguindo com os padrões embutidos.",
                tag = TAG,
            )
        }
    }
}

private fun AppConfigDto.toDomain() = AppConfig(
    cartReservationMinutes = cart.reservationMinutes,
    pixExpirationMinutes = cart.pixExpirationMinutes,
    lowStockThreshold = cart.lowStockThreshold,
    maxInstallments = payment.maxInstallments,
    paymentPublicKey = payment.publicKey,
    isSandbox = payment.isSandbox,
    pixelsPerReal = pixels.pixelsPerReal,
    accountPurgeAfterDays = account.purgeAfterDays,
    termsUrl = legal.termsUrl,
    privacyUrl = legal.privacyUrl,
    maxImageKb = upload.maxImageKb,
    trialDays = trialDays,
)

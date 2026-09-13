package com.pixstop.mobile.domain.access

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiNetworkSpecifier
import android.os.Build
import com.pixstop.mobile.core.logging.AppLogger
import com.pixstop.mobile.domain.model.DomainError
import com.pixstop.mobile.domain.model.Outcome
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

private const val TAG = "FridgeNetwork"

/**
 * Entra na rede da geladeira com `WifiNetworkSpecifier` (Android 10+).
 *
 * A rede pedida assim é do app, não do aparelho: não tem internet, não
 * aparece como a rede do celular, e some quando o app solta. Enquanto ela
 * está presa ao processo, todo pedido HTTP do app sai por ela — por isso a
 * tela só volta a falar com o servidor Pixelstop depois de `leave()`.
 */
class AndroidFridgeNetworkConnector(context: Context) : FridgeNetworkConnector {

    private val connectivity = context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private var callback: ConnectivityManager.NetworkCallback? = null

    override val canJoin: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

    override suspend fun join(ssid: String, password: String): Outcome<Unit> {
        if (!canJoin) {
            return Outcome.Failure(DomainError.Rule("manual_network", "Este Android não deixa o app trocar de rede. Entre na $ssid pelas configurações."))
        }

        leave()

        val specifier = WifiNetworkSpecifier.Builder()
            .setSsid(ssid)
            .apply { if (password.isNotEmpty()) setWpa2Passphrase(password) }
            .build()

        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .removeCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .setNetworkSpecifier(specifier)
            .build()

        return suspendCancellableCoroutine { continuation ->
            val networkCallback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    connectivity.bindProcessToNetwork(network)
                    AppLogger.i("Na rede $ssid", tag = TAG)

                    if (continuation.isActive) continuation.resume(Outcome.Success(Unit))
                }

                override fun onUnavailable() {
                    AppLogger.w("Rede $ssid indisponível", tag = TAG)

                    if (continuation.isActive) {
                        continuation.resume(
                            Outcome.Failure(DomainError.Offline("Não encontrei a rede $ssid. A geladeira está ligada há mais de um minuto?")),
                        )
                    }
                }

                override fun onLost(network: Network) {
                    AppLogger.w("Rede $ssid caiu", tag = TAG)
                    connectivity.bindProcessToNetwork(null)
                }
            }

            callback = networkCallback
            connectivity.requestNetwork(request, networkCallback, JOIN_TIMEOUT_MS)

            continuation.invokeOnCancellation { leave() }
        }
    }

    override fun leave() {
        connectivity.bindProcessToNetwork(null)

        callback?.let {
            runCatching { connectivity.unregisterNetworkCallback(it) }
            callback = null
        }
    }

    private companion object {
        /** O sistema mostra o diálogo de escolha; um minuto dá tempo de ler. */
        const val JOIN_TIMEOUT_MS = 60_000
    }
}

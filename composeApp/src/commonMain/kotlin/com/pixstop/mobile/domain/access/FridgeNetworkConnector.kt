package com.pixstop.mobile.domain.access

import com.pixstop.mobile.domain.model.Outcome

/**
 * Entra na rede WiFi que a geladeira cria no modo de configuração
 * (Fase 9.3), sem a pessoa sair do app.
 *
 * No Android isso é um pedido ao sistema por uma rede com aquele nome, que
 * fica presa a este app enquanto durar e não tira a internet do resto do
 * aparelho. Onde não dá para fazer isso, a tela explica como entrar na rede
 * pelas configurações do celular — e o fluxo continua igual.
 */
interface FridgeNetworkConnector {

    /** Se este aparelho consegue entrar na rede sozinho. */
    val canJoin: Boolean

    /**
     * Entra na rede e prende as conexões do app a ela até `leave()`.
     */
    suspend fun join(ssid: String, password: String): Outcome<Unit>

    /** Solta a rede da geladeira e volta à internet normal. */
    fun leave()
}

/**
 * Onde o sistema não deixa o app trocar de rede: a tela pede que a pessoa
 * entre na `Pixelstop-Setup` à mão, e o resto segue igual.
 */
class ManualFridgeNetworkConnector : FridgeNetworkConnector {

    override val canJoin: Boolean = false

    override suspend fun join(ssid: String, password: String): Outcome<Unit> =
        Outcome.Failure(
            com.pixstop.mobile.domain.model.DomainError.Rule(
                "manual_network",
                "Entre na rede $ssid pelas configurações de WiFi do celular e volte aqui.",
            ),
        )

    override fun leave() = Unit
}

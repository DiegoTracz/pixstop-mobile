package com.pixstop.mobile.domain.access

import com.pixstop.mobile.domain.model.BluetoothUnlockResult
import com.pixstop.mobile.domain.model.DomainError
import com.pixstop.mobile.domain.model.Outcome
import com.pixstop.mobile.domain.model.UnlockTicket

/**
 * Abre a geladeira pelo rádio, quando ela está sem internet (Fase 9.7).
 *
 * O celular acabou de pagar pela internet e leva até a porta um bilhete
 * assinado pelo servidor; a geladeira confere a assinatura sozinha. Nada de
 * pareamento: quem decide é o bilhete, e a conexão é descartável — conecta,
 * escreve, lê o resultado e solta.
 *
 * Onde não há rádio, ou onde a plataforma ainda não faz isso, `isSupported` é
 * falso e a tela nem oferece o caminho.
 */
interface FridgeUnlockConnector {

    /** Este aparelho consegue falar com a geladeira por Bluetooth. */
    val isSupported: Boolean

    /**
     * As permissões que ainda faltam para tentar. Vazio quando já dá para
     * abrir, ou quando a plataforma não pede nenhuma.
     */
    val missingPermissions: List<String>

    /**
     * Procura a geladeira do bilhete, entrega, e devolve o que ela respondeu.
     *
     * A falha aqui é de rádio — não achou, não conectou, não respondeu; o
     * "não" da própria geladeira é um `BluetoothUnlockResult`, e vem como
     * sucesso, porque ele tem uma conversa própria com quem está na porta.
     */
    suspend fun unlock(ticket: UnlockTicket): Outcome<BluetoothUnlockResult>
}

/**
 * Onde o Bluetooth ainda não existe — iOS por enquanto, e qualquer aparelho
 * sem rádio. A tela cai no caminho de sempre: avisar e mandar procurar o
 * responsável.
 */
class UnavailableFridgeUnlockConnector(
    private val message: String = "Este aparelho não abre a geladeira por Bluetooth.",
) : FridgeUnlockConnector {

    override val isSupported: Boolean = false

    override val missingPermissions: List<String> = emptyList()

    override suspend fun unlock(ticket: UnlockTicket): Outcome<BluetoothUnlockResult> =
        Outcome.Failure(DomainError.Rule("ble_unsupported", message))
}

package com.pixstop.mobile.core.storage

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

/**
 * Guarda o token e avisa quando a sessão cai.
 *
 * O token em si mora no `TokenManager`; o que existe aqui é o aviso de queda:
 * o cliente HTTP percebe um 401, emite `sessionExpired`, e a navegação leva a
 * pessoa ao login sem que cada tela precise tratar isso.
 */
class SessionStore(private val tokens: TokenManager) {

    private val _sessionExpired = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    /** Emite quando o servidor recusa o token. A navegação escuta e desloga. */
    val sessionExpired: SharedFlow<Unit> = _sessionExpired

    fun currentToken(): String? = tokens.getToken()

    fun save(token: String) = tokens.saveToken(token)

    fun isLoggedIn(): Boolean = tokens.hasToken()

    /**
     * Encerra a sessão local e avisa quem estiver ouvindo.
     */
    fun expire() {
        tokens.clearAll()
        _sessionExpired.tryEmit(Unit)
    }
}

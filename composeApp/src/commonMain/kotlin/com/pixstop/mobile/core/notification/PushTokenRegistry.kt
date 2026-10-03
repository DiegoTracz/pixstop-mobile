package com.pixstop.mobile.core.notification

import com.pixstop.mobile.core.logging.AppLogger
import com.pixstop.mobile.core.storage.SessionStore
import com.pixstop.mobile.data.repository.NotificationRepository
import com.pixstop.mobile.domain.model.Outcome
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private const val TAG = "Push"

/**
 * O token de push do aparelho e o servidor, de acordo.
 *
 * O token chega quando o sistema quer — às vezes antes do login, às vezes
 * trocado no meio do uso —, e o servidor só pode guardá-lo com alguém logado.
 * Por isso ele fica aqui até haver sessão, e [sync] o leva quando ela existe.
 *
 * A cada abertura do app o token é enviado de novo: o servidor faz `upsert`, e
 * é assim que ele sabe que o aparelho continua vivo e de quem é agora.
 */
class PushTokenRegistry(
    private val notifications: NotificationRepository,
    private val session: SessionStore,
    private val appVersion: String?,
) {

    private val mutex = Mutex()

    private var token: String? = null

    /** O token que o servidor já tem para esta pessoa, nesta execução do app. */
    private var registered: String? = null

    /** O sistema entregou um token, novo ou o de sempre. */
    suspend fun onNewToken(newToken: String) {
        mutex.withLock { token = newToken }
        sync()
    }

    /** Leva o token ao servidor, se houver token, sessão, e ele ainda não estiver lá. */
    suspend fun sync() = mutex.withLock {
        val current = token ?: return@withLock

        if (!session.isLoggedIn() || registered == current) {
            return@withLock
        }

        when (val result = notifications.registerDevice(current, devicePlatform, deviceName(), appVersion)) {
            is Outcome.Success -> registered = current
            is Outcome.Failure -> AppLogger.w("Token de push não registrado: ${result.error.message}", tag = TAG)
        }
    }

    /**
     * Desliga o aparelho da conta antes de sair.
     *
     * Precisa vir antes do logout, enquanto a sessão ainda vale: depois dele o
     * servidor recusaria a chamada, e quem saiu continuaria recebendo os avisos
     * da conta que deixou o aparelho.
     */
    suspend fun unregister() = mutex.withLock {
        val current = registered ?: token ?: return@withLock

        notifications.unregisterDevice(current)
        registered = null
    }
}

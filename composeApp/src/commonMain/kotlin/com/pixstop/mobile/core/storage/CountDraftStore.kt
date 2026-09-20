package com.pixstop.mobile.core.storage

import com.russhwolf.settings.Settings
import com.russhwolf.settings.set
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * O rascunho da contagem, guardado no aparelho.
 *
 * Uma copa sem sinal é comum, e refazer trinta linhas é o que faz alguém
 * parar de contar. Cada toque grava; o aplicativo pode fechar, a bateria pode
 * acabar, a rede pode cair — a contagem continua onde parou.
 *
 * O `clientId` viaja junto porque é ele que torna o reenvio inofensivo: a
 * mesma contagem entregue duas vezes é um documento só, no servidor.
 */
@Serializable
data class CountDraft(
    val clientId: String,
    val startedAt: String,
    /** Produto → quantidade contada. O que não está aqui é "não contado". */
    val counts: Map<String, Int> = emptyMap(),
) {
    val isEmpty: Boolean get() = counts.isEmpty()
}

class CountDraftStore(private val settings: Settings) {

    companion object {
        private const val PREFIX = "count_draft_"

        /** Rascunho velho é mais perigoso que rascunho nenhum. */
        const val VALID_HOURS: Int = 24
    }

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    fun save(applianceId: Long, draft: CountDraft) {
        settings[key(applianceId)] = json.encodeToString(CountDraft.serializer(), draft)
    }

    /**
     * O rascunho daquela geladeira, se ainda vale. `nowEpochHours` entra por
     * fora para o teste não depender do relógio.
     */
    fun load(applianceId: Long): CountDraft? =
        settings.getStringOrNull(key(applianceId))?.let { stored ->
            runCatching { json.decodeFromString(CountDraft.serializer(), stored) }.getOrNull()
        }

    fun clear(applianceId: Long) {
        settings.remove(key(applianceId))
    }

    private fun key(applianceId: Long): String = "$PREFIX$applianceId"
}

package com.pixstop.mobile.domain.model

/**
 * A visita do repositor, em domínio (docs/plans/CONCILIACAO_MOBILE.md).
 *
 * O celular está numa mão e a porta da geladeira na outra: tudo aqui existe
 * para caber nesse minuto. A conciliação acontece como efeito colateral de um
 * trabalho que ele já faz — abastecer — e não como tarefa a mais.
 */
data class CountSheet(
    val applianceId: Long,
    val applianceName: String,
    /** Nasce no servidor e vai no envio: é o que impede ajustar duas vezes. */
    val clientId: String,
    val products: List<CountProduct>,
    val warehouse: List<WarehouseProduct>,
    val lastCount: LastCount? = null,
)

data class CountProduct(
    val id: Long,
    val name: String,
    val category: String?,
    val barcode: String?,
    val imageUrl: String?,
)

data class WarehouseProduct(
    val id: Long,
    val name: String,
    val barcode: String?,
    val inWarehouse: Int,
    val lowStockThreshold: Int?,
) {
    /**
     * Quanto sugerir de reposição: a grade cheia menos o que ficou na porta,
     * limitado pelo que existe no depósito. Enquanto não houver alvo por
     * geladeira, a grade é o dobro do mínimo do produto.
     */
    fun suggestionFor(inAppliance: Int): Int {
        val target = (lowStockThreshold ?: 0) * 2

        return minOf(maxOf(0, target - inAppliance), inWarehouse)
    }
}

data class LastCount(val id: Long, val at: String, val by: String?, val daysAgo: Int)

/** O que a contagem achou. É a primeira vez que quem contou vê o esperado. */
data class CountResult(
    val id: Long,
    val countedItems: Int,
    val skippedItems: Int,
    val missingValue: Double,
    val alert: Boolean,
    val items: List<CountResultItem>,
) {
    val missing: List<CountResultItem> get() = items.filter { it.counted != null && it.difference < 0 }

    val extra: List<CountResultItem> get() = items.filter { it.counted != null && it.difference > 0 }

    val matched: Int get() = items.count { it.counted != null && it.difference == 0 }

    val hasDifferences: Boolean get() = missing.isNotEmpty() || extra.isNotEmpty()
}

data class CountResultItem(
    val productId: Long,
    val product: String,
    val expected: Int,
    val counted: Int?,
    val difference: Int,
    val differenceValue: Double,
    val suggestedReason: LossSuggestion?,
    val recounted: Boolean = false,
)

/**
 * O palpite de quem contou sobre o que aconteceu com o que falta.
 *
 * É sugestão, e o texto da tela diz isso: quem transforma falta em perda é o
 * dono do estoque, no fechamento do mês. Separação de funções.
 */
enum class LossSuggestion(val api: String, val label: String) {
    Theft("theft", "Levado sem pagar"),
    Expiry("expiry", "Vencido"),
    Damage("damage", "Avariado"),
    ;

    companion object {
        fun fromApi(value: String?): LossSuggestion? = entries.firstOrNull { it.api == value }
    }
}

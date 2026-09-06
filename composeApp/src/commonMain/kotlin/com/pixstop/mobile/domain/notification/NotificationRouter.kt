package com.pixstop.mobile.domain.notification

import com.pixstop.mobile.domain.model.AppNotification

/**
 * Traduz um aviso no lugar que ele deveria abrir.
 *
 * Três origens dizem a mesma coisa de formas diferentes e todas passam por
 * aqui, para que o app não tenha três ideias sobre onde um pedido pago fica:
 *
 * - o `action_url` que a API já grava em cada aviso (`/orders/12`), pensado
 *   para o painel web;
 * - o `pixstop://` de um deep link, que chega pelo sistema;
 * - o par `type` + `id` do payload de push.
 *
 * Nada aqui adivinha: link que não é reconhecido não vira alvo, e o app deixa
 * a pessoa na caixa de avisos em vez de abrir uma tela errada.
 */
object NotificationRouter {

    /**
     * Resolve um aviso da caixa: o `action_url` manda, e o `type` cobre os
     * avisos antigos que ainda não gravavam link.
     */
    fun resolve(notification: AppNotification): NotificationTarget? {
        val byType = fromType(notification.type)

        // O aviso de progresso aponta para "Meus pixels" na web, que lá reúne
        // carteira e barra; aqui a barra tem tela própria, e o tipo sabe disso.
        if (byType is NotificationTarget.Progress) {
            return byType
        }

        return fromLink(notification.actionUrl) ?: byType
    }

    /**
     * Resolve um link, seja o caminho do painel web (`/orders/12`) ou o deep
     * link (`pixstop://order/12`).
     */
    fun fromLink(link: String?): NotificationTarget? {
        val path = normalize(link) ?: return null
        val segments = path.split('/').filter { it.isNotBlank() }
        if (segments.isEmpty()) {
            return null
        }

        return when (segments.first()) {
            // `/orders/12` do painel web e `pixstop://order/12` do push
            // apontam para o mesmo pedido.
            "order", "orders" -> segments.getOrNull(1)
                ?.toLongOrNull()
                ?.let { NotificationTarget.Order(it) }
                ?: NotificationTarget.Orders

            "pixels" -> NotificationTarget.Pixels

            // "Meus pixels" na web reúne carteira e progresso; aqui o aviso de
            // progresso já sabe para onde quer ir pelo tipo, e a URL cai na carteira.
            "my-pixels" -> NotificationTarget.Pixels

            "notifications" -> NotificationTarget.Notifications

            // O painel da empresa no app reúne o que na web são páginas
            // separadas — pedidos, plano, add-ons. Assinatura e hardware não
            // têm tela aqui, então param na entrada do painel.
            "company" -> NotificationTarget.Company

            // Retaguarda da plataforma: existe só na web.
            else -> null
        }
    }

    /**
     * Resolve o payload de um push, que traz `type` e, quando o aviso é de um
     * registro específico, o `id` dele.
     */
    fun fromPayload(type: String?, id: String?): NotificationTarget? {
        val target = fromType(type) ?: return null
        val numericId = id?.toLongOrNull()

        return if (target is NotificationTarget.Orders && numericId != null) {
            NotificationTarget.Order(numericId)
        } else {
            target
        }
    }

    private fun fromType(type: String?): NotificationTarget? = when (type) {
        "order_paid", "order_canceled" -> NotificationTarget.Orders
        "pixels_received", "pixels_expiring" -> NotificationTarget.Pixels
        "level_up", "season_closed" -> NotificationTarget.Progress
        "new_order_received", "plan_changed", "subscription_activated",
        "subscription_canceled", "subscription_payment_failed",
        "trial_ending", "trial_expired",
        "addon_contracted", "addon_cancellation_updated",
        -> NotificationTarget.Company

        else -> null
    }

    /**
     * Reduz qualquer uma das formas ao caminho puro: sem esquema, sem host e
     * sem query.
     */
    private fun normalize(link: String?): String? {
        val trimmed = link?.trim().orEmpty()
        if (trimmed.isEmpty()) {
            return null
        }

        val separator = trimmed.indexOf("://")
        val scheme = if (separator < 0) "" else trimmed.take(separator)
        val withoutScheme = when {
            separator < 0 -> trimmed

            // `https://pixstop.com.br/orders/12`: o primeiro segmento é o
            // domínio e não diz nada sobre a tela.
            scheme.equals("http", ignoreCase = true) || scheme.equals("https", ignoreCase = true) ->
                trimmed.substring(separator + 3).substringAfter('/', missingDelimiterValue = "")

            // `pixstop://order/12`: o que a URI chama de host já é o primeiro
            // segmento do caminho. O esquema em si não é conferido aqui — ele
            // muda por ambiente (`pixstop-local`, `pixstop-staging`) e quem
            // garante que só os nossos links chegam é o filtro do manifesto.
            else -> trimmed.substring(separator + 3)
        }

        return withoutScheme.substringBefore('?').substringBefore('#')
    }
}

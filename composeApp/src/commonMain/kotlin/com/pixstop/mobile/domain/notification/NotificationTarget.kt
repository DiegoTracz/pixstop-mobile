package com.pixstop.mobile.domain.notification

import com.pixstop.mobile.domain.access.Destination

/**
 * Para onde um aviso leva.
 *
 * Nem todo aviso leva a algum lugar do aplicativo: parte deles é do painel web
 * — assinatura, add-ons, o retaguarda do administrador da plataforma — e não
 * tem tela equivalente aqui. Esses simplesmente não viram alvo.
 *
 * Cada alvo carrega o [Destination] correspondente para que a mesma matriz de
 * acesso que monta as barras decida se ele pode ser aberto. Um aviso não é
 * autorização: quem perdeu o papel de administrador não deve chegar ao painel
 * da empresa por um aviso antigo.
 */
sealed interface NotificationTarget {

    val destination: Destination

    /** O desfecho de um pedido específico. */
    data class Order(val id: Long) : NotificationTarget {
        override val destination: Destination get() = Destination.Orders
    }

    /** O histórico, quando o aviso é de pedido mas não diz qual. */
    data object Orders : NotificationTarget {
        override val destination: Destination get() = Destination.Orders
    }

    /** A carteira e o extrato de pixels. */
    data object Pixels : NotificationTarget {
        override val destination: Destination get() = Destination.Pixels
    }

    /** O painel do administrador da empresa. */
    data object Company : NotificationTarget {
        override val destination: Destination get() = Destination.Company
    }

    /** A caixa de avisos. É onde um push genérico deixa a pessoa. */
    data object Notifications : NotificationTarget {
        override val destination: Destination get() = Destination.Notifications
    }
}

package com.pixstop.mobile.domain.notification

import com.pixstop.mobile.domain.access.Destination
import com.pixstop.mobile.domain.model.AppNotification
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class NotificationRouterTest {

    private fun aviso(type: String? = null, actionUrl: String? = null) = AppNotification(
        id = "1",
        type = type,
        title = "Pedido pago",
        body = null,
        actionUrl = actionUrl,
        read = false,
        createdAt = null,
    )

    @Test
    fun `o caminho do painel web leva ao pedido`() {
        assertEquals(NotificationTarget.Order(12), NotificationRouter.fromLink("/orders/12"))
    }

    @Test
    fun `o deep link leva ao mesmo lugar que o caminho web`() {
        assertEquals(
            NotificationRouter.fromLink("/orders/12"),
            NotificationRouter.fromLink("pixstop://order/12"),
        )
    }

    @Test
    fun `o esquema muda por ambiente sem mudar o destino`() {
        // O app de homologação instala com `pixstop-staging://`; quem confere o
        // esquema é o filtro do manifesto, não o roteador.
        listOf("pixstop://", "pixstop-staging://", "pixstop-local://").forEach { scheme ->
            assertEquals(NotificationTarget.Order(12), NotificationRouter.fromLink("${scheme}order/12"))
        }
    }

    @Test
    fun `a url completa do site descarta o dominio`() {
        assertEquals(
            NotificationTarget.Order(12),
            NotificationRouter.fromLink("https://pixstop.com.br/orders/12"),
        )
    }

    @Test
    fun `query e ancora nao entram no caminho`() {
        assertEquals(NotificationTarget.Company, NotificationRouter.fromLink("/company/plan?utm=email"))
        assertEquals(NotificationTarget.Pixels, NotificationRouter.fromLink("/pixels#extrato"))
    }

    @Test
    fun `pedido sem numero para no historico`() {
        assertEquals(NotificationTarget.Orders, NotificationRouter.fromLink("/orders"))
        // Um id que não é número seria um caminho de outra coisa, não um pedido.
        assertEquals(NotificationTarget.Orders, NotificationRouter.fromLink("/orders/resumo"))
    }

    @Test
    fun `as paginas so da web nao viram alvo`() {
        // O retaguarda da plataforma e o resto do painel web não têm tela aqui;
        // abrir a caixa de avisos numa tela errada seria pior que não navegar.
        listOf("/admin/hardware", "/admin/subscriptions", "/admin/pixel-purchases", "/sandbox")
            .forEach { assertNull(NotificationRouter.fromLink(it), "não deveria haver alvo para $it") }
    }

    @Test
    fun `link vazio ou ausente nao vira alvo`() {
        listOf(null, "", "   ", "/").forEach { assertNull(NotificationRouter.fromLink(it)) }
    }

    @Test
    fun `o aviso prefere o link ao tipo`() {
        // O link é específico; o tipo só sabe dizer a categoria.
        assertEquals(
            NotificationTarget.Order(7),
            NotificationRouter.resolve(aviso(type = "order_paid", actionUrl = "/orders/7")),
        )
    }

    @Test
    fun `aviso antigo sem link ainda chega ao historico pelo tipo`() {
        assertEquals(NotificationTarget.Orders, NotificationRouter.resolve(aviso(type = "order_paid")))
        assertNull(NotificationRouter.resolve(aviso(type = "hardware_order_placed")))
        assertNull(NotificationRouter.resolve(aviso()))
    }

    @Test
    fun `o payload de push junta tipo e id`() {
        assertEquals(NotificationTarget.Order(9), NotificationRouter.fromPayload("order_paid", "9"))
        assertEquals(NotificationTarget.Orders, NotificationRouter.fromPayload("order_paid", null))
        // Um id que veio para um tipo que não é de pedido não inventa um pedido.
        assertEquals(NotificationTarget.Company, NotificationRouter.fromPayload("plan_changed", "9"))
        assertNull(NotificationRouter.fromPayload("desconhecido", "9"))
    }

    @Test
    fun `todo alvo diz por qual destino passa`() {
        assertEquals(Destination.Orders, NotificationTarget.Order(1).destination)
        assertEquals(Destination.Orders, NotificationTarget.Orders.destination)
        assertEquals(Destination.Pixels, NotificationTarget.Pixels.destination)
        assertEquals(Destination.Company, NotificationTarget.Company.destination)
        assertEquals(Destination.Notifications, NotificationTarget.Notifications.destination)
    }

    @Test
    fun `subir de nivel e virar a temporada levam a barra de XP mesmo com o link da carteira`() {
        val levelUp = NotificationRouter.resolve(aviso(type = "level_up", actionUrl = "/my-pixels"))
        val season = NotificationRouter.resolve(aviso(type = "season_closed", actionUrl = "/my-pixels"))

        assertEquals(NotificationTarget.Progress, levelUp)
        assertEquals(Destination.Progress, season?.destination)
        // O mesmo link, num aviso de pixels, continua na carteira.
        assertEquals(NotificationTarget.Pixels, NotificationRouter.resolve(aviso(type = "pixels_received", actionUrl = "/my-pixels")))
    }

    @Test
    fun `o link do convite com pixels leva ao convite em maiusculas`() {
        assertEquals(NotificationTarget.Invite("ABC123XY"), NotificationRouter.fromLink("/c/abc123xy"))
        assertEquals(NotificationTarget.Invite("ABC123XY"), NotificationRouter.fromLink("pixstop://c/ABC123XY"))
        assertEquals(Destination.Home, NotificationRouter.fromLink("/c/ABC123XY")?.destination)
        assertNull(NotificationRouter.fromLink("/c/"))
    }
}

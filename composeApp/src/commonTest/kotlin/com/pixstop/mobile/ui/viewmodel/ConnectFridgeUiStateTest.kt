package com.pixstop.mobile.ui.viewmodel

import com.pixstop.mobile.domain.model.FridgeDevice
import com.pixstop.mobile.domain.model.FridgePresence
import com.pixstop.mobile.domain.model.FridgeStock
import com.pixstop.mobile.domain.model.PortalMode
import com.pixstop.mobile.domain.model.PortalStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * O fluxo de conectar uma geladeira fala com dois lados que não se enxergam:
 * a geladeira, pela rede dela, e o servidor, pela internet. As transições
 * entre os passos são puras para poderem ser conferidas sem nenhum dos dois.
 */
class ConnectFridgeUiStateTest {

    private fun geladeira(
        id: Long = 7,
        presence: FridgePresence = FridgePresence.Pending,
        code: String? = "483921",
        rssi: Int? = null,
    ) = FridgeDevice(
        id = id,
        name = "Geladeira da copa",
        type = "gateway",
        presence = presence,
        activationCode = code,
        activationExpiresAt = null,
        lastSeenAt = null,
        signalStrength = rssi,
        firmwareVersion = null,
        doorOpen = null,
        applianceName = null,
    )

    @Test
    fun `so as geladeiras aguardando ativacao aparecem para conectar`() {
        val state = ConnectFridgeUiState(
            devices = listOf(geladeira(1), geladeira(2, FridgePresence.Online, code = null), geladeira(3, FridgePresence.Offline, code = null)),
            isLoading = false,
        )

        assertEquals(listOf(1L), state.pendingDevices.map { it.id })
    }

    @Test
    fun `o codigo aparece como se dita, em dois blocos`() {
        assertEquals("483 921", geladeira().formattedCode)
        assertNull(geladeira(code = null).formattedCode)
        assertFalse(geladeira(code = null).hasCode)
    }

    @Test
    fun `configurar exige rede e codigo, e nao pode estar ocupado`() {
        val base = ConnectFridgeUiState(chosen = geladeira(), step = ConnectStep.Network, isLoading = false)

        assertFalse(base.canConfigure)
        assertTrue(base.copy(ssid = "Rede-da-Empresa").canConfigure)
        assertFalse(base.copy(ssid = "Rede-da-Empresa", isWorking = true).canConfigure)
        assertFalse(base.copy(ssid = "Rede-da-Empresa", chosen = geladeira(code = null)).canConfigure)
    }

    @Test
    fun `o portal dizendo done avanca para esperar o servidor`() {
        val state = ConnectFridgeUiState(chosen = geladeira(), step = ConnectStep.Configuring, isWorking = true, isLoading = false)

        val depois = state.withPortalStatus(PortalStatus(PortalMode.Done, "Geladeira ativada.", "1.1.0"))

        assertEquals(ConnectStep.WaitingOnline, depois.step)
        assertEquals("Geladeira ativada.", depois.portalMessage)
        assertFalse(depois.isWorking)
        assertNull(depois.error)
    }

    @Test
    fun `o portal dizendo erro volta para a rede com a mensagem, sem reiniciar nada`() {
        val state = ConnectFridgeUiState(chosen = geladeira(), step = ConnectStep.Configuring, ssid = "Rede-da-Empresa", isWorking = true, isLoading = false)

        val depois = state.withPortalStatus(PortalStatus(PortalMode.Error, "Não consegui entrar na rede Rede-da-Empresa.", null))

        assertEquals(ConnectStep.Network, depois.step)
        assertEquals("Não consegui entrar na rede Rede-da-Empresa.", depois.error)
        // A rede e o código continuam: é só corrigir a senha.
        assertEquals("Rede-da-Empresa", depois.ssid)
        assertEquals("483921", depois.code)
        assertFalse(depois.isWorking)
    }

    @Test
    fun `conectando e provisionando so atualizam a mensagem`() {
        val state = ConnectFridgeUiState(chosen = geladeira(), step = ConnectStep.Configuring, isLoading = false)

        val conectando = state.withPortalStatus(PortalStatus(PortalMode.Connecting, "Entrando na rede…", null))
        val ativando = conectando.withPortalStatus(PortalStatus(PortalMode.Provisioning, "Ativando…", null))

        assertEquals(ConnectStep.Configuring, ativando.step)
        assertEquals("Ativando…", ativando.portalMessage)
    }

    @Test
    fun `setup fora do passo de configuracao nao mexe em nada`() {
        val state = ConnectFridgeUiState(chosen = geladeira(), step = ConnectStep.Code, isLoading = false)

        assertSame(state, state.withPortalStatus(PortalStatus(PortalMode.Setup, "Aguardando.", null)))
    }

    @Test
    fun `o servidor dizendo online fecha o fluxo e guarda o sinal`() {
        val state = ConnectFridgeUiState(devices = listOf(geladeira()), chosen = geladeira(), step = ConnectStep.WaitingOnline, isLoading = false)

        val depois = state.withPresence(geladeira(presence = FridgePresence.Online, code = null, rssi = -52))

        assertEquals(ConnectStep.Done, depois.step)
        assertEquals(-52, depois.chosen?.signalStrength)
        assertEquals(FridgePresence.Online, depois.devices.single().presence)
        assertEquals("Geladeira da copa está online.", depois.message)
    }

    @Test
    fun `online enquanto o codigo esta na tela tambem fecha, porque alguem configurou pelo navegador`() {
        val state = ConnectFridgeUiState(chosen = geladeira(), step = ConnectStep.Code, isLoading = false)

        assertEquals(ConnectStep.Done, state.withPresence(geladeira(presence = FridgePresence.Online, code = null)).step)
    }

    @Test
    fun `um sinal de outra geladeira nao fecha o fluxo desta`() {
        val state = ConnectFridgeUiState(devices = listOf(geladeira(7), geladeira(8)), chosen = geladeira(7), step = ConnectStep.WaitingOnline, isLoading = false)

        val depois = state.withPresence(geladeira(8, FridgePresence.Online, code = null))

        assertEquals(ConnectStep.WaitingOnline, depois.step)
        assertEquals(7L, depois.chosen?.id)
        assertEquals(FridgePresence.Online, depois.devices.first { it.id == 8L }.presence)
    }

    @Test
    fun `offline nao fecha o fluxo, so atualiza a lista`() {
        val state = ConnectFridgeUiState(devices = listOf(geladeira()), chosen = geladeira(), step = ConnectStep.WaitingOnline, isLoading = false)

        val depois = state.withPresence(geladeira(presence = FridgePresence.Offline, code = null))

        assertEquals(ConnectStep.WaitingOnline, depois.step)
        assertEquals(FridgePresence.Offline, depois.chosen?.presence)
    }

    @Test
    fun `criar aceita nome em branco, recusa nome curto e nao pode estar ocupado`() {
        val state = ConnectFridgeUiState(isLoading = false)

        // Em branco o servidor batiza de "Pixelstop 01"; duas letras é engano.
        assertTrue(state.canCreate)
        assertFalse(state.copy(newName = "Ge").canCreate)
        assertTrue(state.copy(newName = "Geladeira").canCreate)
        assertFalse(state.copy(newName = "Geladeira", isWorking = true).canCreate)
    }

    @Test
    fun `depois de conectar, a tela diz o que ainda falta para a vitrine encher`() {
        // Empresa sem produto nenhum: o estoque da geladeira nem é o assunto.
        assertEquals(
            "Falta cadastrar os produtos da empresa. Depois é só informar o estoque desta geladeira.",
            FridgeStock(applianceName = "Copa", inAppliance = 0, inCompany = 0).nextStep,
        )

        // Geladeira sem equipamento: não há onde o estoque morar.
        assertEquals(
            "Falta ligar esta geladeira a um equipamento no painel para ela receber estoque.",
            FridgeStock(applianceName = null, inAppliance = 0, inCompany = 8).nextStep,
        )

        assertEquals(
            "Os produtos já estão cadastrados. Falta informar o estoque de Copa para a vitrine encher.",
            FridgeStock(applianceName = "Copa", inAppliance = 0, inCompany = 8).nextStep,
        )

        assertEquals(
            "1 produto já está em Copa. Pedidos pagos abrem a trava, e toda abertura fica gravada.",
            FridgeStock(applianceName = "Copa", inAppliance = 1, inCompany = 8).nextStep,
        )

        assertEquals(
            "6 produtos já estão em Copa. Pedidos pagos abrem a trava, e toda abertura fica gravada.",
            FridgeStock(applianceName = "Copa", inAppliance = 6, inCompany = 8).nextStep,
        )
    }
}
